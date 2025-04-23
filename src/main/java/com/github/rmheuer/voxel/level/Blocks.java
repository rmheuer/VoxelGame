package com.github.rmheuer.voxel.level;

public final class Blocks {
    public static final byte ID_AIR = 0;
    public static final byte ID_SOLID = 1;
    public static final byte ID_WATER = 2;
    public static final byte ID_LAVA = 3;

    public static boolean isOpaque(byte id) {
        return id == ID_SOLID;
    }
}
