package com.github.rmheuer.voxel.client;

public final class ServerAddress {
    private final String host;
    private final int port;

    public ServerAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
