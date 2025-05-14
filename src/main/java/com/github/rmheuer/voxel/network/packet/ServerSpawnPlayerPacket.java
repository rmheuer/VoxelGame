package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerSpawnPlayerPacket implements ServerPacket {
    private byte playerId;
    private String playerName;
    private float x, y, z;
    private short yaw;
    private short pitch;

    public ServerSpawnPlayerPacket() {}

    public ServerSpawnPlayerPacket(byte playerId, String playerName, float x, float y, float z, short yaw, short pitch) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        playerId = in.readSByte();
        playerName = in.readString();
        x = in.readFShort();
        y = in.readFShort();
        z = in.readFShort();
        yaw = in.readUByte();
        pitch = in.readUByte();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeSByte(playerId);
        out.writeString(playerName);
        out.writeFShort(x);
        out.writeFShort(y);
        out.writeFShort(z);
        out.writeUByte(yaw);
        out.writeUByte(pitch);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onSpawnPlayer(this);
    }

    public byte getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public short getYaw() {
        return yaw;
    }

    public short getPitch() {
        return pitch;
    }

    @Override
    public String toString() {
        return "ServerSpawnPlayerPacket{"
            + "playerId=" + playerId + ", "
            + "playerName='" + playerName + "', "
            + "x=" + x + ", "
            + "y=" + y + ", "
            + "z=" + z + ", "
            + "yaw=" + yaw + ", "
            + "pitch=" + pitch
            + "}";
    }
}
