package com.newrelic.codingchallenge;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Logger;

public class NumberWriter implements Runnable {
    public static int MAX_RANDOM = 999999999;

    private final Logger _logger;
    private final Random _random;

    public NumberWriter(final Logger logger) {
        _logger = logger;
        _random = new Random();
    }

    @Override
    public void run() {
        try (Socket socket = new Socket("localhost", 4000)) {
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            for (int i = 0; i <= 250000; i++) {
                final String nextRandom = String.format("%09d", _random.nextInt(MAX_RANDOM));
                outputStream.writeBytes(String.format("%s\n", nextRandom));
            }

            _logger.info("Wrote 250,000 numbers to socket");
            outputStream.close();
        } catch (Exception ex) {
            _logger.severe(String.format("Error writing to socket: %s", ex.getMessage()));
        }
    }
}
