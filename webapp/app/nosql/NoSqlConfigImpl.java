package nosql;

import java.util.Map;

import play.Play;

import com.alvazan.orm.api.base.Bootstrap;
import com.alvazan.play.NoSqlConfig;

public class NoSqlConfigImpl implements NoSqlConfig {

	@Override
	public void configure(Map<String, Object> props) {
		String portStr = (String) Play.configuration.get("nosql.cassandra.thriftport");
		props.put(Bootstrap.CASSANDRA_THRIFT_PORT, portStr);
		props.put(Bootstrap.CASSANDRA_CF_CREATE_CALLBACK, new OurCfMetaCallback());
	}

}
