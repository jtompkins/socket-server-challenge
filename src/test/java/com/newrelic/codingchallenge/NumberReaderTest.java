package com.newrelic.codingchallenge;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

public class NumberReaderTest {
    private ConcurrentLinkedQueue<Integer> readQueue;
    private ConcurrentLinkedQueue<Integer> logQueue;
    private AtomicInteger uniques;
    private AtomicInteger duplicates;
    private Set<Integer> seenNumbers;
    private AtomicBoolean terminationSignal;
    private Logger logger;

    private NumberReader reader;

    @Before
    public void setup() {
        readQueue = new ConcurrentLinkedQueue<>();
        logQueue = new ConcurrentLinkedQueue<>();
        uniques = new AtomicInteger();
        duplicates = new AtomicInteger();
        seenNumbers = new HashSet<Integer>();
        terminationSignal = new AtomicBoolean();
        logger = null;

        reader = new NumberReader(readQueue, logQueue, uniques, duplicates, seenNumbers, terminationSignal, logger);
    }

    @Test(timeout = 1000)
    public void testItTerminatesTheRunnerWhenTheTerminateSignalIsSet() throws InterruptedException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> f = service.submit(reader);

        terminationSignal.set(true);

        // this seems gross but I'm not really sure how to test it otherwise
        Thread.sleep(100);

        assertThat(f.isDone(), equalTo(true));
    }

    @Test
    public void testItReadsAUniqueNumberFromTheQueueAndUpdatesUniqueAtomics() throws InterruptedException {
        readQueue.add(1);

        reader.evaluateQueue();

        assertThat("seenNumbers contains the number", seenNumbers.contains(1), equalTo(true));
        assertThat("uniques has been incremented", uniques.get(), equalTo(1));
        assertThat("duplicates has not been incremented", duplicates.get(), equalTo(0));
    }

    @Test
    public void testItReadsAUniqueNumberFromTheQueueAndSendsItToBeLoggeed() throws InterruptedException {
        readQueue.add(1);

        reader.evaluateQueue();

        assertThat("the log queue containes the number", logQueue.peek(), equalTo(1));
    }

    @Test
    public void testItReadsADuplicateNumberFromTheQueueAndUpdatesDuplicateAtomics() throws InterruptedException {
        seenNumbers.add(1);
        readQueue.add(1);

        reader.evaluateQueue();

        assertThat("seenNumbers contains the number", seenNumbers.contains(1), equalTo(true));
        assertThat("duplicates has been incremented", duplicates.get(), equalTo(1));
    }

    @Test
    public void testItReadsADuplicateNumberFromTheQueueAndDoesNotSendItToBeLoggeed() throws InterruptedException {
        seenNumbers.add(1);
        readQueue.add(1);

        reader.evaluateQueue();

        assertThat("the log queue containes the number", logQueue.size(), equalTo(0));
    }
}
