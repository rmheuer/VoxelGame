package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerDisconnectPacket implements ServerPacket {
    private String reason;

    public ServerDisconnectPacket() {}

    public ServerDisconnectPacket(String reason) {
        this.reason = reason;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        reason = in.readString();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeString(reason);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onDisconnect(this);
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "ServerDisconnectPacket{" +
                "reason='" + reason + '\'' +
                '}';
    }
}
