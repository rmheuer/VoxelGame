package com.github.rmheuer.voxel.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class ServerMain {
    public static void main(String[] args) throws IOException {
        int port = 25565;

        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.println("Accepting connections on port " + port);
            while (true) {
                Socket client = socket.accept();
                ClientConnectionThread handler = new ClientConnectionThread(client);
                handler.start();
            }
        }
    }
}
