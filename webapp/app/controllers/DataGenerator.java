package controllers;

import gov.nrel.util.Utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import models.RoleMapping;
import models.SecureResourceGroupXref;
import models.SecureTable;
import models.SecurityGroup;
import models.EntityUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.Response;
import play.mvc.Util;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.exc.ParseException;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z5api.IndexColumnInfo;
import com.alvazan.orm.api.z5api.IndexPoint;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.orm.api.z8spi.meta.ViewInfo;
import com.alvazan.play.NoSql;

import controllers.modules.util.ModuleStreamer;

public class DataGenerator extends Controller {

	public static final Logger log = LoggerFactory.getLogger(DataGenerator.class);
	
	public static void generateData(String table, Long start, Long end, Long frequency, Float highlimit, Float lowlimit) throws InterruptedException, ExecutionException {
		
		if(start > end)
			notFound("start time cannot be larger than end time");
		
		if (frequency == null || frequency == 0)
			frequency = (end - start)/5000;
		if ((highlimit == null ||highlimit == 0.0f) && (lowlimit==null ||lowlimit==0.0f)) {
			highlimit = 1000f;
			lowlimit = 0f;
		}
			
	    String param = request.params.get("callback");
	    
		//OKAY, at this point, we are ready to rock and roll
		
		ModuleStreamer.writeHeader(param);

	    FakeDataGenerator gen = new FakeDataGenerator(highlimit, lowlimit);
	    for(long time=start; time<=end; time+=frequency) {
	    	String chunk = "{\"time\":"+time+",\"value\":"+ gen.next() +"}";
	    	if (time+frequency <= end)
	    		chunk += ",";
			response.writeChunk(chunk);
	    }
	    
	    ModuleStreamer.writeFooter(param, null);

	}
		
	public static class FakeDataGenerator implements Iterator {
		private Float highLimit;
		private Float lowLimit;
		private Random r = new Random();
		private float lastval;
		
		public FakeDataGenerator(Float highLimit, Float lowLimit) {
			this.highLimit=highLimit;
			this.lowLimit=lowLimit;
			lastval = r.nextFloat() * (highLimit - lowLimit) + lowLimit;
		}
		
		@Override
		public boolean hasNext() {
			//Theres always more fake data...
			return true;
		}

		@Override
		public Object next() {
			lastval = r.nextFloat() * (highLimit - lowLimit) + lowLimit;
			return lastval;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
}
