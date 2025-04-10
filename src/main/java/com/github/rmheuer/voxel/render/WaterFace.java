package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.math.CubeFace;

public final class WaterFace {
    public final int x, y, z; // Block position in section
    public final CubeFace face;

    public WaterFace(int x, int y, int z, CubeFace face) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
    }

    @Override
    public String toString() {
        return "WaterFace{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", face=" + face +
                '}';
    }
}
