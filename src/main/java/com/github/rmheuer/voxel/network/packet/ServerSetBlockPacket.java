package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerSetBlockPacket implements ServerPacket {
    private short x, y, z;
    private byte blockId;

    public ServerSetBlockPacket() {}

    public ServerSetBlockPacket(short x, short y, short z, byte blockId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockId = blockId;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        x = in.readShort();
        y = in.readShort();
        z = in.readShort();
        blockId = (byte) in.readUByte();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeShort(x);
        out.writeShort(y);
        out.writeShort(z);
        out.writeUByte(blockId);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onSetBlock(this);
    }

    public short getX() {
        return x;
    }

    public short getY() {
        return y;
    }

    public short getZ() {
        return z;
    }

    public byte getBlockId() {
        return blockId;
    }

    @Override
    public String toString() {
        return "ServerSetBlockPacket{"
            + "x=" + x + ", "
            + "y=" + y + ", "
            + "z=" + z + ", "
            + "blockId=" + blockId
            + "}";
    }
}
