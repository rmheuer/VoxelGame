package com.github.rmheuer.voxel.level;

import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.azalea.render.Colors;

public final class Blocks {
    public static final byte ID_AIR = 0;
    public static final byte ID_SOLID = 1;
    public static final byte ID_WATER = 2;
    public static final byte ID_LAVA = 3;
    public static final byte ID_CROSS = 4;
    public static final byte ID_SLAB = 5;

    public static boolean blocksLight(byte id) {
        switch (id) {
            case ID_SOLID:
            case ID_SLAB:
            case ID_LAVA:
                return true;
            default:
                return false;
        }
    }

    public static OcclusionType getOcclusion(byte id, CubeFace face) {
        switch (id) {
            case ID_AIR:
            case ID_WATER:
            case ID_LAVA:
            case ID_CROSS:
                return OcclusionType.NONE;

            case ID_SOLID:
                return OcclusionType.FULL;

            case ID_SLAB:
                if (face == CubeFace.POS_Y)
                    return OcclusionType.NONE;
                else if (face == CubeFace.NEG_Y)
                    return OcclusionType.FULL;
                else
                    return OcclusionType.HALF;

            default:
                throw new AssertionError();
        }
    }

    public static int getColor(byte id) {
        switch (id) {
            case ID_SOLID:
                return Colors.RGBA.WHITE;
            case ID_WATER:
                return Colors.RGBA.fromFloats(0.3f, 0.3f, 1.0f, 0.6f);
            case ID_LAVA:
                return Colors.RGBA.fromFloats(1.0f, 0.5f, 0.0f);
            case ID_CROSS:
                return Colors.RGBA.fromFloats(0.2f, 0.5f, 0.2f);
            case ID_SLAB:
                return Colors.RGBA.fromFloats(0.8f, 0.6f, 0.6f);

            default:
                return Colors.RGBA.MAGENTA;
        }
    }
}
