package com.newrelic.codingchallenge;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class NumberReader implements Runnable {
    private ConcurrentLinkedQueue<Integer> _readQueue;
    private ConcurrentLinkedQueue<Integer> _logQueue;
    private AtomicInteger _uniques;
    private AtomicInteger _duplicates;
    private Set<Integer> _seenNumbers;
    private AtomicBoolean _terminationSignal;
    private Logger _logger;

    public NumberReader(ConcurrentLinkedQueue<Integer> readQueue, ConcurrentLinkedQueue<Integer> logQueue,
            AtomicInteger uniques, AtomicInteger duplicates, Set<Integer> seenNumbers, AtomicBoolean terminationSignal,
            Logger logger) {
        _readQueue = readQueue;
        _logQueue = logQueue;
        _uniques = uniques;
        _duplicates = duplicates;
        _seenNumbers = seenNumbers;
        _terminationSignal = terminationSignal;
        _logger = logger;
    }

    @Override
    public void run() {
        while (!_terminationSignal.get()) {
            try {
                evaluateQueue();
            } catch (InterruptedException ex) {
                _logger.severe(String.format("Encountered error while reading from queue: %s", ex.getMessage()));
                break;
            }
        }
    }

    public void evaluateQueue() throws InterruptedException {
        if (_readQueue.isEmpty()) {
            return;
        }

        Integer newNumber = _readQueue.poll();

        if (_seenNumbers.add(newNumber)) {
            _uniques.incrementAndGet();
            _logQueue.add(newNumber);
        } else {
            _duplicates.incrementAndGet();
        }
    }
}
