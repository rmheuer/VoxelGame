package com.github.rmheuer.voxel.network.cpe.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;
import com.github.rmheuer.voxel.network.packet.ServerPacket;

import java.io.IOException;

public final class ServerBulkBlockUpdatePacket implements ServerPacket {
    public static final int MAX_UPDATES = 256;

    private int[] indices;
    private byte[] blocks;

    public ServerBulkBlockUpdatePacket() {}

    public ServerBulkBlockUpdatePacket(int[] indices, byte[] blocks) {
        if (indices.length > MAX_UPDATES || blocks.length > MAX_UPDATES)
            throw new IllegalArgumentException("Too many updates!");
        if (indices.length != blocks.length)
            throw new IllegalArgumentException("Array sizes do not match");

        this.indices = indices;
        this.blocks = blocks;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        int count = in.readUByte() + 1;
        indices = in.readInts(count);
        in.skipBytes(1024 - 4 * count);
        blocks = in.readBytes(count);
        // Don't bother reading the remaining padding
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeUByte(indices.length - 1);
        out.writeInts(indices);
        out.writeZeros(1024 - 4 * indices.length);
        out.writeBytes(blocks);
        out.writeZeros(256 - blocks.length);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onBulkBlockUpdate(this);
    }

    public int[] getIndices() {
        return indices;
    }

    public byte[] getBlocks() {
        return blocks;
    }

    @Override
    public String toString() {
        return "ServerBulkBlockUpdatePacket[" + indices.length + " changes]";
    }
}
