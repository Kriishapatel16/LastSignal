package scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Starts and stops application-scoped background check-in jobs. */
public final class SchedulerService {
    private static final Logger LOGGER = Logger.getLogger(SchedulerService.class.getName());
    private ScheduledExecutorService executor;

    public synchronized void start() {
        if (executor != null && !executor.isShutdown()) return;

        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "lastsignal-scheduler-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, ex) ->
                    LOGGER.log(Level.SEVERE, "Uncaught scheduler error.", ex));
            return thread;
        };

        executor = Executors.newScheduledThreadPool(2, factory);
        executor.scheduleWithFixedDelay(new CheckInMonitorTask(), 10, 60, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(new EscalationTask(), 15, 30, TimeUnit.SECONDS);
        LOGGER.info("LastSignal background scheduler started.");
    }

    public synchronized void stop() {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        executor = null;
        LOGGER.info("LastSignal background scheduler stopped.");
    }
}
