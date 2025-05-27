package com.github.rmheuer.voxel.network.cpe.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;
import com.github.rmheuer.voxel.network.packet.ServerPacket;

import java.io.IOException;

public final class ServerSetClickDistancePacket implements ServerPacket {
    private float distance;

    public ServerSetClickDistancePacket() {}

    public ServerSetClickDistancePacket(float distance) {
        this.distance = distance;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        distance = in.readFShort();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeFShort(distance);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onSetClickDistance(this);
    }

    public float getDistance() {
        return distance;
    }

    @Override
    public String toString() {
        return "ServerSetClickDistancePacket{"
            + "distance=" + distance
            + "}";
    }
}
