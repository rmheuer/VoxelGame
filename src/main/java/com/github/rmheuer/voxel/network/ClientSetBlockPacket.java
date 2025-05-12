package com.github.rmheuer.voxel.network;

import java.io.IOException;

public final class ClientSetBlockPacket implements Packet {
    public enum Mode {
        PLACED,
        DESTROYED
    }

    private short x, y, z;
    private Mode mode;
    private byte blockId;

    public ClientSetBlockPacket() {}

    public ClientSetBlockPacket(short x, short y, short z, Mode mode, byte blockId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.mode = mode;
        this.blockId = blockId;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        x = in.readShort();
        y = in.readShort();
        z = in.readShort();
        mode = in.readUByte() != 0 ? Mode.PLACED : Mode.DESTROYED;
        blockId = (byte) in.readUByte();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeShort(x);
        out.writeShort(y);
        out.writeShort(z);
        out.writeUByte(mode == Mode.PLACED ? 1 : 0);
        out.writeUByte(blockId);
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

    public Mode getMode() {
        return mode;
    }

    public byte getBlockId() {
        return blockId;
    }

    @Override
    public String toString() {
        return "ClientSetBlockPacket{"
            + "x=" + x + ", "
            + "y=" + y + ", "
            + "z=" + z + ", "
            + "mode=" + mode + ", "
            + "blockId=" + blockId
            + "}";
    }
}
