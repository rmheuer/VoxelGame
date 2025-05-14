package com.github.rmheuer.voxel.network;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PacketDataInput {
    private final DataInputStream in;

    public PacketDataInput(DataInputStream in) {
        this.in = in;
    }

    public short readUByte() throws IOException {
        return (short) in.readUnsignedByte();
    }

    public byte readSByte() throws IOException {
        return in.readByte();
    }

    public float readFByte() throws IOException {
        return in.readByte() / 32.0f;
    }

    public short readShort() throws IOException {
        return in.readShort();
    }

    public float readFShort() throws IOException {
        return in.readShort() / 32.0f;
    }

    public String readString() throws IOException {
        byte[] data = new byte[64];
        in.readFully(data);

        String str = new String(data, StandardCharsets.US_ASCII);
        return str.stripTrailing();
    }

    public byte[] readBytes(int count) throws IOException {
        byte[] buf = new byte[count];
        in.readFully(buf);
        return buf;
    }

    public void skipBytes(int count) throws IOException {
        in.skipBytes(count);
    }

    public int available() throws IOException {
        return in.available();
    }
}
