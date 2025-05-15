package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerRelativeMoveAndLookPacket implements ServerPacket {
    private byte playerId;
    private float deltaX;
    private float deltaY;
    private float deltaZ;
    private float yaw;
    private float pitch;

    public ServerRelativeMoveAndLookPacket() {}

    public ServerRelativeMoveAndLookPacket(byte playerId, float deltaX, float deltaY, float deltaZ, float yaw, float pitch) {
        this.playerId = playerId;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        playerId = in.readSByte();
        deltaX = in.readFByte();
        deltaY = in.readFByte();
        deltaZ = in.readFByte();
        yaw = in.readAngle();
        pitch = in.readAngle();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeSByte(playerId);
        out.writeFByte(deltaX);
        out.writeFByte(deltaY);
        out.writeFByte(deltaZ);
        out.writeAngle(yaw);
        out.writeAngle(pitch);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onRelativeMoveAndLook(this);
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

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    @Override
    public String toString() {
        return "ServerRelativeMoveAndLookPacket{" +
                "playerId=" + playerId +
                ", deltaX=" + deltaX +
                ", deltaY=" + deltaY +
                ", deltaZ=" + deltaZ +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }
}
