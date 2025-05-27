package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

import java.io.IOException;

public final class ServerLevelDataChunkPacket implements ServerPacket {
    public static final int MAX_SIZE = 1024;

    private byte[] chunkData;
    private short percentComplete;

    public ServerLevelDataChunkPacket() {}

    public ServerLevelDataChunkPacket(byte[] chunkData, short percentComplete) {
        if (chunkData.length > MAX_SIZE)
            throw new IllegalArgumentException("Chunk data too large: " + chunkData.length + " > " + MAX_SIZE);

        this.chunkData = chunkData;
        this.percentComplete = percentComplete;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        short chunkLength = in.readShort();
        if (chunkLength < 0 || chunkLength > MAX_SIZE)
            throw new IOException("Invalid chunk length: " + chunkLength);

        chunkData = in.readBytes(chunkLength);
        in.skipBytes(MAX_SIZE - chunkLength);
        percentComplete = in.readUByte();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeShort((short) chunkData.length);
        out.writeBytes(chunkData);

        // Padding
        if (chunkData.length < MAX_SIZE)
            out.writeZeros(MAX_SIZE - chunkData.length);

        out.writeUByte(percentComplete);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onLevelDataChunk(this);
    }

    public byte[] getChunkData() {
        return chunkData;
    }

    public short getPercentComplete() {
        return percentComplete;
    }

    @Override
    public String toString() {
        return "ServerLevelDataChunkPacket{"
            + "chunkData=[" + chunkData.length + " bytes], "
            + "percentComplete=" + percentComplete
            + "}";
    }
}
