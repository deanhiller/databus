/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nrel.modbusclient;

// Import the modbus4j protocol library
import gov.nrel.consumer.DatabusSender;
import gov.nrel.consumer.RejectedExecHandler;
import gov.nrel.consumer.beans.DatabusBean;
import gov.nrel.consumer.beans.Device;
import gov.nrel.consumer.beans.Stream;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.ip.IpParameters;

/**
 * Simple example how to configure a MODBUS/TCP protocol and read values.
 */
public class ModBusClient {

	private static final Logger log = LoggerFactory.getLogger(ModBusClient.class);
	static String SDI_HOST_URL; // = "http://sdi1.nrel.gov:8080/SDI";
	static String modName; // = "modRaw";
	static Integer threadPoolSize;
	static Integer numberOfIterations;
	static Integer iterationNumber;
	static Map<String, Meter> meterList;
	static Iterator<String> meterIt;
	static JSONObject meterMetadata;

	// databus variables
	static String GROUP_NAME;
	static String USERNAME;
	static String KEY;
	static String streamTable;
	static String deviceTable;
	static Integer pointsPerSend = 10;
	private static long timeBetweenPolls;

	private synchronized Meter getNextMeter() {

		if (meterIt.hasNext()) {

			String key = meterIt.next();

			if (key != null) {

				return meterList.get(key);
			}
		} else {
			// 0 means infinite loop
			if (iterationNumber++ < numberOfIterations
					|| numberOfIterations == 0) {
				log.info("Resetting the iterator");
				meterIt = meterList.keySet().iterator();
				return meterList.get(meterIt.next());
			}
		}
		return null;
	}

	/**
	 * Main method.
	 * 
	 * @throws IOException
	 * @throws SecurityException
	 */
	public static void main(String[] args) throws SecurityException,
			IOException {
		ThreadPoolExecutor threadPool = null;
		try {
			SLF4JBridgeHandler.install();
			
			log.info("Starting ModBusClient...");
			
			Properties properties = loadProperties(args[1]);
	
	//		FileInputStream configFile = new FileInputStream(
	//				"../conf/logging.properties");
	//		LogManager.getLogManager().readConfiguration(configFile);
			log.info("Starting ModBusClient2222...");
	
			SDI_HOST_URL = properties.getProperty("SDI_HOST_URL");
			String url = properties.getProperty("databus-url");
			
			boolean isSecure = false;
			int port = 80;
			if(url.startsWith("https")) {
				isSecure = true;
				port = 443;
			}
			
			int index = url.indexOf("://");
			String host = url.substring(index+3);
			if(host.contains(":")) {
				int index2 = host.indexOf(":");
				String portStr = host.substring(index2+1);
				port = Integer.parseInt(portStr);
				host = host.substring(0, index2);
			}
			
			modName = properties.getProperty("modName");
			threadPoolSize = Integer.parseInt(properties
					.getProperty("thread-pool-size"));
			numberOfIterations = Integer.parseInt(properties
					.getProperty("number-of-iterations"));
			int pollTime = Integer.parseInt(properties.getProperty("estimated-poll-time"));
			GROUP_NAME = properties.getProperty("database");
			USERNAME = properties.getProperty("user");
			KEY = properties.getProperty("key");
			deviceTable = properties.getProperty("deviceTable");
			streamTable = properties.getProperty("streamTable");
			
			meterList = new HashMap<String, Meter>();
	
			// initialize databus stuff
			LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(
					1000);
			RejectedExecutionHandler rejectedExec = new RejectedExecHandler();
			ExecutorService recorderSvc = new ThreadPoolExecutor(20, 20, 120,
					TimeUnit.SECONDS, queue, rejectedExec);
	
			log.info("username=" + USERNAME + "\nkey=" + KEY + "\nport=" + port);
			final DatabusSender sender = new DatabusSender(USERNAME, KEY,
					deviceTable, streamTable, recorderSvc, host, port, isSecure);

			meterMetadata = new JSONObject(new JSONTokener(
					new InputStreamReader(new FileInputStream(args[2]))));

			loadMeters(args[0], sender);
			writeMeters();

			threadPoolSize = Math.min(threadPoolSize, meterList.size());
			double devicesPerThread = ((double)meterList.size()) / threadPoolSize;
			timeBetweenPolls = (long) (pollTime / devicesPerThread);
			
			meterIt = meterList.keySet().iterator();

			iterationNumber = 0;

			threadPool = new ThreadPoolExecutor(threadPoolSize, threadPoolSize,
					10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(
							threadPoolSize));

			while (threadPool.getPoolSize() < threadPool.getMaximumPoolSize()) {
				threadPool.execute(new Runnable() {

					@Override
					public void run() {
						try {
							new ModBusClient().poll(sender);
						} catch (Exception e) {
							log.warn("Exception", e);
						}
					}
				});
			}

			log.info("done");
		} catch (Exception e) {
			log.warn( "EXCEPTION", e);

		} finally {

			if(threadPool != null) {
				while (!threadPool.isTerminated()) {
					try {
						threadPool.shutdown();
						Thread.sleep(1000);
					} catch (InterruptedException e) {
	
						log.warn( "EXCEPTION", e);
					}
				}
			}

			log.info("exiting");
		}
	}

	static void writeMeters() {
		try {
			JSONObject jObj = new JSONObject();

			Iterator<String> meterIt = meterList.keySet().iterator();
			while (meterIt.hasNext()) {
				String meter = meterIt.next();
				JSONObject meterObj = new JSONObject();
				Meter m = meterList.get(meter);

				meterObj.put("building", m.getBuilding());
				meterObj.put("ip", m.getIp());
				meterObj.put("model", m.getModel());
				meterObj.put("name", m.getName());
				meterObj.put("phenomena", m.getPhenomena());
				meterObj.put("serial", m.getSerial());
				meterObj.put("slave", m.getSlave());
				meterObj.put("zone", m.getZone());
				jObj.put(meter, meterObj);
			}

			FileWriter file = new FileWriter("./meter.json");
			file.write(jObj.toString());
			file.flush();
			file.close();

		} catch (Exception e) {
			log.warn( "EXCEPTION", e);
		}
	}

	/**
	 * Load meter data from CSV file assumes format Serial Number, Model,
	 * Description, Building, Phenomena, Zone, IP, Slave
	 * 
	 * @param filename
	 */
	private static void loadMeters(String strFile, DatabusSender sender) {
		try {

			// create BufferedReader to read csv file
			BufferedReader br = new BufferedReader(new FileReader(strFile));
			String strLine = "";
			StringTokenizer st = null;
			int lineNumber = 0, tokenNumber = 0;

			br.readLine();
			// read comma separated file line by line
			while ((strLine = br.readLine()) != null) {
				lineNumber++;

				// break comma separated line using ","
				String[] line = strLine.split(",");
				log.info("Processing line = " + strLine);

				if (line.length == 8 && line[7].startsWith("Slave")) {
					Meter newMeter = new Meter(line[0], line[1], line[2],
							line[3], line[4], line[5], line[6], line[7]);
					meterList.put(
							newMeter.getSerial() + ":" + newMeter.getName(),
							newMeter);
					registerWithDatabus(newMeter, sender);
				}
			}

		} catch (Exception e) {
			log.warn( "Exception while reading csv file: " + e, e);
		}
	}

	/**
	 * Load property values.
	 */
	private static Properties loadProperties(String fileName) {

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(fileName));
		} catch (IOException e) {

			log.warn( e.getMessage(), e);
		}

		return properties;
	}

	/**
	 * Poll method.
	 */
	public void poll(DatabusSender sender) throws Exception {
		List<DatabusBean> data = new ArrayList<DatabusBean>();

		while (true) {

			Meter meter = getNextMeter();

			if (meter == null) {

				log.info("no more meters to process, exiting thread "
						+ Thread.currentThread().getId());
				return;
			}

			ModbusFactory factory = new ModbusFactory();
			IpParameters params = new IpParameters();
			
			String[] split = meter.getIp().split("/");
			String host = meter.getIp();
			int port = 502;
			if(split.length == 2) {
				host = split[0];
				port = new Integer(split[1]);
			}

			params.setHost(host);
			params.setPort(port);
			ModbusMaster master = factory.createTcpMaster(params, false);

			log.info("pooling meter ID " + meter.getSerial());

			try {
				Long time = System.currentTimeMillis();
				if (meterMetadata.has(meter.getModel())) {
					master.init();
					JSONObject meterMeta = meterMetadata.getJSONObject(meter
							.getModel());
					Iterator<String> streamIt = meterMeta.keys();
					log.info("going to post for time="+time+" for meters="+meter.getModel());
					
					while (streamIt.hasNext()) {
						String stream = streamIt.next();
						processStream(sender, data, meter, master, time,
								meterMeta, stream);
					}
				}

				long dif = System.currentTimeMillis() - time;

				if (dif < timeBetweenPolls) {
					Thread.sleep(timeBetweenPolls - dif);
				}
			} finally {
				master.destroy();
			}
		}
	}

	private void processStream(DatabusSender sender, List<DatabusBean> data,
			Meter meter, ModbusMaster master, Long time, JSONObject meterMeta,
			String stream) throws JSONException {
		try {
			processStreamImpl(sender, data, meter, master, time, meterMeta,
				stream);
		} catch(Exception e) {
			log.warn( "Exception reading then posting stream...ignore and continue", e);
		}
	}

	private void processStreamImpl(DatabusSender sender,
			List<DatabusBean> data, Meter meter, ModbusMaster master,
			Long time, JSONObject meterMeta, String stream)
			throws JSONException {

		JSONObject streamMeta = meterMeta.getJSONObject(stream);
		String streamType = streamMeta.getString("type");
		JSONArray registerArray = streamMeta
				.getJSONArray("registers");

		Long value = 0l;

		List<Info> valuesAndMultipliers = new ArrayList<Info>();
		if (streamType.equals("direct")) {
			JSONObject reg = registerArray.getJSONObject(0);
			value = meter.read(master, reg);
			valuesAndMultipliers.add(new Info(value));
		} else {
			if (streamType.equals("multiply")) {
				value = 1l;
			}

			for (int i = 0; i < registerArray.length(); i++) {
				JSONObject reg = registerArray.getJSONObject(0);
				if (streamType.equals("add")) {
					value += meter.read(master, reg);
					valuesAndMultipliers.add(new Info("add", value));
				} else if (streamType.equals("multiply")) {
					value *= meter.read(master, reg);
					valuesAndMultipliers.add(new Info("multiplier", value));
				}
			}
		}

		/*sendDataToSDI(SDI_HOST_URL + "/" + modName,
				meter.getSerial() + "_" + stream,
				time.toString(), value.toString());*/

		DatabusBean point = databusPoint(meter.getSerial()
					+ stream, time, value.doubleValue(), valuesAndMultipliers);
		
		data.add(point);
		if (data.size() >= pointsPerSend) {
			log.info("posting data size="+data.size());
			try {
				sendToDatabus(sender, data);
			} finally {
				data.clear();
				log.info("11dataset size="+data.size());
			}
		}

		log.info("Going to post data: "
				+ meter.getModel() + "," + meter.getBuilding()
				+ " " + meter.getName() + "," + meter.getSerial()+","+ value);
	}

	public static void registerWithDatabus(Meter meter, DatabusSender sender) {
		
		try {
			registerWithDatabusImpl(meter, sender);
		} catch(Exception e) {
			log.warn("issue registering with Databus, ignoring.  meter="+meter);
		}
	}
	public static void registerWithDatabusImpl(Meter meter, DatabusSender sender) {

		log.info("Registering Device serial#=" + meter.getSerial()
				+ " if MeterMeta has " + meterMetadata.has(meter.getModel()));

		if (meterMetadata.has(meter.getModel())) {
			JSONObject meterMeta;
			meterMeta = getMeterMeta(meter);

			Device d = new Device();
			d.setAddress(meter.getIp());
			d.setBldg(meter.getBuilding());
			d.setDeviceDescription(meter.getName());
			d.setDeviceId(meter.getSerial());
			d.setEndUse(meter.getPhenomena());
			d.setOwner("NREL");
			d.setProtocol("ModBus");
			d.setSite(meter.getName().contains("NWTC") ? "NWTC" : "STM");

			log.info("Registering Device serial#=" + d.getDeviceId());

			Iterator<String> streamIt = meterMeta.keys();
			while (streamIt.hasNext()) {
				String stream = streamIt.next();
				log.info("Registering " +meter.getSerial()+""+ stream);

				Stream s = new Stream();
				s.setAggInterval("raw");
				s.setAggType("raw");
				s.setDevice(d.getDeviceId());
				s.setProcessed("raw");
				s.setStreamDescription(d.getDeviceDescription() + stream);
				s.setStreamId(d.getDeviceId() + stream);
				s.setStreamType("analogOutput");
				s.setTableName(d.getDeviceId() + stream);
				s.setVirtual("false");

				// find units from stream name
				String units = "NA";
				if (stream.contains("Factor")) {
					units = "ratio";
				} else if (stream.contains("Power")) {
					units = "kw";
				} else if (stream.contains("Energy")) {
					units = "kwH";
				}
				s.setUnits(units);

				sender.postNewStream(s, d, GROUP_NAME, stream);

				try {
					log.info("REGISTER STREAM " + s.getTableName());
				} catch (Exception e) {
					log.warn("Failed Registration", e);
				}
			}
		}
	}

	private static JSONObject getMeterMeta(Meter meter) {
		try {
			JSONObject meterMeta;
			meterMeta = meterMetadata.getJSONObject(meter.getModel());
			return meterMeta;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public DatabusBean databusPoint(String id, Long time, Double value, List<Info> valuesAndMultipliers) {
		DatabusBean point = new DatabusBean();
		point.setTableName(id);
		point.setTime(time);
		point.setValue(value);
		if(value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
			log.warn("value seems to be out of range="+value+" at time="+time+" table="+id+" registerstuff="+valuesAndMultipliers);
		}
		return point;
	}

	public void sendToDatabus(DatabusSender sender, List<DatabusBean> data) {
		sender.postData(data);
	}

	public void sendDataToSDI(String hostUrl, String id, String time,
			String value) {
		try {
			// Build JSON
			JSONObject jObj = new JSONObject();
			jObj.put(time, value);

			// Send data
			URL url = new URL(hostUrl + "/" + id);

			log.info(url + "");

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("PUT");
			conn.setReadTimeout(1000);
			OutputStreamWriter wr = new OutputStreamWriter(
					conn.getOutputStream());
			jObj.write(wr);
			wr.flush();

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));

			String line;
			while ((line = rd.readLine()) != null) {
				log.info(line);
			}

			wr.close();
			rd.close();

			// wr.close();
		} catch (Exception e) {
			log.warn( "EXCEPTION", e);
		}
	}
}
