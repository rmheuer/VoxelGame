package com.github.rmheuer.voxel.network;

import java.io.IOException;

public interface Packet {
    void read(PacketDataInput in) throws IOException;

    void write(PacketDataOutput out) throws IOException;
}
