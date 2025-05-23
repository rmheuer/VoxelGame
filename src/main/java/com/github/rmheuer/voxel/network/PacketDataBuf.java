package com.github.rmheuer.voxel.network;

import com.github.rmheuer.azalea.math.MathUtil;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class PacketDataBuf implements PacketDataInput, PacketDataOutput {
    public static final int MAX_STRING_LEN = 64;

    private final ByteBuf buf;

    public PacketDataBuf(ByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public short readUByte() {
        return buf.readUnsignedByte();
    }

    @Override
    public void writeUByte(int val) {
        buf.writeByte(val);
    }

    @Override
    public byte readSByte() {
        return buf.readByte();
    }

    @Override
    public void writeSByte(byte val) {
        buf.writeByte(val);
    }

    @Override
    public float readFByte() {
        return buf.readByte() / 32.0f;
    }

    @Override
    public void writeFByte(float val) {
        buf.writeByte((byte) (val * 32.0f));
    }

    @Override
    public float readAngle() {
        int val = buf.readUnsignedByte();
        return -val / 256.0f * (float) Math.PI * 2;
    }

    @Override
    public void writeAngle(float radians) {
        float scaled = -radians / ((float) Math.PI * 2) * 256.0f;
        int wrapped = (int) MathUtil.wrap(scaled, 0, 256);
        buf.writeByte(wrapped);
    }

    @Override
    public short readShort() {
        return buf.readShort();
    }

    @Override
    public void writeShort(short val) {
        buf.writeShort(val);
    }

    @Override
    public float readFShort() {
        return buf.readShort() / 32.0f;
    }

    @Override
    public void writeFShort(float val) {
        buf.writeShort((short) (val * 32.0f));
    }

    @Override
    public int readInt() {
        return buf.readInt();
    }

    @Override
    public void writeInt(int val) {
        buf.writeInt(val);
    }

    @Override
    public String readString() {
        byte[] data = readBytes(MAX_STRING_LEN);
        String str = new String(data, StandardCharsets.US_ASCII);
        return str.stripTrailing();
    }

    @Override
    public void writeString(String str) throws IOException {
        byte[] ascii = str.getBytes(StandardCharsets.US_ASCII);
        if (ascii.length > MAX_STRING_LEN)
            throw new IOException("String too long to send");

        buf.writeBytes(ascii);
        if (ascii.length < MAX_STRING_LEN) {
            byte[] pad = new byte[MAX_STRING_LEN - ascii.length];
            Arrays.fill(pad, (byte) 0x20);
            buf.writeBytes(pad);
        }
    }

    @Override
    public byte[] readBytes(int count) {
        byte[] bytes = new byte[count];
        buf.readBytes(bytes);
        return bytes;
    }

    @Override
    public void writeBytes(byte[] bytes) {
        buf.writeBytes(bytes);
    }

    @Override
    public void skipBytes(int count) {
        buf.skipBytes(count);
    }
}
