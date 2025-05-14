package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerLevelFinalizePacket implements ServerPacket {
    private short sizeX, sizeY, sizeZ;

    public ServerLevelFinalizePacket() {}

    public ServerLevelFinalizePacket(short sizeX, short sizeY, short sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        sizeX = in.readShort();
        sizeY = in.readShort();
        sizeZ = in.readShort();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeShort(sizeX);
        out.writeShort(sizeY);
        out.writeShort(sizeZ);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onLevelFinalize(this);
    }

    public short getSizeX() {
        return sizeX;
    }

    public short getSizeY() {
        return sizeY;
    }

    public short getSizeZ() {
        return sizeZ;
    }

    @Override
    public String toString() {
        return "ServerLevelFinalizePacket{"
            + "sizeX=" + sizeX + ", "
            + "sizeY=" + sizeY + ", "
            + "sizeZ=" + sizeZ
            + "}";
    }
}
