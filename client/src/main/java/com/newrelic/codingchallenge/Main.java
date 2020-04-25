package com.newrelic.codingchallenge;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) throws IOException {
        Logger logger = Logger.getGlobal();
        ScheduledExecutorService service = Executors.newScheduledThreadPool(5);

        for (int i = 0; i < 5; i++) {
            service.scheduleAtFixedRate(new NumberWriter(logger), 0, 1, TimeUnit.SECONDS);
        }
    }
}
