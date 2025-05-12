package com.github.rmheuer.voxel.network;

public final class ServerLevelInitPacket implements Packet {
    public static final ServerLevelInitPacket INSTANCE = new ServerLevelInitPacket();

    // No data

    private ServerLevelInitPacket() {}

    @Override
    public void read(PacketDataInput in) {}

    @Override
    public void write(PacketDataOutput out) {}

    @Override
    public String toString() {
        return "ServerLevelInitPacket{}";
    }
}
