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
		KeyspaceDefinition keyDef = (KeyspaceDefinition) def;
		keyDef.setStrategyClass("org.apache.cassandra.locator.NetworkTopologyStrategy");
		Map<String, String> map = new HashMap<String, String>();
		map.put("strategy_options", "{DC1:3}");
		keyDef.setStrategyOptions(map);
		return keyDef;
	}

}
