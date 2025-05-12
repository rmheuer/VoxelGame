package com.github.rmheuer.voxel.network;

import java.io.IOException;

public interface PacketDataOutput {
    void writeUByte(int val) throws IOException;

    void writeSByte(byte val) throws IOException;

    void writeFByte(float val) throws IOException;
    
    void writeShort(short val) throws IOException;

    void writeFShort(float val) throws IOException;

    void writeString(String val) throws IOException;

    void writeBytes(byte[] data) throws IOException;
}
