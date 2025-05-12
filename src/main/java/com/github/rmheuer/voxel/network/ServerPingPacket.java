package com.github.rmheuer.voxel.network;

public final class ServerPingPacket implements Packet {
    public static final ServerPingPacket INSTANCE = new ServerPingPacket();

    // No data

    private ServerPingPacket() {}

    @Override
    public void read(PacketDataInput in) {}

    @Override
    public void write(PacketDataOutput out) {}

    @Override
    public String toString() {
        return "ServerPingPacket{}";
    }
}
