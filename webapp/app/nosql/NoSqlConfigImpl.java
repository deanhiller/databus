package nosql;

import java.util.Map;

import com.alvazan.orm.api.base.Bootstrap;
import com.alvazan.play.NoSqlConfig;

public class NoSqlConfigImpl implements NoSqlConfig {

	@Override
	public void configure(Map<String, Object> props) {
		props.put(Bootstrap.CASSANDRA_CF_CREATE_CALLBACK, new OurCfMetaCallback());
	}

}
