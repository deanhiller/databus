package nosql;

import java.util.HashMap;
import java.util.Map;

import com.alvazan.orm.api.z8spi.CreateCfCallback;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;

public class OurCfMetaCallback implements CreateCfCallback {

	@Override
	public Object modifyColumnFamily(String cfName, Object def) {
		ColumnFamilyDefinition cfDef = (ColumnFamilyDefinition) def;
		
		if(cfName.startsWith("nreldata")) {
			cfDef.setCompactionStrategy("LeveledCompactionStrategy");
			Map<String, String> options = new HashMap<String, String>();
			options.put("sstable_size_in_mb", "10");
			cfDef.setCompactionStrategyOptions(options);
		}

		return cfDef;
	}

}
