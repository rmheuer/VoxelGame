package com.github.rmheuer.voxel.network.cpe.packet;

import com.github.rmheuer.voxel.network.*;
import com.github.rmheuer.voxel.network.packet.BidiPacket;

import java.io.IOException;

public final class BidiExtInfoPacket implements BidiPacket {
    private String appName;
    private short extensionCount;

    public BidiExtInfoPacket() {}

    public BidiExtInfoPacket(String appName, short extensionCount) {
        this.appName = appName;
        this.extensionCount = extensionCount;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        appName = in.readString();
        extensionCount = in.readShort();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeString(appName);
        out.writeShort(extensionCount);
    }

    @Override
    public void handle(BidiPacketListener listener) {
        listener.onExtInfo(this);
    }

    public String getAppName() {
        return appName;
    }

    public short getExtensionCount() {
        return extensionCount;
    }

    @Override
    public String toString() {
        return "BidiExtInfoPacket{" +
                "appName='" + appName + '\'' +
                ", extensionCount=" + extensionCount +
                '}';
    }
}
