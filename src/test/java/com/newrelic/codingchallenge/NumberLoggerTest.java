package com.newrelic.codingchallenge;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

public class NumberLoggerTest {
    private ConcurrentLinkedQueue<Integer> logQueue;
    private AtomicBoolean terminationSignal;
    private Logger logger;
    private BufferedWriter writer;

    private NumberLogger numberLogger;

    @Before
    public void setup() {
        logQueue = new ConcurrentLinkedQueue<>();
        terminationSignal = new AtomicBoolean();
        logger = mock(Logger.class);
        writer = mock(BufferedWriter.class);

        numberLogger = new NumberLogger(writer, logQueue, terminationSignal, logger);
    }

    @Test(timeout = 1000)
    public void testItTerminatesTheLoggerWhenTheTerminateSignalIsSet() throws InterruptedException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> f = service.submit(numberLogger);

        terminationSignal.set(true);

        // this seems gross but I'm not really sure how to test it otherwise
        Thread.sleep(100);

        assertThat(f.isDone(), equalTo(true));
    }

    @Test
    public void testItWritesNumbersFromTheQueueToTheBuffer() throws IOException {
        logQueue.add(1);

        numberLogger.evaluateQueue();

        verify(writer).write("1");

        assertThat(logQueue.size(), equalTo(0));
    }
}
