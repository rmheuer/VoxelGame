package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerDespawnPlayerPacket implements ServerPacket {
    private byte playerId;

    public ServerDespawnPlayerPacket() {}

    public ServerDespawnPlayerPacket(byte playerId) {
        this.playerId = playerId;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        playerId = in.readSByte();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeSByte(playerId);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onDespawnPlayer(this);
    }

    public byte getPlayerId() {
        return playerId;
    }

    @Override
    public String toString() {
        return "ServerDespawnPlayerPacket{" +
                "playerId=" + playerId +
                '}';
    }
}
