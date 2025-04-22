package com.github.rmheuer.voxel.render;

public final class LightingConstants {
    public static final float SHADE_UP = 1.0f;
    public static final float SHADE_FRONT_BACK = 0.9f;
    public static final float SHADE_LEFT_RIGHT = 0.8f;
    public static final float SHADE_DOWN = 0.7f;

    public static final float SHADE_LIT = 1.0f;
    public static final float SHADE_SHADOW = 0.7f;

    private LightingConstants() {
        throw new AssertionError();
    }
}
