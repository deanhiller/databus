package gov.nrel.util;

import java.util.ArrayList;
import java.util.List;

import models.SecureSchema;
import models.StreamAggregation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.Indexing;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.play.NoSql;

public class Upgrade7Bean extends UpgradeAbstract {

	private static final Logger log = LoggerFactory.getLogger(Upgrade7Bean.class);
	
	public Upgrade7Bean() {
		//leave as -1 until we are done
		super(-1);
	}

	@Override
	protected void upgradeImpl() {
		if (log.isInfoEnabled())
			log.info("running upgrade");
		NoSqlEntityManager em = NoSql.em();

		if (log.isInfoEnabled())
			log.info("running upgrade");
		
		// add upgrade methods here
		checkDbAggregations();
		em.flush();
		
	}
	
	private void checkDbAggregations() {
		Indexing.setForcedIndexing(true);
		
		List<SecureSchema> schemas = SecureSchema.findAll(NoSql.em());
		List<String> names = new ArrayList<String>();
		int counter = 0;
		for(SecureSchema s : schemas) {
			List<StreamAggregation> aggs = s.getAggregations();
			counter += aggs.size();
			if (log.isInfoEnabled())
				log.info("agggregation last count="+counter);
			for(StreamAggregation agg : aggs) {
				//NoSql.em().put(agg);
				//NoSql.em().flush();
			}
		}
		
		Indexing.setForcedIndexing(false);

		if (log.isInfoEnabled())
			log.info(" names aggregations="+names.size()+" names="+names);
		/**
		 * Its possible that some DBs (schemas) do not have aggregations listed correctly.  Somehow
		 * adding a new aggregation to a schema was missed in StreamAggregation.setSchema() along
		 * with SumStreamsMod.postAggregation() wasn't pushing the DB after adding the aggregation.
		 * 
		 * We need to go through every aggregation and make sure that it is listed in its owning
		 * DB.
		 */
		
		Cursor<KeyValue<StreamAggregation>> aggs = StreamAggregation.findAll(NoSql.em());

//		while(aggs.next()) {
//			KeyValue<StreamAggregation> current = aggs.getCurrent();
//			StreamAggregation value = current.getValue();
//			
//			SecureSchema schema = value.getSchema();
//			List<String> names = fetchNames(schema);
//			log.info("checking aggregation="+value.getName()+" owning schema="+schema.getName()+" child aggs="+names);
//
//			value.setSchema(schema);
//			NoSql.em().put(schema);
//			NoSql.em().put(value);
//			NoSql.em().flush();
//			SecureSchema newSchema = NoSql.em().find(SecureSchema.class, schema.getId());
//			log.info("new schema="+newSchema.getName()+" children="+fetchNames(newSchema));
//		}
	}

	private List<StreamAggregation> copy(List<StreamAggregation> aggs) {
		List<StreamAggregation> newAggs = new ArrayList<StreamAggregation>();
		for(StreamAggregation a : aggs) {
			newAggs.add(a);
		}
		return newAggs;
	}

	private List<String> fetchNames(SecureSchema schema) {
		List<StreamAggregation> aggregations = schema.getAggregations();
		List<String> names = new ArrayList<String>();
		for(StreamAggregation child : aggregations) {
			names.add(child.getName());
		}
		return names;
	}

}
