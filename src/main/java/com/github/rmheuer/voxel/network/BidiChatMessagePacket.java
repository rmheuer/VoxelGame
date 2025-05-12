package com.github.rmheuer.voxel.network;

import java.io.IOException;

public final class BidiChatMessagePacket implements Packet {
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
