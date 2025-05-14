package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerIdPacket implements ServerPacket {
    private short protocolVersion;
    private String serverName;
    private String serverMotd;
    private boolean op;

    public ServerIdPacket() {}

    public ServerIdPacket(short protocolVersion, String serverName, String serverMotd, boolean op) {
        this.protocolVersion = protocolVersion;
        this.serverName = serverName;
        this.serverMotd = serverMotd;
        this.op = op;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        protocolVersion = in.readUByte();
        serverName = in.readString();
        serverMotd = in.readString();
        op = in.readUByte() == 0x64;
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeUByte(protocolVersion);
        out.writeString(serverName);
        out.writeString(serverMotd);
        out.writeUByte(op ? 0x64 : 0x00);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onServerId(this);
    }

    public short getProtocolVersion() {
        return protocolVersion;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerMotd() {
        return serverMotd;
    }

    public boolean isOp() {
        return op;
    }

    @Override
    public String toString() {
        return "ServerIdPacket{"
            + "protocolVersion=" + protocolVersion + ", "
            + "serverName='" + serverName + "', "
            + "serverMotd='" + serverMotd + "', "
            + "op=" + op
            + "}";
    }
}
