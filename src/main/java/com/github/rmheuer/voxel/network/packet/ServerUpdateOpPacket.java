package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerUpdateOpPacket implements ServerPacket {
    private boolean op;

    public ServerUpdateOpPacket() {}

    public ServerUpdateOpPacket(boolean op) {
        this.op = op;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        op = in.readUByte() == 0x64;
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeUByte(op ? 0x64 : 0x00);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onUpdateOp(this);
    }

    @Override
    public String toString() {
        return "ServerUpdateOpPacket{" +
                "op=" + op +
                '}';
    }
}
