package com.github.rmheuer.voxel.network.cpe.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;
import com.github.rmheuer.voxel.network.packet.ServerPacket;

import java.io.IOException;

public final class ServerExtEntityTeleportPacket implements ServerPacket {
    public enum MoveMode {
        NONE,
        ABSOLUTE_INSTANT,
        ABSOLUTE_SMOOTH,
        RELATIVE_INSTANT,
        RELATIVE_SMOOTH
    }

    public enum LookMode {
        NONE,
        INSTANT,
        SMOOTH
    }

    private byte entityId;
    private MoveMode moveMode;
    private LookMode lookMode;
    private float x, y, z;
    private float yaw, pitch;

    public ServerExtEntityTeleportPacket() {}

    public ServerExtEntityTeleportPacket(byte entityId, MoveMode moveMode, LookMode lookMode, float x, float y, float z, float yaw, float pitch) {
        this.entityId = entityId;
        this.moveMode = moveMode;
        this.lookMode = lookMode;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public void read(PacketDataInput in) throws IOException {
        entityId = in.readSByte();

        int behaviorMask = in.readUByte();

        if ((behaviorMask & 1) != 0) {
            switch ((behaviorMask >> 1) & 0b11) {
                case 0b00: moveMode = MoveMode.ABSOLUTE_INSTANT; break;
                case 0b01: moveMode = MoveMode.ABSOLUTE_SMOOTH; break;
                case 0b10: moveMode = MoveMode.RELATIVE_SMOOTH; break;
                case 0b11: moveMode = MoveMode.RELATIVE_INSTANT; break;
            }
        } else {
            moveMode = MoveMode.NONE;
        }

        if ((behaviorMask & (1 << 5)) != 0) {
            lookMode = (behaviorMask & (1 << 6)) != 0
                    ? LookMode.SMOOTH
                    : LookMode.INSTANT;
        } else {
            lookMode = LookMode.NONE;
        }

        x = in.readFShort();
        y = in.readFShort();
        z = in.readFShort();
        yaw = in.readAngle();
        pitch = in.readAngle();
    }

    @Override
    public void write(PacketDataOutput out) throws IOException {
        out.writeSByte(entityId);

        int mask = 0;
        if (moveMode != MoveMode.NONE) {
            mask |= 1;
            switch (moveMode) {
                case ABSOLUTE_SMOOTH: mask |= 0b010; break;
                case RELATIVE_SMOOTH: mask |= 0b100; break;
                case RELATIVE_INSTANT: mask |= 0b110; break;
            }
        }
        if (lookMode != LookMode.NONE) {
            mask |= 1 << 5;
            if (lookMode == LookMode.SMOOTH)
                mask |= 1 << 6;
        }
        out.writeUByte(mask);

        out.writeFShort(x);
        out.writeFShort(y);
        out.writeFShort(z);
        out.writeAngle(yaw);
        out.writeAngle(pitch);
    }

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onExtEntityTeleport(this);
    }

    public byte getEntityId() {
        return entityId;
    }

    public MoveMode getMoveMode() {
        return moveMode;
    }

    public LookMode getLookMode() {
        return lookMode;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    @Override
    public String toString() {
        return "ServerExtEntityTeleportPacket{" +
                "entityId=" + entityId +
                ", moveMode=" + moveMode +
                ", lookMode=" + lookMode +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }
}
