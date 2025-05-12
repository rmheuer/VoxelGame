package com.github.rmheuer.voxel.network;

import java.io.IOException;

public interface PacketDataInput {
    short readUByte() throws IOException;

    byte readSByte() throws IOException;

    float readFByte() throws IOException;

    short readShort() throws IOException;

    float readFShort() throws IOException;

    String readString() throws IOException;

    byte[] readBytes(int count) throws IOException;

    void skipBytes(int count) throws IOException;
}
