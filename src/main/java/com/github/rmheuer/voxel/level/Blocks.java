package com.github.rmheuer.voxel.level;

import com.github.rmheuer.azalea.render.Colors;

public final class Blocks {
    public static final byte ID_AIR = 0;
    public static final byte ID_SOLID = 1;
    public static final byte ID_WATER = 2;
    public static final byte ID_LAVA = 3;

    public static boolean isTransparent(byte id) {
        return id != ID_SOLID;
    }

    public static int getColor(byte id) {
        switch (id) {
            case ID_SOLID:
                return Colors.RGBA.WHITE;
            case ID_WATER:
                return Colors.RGBA.fromFloats(0.3f, 0.3f, 1.0f, 0.6f);
            case ID_LAVA:
                return Colors.RGBA.fromFloats(1.0f, 0.5f, 0.0f);

            default:
                return Colors.RGBA.MAGENTA;
        }
    }
}
