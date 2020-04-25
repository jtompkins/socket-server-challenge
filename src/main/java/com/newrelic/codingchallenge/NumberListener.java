package com.newrelic.codingchallenge;

import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class NumberListener implements Runnable {
    private Socket _socket;
    private ConcurrentLinkedQueue<Integer> _readQueue;
    private AtomicBoolean _terminationSignal;
    private Logger _logger;

    public NumberListener(Socket socket, ConcurrentLinkedQueue<Integer> readQueue, AtomicBoolean terminationSignal,
            Logger logger) {
        _socket = socket;
        _readQueue = readQueue;
        _terminationSignal = terminationSignal;
        _logger = logger;
    }

    @Override
    public void run() {
        try {
            Scanner in = new Scanner(_socket.getInputStream());

            while (!_terminationSignal.get()) {
                if (!in.hasNextLine() || !handleSocketInput(in)) {
                    break;
                }
            }

            in.close();

            if (!_socket.isClosed()) {
                _socket.close();
            }
        } catch (Exception ex) {
            _logger.severe(String.format("Encountered error while reading from socket: %s", ex.getMessage()));
        }
    }

    public void shutdown() {
        try {
            if (!_socket.isClosed()) {
                _socket.close();
            }
        } catch (Exception ex) {
            _logger.severe(String.format("Error while closing socket: %s", ex.getMessage()));
        }
    }

    public boolean handleSocketInput(Scanner in) {
        String socketInput = in.nextLine();

        if (!socketInput.matches("\\d{9}|terminate")) {
            return false;
        }

        if (socketInput.equals("terminate")) {
            _terminationSignal.set(true);
        } else {
            _readQueue.add(Integer.parseInt(socketInput));
        }

        return true;
    }
}
