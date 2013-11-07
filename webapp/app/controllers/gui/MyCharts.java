package controllers.gui;

import gov.nrel.util.Utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

import models.ChartDbo;
import models.EntityUser;
import models.ScriptChart;
import models.message.ChartMeta;
import models.message.ChartPageMeta;
import models.message.ChartVarMeta;
import models.message.RegisterMessage;
import models.message.StreamEditor;
import models.message.StreamModule;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.play.NoSql;

import play.Play;
import play.mvc.Controller;
import play.mvc.Scope.Flash;
import play.mvc.With;
import play.templates.JavaExtensions;
import play.vfs.VirtualFile;
import controllers.Parsing;
import controllers.gui.auth.GuiSecure;
import controllers.gui.util.Chart;
import controllers.gui.util.ChartInfo;
import controllers.gui.util.ChartSeries;
import controllers.gui.util.ChartUtil;
import controllers.gui.util.Info;
import controllers.modules2.RawProcessor;
import controllers.modules2.framework.ModuleController;
import controllers.modules2.framework.RawProcessorFactory;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.PullProcessor;
import de.undercouch.bson4jackson.BsonFactory;

@With(GuiSecure.class)
public class MyCharts extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyCharts.class);
	private static final ObjectMapper mapper = new ObjectMapper();

	public static void createStep1(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		Chart chart = deserializeChart(variables);
		String url = variables.get("url");
		String title = variables.get("title");
		if(chart == null)
			chart = new Chart();
		if(chart.getTitle() == null)
			chart.setTitle(title);
		render("@createChart", variables, encoded, chart, url);
	}
	
	public static void modifyRedirect(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		Chart chart = deserializeChart(variables);
		variables.put("title", chart.getTitle());
		encoded = ChartUtil.encodeVariables(variables);
		MyChartsGeneric.modifyChart(encoded);
	}

	public static Chart deserializeChart(Map<String, String> variables) {
		String encoding = variables.get("__chart");
		if(encoding == null)
			return null;
		Chart chart = deserialize(encoding);
		return chart;
	}

	public static void postStep1(String encoded, Chart chart, String url) throws UnsupportedEncodingException {
		if(chart.getTitle() == null || "".equals(chart.getTitle().trim()))
			validation.addError("chart.title", "This is a required field");

		if("".equals(chart.getAxis1().getName()))
			validation.addError("chart.axis1.name", "Name is a required field");

		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		StreamEditor editor = DataStreamUtil.decode(variables);
		StreamModule stream = editor.getStream();
		chart.fillInAxisColors();
		encoded = createUrl(variables, chart, url, 1);

		createStep2(encoded);
	}
	
	public static void createStep2(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		Chart chart = deserializeChart(variables);
		
		if(StringUtils.isEmpty(chart.getTimeColumn())) {
			String timeCol = variables.get("timeColumn");
			chart.setTimeColumn(timeCol);
		}
		if(StringUtils.isEmpty(chart.getSeries1().getName())) {
			String valCol = variables.get("valueColumn");
			chart.getSeries1().setName(valCol);
		}
		
		StreamEditor editor = DataStreamUtil.decode(variables);
		List<String> columnNames = fetchColumnNames(editor);
		
		chart.initNames();

		List<String> columnNamesOnly = new ArrayList<String>();
		columnNamesOnly.addAll(columnNames);
		columnNames.add("{NONE}");
		
		render(encoded, chart, columnNames, columnNamesOnly);
	}

	private static List<String> fetchColumnNames(StreamEditor editor) {
		List<String> colNames = new ArrayList<String>();
		StreamModule mod = editor.getStream();
		fetchNames(mod.getStreams().get(0), colNames);
		colNames.add("time");
		return colNames;
	}

	private static void fetchNames(StreamModule mod, List<String> colNames) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> procs = factory.fetchPullProcessors();
		MetaInformation meta = procs.get(mod.getModule()).getGuiMeta();
		
		if(meta.isTerminal()) {
			Map<String, String> params = mod.getParams();
			String oneCol = params.get(RawProcessor.COL_NAME);
			colNames.add(oneCol);
		}
		
		for(StreamModule child : mod.getStreams()) {
			fetchNames(child, colNames);
		}
	}

	public static void postStep2(String encoded, Chart chart) throws UnsupportedEncodingException {
		chart.fillIn();
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		encoded = createUrl(variables, chart, null, 2);
		createStep3(encoded);
	}
	
	public static void createStep3(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		Chart chart = deserializeChart(variables);
		ChartVarMeta meta = new ChartVarMeta();
		meta.setRequired(true);
		meta.setHelp("The times to retrieve data for");
		TimePanel v = new TimePanel(meta );
		v.fillWrapper(variables);
		
		render(encoded, chart, v);
	}

	public static void postStep3(String encoded, Chart chart) throws UnsupportedEncodingException {
		chart.fillIn();
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		Map<String, String[]> paramMap = params.all();
		for(String key : paramMap.keySet()) {
			if(key.equals("rangeType")) {
				String rangeTypeValue = paramMap.get(key)[0];
				putVariablesInMap(paramMap, variables, rangeTypeValue);
			}
		}
		
		encoded = createUrl(variables, chart, null, 2);
		drawChart(encoded);
	}
	
	private static void putVariablesInMap(Map<String, String[]> paramMap,
			Map<String, String> variablesMap, String rangeTypeValue) {
		variablesMap.put("_daterangetype", rangeTypeValue);
		if("daterange".equals(rangeTypeValue)) {
			String fromEpoch = maybeConvert("from", paramMap);
			String toEpoch = maybeConvert("to", paramMap);
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

	private static String maybeConvert(String javascriptKey, Map<String, String[]> paramMap) {
		String selection = paramMap.get("chart."+javascriptKey+".type")[0];
		if("zerobased".equals(selection)) {
			String currentValue = paramMap.get("chart."+javascriptKey)[0];
			if(StringUtils.isEmpty(currentValue)) {
				validation.addError(javascriptKey, "This field is required");
			}
			return currentValue;
		} else {
			String date = paramMap.get("chart."+javascriptKey+".date")[0];
			String time = paramMap.get("chart."+javascriptKey+".time")[0];
			if(StringUtils.isEmpty(date)) {
				validation.addError(javascriptKey, "This field is required");
			}
			if(StringUtils.isEmpty(time)) {
				validation.addError(javascriptKey, "This field is required");
			}
			if(validation.hasErrors()) {
				return null;
			}

			String dateTime = date+" "+time;
			LocalDateTime dt = MyChartsGeneric.fmt.parseLocalDateTime(dateTime);
			//DateTime dt = fmt.parseDateTime(dateTime);
			DateTime dtWithTimeZone = dt.toDateTime();
			long millis = dtWithTimeZone.getMillis();
			return ""+millis;
		}
	}
	
	public static void drawChart(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		Chart chart = deserializeChart(variables);
		//modifies the url to be correct with start time endtime or first values type url
		ChartUtil.modifyVariables(variables, true);
		String url = variables.get("url");
		int height = 0;
		
		render(encoded, chart, height, url);
	}
	
	public static void drawJustChart(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		Chart chart = deserializeChart(variables);
		int height = 0;
		String heightStr = params.get("height");
		if(heightStr != null) {
			String heightEsc = JavaExtensions.escapeJavaScript(heightStr);
			height = Integer.parseInt(heightEsc);
		}
		//modifies the url to be correct with start time endtime or first values type url
		ChartUtil.modifyVariables(variables, true);
		String url = variables.get("url");
		render(chart, height, encoded, url);
	}
	
	private static String createUrl(Map<String, String> variables, Chart chart, String url, int stepNumber) {
		String encoding;
		try {
			encoding = mapper.writeValueAsString(chart);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		byte[] bytes = encoding.getBytes();
		encoding = Base64.encodeBase64URLSafeString(bytes);

		if(log.isInfoEnabled())
			log.info("length="+encoding.length()+" result="+encoding);
		
		variables.put("__chart", encoding);
		if(url != null)
			variables.put("url", url);
		String encoded = ChartUtil.encodeVariables(variables );
		
		if(validation.hasErrors()) {
			flash.error("You have errors in your form below");
			validation.keep();
			flash.keep();
			if(stepNumber == 1)
				createStep1(encoded);
			else
				createStep2(encoded);
		}
		
		return encoded;
	}

	private static Long convertLong(String str) {
		try {
			return Long.parseLong(str);
		} catch(NumberFormatException e) {
			return null;
		}
	}

	private static Chart deserialize(String encodedChart) {
		try {
			byte[] decoded = Base64.decodeBase64(encodedChart);
			String json = new String(decoded);
			return mapper.readValue(json, Chart.class);
		} catch (JsonParseException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void addChartToDashboard(String encoded) {
		Map<String, String> variables = ChartUtil.decodeVariables(encoded);
		Chart info = deserializeChart(variables);
		
		ChartDbo chart = new ChartDbo();
		chart.setChartId(ChartInfo.BUILT_IN_CHART1);
		chart.setEncodedVariablesRaw(encoded);
		chart.setBuiltin(true);
		//he could create multiples of the same chart so just timestamp it as he would not
		//be fast enough to create ones with an id clash...
		chart.setId(ChartInfo.BUILT_IN_CHART1+System.currentTimeMillis());
		chart.setTitle(info.getTitle());
		EntityUser user = Utility.getCurrentUser(session);
		List<ChartDbo> charts = user.getCharts();
		charts.add(chart);
		NoSql.em().put(user);
		NoSql.em().flush();
		
		Settings.charts();
	}
}
