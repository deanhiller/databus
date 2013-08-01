package controllers.gui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.message.ChartMeta;
import models.message.ChartPageMeta;
import models.message.ChartVarMeta;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.auth.GuiSecure;

import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import play.templates.JavaExtensions;
import play.vfs.VirtualFile;

@With(GuiSecure.class)
public class MyChartsGeneric extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyChartsGeneric.class);

	private static Map<String, ChartInfo> filenameToChart;
	private static List<ChartInfo> sortedCharts;
	private static String META_VER = "--metasource-version--";
	private static String META_META = "--metasource-meta--";
	private static String META_HEAD = "--metasource-header--";
	private static String META_LARGE = "--metasource-largechart--";
	private static String META_SMALL = "--metasource-smallchart--";
	private static final ObjectMapper mapper = new ObjectMapper();
	
	static {
		reloadChartsIfNeeded();
	}

	private static void reloadChartsIfNeeded() {
		boolean hasChanges = addBuiltInCharts();
		VirtualFile highchartsDir = Play.getVirtualFile("/public/Charts");
		
		List<VirtualFile> highCharts = highchartsDir.list();
		for(VirtualFile vfile : highCharts) {
			if(vfile.isDirectory()) {
				log.info("skipping directory="+vfile.getName());
				continue; //skip directories
			}
			String chartData = vfile.contentAsString();
			if(!chartData.startsWith(META_VER)) {
				log.info("Chart not loaded since --metasource-version-- is not the first line/characters in the file.  file="+vfile.getName());
				continue;
			}
			
			String name = vfile.getName();
			long lastModified = vfile.getRealFile().lastModified();
			ChartInfo chartInfo = filenameToChart.get(name);
			if(chartInfo == null
					|| lastModified > chartInfo.getLastModified()) {
				hasChanges = true;
				loadChart(vfile, chartData, lastModified);
			}
		}

		if(hasChanges) {
			sortedCharts = new ArrayList<ChartInfo>();
			sortedCharts.addAll(filenameToChart.values());
			Collections.sort(sortedCharts, new ChartComparator());
		}
	}
	
	private static void loadChart(VirtualFile vfile, String chartData, long lastModified) {
		String name = vfile.getName();
		log.info("loading chart="+name);
		
		int indexMeta = chartData.indexOf(META_META);
		int indexHead = chartData.indexOf(META_HEAD);
		int indexLarge = chartData.indexOf(META_LARGE);
		int indexSmall = chartData.indexOf(META_SMALL);
		if(indexMeta < 0) {
			log.warn("Faulty chart found missing section "+META_META+" so skipping chart load. filename="+name);
			return;
		} else if(indexHead < 0) {
			log.warn("Faulty chart found missing section "+META_HEAD+" so skipping chart load. filename="+name);
			return;			
		} else if(indexLarge < 0) {
			log.warn("Faulty chart found missing section "+META_LARGE+" so skipping chart load. fielname="+ name);
			return;
		}

		int chartVersion;
		String version = chartData.substring(META_VER.length(), indexMeta).trim();
		try {
			chartVersion = Integer.parseInt(version);
		} catch(NumberFormatException e) {
			log.warn("faulty chart with version string="+version+" could not be converted to an integer.  skipping chart load="+name);
			return;
		}
		
		String jsonHeader = chartData.substring(indexMeta+META_META.length(), indexHead);

		ChartMeta chartMeta;
		try {
			chartMeta = mapper.readValue(jsonHeader, ChartMeta.class);
		} catch (JsonParseException e) {
			log.warn("Could not parse json in "+META_META+" section of file="+name+"("+e.getMessage()+").  We are skipping the chart load of this file", e);
			return;
		} catch (JsonMappingException e) {
			log.warn("Could not parse json in "+META_META+" section of file="+name+"("+e.getMessage()+").  We are skipping the chart load of this file", e);
			return;
		} catch (IOException e) {
			log.warn("Could not parse json in "+META_META+" section of file="+name+"("+e.getMessage()+").  We are skipping the chart load of this file", e);
			return;
		}

		String htmlHeaders = chartData.substring(indexHead+META_HEAD.length(), indexLarge);
		
		String largeChart;
		String smallChart;
		if(indexSmall > 0) {
			largeChart = chartData.substring(indexLarge+META_LARGE.length(), indexSmall);
			smallChart = chartData.substring(indexSmall+META_SMALL.length());
		} else {
			largeChart = chartData.substring(indexLarge+META_LARGE.length());
			smallChart = null;
		}
		
		ChartInfo info = new ChartInfo();
		info.setAndConvertId(name);
		info.setChartVersion(chartVersion);
		info.setChartMeta(chartMeta);
		info.setHtmlHeaders(htmlHeaders);
		info.setLargeChart(largeChart);
		info.setSmallChart(smallChart);
		info.setLastModified(lastModified);
		
		filenameToChart.put(info.getId(), info);
	}

	private static boolean addBuiltInCharts() {
		if(filenameToChart != null)
			return false;
		filenameToChart = new HashMap<String, ChartInfo>();
		ChartInfo chartInfo = new ChartInfo("/chartbasic/createchart", "3-Axis Builtin Chart", "A basic chart with 1 to 3 y axis and can draw up to 5 lines");
		filenameToChart.put(chartInfo.getId(), chartInfo);

		return true;
	}

	public static void selectChart() {
		reloadChartsIfNeeded();
		List<ChartInfo> charts = sortedCharts;
		render(charts);
	}
	
	public static void postSelectedChart(String chartId) {
		ChartInfo chart = fetchChart(chartId);
		
		if(chart.isBuiltinChart()) {
			redirect(chart.getRoute());
		}
		String encoded = "start";
		chartVariables(chartId, 0, encoded);
	}

	public static void chartVariables(String chartId, int page, String encoded) {
		ChartInfo chart = fetchChart(chartId);
		if(page < 0 || page >= chart.getChartMeta().getPages().size())
			notFound("this page is not found");
		ChartPageMeta pageMeta = chart.getChartMeta().getPages().get(page);
		if(!"start".equals(encoded)) {
			Map<String, String> variables = decodeVariables(encoded);
			for(ChartVarMeta var : pageMeta.getVariables()) {
				String value = variables.get(var.getNameInJavascript());
				var.setValue(value);
			}
		}

		String subtitle = "("+(page+1) +" of "+chart.getChartMeta().getPages().size()+")";
		boolean isLastPage = false;
		if(page == chart.getChartMeta().getPages().size()-1)
			isLastPage = true;
		render(chart, pageMeta, chartId, page, encoded, isLastPage, subtitle);
	}

	private static ChartInfo fetchChart(String chartId) {
		reloadChartsIfNeeded();
		ChartInfo chart = filenameToChart.get(chartId);
		if(chart == null)
			notFound("chart="+chartId+" was not found in our Charts directory");
		return chart;
	}

	public static void postVariables(String chartId, int page, String encoded) {
		ChartInfo chart = fetchChart(chartId);

		Map<String, String[]> paramMap = params.all();
		Map<String, String> variablesMap = decodeVariables(encoded);
		for(String key : paramMap.keySet()) {
			if(key.startsWith("chart.")) {
				String[] values = paramMap.get(key);
				String value = values[0];
				String javascriptKey = key.substring("chart.".length());
				if(javascriptKey.equals("url"))
					value = stripOffHost(value);
				variablesMap.put(javascriptKey, value);
			}
		}

		try {
			encoded = mapper.writeValueAsString(variablesMap);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		byte[] bytes = encoded.getBytes();
		encoded = Base64.encodeBase64URLSafeString(bytes);
		
		page = page+1;
		if(page >= chart.getChartMeta().getPages().size())
			drawChart(chartId, encoded);
		else
			chartVariables(chartId, page, encoded);
	}

	private static String stripOffHost(String value) {
		if(!value.startsWith("http"))
			return value;
		int index = value.indexOf("//");
		String val = value.substring(index+2);
		int nextIndex = val.indexOf("/");
		String relativeUrl = val.substring(nextIndex);
		return relativeUrl;
	}

	public static void drawChart(String chartId, String encoded) {
		ChartInfo info = fetchChart(chartId);
		Map<String, String> variables = decodeVariables(encoded);

		String largeChart = info.getLargeChart();
		String chart = replaceVariables(largeChart, variables);
		render(info, chart, variables, chartId, encoded);
	}

	public static void largeChart(String chartId, String encoded) {
		ChartInfo info = fetchChart(chartId);
		Map<String, String> variables = decodeVariables(encoded);

		String largeChart = info.getLargeChart();
		String chart = replaceVariables(largeChart, variables);
		render("@chartonly", info, chart, variables, chartId, encoded);
	}

	public static void smallChart(String chartId, String encoded) {
		ChartInfo info = fetchChart(chartId);
		Map<String, String> variables = decodeVariables(encoded);

		String largeChart = info.getSmallChart();
		String chart = replaceVariables(largeChart, variables);
		render("@chartonly", info, chart, variables, chartId, encoded);
	}

	private static Map<String, String> decodeVariables(String encoded) {
		if("start".equals(encoded))
			return new HashMap<String, String>();
		byte[] decoded = Base64.decodeBase64(encoded);
		String json = new String(decoded);
		Map<String, String> variables;
		try {
			variables = mapper.readValue(json, Map.class);
		} catch (JsonParseException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for(String key : variables.keySet()) {
			String value = variables.get(key);
			String newVal = JavaExtensions.escapeJavaScript(value);
			if(key.equals("url"))
				newVal = newVal.replace("\\/", "/");
			variables.put(key, newVal);
		}
		return variables;
	}

	private static String replaceVariables(String largeChart, Map<String, String> variables) {
		//replace comments first so if there are variables in the comment, they will not be replaced since
		//they are removed from the file anyways.
		largeChart = largeChart.replaceAll("\\*\\{[\\w\\W]*?\\}\\*", "");
		
		for(String key : variables.keySet()) {
			String val = variables.get(key);
			largeChart = largeChart.replaceAll("\\$\\{"+key+"\\}", val);
		}

		return largeChart;
	}
}
