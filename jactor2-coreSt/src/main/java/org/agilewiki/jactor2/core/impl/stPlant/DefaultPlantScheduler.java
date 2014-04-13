package org.agilewiki.jactor2.core.impl.stPlant;

import org.agilewiki.jactor2.core.plant.PlantScheduler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A scheduler for Plant, created by PlantConfiguration.
 */
public class DefaultPlantScheduler implements PlantScheduler {

    private volatile long currentTimeMillis;

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    /**
     * Creates the default plantScheduler.
     */
    public DefaultPlantScheduler() {
        scheduledThreadPoolExecutor =
                new ScheduledThreadPoolExecutor(getSchedulerPoolSize());
        currentTimeMillis = System.currentTimeMillis();
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                currentTimeMillis = System.currentTimeMillis();
            }
        }, getHeartbeatMillis(), getHeartbeatMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Controls how often currentTimeMillis is updated: every 500 milliseconds.
     * @return The number of milliseconds between updates to currentTimeMillis.
     */
    protected long getHeartbeatMillis() { return 500; }

    /**
     * Determines the size of the scheduledThreadPool: 2.
     *
     * @return Returns the number of threads in the scheduledThreadPool.
     */
    protected int getSchedulerPoolSize() { return 2; }

    @Override
    public long currentTimeMillis() { return currentTimeMillis; }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long _millisecondDelay) {
        return scheduledThreadPoolExecutor.schedule(runnable, _millisecondDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long _millisecondDelay) {
        return scheduledThreadPoolExecutor.scheduleAtFixedRate(runnable, _millisecondDelay,
                _millisecondDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        scheduledThreadPoolExecutor.shutdown();
    }
}
