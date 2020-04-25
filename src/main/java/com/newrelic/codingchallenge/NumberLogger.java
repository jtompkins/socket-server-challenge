package com.newrelic.codingchallenge;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class NumberLogger implements Runnable {
    private BufferedWriter _writer;
    private ConcurrentLinkedQueue<Integer> _logQueue;
    private AtomicBoolean _terminationSignal;
    private Logger _logger;

    public NumberLogger(BufferedWriter writer, ConcurrentLinkedQueue<Integer> logQueue, AtomicBoolean terminationSignal,
            Logger logger) {
        _writer = writer;
        _logQueue = logQueue;
        _terminationSignal = terminationSignal;
        _logger = logger;
    }

    @Override
    public void run() {
        while (!_terminationSignal.get()) {
            try {
                evaluateQueue();
            } catch (IOException ioException) {
                _logger.severe(
                        String.format("Encountered exception while writing to log: %s", ioException.getMessage()));
                break;
            }
        }
    }

    public void evaluateQueue() throws IOException {
        if (_logQueue.isEmpty()) {
            return;
        }

        _writer.write(String.format("%d", _logQueue.poll()));
        _writer.newLine();
    }
}
