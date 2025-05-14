package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerLookPacket implements ServerPacket {
    private byte playerId;
    private short yaw;
    private short pitch;

    public ServerLookPacket() {}

    public ServerLookPacket(byte playerId, short yaw, short pitch) {
        this.playerId = playerId;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        playerId = in.readSByte();
        yaw = in.readUByte();
        pitch = in.readUByte();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeSByte(playerId);
        out.writeUByte(yaw);
        out.writeUByte(pitch);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onLook(this);
    }

    public byte getPlayerId() {
        return playerId;
    }

    public short getYaw() {
        return yaw;
    }

    public short getPitch() {
        return pitch;
    }

    @Override
    public String toString() {
        return "ServerLookPacket{" +
                "playerId=" + playerId +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }
}
