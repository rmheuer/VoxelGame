package com.github.rmheuer.voxel.server;

import java.util.Queue;
import java.util.Scanner;

public final class ConsoleInputThread extends Thread {
    private final Queue<String> inputQueue;
    private volatile boolean running;

    public ConsoleInputThread(Queue<String> inputQueue) {
        super("Console Input");
        this.inputQueue = inputQueue;
        setDaemon(true);

        running = true;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        while (running) {
            String line = scanner.nextLine();
            inputQueue.add(line);
        }
    }

    public void close() {
        running = false;
    }
}
