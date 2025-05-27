package com.github.rmheuer.voxel.network.cpe.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;
import com.github.rmheuer.voxel.network.packet.ServerPacket;

import java.io.IOException;

public final class ServerSetTextColorPacket implements ServerPacket {
    private int r, g, b, a;
    private char code;

    public ServerSetTextColorPacket() {}

    public ServerSetTextColorPacket(int r, int g, int b, int a, char code) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.code = code;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        r = in.readUByte();
        g = in.readUByte();
        b = in.readUByte();
        a = in.readUByte();
        code = (char) in.readUByte();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeUByte(r);
        out.writeUByte(g);
        out.writeUByte(b);
        out.writeUByte(a);
        out.writeUByte(code);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onSetTextColor(this);
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    public int getA() {
        return a;
    }

    public char getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "ServerSetTextColorPacket{" +
                "r=" + r +
                ", g=" + g +
                ", b=" + b +
                ", a=" + a +
                ", code=" + code +
                '}';
    }
}
