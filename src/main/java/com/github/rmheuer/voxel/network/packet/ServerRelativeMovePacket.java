package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerRelativeMovePacket implements ServerPacket {
    private byte playerId;
    private float deltaX;
    private float deltaY;
    private float deltaZ;
    public ServerRelativeMovePacket() {}

    public ServerRelativeMovePacket(byte playerId, float deltaX, float deltaY, float deltaZ) {
        this.playerId = playerId;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        playerId = in.readSByte();
        deltaX = in.readFByte();
        deltaY = in.readFByte();
        deltaZ = in.readFByte();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeSByte(playerId);
        out.writeFByte(deltaX);
        out.writeFByte(deltaY);
        out.writeFByte(deltaZ);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onRelativeMove(this);
    }

    public byte getPlayerId() {
        return playerId;
    }

    public float getDeltaX() {
        return deltaX;
    }

    public float getDeltaY() {
        return deltaY;
    }

    public float getDeltaZ() {
        return deltaZ;
    }

    @Override
    public String toString() {
        return "ServerRelativeMovePacket{" +
                "playerId=" + playerId +
                ", deltaX=" + deltaX +
                ", deltaY=" + deltaY +
                ", deltaZ=" + deltaZ +
                '}';
    }
}
