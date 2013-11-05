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
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.MyChartsGeneric;

import play.Play;
import play.mvc.Scope.Flash;
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
				String errorMsg = loadChart(vfile, chartData, lastModified);
				if(errorMsg != null) {
					Flash.current().error(errorMsg);
				}
			}
		}

		if(hasChanges) {
			sortedCharts = new ArrayList<ChartInfo>();
			sortedCharts.addAll(filenameToChart.values());
			Collections.sort(sortedCharts, new ChartComparator());
		}
	}
	
	private static String loadChart(VirtualFile vfile, String chartData, long lastModified) {
		String name = vfile.getName();
		log.info("loading chart="+name);
		
		int indexMeta = chartData.indexOf(META_META);
		int indexHead = chartData.indexOf(META_HEAD);
		int indexLarge = chartData.indexOf(META_LARGE);
		int indexSmall = chartData.indexOf(META_SMALL);
		if(indexMeta < 0) {
			String msg = "Faulty chart found missing section "+META_META+" so skipping chart load. filename="+name;
			log.warn(msg);
			return msg;
		} else if(indexHead < 0) {
			String msg = "Faulty chart found missing section "+META_HEAD+" so skipping chart load. filename="+name;
			log.warn(msg);
			return msg;			
		} else if(indexLarge < 0) {
			String msg = "Faulty chart found missing section "+META_LARGE+" so skipping chart load. fielname="+ name;
			log.warn(msg);
			return msg;
		}

		int chartVersion;
		String version = chartData.substring(META_VER.length(), indexMeta).trim();
		try {
			chartVersion = Integer.parseInt(version);
		} catch(NumberFormatException e) {
			String msg = "faulty chart with version string="+version+" could not be converted to an integer.  skipping chart load="+name;
			log.warn(msg);
			return msg;
		}
		
		String jsonHeader = chartData.substring(indexMeta+META_META.length(), indexHead);

		ChartMeta chartMeta;
		try {
			chartMeta = mapper.readValue(jsonHeader, ChartMeta.class);
		} catch (JsonParseException e) {
			String msg = "Could not parse json in "+META_META+" section of file="+name+"("+e.getMessage()+").  We are skipping the chart load of this file"; 
			log.warn(msg, e);
			return msg;
		} catch (JsonMappingException e) {
			String msg = "Could not parse json in "+META_META+" section of file="+name+"("+e.getMessage()+").  We are skipping the chart load of this file";
			log.warn(msg, e);
			return msg;
		} catch (IOException e) {
			String msg = "Could not parse json in "+META_META+" section of file="+name+"("+e.getMessage()+").  We are skipping the chart load of this file";
			log.warn(msg, e);
			return msg;
		}

		if(chartMeta.getPages().size() <= 0) {
			String msg = "There are no configured pages in section "+META_META+" of file="+name+".  We are skipping the chart load as we need to know what to ask the user"; 
			log.warn(msg);
			return msg;
		}
		
		ChartPageMeta page = chartMeta.getPages().get(0);
		boolean foundTitle = false;
		for(ChartVarMeta var : page.getVariables()) {
			if("url".equals(var.getNameInJavascript()))
				foundTitle = true;
		}
		if(!foundTitle) {
			String msg = "There is no url found on first page and variable with 'url' for javascriptInName is required for any chart(we use this later).  Chart="+name+" will not be loaded and will be skipped";
			log.warn(msg);
			return msg;
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
		return null;
	}

	private static boolean addBuiltInCharts() {
		if(filenameToChart != null)
			return false;
		filenameToChart = new HashMap<String, ChartInfo>();
		ChartInfo chartInfo = new ChartInfo(ChartInfo.BUILT_IN_CHART1, "/chartbasic/createchart", "3-Axis Builtin Chart", "A basic chart with 1 to 3 y axis and can draw up to 5 lines");
		filenameToChart.put(chartInfo.getId(), chartInfo);

		return true;
	}

	public static List<ChartInfo> fetchCharts() {
		reloadChartsIfNeeded();
		return sortedCharts;
	}

	public static String encodeVariables(Map<String, String> variablesMap) {
		String first;
		try {
			first = mapper.writeValueAsString(variablesMap);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	
		byte[] bytes = first.getBytes();
		String encoded = Base64.encodeBase64URLSafeString(bytes);
		return encoded;
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
		else if(encoded.startsWith("url="))
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
			if(key.equals("url") || key.equals("intervalUrl"))
				newVal = newVal.replace("\\/", "/");
			variables.put(key, newVal);
		}
		return variables;
	}
	
	public static String replaceVariables(String chart, Map<String, String> variables) {
		//look for special rangetype variable first
		modifyVariables(variables, true);
		
		//replace comments first so if there are variables in the comment, they will not be replaced since
		//they are removed from the file anyways.
		chart = chart.replaceAll("\\*\\{[\\w\\W]*?\\}\\*", "");
		
		for(String key : variables.keySet()) {
			String val = variables.get(key);
			chart = chart.replaceAll("\\$\\{"+key+"\\}", val);
		}

		return chart;
	}

	public static void modifyVariables(Map<String, String> variables, boolean webRequest) {
		String type = variables.get("_daterangetype");
		if(type != null) {
			String url = variables.get("url");
			String shortUrl = addCallbackParam(webRequest, url);
			variables.put("shortUrl", shortUrl);
			if("daterange".equals(type)) {
				String from = variables.get("_fromepoch");
				String to = variables.get("_toepoch");
				url += "?start="+from+"&end="+to;
			} else {
				String number = variables.get("_numberpoints");
				String ending = url.substring("/api".length());
				if(ending.contains("?"))
					url = "/api/firstvaluesV1/"+number+ending+"&reverse=true";
				else
					url = "/api/firstvaluesV1/"+number+ending+"?reverse=true";
			}
			
			url = addCallbackParam(webRequest, url);

			variables.put("url", url);
		}
	}

	private static String addCallbackParam(boolean webRequest, String url) {
		if(webRequest) {
			//add stuff for clean json callback
			if(url.contains("?"))
				url = url+"&callback=?";
			else
				url = url+"?callback=?";
		}
		return url;
	}
}
