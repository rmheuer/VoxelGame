package com.github.rmheuer.voxel.client.server_lists.betacraft;

// TODO: Betacraft provides icon and description, ClassiCube doesn't
public final class ServerEntry {
    public final String name;
    public final int playerCount, maxPlayers;
    public final String address;
    public final boolean onlineMode;

    public ServerEntry(String name, int playerCount, int maxPlayers, String address, boolean onlineMode) {
        this.name = name;
        this.playerCount = playerCount;
        this.maxPlayers = maxPlayers;
        this.address = address;
        this.onlineMode = onlineMode;
    }

    @Override
    public String toString() {
        return "ServerEntry{" +
                "name='" + name + '\'' +
                ", playerCount=" + playerCount +
                ", maxPlayers=" + maxPlayers +
                ", address='" + address + '\'' +
                ", onlineMode=" + onlineMode +
                '}';
    }
}
