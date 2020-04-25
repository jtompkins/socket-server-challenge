package com.newrelic.codingchallenge;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class NumberReporter implements Runnable {
    private AtomicInteger _uniques;
    private AtomicInteger _duplicates;
    private Set<Integer> _seenNumbers;
    private Logger _logger;

    public NumberReporter(AtomicInteger uniques, AtomicInteger duplicates, Set<Integer> seenNumbers, Logger logger) {
        _uniques = uniques;
        _duplicates = duplicates;
        _seenNumbers = seenNumbers;
        _logger = logger;
    }

    @Override
    public void run() {
        int numUniques = _uniques.getAndSet(0);
        int numDuplicates = _duplicates.getAndSet(0);
        int numNumbers = _seenNumbers.size();

        _logger.info(String.format("Received %d unique numbers, %d duplicates. Unique total: %d", numUniques,
                numDuplicates, numNumbers));
    }
}
