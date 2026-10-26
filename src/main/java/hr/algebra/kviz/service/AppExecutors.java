package hr.algebra.kviz.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Zajednicki bazeni dretvi aplikacije. Sve dretve su daemon dretve kako ne bi
 * sprijecile gasenje aplikacije.
 */
public final class AppExecutors {

    private static final ExecutorService BACKGROUND =
            Executors.newCachedThreadPool(daemonFactory("kviz-background"));
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(2, daemonFactory("kviz-scheduler"));
    private static final ExecutorService SEQUENTIAL_IO =
            Executors.newSingleThreadExecutor(daemonFactory("kviz-replay-writer"));

    private AppExecutors() {
    }

    /** Za I/O poslove: datoteke, mreza, RMI pozivi. */
    public static ExecutorService background() {
        return BACKGROUND;
    }

    /** Za periodicke poslove: odbrojavanje vremena za odgovor. */
    public static ScheduledExecutorService scheduler() {
        return SCHEDULER;
    }

    /** Jedna dretva za zapise koji moraju ostati poredani (potezi u replay XML-u). */
    public static ExecutorService sequentialIo() {
        return SEQUENTIAL_IO;
    }

    public static ThreadFactory daemonFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public static void shutdown() {
        BACKGROUND.shutdownNow();
        SCHEDULER.shutdownNow();
        SEQUENTIAL_IO.shutdown();
    }
}
