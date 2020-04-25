package com.newrelic.codingchallenge;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

public class NumberReporterTest {
    private AtomicInteger uniques;
    private AtomicInteger duplicates;
    private Set<Integer> seenNumbers;
    private Logger logger;

    private NumberReporter reporter;

    @Before
    public void setup() {
        uniques = new AtomicInteger();
        duplicates = new AtomicInteger();
        seenNumbers = new HashSet<>();
        logger = mock(Logger.class);

        reporter = new NumberReporter(uniques, duplicates, seenNumbers, logger);
    }

    @Test
    public void testItResetsTheAtomics() {
        uniques.set(1);
        duplicates.set(1);

        reporter.run();

        assertThat(uniques.get(), equalTo(0));
        assertThat(duplicates.get(), equalTo(0));
    }

    @Test
    public void testItLogsThePreviousValues() {
        uniques.set(1);
        duplicates.set(1);
        seenNumbers.add(1);

        reporter.run();

        verify(logger).info("Received 1 unique numbers, 1 duplicates. Unique total: 1");
    }
}
