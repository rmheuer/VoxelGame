package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.math.CubeFace;
import org.joml.Vector3f;

public final class WaterFace {
    public final int x, y, z; // Block position in section
    public final CubeFace face;
    public final float botH, topH, depth;

    public WaterFace(int x, int y, int z, CubeFace face, float botH, float topH, float depth) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.botH = botH;
        this.topH = topH;
        this.depth = depth;
    }

    public Vector3f getCenterPos(float ox, float oy, float oz) {
        switch (face) {
            case POS_X:
                return new Vector3f(ox + x + 1 - depth, oy + y + (botH + topH) / 2, oz + z + 0.5f);
            case POS_Y:
                return new Vector3f(ox + x + 0.5f, oy + y + 1 - depth, oz + z + 0.5f);
            case POS_Z:
                return new Vector3f(ox + x + 0.5f, oy + y + (botH + topH) / 2, oz + z + 1 - depth);
            case NEG_X:
                return new Vector3f(ox + x + depth, oy + y + (botH + topH) / 2, oz + z + 0.5f);
            case NEG_Y:
                return new Vector3f(ox + x + 0.5f, oy + y + depth, oz + z + 0.5f);
            case NEG_Z:
                return new Vector3f(ox + x + 0.5f, oy + y + (botH + topH) / 2, oz + z + depth);
        }
        throw new IllegalArgumentException();
    }
}
