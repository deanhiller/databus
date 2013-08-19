package nosql;

import java.util.HashMap;
import java.util.Map;

import com.alvazan.orm.api.z8spi.CreateCfCallback;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;

public class OurCfMetaCallback implements CreateCfCallback {

	@Override
	public Object modifyColumnFamily(String cfName, Object def) {
		ColumnFamilyDefinition cfDef = (ColumnFamilyDefinition) def;
		
		if(cfName.startsWith("nreldata")) {
			cfDef.setCompactionStrategy("LeveledCompactionStrategy");
			Map<String, String> options = new HashMap<String, String>();
			options.put("sstable_size_in_mb", "50");
			cfDef.setCompactionStrategyOptions(options);
		}

		return cfDef;
	}

	@Override
	public Object configureKeySpace(String keyspaceName, Object def) {
		//With the default cassandra settings using SimpleSnitch
		//we cannot really do the below.  Users instead can do two things
		//1. change cassandra.yaml to PropertyFileSnitch from SimpleSnitch
		//2. run update keyspace databus5 with placement_strategy = 'org.apache.cassandra.locator.NetworkTopologyStrategy' and strategy_options = {DC1:3};
		//   in cassandra-cli 
		//then they can use the datacenter awareness stuff
//		KeyspaceDefinition keyDef = (KeyspaceDefinition) def;
//		keyDef.setStrategyClass("org.apache.cassandra.locator.NetworkTopologyStrategy");
//		Map<String, String> map = new HashMap<String, String>();
//		map.put("DC1", "3");
//		//map.put("strategy_options", "DC1:3");
//		keyDef.setStrategyOptions(map);
		return def;
	}

}
