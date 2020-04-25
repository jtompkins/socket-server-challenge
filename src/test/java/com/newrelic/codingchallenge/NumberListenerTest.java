package com.newrelic.codingchallenge;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.net.Socket;

import org.junit.Before;
import org.junit.Test;

public class NumberListenerTest {

    private ConcurrentLinkedQueue<Integer> readerQueue;
    private AtomicBoolean terminationSignal;
    private Logger logger;
    private Scanner scanner;
    private Socket socket;

    private NumberListener listener;

    @Before
    public void setup() {
        readerQueue = new ConcurrentLinkedQueue<>();
        terminationSignal = new AtomicBoolean();
        logger = mock(Logger.class);
        socket = mock(Socket.class);

        listener = new NumberListener(socket, readerQueue, terminationSignal, logger);
    }

    @Test(timeout = 1000)
    public void testItTerminatesTheListenerWhenTheTerminateSignalIsSet() throws InterruptedException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> f = service.submit(listener);

        terminationSignal.set(true);

        // this seems gross but I'm not really sure how to test it otherwise
        Thread.sleep(100);

        assertThat(f.isDone(), equalTo(true));
    }

    @Test
    public void testItAcceptsProperlyFormattedInput() {
        scanner = new Scanner("100000000");

        boolean result = listener.handleSocketInput(scanner);

        assertThat(result, equalTo(true));
    }

    @Test
    public void testItQueuesTheNumberWhenItReceivesNumericInput() throws InterruptedException {
        scanner = new Scanner("100000000");

        listener.handleSocketInput(scanner);

        assertThat(readerQueue.poll(), equalTo(100000000));
    }

    @Test
    public void testItSetsTheTerminationSignalWhenItReceivesTerminationInput() {
        scanner = new Scanner("terminate");

        listener.handleSocketInput(scanner);

        assertThat(terminationSignal.get(), equalTo(true));
    }

    @Test
    public void testItRejectsNonNumericInput() {
        scanner = new Scanner("10000000t");

        listener.handleSocketInput(scanner);

        assertThat(terminationSignal.get(), equalTo(false));
    }

    @Test
    public void testItRejectsNumericInputThatIsTooSmall() {
        scanner = new Scanner("1");

        listener.handleSocketInput(scanner);

        assertThat(terminationSignal.get(), equalTo(false));
    }

    @Test
    public void testItRejectsNumericInputThatIsTooLarge() {
        scanner = new Scanner("1000000001");

        listener.handleSocketInput(scanner);

        assertThat(terminationSignal.get(), equalTo(false));
    }
}
