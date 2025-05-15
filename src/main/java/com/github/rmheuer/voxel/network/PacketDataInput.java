package com.github.rmheuer.voxel.network;

public interface PacketDataInput {
    short readUByte();

    byte readSByte();

    float readFByte();

    // Radians
    float readAngle();

    short readShort();

    float readFShort();

    String readString();

    byte[] readBytes(int count);

    void skipBytes(int count);
}
