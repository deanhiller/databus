package gov.nrel.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.netflix.astyanax.connectionpool.HostConnectionPool;
import com.netflix.astyanax.connectionpool.impl.AbstractLatencyScoreStrategyImpl;


public class PlayormSmaLatencyScore extends AbstractLatencyScoreStrategyImpl {
	public static final Logger log = LoggerFactory.getLogger(PlayormSmaLatencyScore.class);

private static final String NAME = "SMA";
    
    private final int    windowSize;

    public PlayormSmaLatencyScore(int updateInterval, int resetInterval, int windowSize, int blockedThreshold, double keepRatio, double scoreThreshold) {
        super(NAME, updateInterval, resetInterval, blockedThreshold, keepRatio, scoreThreshold);
        this.windowSize     = windowSize;
    }
    
    public PlayormSmaLatencyScore(int updateInterval, int resetInterval, int windowSize, double badnessThreshold) {
        this(updateInterval, resetInterval, windowSize, DEFAULT_BLOCKED_THREAD_THRESHOLD, DEFAULT_KEEP_RATIO, badnessThreshold);
    }

    public PlayormSmaLatencyScore() {
        super(NAME);
        this.windowSize = 20;
    }

    
    /**
     * Comparator used to sort hosts by score
     */
    private Comparator<HostConnectionPool<?>> scoreComparator = new Comparator<HostConnectionPool<?>>() {
        @Override
        public int compare(HostConnectionPool<?> p1, HostConnectionPool<?> p2) {
            double score1 = p1.getScore();
            double score2 = p2.getScore();
            if (score1 < score2) {
                return -1;
            }
            else if (score1 > score2) {
                return 1;
            }
            return 0;
        }
    };

    /**
     * Comparator used to sort hosts by number of buys + blocked operations
     */
    private Comparator<HostConnectionPool<?>> busyComparator = new Comparator<HostConnectionPool<?>>() {
        @Override
        public int compare(HostConnectionPool<?> p1, HostConnectionPool<?> p2) {
            return p1.getBusyConnectionCount() + p1.getBlockedThreadCount() - p2.getBusyConnectionCount() - p2.getBlockedThreadCount();
        }
    };
    
    @Override
    public <CL> List<HostConnectionPool<CL>> sortAndfilterPartition(List<HostConnectionPool<CL>> srcPools,
            AtomicBoolean prioritized) {
        List<HostConnectionPool<CL>> pools = Lists.newArrayList(srcPools);
        Collections.sort(pools, scoreComparator);
        prioritized.set(false);
        int poolSize = pools.size();
        int keep     = (int) Math.max(1, Math.ceil(poolSize * getKeepRatio()));

        // Step 1: Remove any host that is current reconnecting
        Iterator<HostConnectionPool<CL>> iter = pools.iterator();
        while (iter.hasNext()) {
            HostConnectionPool<CL> pool = iter.next();
            if (pool.isReconnecting()) {
            	if(log.isDebugEnabled())
    				log.debug("**** Removing host (reconnecting) : " + pool.toString());
                iter.remove();
            }
        }
        
        // Step 2: Filter out hosts that are too slow and keep at least the best keepRatio hosts
        if (pools.size() > keep) {
            Collections.sort(pools, busyComparator);
            HostConnectionPool<CL> poolFirst = pools.get(0);
            int firstBusy = poolFirst.getBusyConnectionCount() - poolFirst.getBlockedThreadCount();
            for (int i = pools.size() - 1; i >= keep; i--) {
                HostConnectionPool<CL> pool  = pools.get(i);
                int busy = pool.getBusyConnectionCount() + pool.getBlockedThreadCount();
                if ( (busy - firstBusy) > getBlockedThreshold()) {
                	if(log.isDebugEnabled())
        				log.debug("**** Removing host (blocked) : " + pool.toString());
                    pools.remove(i);
                }
            }
        }
        
        if (pools.size() > 0) {
            // Step 3: Filter out hosts that are too slow and keep at least the best keepRatio hosts
            int first = 0;
            for (; pools.get(0).getScore() == 0.0 && first < pools.size(); first++);
            
            if (first < pools.size()) {
                double scoreFirst = pools.get(first).getScore();
                if(log.isDebugEnabled())
    				log.debug("First : " + scoreFirst);
                if (scoreFirst > 0.0) {
                	TreeMap<Double, Integer> slowNodes = new TreeMap<Double, Integer>();
                    for (int i = 0; i < pools.size() ; i++) {
                        HostConnectionPool<CL> pool  = pools.get(i);
                        if(log.isDebugEnabled())
            				log.debug(i + " : " + pool.getScore() + " threshold:" + getScoreThreshold());
                        if ((pool.getScore() / scoreFirst) > getScoreThreshold()) {
                        	if(log.isDebugEnabled())
                				log.debug("**** Marking host for removal (score) : " + pool.toString()+", at location "+i + " : " + pool.getScore() + " threshold:" + getScoreThreshold());
                            slowNodes.put(pool.getScore(), i);
                        }
                    }
                    while (pools.size() > keep && !slowNodes.isEmpty()) {
                    	int index = slowNodes.lastEntry().getValue().intValue();
                    	if(log.isDebugEnabled())
            				log.debug("**** Removing host (score) : " + pools.get(index).toString()+", at location "+index + " : " + pools.get(index).getScore() + "ratio of " + (pools.get(index).getScore()/scoreFirst) +" threshold:" + getScoreThreshold());
                    	pools.remove(index);
                    	slowNodes.remove(slowNodes.lastKey());
                    }
                }
            }
        }
        // Step 4: Shuffle the hosts 
        Collections.shuffle(pools);
        
        return pools;
    }
    
    public final Instance newInstance() {
        return new Instance() {
            private final LinkedBlockingQueue<Long> latencies = new LinkedBlockingQueue<Long>(windowSize);
            private volatile Double cachedScore = 0.0d;
    
            @Override
            public void addSample(long sample) {
                if (!latencies.offer(sample)) {
                    try {
                        latencies.remove();
                    }
                    catch (NoSuchElementException e) {
                    }
                    latencies.offer(sample);
                }
            }
    
            @Override
            public double getScore() {
                return cachedScore;
            }
    
            @Override
            public void reset() {
                latencies.clear();
            }
    
            @Override
            public void update() {
                cachedScore = getMean();
            }
    
            private double getMean() {
                long sum = 0;
                int count = 0;
                for (long d : latencies) {
                    sum += d;
                    count++;
                }
                return (count > 0) ? sum / count : 0.0;
            }
        };
    }
}
