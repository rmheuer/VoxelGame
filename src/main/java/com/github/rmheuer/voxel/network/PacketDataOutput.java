package com.github.rmheuer.voxel.network;

import java.io.IOException;

public interface PacketDataOutput {
    void writeUByte(int val);

    void writeSByte(byte val);

    void writeFByte(float val);

    void writeAngle(float radians);

    void writeShort(short val);

    void writeFShort(float val);

    void writeInt(int val);

    void writeString(String val) throws IOException;

    void writeBytes(byte[] data);
}
