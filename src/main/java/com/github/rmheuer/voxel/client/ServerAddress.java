package com.github.rmheuer.voxel.client;

public final class ServerAddress {
    public static boolean isValid(String address) {
        return parse(address) != null;
    }

    public static ServerAddress parse(String address) {
        if (address.isEmpty())
            return null;

        int lastColon = address.lastIndexOf(':');
        if (lastColon < 0)
            return new ServerAddress(address, 25565);

        String host;
        if (address.charAt(0) == '[') {
            int close = address.indexOf(']');
            if (close < 0)
                return null;

            if (lastColon != close + 1)
                return null;

            host = address.substring(1, close);
        } else {
            int firstColon = address.indexOf(':');
            if (firstColon != lastColon) {
                // Probably an IPv6 address
                return new ServerAddress(address, 25565);
            }

            host = address.substring(0, lastColon);
        }

        try {
            int port = Integer.parseInt(address.substring(lastColon + 1));
            if (port < 0 || port > 65535)
                return null;

            return new ServerAddress(host, port);
        } catch (NumberFormatException e) {
            return null;
        }
    }

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
