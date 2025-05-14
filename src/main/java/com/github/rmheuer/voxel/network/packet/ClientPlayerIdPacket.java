package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.ClientPacketListener;
import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;

import java.io.IOException;

public final class ClientPlayerIdPacket implements ClientPacket {
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

    @Override
    public void handleClient(ClientPacketListener listener) {
        listener.onPlayerId(this);
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
