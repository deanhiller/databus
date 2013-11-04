package controllers.gui;

import gov.nrel.util.Utility;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import models.ChartDbo;
import models.EntityUser;
import models.message.ChartMeta;
import models.message.ChartPageMeta;
import models.message.ChartVarMeta;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.play.NoSql;

import controllers.gui.auth.GuiSecure;
import controllers.gui.util.Chart;
import controllers.gui.util.ChartComparator;
import controllers.gui.util.ChartInfo;
import controllers.gui.util.ChartUtil;
import controllers.modules2.framework.ModuleController;
import controllers.modules2.framework.RawProcessorFactory;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;

import play.Play;
import play.mvc.Controller;
import play.mvc.Scope.Params;
import play.mvc.With;
import play.mvc.results.BadRequest;
import play.mvc.results.NotFound;
import play.templates.JavaExtensions;
import play.vfs.VirtualFile;

@With(GuiSecure.class)
public class MyChartsGeneric extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyChartsGeneric.class);
	public static final DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMMM dd, yyyy HH:mm:ss");
	private static ChartUtil chartUtil = new ChartUtil();
	
	public static void selectChart() {
		List<ChartInfo> charts = chartUtil.fetchCharts();
		String encoded = "start";
		render(charts, encoded);
	}

	public static void modifyChart(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		String chartId = variables.get("__chartId");
		List<ChartInfo> charts = chartUtil.fetchCharts();
		render("@selectChart", charts, chartId, encoded);
	}

	public static void postSelectedChart(String encoded, String chartId) {
		//setup the defaults on the post as we post the selected chart
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		ChartInfo chart = ChartUtil.fetchChart(chartId);
		variables.put("__chartId", chartId);
		
		if(chart.isBuiltinChart()) {
			encoded = ChartUtil.encodeVariables(variables);
			MyCharts.createStep1(encoded);
		}
		
		ChartMeta meta = chart.getChartMeta();

		for(ChartPageMeta pMeta : meta.getPages()) {
			for(ChartVarMeta var : pMeta.getVariables()) {
				String currentVal = variables.get(var.getNameInJavascript());
				String value = var.getDefaultValue();
				if(value != null && currentVal == null) {
					variables.put(var.getNameInJavascript(), value);
				}
			}
		}
		encoded = ChartUtil.encodeVariables(variables);
		
		chartVariables(0, encoded);
	}

	private static ChartInfo fetchChart(Map<String, String> variables) {
		String chartId = variables.get("__chartId");
		ChartInfo chart = ChartUtil.fetchChart(chartId);
		if(chart == null)
			throw new NotFound("chart="+chartId+" was not found in our Charts directory");
		return chart;
	}

	public static void chartVariables(int page, String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		ChartInfo chart = fetchChart(variables);
		if(page < 0 || page >= chart.getChartMeta().getPages().size())
			notFound("this page is not found");
		ChartMeta meta = chart.getChartMeta();
		ChartPageMeta pageMeta = meta.getPages().get(page);
		List<VarWrapper> variableList = new ArrayList<VarWrapper>();
		if(!"start".equals(encoded)) {
			fillInVariables(pageMeta, variables, variableList);
		}

		List<String> columnNames = new ArrayList<String>();
		if(page > 0 && needsColumns(pageMeta)) {
			String url = variables.get("url");
			RawProcessorFactory factory = ModuleController.fetchFactory();
			RawProcessorFactory.threadLocal.set("passthroughV1");
			PullProcessor top = (PullProcessor) factory.get();
			VisitorInfo visitor = new VisitorInfo(null, null, false, NoSql.em());
			String path = url.substring("/api/".length());
			PullProcessor root = (PullProcessor) top.createPipeline(path, visitor, null, true);
			ReadResult result = root.read();
			if(result.isEndOfStream()) {
				flash.error("This stream cannot be used at this point as no data comes out(either raw data tables don't have it or a module causes no data to come out)");
				chartVariables(page, encoded);
			}
				
			TSRelational row = result.getRow();
			for(Entry<String, Object> entry : row.entrySet()) {
				columnNames.add(entry.getKey());
			}
		}

		String subtitle = "("+(page+1) +" of "+chart.getChartMeta().getPages().size()+")";
		boolean isLastPage = false;
		if(page == chart.getChartMeta().getPages().size()-1)
			isLastPage = true;
		render(chart, variableList, page, encoded, isLastPage, subtitle, columnNames);
	}

	private static boolean needsColumns(ChartPageMeta pageMeta) {
		List<ChartVarMeta> variables = pageMeta.getVariables();
		for(ChartVarMeta meta : variables) {
			if(meta.isColumnSelector())
				return true;
		}
		return false;
	}

	private static void fillInVariables(ChartPageMeta pageMeta,
			Map<String, String> variables, List<VarWrapper> variableList) {
		for(ChartVarMeta var : pageMeta.getVariables()) {
			VarWrapper varWrapper;
			if(var.isTimePanel()) {
				TimePanel p = new TimePanel(var);
				p.fillWrapper(variables);
				varWrapper = p;
			} else {
				String value = variables.get(var.getNameInJavascript());
				varWrapper = new VarWrapper(var, value);
			}
			variableList.add(varWrapper);
		}
	}

	public static void postVariables(int page, String encoded) {
		Map<String, String> variablesMap = ChartUtil.decodeVariables(encoded);
		ChartInfo chart = fetchChart(variablesMap);

		Map<String, String[]> paramMap = params.all();
		for(String key : paramMap.keySet()) {
			if(key.startsWith("chart.")) {
				String[] values = paramMap.get(key);
				String value = values[0];
				String javascriptKey = key.substring("chart.".length());
				
				ChartVarMeta meta = findMeta(chart, javascriptKey);
				if(meta == null)
					continue;

				if(meta.isDateTime()) {
					//we need to convert here to offset since epoch before storing the stuff
					value = maybeConvert(javascriptKey, paramMap, meta);
				} else {
					if(StringUtils.isEmpty(value) && meta.isRequired()) {
						validation.addError(javascriptKey, "This field is required");
					}
				}
				
				if(javascriptKey.endsWith("url"))
					value = stripOffHost(value);
				variablesMap.put(javascriptKey, value);
			} else if(key.equals("rangeType")) {
				String rangeTypeValue = paramMap.get(key)[0];
				putVariablesInMap(paramMap, variablesMap, rangeTypeValue, chart);
			}
		}

		if(page == 0) {
			String url = variablesMap.get("url");
			RawProcessorFactory factory = ModuleController.fetchFactory();
			RawProcessorFactory.threadLocal.set("passthroughV1");
			PullProcessor top = (PullProcessor) factory.get();
			VisitorInfo visitor = new VisitorInfo(null, null, false, NoSql.em());
			if(!StringUtils.isEmpty(url)) {
				String path = url.substring("/api/".length());
				try {
					PullProcessor root = (PullProcessor) top.createPipeline(path, visitor, null, true);
					ReadResult result = root.read();
					if(result.isEndOfStream()) {
						params.flash();
						validation.keep();
						flash.error("This stream cannot be used at this point as no data comes out(either raw data tables don't have it or a module causes no data to come out)");
						chartVariables(page, encoded);
					}
				} catch(Exception e) {
					params.flash();
					validation.keep();
					log.info("Invalid url from user="+url, e);
					flash.error("This stream url is invalid, "+e.getMessage());
					chartVariables(page, encoded);
				}
			}
		}
		
		String url = variablesMap.get("url");
		encoded = ChartUtil.encodeVariables(variablesMap);
		if(validation.hasErrors()) {
			params.flash();
			validation.keep();
			flash.error("Your form has errors below");
			chartVariables(page, encoded);
		} else if(page == 0 && url == null) {
			params.flash();
			validation.keep();
			flash.error("Script seems corrupt.  We should have url variable by the second page to use");
			chartVariables(page, encoded);
		}
		
		page = page+1;
		if(page >= chart.getChartMeta().getPages().size()) {
			encoded = ChartUtil.encodeVariables(variablesMap);
			drawChart(encoded);
		} else
			chartVariables(page, encoded);
	}

	private static void putVariablesInMap(Map<String, String[]> paramMap,
			Map<String, String> variablesMap, String rangeTypeValue, ChartInfo chart) {
		String name = paramMap.get("javascriptName")[0];
		ChartVarMeta varMeta = findMeta(chart, name);
		
		variablesMap.put("_daterangetype", rangeTypeValue);
		if("daterange".equals(rangeTypeValue)) {
			String fromEpoch = maybeConvert("from", paramMap, varMeta);
			String toEpoch = maybeConvert("to", paramMap, varMeta);
			variablesMap.put("_fromepoch", fromEpoch);
			variablesMap.put("_toepoch", toEpoch);
		} else {
			String numberPoints = paramMap.get("chart.numpoints")[0];
			if(StringUtils.isEmpty(numberPoints)) {
				validation.addError("numpoints", "This field is required");
			}

			variablesMap.put("_numberpoints", numberPoints);
		}
	}

	private static String maybeConvert(String javascriptKey, Map<String, String[]> paramMap, ChartVarMeta meta) {
		String selection = paramMap.get("chart."+javascriptKey+".type")[0];
		if("zerobased".equals(selection)) {
			String currentValue = paramMap.get("chart."+javascriptKey)[0];
			if(StringUtils.isEmpty(currentValue) && meta.isRequired()) {
				validation.addError(javascriptKey, "This field is required");
			}
			return currentValue;
		} else {
			String date = paramMap.get("chart."+javascriptKey+".date")[0];
			String time = paramMap.get("chart."+javascriptKey+".time")[0];
			if(StringUtils.isEmpty(date) && meta.isRequired()) {
				validation.addError(javascriptKey, "This field is required");
			}
			if(StringUtils.isEmpty(time) && meta.isRequired()) {
				validation.addError(javascriptKey, "This field is required");
			}
			if(validation.hasErrors()) {
				return null;
			}

			String dateTime = date+" "+time;
			LocalDateTime dt = fmt.parseLocalDateTime(dateTime);
			//DateTime dt = fmt.parseDateTime(dateTime);
			DateTime dtWithTimeZone = dt.toDateTime();
			long millis = dtWithTimeZone.getMillis();
			return ""+millis;
		}
	}

	private static ChartVarMeta findMeta(ChartInfo chart, String javascriptKey) {
		ChartMeta meta = chart.getChartMeta();
		List<ChartPageMeta> pages = meta.getPages();
		for(ChartPageMeta m : pages) {
			for(ChartVarMeta varMeta : m.getVariables()) {
				String nameInJavascript = varMeta.getNameInJavascript();
				if(nameInJavascript == null) {
					throw new RuntimeException("This chart is missing 'nameInJavascript' for Chart meta data, variable type="+varMeta.getType());
				}
				if(nameInJavascript.equals(javascriptKey))
					return varMeta;
			}
		}
		return null;
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

	public static void drawChart(String encoded) {
		EntityUser user = Utility.getCurrentUser(session);
		List<ChartDbo> charts = user.getCharts();
		
		//special case for showing remote pages
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		ChartInfo info = fetchChart(variables);
		
		//setup the correct urls and such...
		ChartUtil.modifyVariables(variables, false);
		
		ChartDbo chart = new ChartDbo();
		chart.setChartId(info.getId());
		chart.setEncodedVariables(encoded);
		String title = chart.getTitle();
		String url = chart.getLargeChartUrl();
		render(charts, variables, url, title, encoded);
	}

	public static void largeChart(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		ChartInfo info = fetchChart(variables);
		String c = info.getLargeChart();
		String chart = ChartUtil.replaceVariables(c, variables);
		render("@chartonly", info, chart, encoded);
	}

	public static void smallChart(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		ChartInfo info = fetchChart(variables);
		String c = info.getSmallChart();
		String chart = ChartUtil.replaceVariables(c, variables);
		render("@chartonly", info, chart, encoded);
	}

	public static void addChartToDashboard(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		ChartInfo info = fetchChart(variables);
		ChartDbo chart = new ChartDbo();
		chart.setChartId(info.getId());
		chart.setEncodedVariables(encoded);
		//he could create multiples of the same chart so just timestamp it as he would not
		//be fast enough to create ones with an id clash...
		chart.setId(info.getId()+System.currentTimeMillis());

		EntityUser user = Utility.getCurrentUser(session);
		List<ChartDbo> charts = user.getCharts();
		charts.add(chart);
		NoSql.em().put(user);
		NoSql.em().flush();
		
		Settings.charts();
	}
}
