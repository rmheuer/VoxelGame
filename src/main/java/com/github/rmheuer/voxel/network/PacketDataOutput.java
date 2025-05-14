package com.github.rmheuer.voxel.network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class PacketDataOutput {
    private final DataOutputStream out;

    public PacketDataOutput(DataOutputStream out) {
        this.out = out;
    }

    public void writeUByte(int val) throws IOException {
        out.writeByte(val);
    }

    public void writeSByte(byte val) throws IOException {
        out.writeByte(val);
    }

    public void writeFByte(float val) throws IOException {
        out.writeByte((byte) (val * 32));
    }

    public void writeShort(short val) throws IOException {
        out.writeShort(val);
    }

    public void writeFShort(float val) throws IOException {
        out.writeShort((short) (val * 32));
    }

    public void writeString(String val) throws IOException {
        byte[] data = val.getBytes(StandardCharsets.US_ASCII);
        if (data.length > 64)
            throw new IOException("String too long to send");

        out.write(data);
        if (data.length < 64) {
            byte[] pad = new byte[64 - data.length];
            Arrays.fill(pad, (byte) 0x20);
            out.write(pad);
        }
    }

    public void writeBytes(byte[] data) throws IOException {
        out.write(data);
    }
}
