package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;

import java.io.IOException;

public interface Packet {
    void read(PacketDataInput in) throws IOException;

    void write(PacketDataOutput out) throws IOException;
}
