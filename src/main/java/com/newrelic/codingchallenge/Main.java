package com.newrelic.codingchallenge;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Main {
    public static String LOG_FILE_PATH = "./numbers.log";

    public static void main(final String[] args) throws Exception {
        // create the queues and atomic counters necessary for all of this to connect
        // - a concurrent queue for the readers
        // - a concurrent queue for the logger
        // - atomic integers for the reporter
        // - a concurrent set for the list of unique numbers
        final ConcurrentLinkedQueue<Integer> readQueue = new ConcurrentLinkedQueue<Integer>();
        final ConcurrentLinkedQueue<Integer> logQueue = new ConcurrentLinkedQueue<Integer>();
        final AtomicInteger uniques = new AtomicInteger();
        final AtomicInteger duplicates = new AtomicInteger();
        final AtomicBoolean terminationSignal = new AtomicBoolean();
        final Set<Integer> seenNumbers = ConcurrentHashMap.newKeySet();
        final Logger logger = Logger.getGlobal();

        // create the log file and writeable buffer
        final BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH));

        // start up a thread pool for the number readers
        // - if the number wasn't in the set, it increments the "new" atomic
        // counter and sends the number to be written to the log
        // - if the number was in the set, it increments the "dup" atomic counter
        logger.info("Starting reader thread");

        ExecutorService readerService = Executors.newSingleThreadExecutor();
        readerService.execute(
                new NumberReader(readQueue, logQueue, uniques, duplicates, seenNumbers, terminationSignal, logger));

        // start up a thread pool for the logger
        logger.info("Starting logger thread");

        ExecutorService loggerService = Executors.newSingleThreadExecutor();
        loggerService.execute(new NumberLogger(writer, logQueue, terminationSignal, logger));

        // start up a scheduled thread pool for the reporter
        // - every 10 seconds, the reporter reads out the value of the counters and
        // writes them to stdout, then resets the counters
        logger.info("Starting reporter thread");

        ScheduledExecutorService reporterService = Executors.newScheduledThreadPool(1);
        reporterService.scheduleAtFixedRate(new NumberReporter(uniques, duplicates, seenNumbers, logger), 10, 10,
                TimeUnit.SECONDS);

        ExecutorService listenerService = Executors.newFixedThreadPool(5);
        ServerSocket server;
        List<NumberListener> listeners = new ArrayList<>();

        // this is here and not in a try-with-resources so the dumb gatekeeper
        // service can close it later
        server = new ServerSocket(4000);

        // DEBUG ONLY - start up a thread pool that reports backpressure on the queues
        // logger.info("Starting backpressure thread");

        // ScheduledExecutorService backpressureService =
        // Executors.newScheduledThreadPool(1);

        // backpressureService.scheduleAtFixedRate(() -> {
        // logger.info(String.format("STATUS: Active threads: %d; Read Queue: %d; Log
        // Queue: %d",
        // ((ThreadPoolExecutor) listenerService).getActiveCount(), readQueue.size(),
        // logQueue.size()));
        // }, 1, 1, TimeUnit.SECONDS);

        // This seems like a horrible hack but I haven't been able to find a better way
        // to do it, either in my head or on the internet. Both the accept call on the
        // server and the read calls in the listener block, so they don't see the
        // termination signal change until another connection is attempted or the client
        // disconnects, respectively.
        //
        // Since I haven't found a cleaner way to prevent that blocking, I'll just check
        // the status of the terminate signal every second and if it's set, kill
        // everything manually.
        //
        // A day later: maybe something like a Guava service manager could make this
        // easier?
        ScheduledExecutorService gatekeeperService = Executors.newScheduledThreadPool(1);

        gatekeeperService.scheduleAtFixedRate(() -> {
            if (terminationSignal.get()) {
                logger.info("Shutting down");

                for (NumberListener listener : listeners) {
                    listener.shutdown();
                }

                listenerService.shutdownNow();
                readerService.shutdownNow();
                loggerService.shutdownNow();
                reporterService.shutdownNow();
                gatekeeperService.shutdownNow();
                // backpressureService.shutdownNow();

                try {
                    server.close();
                    writer.close();
                } catch (Exception ex) {
                }
            }
        }, 1, 1, TimeUnit.SECONDS);

        // start up a server with 5 listeners
        logger.info("Starting server on port 4000");

        while (!terminationSignal.get()) {
            try {
                NumberListener numberListener = new NumberListener(server.accept(), readQueue, terminationSignal,
                        logger);
                listeners.add(numberListener);
                listenerService.execute(numberListener);
            } catch (Exception ex) {
                logger.info(String.format("Exception while connecting: %s", ex.getMessage()));
            }
        }
    }
}
