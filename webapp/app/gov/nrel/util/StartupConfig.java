package gov.nrel.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.PlayPlugin;

import com.alvazan.orm.api.base.Bootstrap;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.z8spi.SpiConstants;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.AstyanaxContext.Builder;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolType;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.connectionpool.impl.EmaLatencyScoreStrategyImpl;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ConsistencyLevel;

public class StartupConfig extends PlayPlugin {

	public static final Logger log = LoggerFactory.getLogger(StartupConfig.class);
	public static final String CASSANDRA_BUILDER = SpiConstants.CASSANDRA_BUILDER;
	public static final String CASSANDRA_CLUSTERNAME = "nosql.cassandra.clusterName";
	public static final String CASSANDRA_KEYSPACE = "nosql.cassandra.keyspace";
	public static final String CASSANDRA_SEEDS = "nosql.cassandra.seeds";
	
	@Override
	public void onApplicationStart() {
		if (log.isInfoEnabled())
			log.info("Overriding playorm configuration for SDI.  Using StartupConfig to configure cassandra settings");
		List<Class> classes = Play.classloader.getAnnotatedClasses(NoSqlEntity.class);
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(Bootstrap.LIST_OF_EXTRA_CLASSES_TO_SCAN_KEY, classes);
        properties.put(Bootstrap.AUTO_CREATE_KEY, "create");
        
        for(java.util.Map.Entry<Object, Object> entry : Play.configuration.entrySet()) {
        	properties.put((String) entry.getKey(), entry.getValue());
        }

		if("cassandra".equals((String) properties.get(Bootstrap.TYPE))) {
			String clusterName = (String) properties.get(CASSANDRA_CLUSTERNAME);
			String keyspace = (String) properties.get(CASSANDRA_KEYSPACE);
			String seeds = (String) properties.get(CASSANDRA_SEEDS);
			if(clusterName == null || keyspace == null || seeds == null)
				throw new IllegalArgumentException("Must supply the nosql.cassandra.* properties.  Read Bootstrap.java for values");
			
			Builder builder = new AstyanaxContext.Builder()
		    .forCluster(clusterName)
		    .forKeyspace(keyspace)
		    .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()      
		        .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE)
		        .setConnectionPoolType(ConnectionPoolType.TOKEN_AWARE)
		    )
		    .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
		        .setMaxConnsPerHost(20)
		        .setInitConnsPerHost(2)
		        .setSeeds(seeds)
		        .setConnectTimeout(10000)
		        .setLatencyAwareUpdateInterval(10000)  // Will resort hosts per token partition every 10 seconds
		        .setLatencyAwareResetInterval(10000) // Will clear the latency every 10 seconds
		        .setLatencyAwareBadnessThreshold(0.50f) // Will sort hosts if a host is more than 100% slower than the best and always assign connections to the fastest host, otherwise will use round robin
		        .setLatencyAwareWindowSize(100) // Uses last 100 latency samples
		        .setLatencyScoreStrategy(new PlayormEmaLatencyScore(10)) // Enabled SMA.  Omit this to use round robin with a token range
	
		    )
		    .withConnectionPoolMonitor(new CountingConnectionPoolMonitor());
			
			
			if(seeds.contains(",")) {
				//for a multi-node cluster, we want the test suite using quorum on writes and
				//reads so we have no issues...
				AstyanaxConfigurationImpl config = new AstyanaxConfigurationImpl();
				config.setDefaultWriteConsistencyLevel(ConsistencyLevel.CL_QUORUM);
				config.setDefaultReadConsistencyLevel(ConsistencyLevel.CL_LOCAL_QUORUM);
				config.setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE);
		        config.setConnectionPoolType(ConnectionPoolType.TOKEN_AWARE);
				builder = builder.withAstyanaxConfiguration(config);
			}
			Play.configuration.put(Bootstrap.CASSANDRA_BUILDER, builder);
			properties.put(Bootstrap.CASSANDRA_BUILDER, builder);
		}
	}
}
