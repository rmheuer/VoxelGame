package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.ClientPacketListener;
import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class BidiChatMessagePacket implements ClientPacket, ServerPacket {
    private byte playerId;
    private String message;

    public BidiChatMessagePacket() {}

    public BidiChatMessagePacket(byte playerId, String message) {
        this.playerId = playerId;
        this.message = message;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        playerId = in.readSByte();
        message = in.readString();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeSByte(playerId);
        out.writeString(message);
    }

    @Override
    public void handleClient(ClientPacketListener listener) {
        listener.onChatMessage(this);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onChatMessage(this);
    }

    public byte getPlayerId() {
        return playerId;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "BidiChatMessagePacket{"
            + "playerId=" + playerId + ", "
            + "message='" + message + "'"
            + "}";
    }
}
