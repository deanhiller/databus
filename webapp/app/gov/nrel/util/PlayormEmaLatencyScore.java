package gov.nrel.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.netflix.astyanax.connectionpool.HostConnectionPool;
import com.netflix.astyanax.connectionpool.impl.AbstractLatencyScoreStrategyImpl;

/**
 * Calculate latency as an exponential moving average.
 * 
 * @author elandau
 */
public class PlayormEmaLatencyScore extends AbstractLatencyScoreStrategyImpl {
	public static final Logger log = LoggerFactory.getLogger(PlayormEmaLatencyScore.class);
    private final static String NAME = "EMA";
    
    private final int    windowSize;
    
    
    
    public PlayormEmaLatencyScore(int updateInterval, int resetInterval, int windowSize, int blockedThreshold, double keepRatio, double scoreThreshold) {
        super(NAME, updateInterval, resetInterval, blockedThreshold, keepRatio, scoreThreshold);
        if(log.isDebugEnabled())
			log.debug("Constructing PlayormEmaLatencyScore to override astyanax default");
        this.windowSize = windowSize;
    }
    
    public PlayormEmaLatencyScore(int updateInterval, int resetInterval, int windowSize) {
    	super(NAME, updateInterval, resetInterval);
    	if(log.isDebugEnabled())
			log.debug("Constructing PlayormEmaLatencyScore to override astyanax default");
        this.windowSize = windowSize;
    }
    
    public PlayormEmaLatencyScore(int windowSize) {
        super(NAME);
        if(log.isDebugEnabled())
			log.debug("Constructing PlayormEmaLatencyScore to override astyanax default");
        this.windowSize = windowSize;
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
    	if(log.isDebugEnabled())
			log.debug("in sortAndFilterPartition in PlayormEmaLatencyScore");
        List<HostConnectionPool<CL>> pools = Lists.newArrayList(srcPools);
        Collections.sort(pools, scoreComparator);
        prioritized.set(false);
        int poolSize = pools.size();
        int keep     = (int) Math.max(1, Math.ceil(poolSize * getKeepRatio()));

        // Step 1: Remove any host that is current reconnecting
        if(log.isDebugEnabled())
			log.debug("about to remove any hosts that are currently reconnecting.  There are "+pools.size()+" hosts");
        Iterator<HostConnectionPool<CL>> iter = pools.iterator();
        while (iter.hasNext()) {
            HostConnectionPool<CL> pool = iter.next();
            if (pool.isReconnecting()) {
//            	if(log.isDebugEnabled())
//                log.debug("**** Removing host (reconnecting) : " + pool.toString());
                iter.remove();
            }
        }
        if(log.isDebugEnabled())
			log.debug("removed reconnecting hosts, there are "+pools.size()+" hosts left.  Now removing hosts that are to slow by busyComparator.");
        
        // Step 2: Filter out hosts that are too slow and keep at least the best keepRatio hosts
        if (pools.size() > keep) {
            Collections.sort(pools, busyComparator);
            HostConnectionPool<CL> poolFirst = pools.get(0);
            int firstBusy = poolFirst.getBusyConnectionCount() - poolFirst.getBlockedThreadCount();
            for (int i = pools.size() - 1; i >= keep; i--) {
                HostConnectionPool<CL> pool  = pools.get(i);
                int busy = pool.getBusyConnectionCount() + pool.getBlockedThreadCount();
                if ( (busy - firstBusy) > getBlockedThreshold()) {
//                	if(log.isDebugEnabled())
//                    log.debug("**** Removing host (blocked) : " + pool.toString());
                    pools.remove(i);
                }
            }
        }
        if(log.isDebugEnabled())
			log.debug("removed slow by busyComparator, there are now "+pools.size()+" hosts left.  Now removing hosts that are slow by score.");
        
        // Step 3: Filter out hosts that are too slow and keep at least the best keepRatio hosts
        Collections.sort(pools, scoreComparator);
        int first = 0;
        String logmsg = "pool scores are ";
        if (log.isDebugEnabled()) {
	        for (int i=0; i < pools.size(); i++)
	        	logmsg += "pools("+i+")="+pools.get(i).getScore()+", ";
	        log.debug(logmsg);
        }
        for (; pools.get(0).getScore() == 0.0 && first < pools.size(); first++);
        if(log.isDebugEnabled())
			log.debug("first is "+first+" pools.size() is "+pools.size());
        if (first < pools.size()) {
            double scoreFirst = pools.get(first).getScore();
            if(log.isDebugEnabled()) {
				log.debug("First : " + scoreFirst);
				log.debug("--- listing pools ---");
				for(int i=0; i<pools.size(); i++) {
					log.debug("--- pool: "+i+" score:"+pools.get(i).getScore()+", ratio:"+(pools.get(i).getScore()/scoreFirst)+", host: "+pools.get(i));
				}
            }
            if (scoreFirst > 0.0) {
                for (int i = pools.size() - 1; i >= keep && i > first; i--) {
                    HostConnectionPool<CL> pool  = pools.get(i);
                    //if(log.isDebugEnabled())
        				//log.debug(i + " : " + pool.getScore() + " threshold:" + getScoreThreshold());
                    if ((pool.getScore() / scoreFirst) > getScoreThreshold()) {
                    	if(log.isDebugEnabled())
            				log.debug("**** removing host (score) : " + pool.toString()+", at location "+i + " : " + pool.getScore());
                        pools.remove(i);
                    }
                    else {
                        break;
                    }
                }
            }
        }
        if(log.isDebugEnabled())
			log.debug("Finally, after all removals there are "+pools.size()+" hosts left.");
        
        // Step 4: Shuffle the hosts 
        Collections.shuffle(pools);
        
        return pools;
    }
    
    @Override
    public final Instance newInstance() {
        return new Instance() {
            private final LinkedBlockingQueue<Long> latencies = new LinkedBlockingQueue<Long>(windowSize);
            private volatile double cachedScore = 0.0d;
            private int    numSamples=0;
    
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
                cachedScore = 0.0;
                latencies.clear();
            }
    
            /**
             * Drain all the samples and update the cached score
             */
            @Override
            public void update() {
                Double ema = cachedScore;
                ArrayList<Long> samples = Lists.newArrayList();
                latencies.drainTo(samples);
                if (samples.size() == 0) {
                    //samples.add(0L);
                	samples.add(Math.round(cachedScore*.9));
                }                    
                
                if (ema == 0.0) {
                    ema = (double)samples.remove(0);
                }
                
                for (Long sample : samples) {
                	if (numSamples < windowSize)
                		numSamples++;
                	if (numSamples == 1)
                		ema = (double)sample;
                	else
                		ema = ((ema*(numSamples-1))+sample)/numSamples;
                }
                cachedScore = ema;
            }
        };
    }
}
