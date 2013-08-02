package controllers.gui.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.message.ChartMeta;
import models.message.ChartPageMeta;
import models.message.ChartVarMeta;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.mvc.results.NotFound;
import play.templates.JavaExtensions;
import play.vfs.VirtualFile;

public class ChartUtil {
	private static final Logger log = LoggerFactory.getLogger(ChartUtil.class);
	
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

		if(chartMeta.getPages().size() <= 0) {
			log.warn("There are no configured pages in section "+META_META+" of file="+name+".  We are skipping the chart load as we need to know what to ask the user");
			return;
		}
		
		ChartPageMeta page = chartMeta.getPages().get(0);
		boolean foundTitle = false;
		for(ChartVarMeta var : page.getVariables()) {
			if("title".equals(var.getNameInJavascript()))
				foundTitle = true;
		}
		if(!foundTitle) {
			log.warn("There is no title found on first page and variable with 'title' for javascriptInName is required for any chart title(we use this later).  Chart="+name+" will not be loaded and will be skipped");
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

	public List<ChartInfo> fetchCharts() {
		reloadChartsIfNeeded();
		return sortedCharts;
	}

	public static ChartInfo fetchChart(String chartId) {
		ChartInfo chart = fetchChartImpl(chartId);
		if(chart == null)
			throw new NotFound("chart="+chartId+" was not found in our Charts directory");
		return chart;
	}
	
	private static ChartInfo fetchChartImpl(String chartId) {
		reloadChartsIfNeeded();
		ChartInfo chart = filenameToChart.get(chartId);
		return chart;
	}
	
	public static Map<String, String> decodeVariables(String encoded) {
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
	
	public static String replaceVariables(String chart, Map<String, String> variables) {
		//replace comments first so if there are variables in the comment, they will not be replaced since
		//they are removed from the file anyways.
		chart = chart.replaceAll("\\*\\{[\\w\\W]*?\\}\\*", "");
		
		for(String key : variables.keySet()) {
			String val = variables.get(key);
			chart = chart.replaceAll("\\$\\{"+key+"\\}", val);
		}

		return chart;
	}
}
