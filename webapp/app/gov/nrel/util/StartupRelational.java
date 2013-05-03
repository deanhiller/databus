package gov.nrel.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.EntityUser;
import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;

import com.alvazan.play.NoSql;

import controllers.ApiPostDataPointsImpl;
import controllers.ApiRegistrationImpl;

public class StartupRelational {

	public static final String RELATIONAL_NAME = "relationalForTests";
	public static final int GUID = 56;
	public static final String NAME = "deansPartyCube";
	public static final int NUM_ROOMS = 56;
	
	public static void createStuff() {
		createRelational(RELATIONAL_NAME);
		
		//create another
		
	}
	
	static void createRelational(String name) {
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.RELATIONAL_TABLE);
		msg.setSchema(StartupDetailed.GROUP1);
		msg.setModelName(name);
		
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		StartupUtils.createColumn(cols, "guid", "BigInteger", "oei:guid", true, true);
		StartupUtils.createColumn(cols, "name", "String", "oei:name", false, false);
		StartupUtils.createColumn(cols, "building", "String", "oei:bldg", false, false);
		StartupUtils.createColumn(cols, "numRooms", "BigInteger", "oei:number", false, false);
		msg.setColumns(cols);	

		EntityUser dean = StartupDetailed.DEAN;
		//register the time series table we will use
		ApiRegistrationImpl.registerImpl(msg, dean.getUsername(), dean.getApiKey());

		putData(name, GUID, NAME, "deansShack", NUM_ROOMS, false);
	}
	
	static void putData(String tableName, long guid, String name, String bldg, int numRooms, boolean isUpdate) {
		Map<String, Object> json = createDataPoint(tableName, guid, name, bldg,
				numRooms, isUpdate);
		EntityUser dean = StartupDetailed.DEAN;
		ApiPostDataPointsImpl.postDataImpl(null, json, dean.getUsername(), dean.getApiKey());
	}

	public static Map<String, Object> createDataPoint(String tableName,
			long guid, String name, String bldg, int numRooms, boolean isUpdate) {
		Map<String, Object> json = new HashMap<String, Object>();
		json.put("_tableName", tableName);
		json.put("guid", guid);
		json.put("name", name);
		json.put("building", bldg);
		json.put("numRooms", numRooms);
		if(isUpdate)
			json.put("_update", "true");
		return json;
	}
}
