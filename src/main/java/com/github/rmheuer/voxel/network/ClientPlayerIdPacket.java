package com.github.rmheuer.voxel.network;

import java.io.IOException;

public final class ClientPlayerIdPacket implements Packet {
    private short protocolVersion;
    private String username;
    private String verificationKey;

    public ClientPlayerIdPacket() {}

    public ClientPlayerIdPacket(short protocolVersion, String username, String verificationKey) {
        this.protocolVersion = protocolVersion;
        this.username = username;
        this.verificationKey = verificationKey;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        protocolVersion = in.readUByte();
        username = in.readString();
        verificationKey = in.readString();
        in.readUByte(); // Unused padding
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeUByte(protocolVersion);
        out.writeString(username);
        out.writeString(verificationKey);
        out.writeUByte(0x00); // Unused padding
    }

    public short getProtocolVersion() {
        return protocolVersion;
    }

    public String getUsername() {
        return username;
    }

    public String getVerificationKey() {
        return verificationKey;
    }

    @Override
    public String toString() {
        return "ClientPlayerIdPacket{"
            + "protocolVersion=" + protocolVersion + ", "
            + "username='" + username + "', "
            + "verificationKey='" + verificationKey + "'"
            + "}";
    }
}
