package com.github.rmheuer.voxel.network.cpe.packet;

import com.github.rmheuer.voxel.network.BidiPacketListener;
import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.packet.BidiPacket;

import java.io.IOException;

public final class BidiExtEntryPacket implements BidiPacket {
    private String extName;
    private int version;

    public BidiExtEntryPacket() {}

    public BidiExtEntryPacket(String extName, int version) {
        this.extName = extName;
        this.version = version;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        extName = in.readString();
        version = in.readInt();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeString(extName);
        out.writeInt(version);
    }

    @Override
    public void handle(BidiPacketListener listener) {
        listener.onExtEntry(this);
    }

    public String getExtName() {
        return extName;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "BidiExtEntryPacket{" +
                "extName='" + extName + '\'' +
                ", version=" + version +
                '}';
    }
}
