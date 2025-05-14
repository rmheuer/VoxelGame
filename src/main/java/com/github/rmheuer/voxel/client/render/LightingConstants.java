package com.github.rmheuer.voxel.client.render;

/** Constants for lighting the level. */
public final class LightingConstants {
    // Shade multipliers for each cube face
    public static final float SHADE_UP = 1.0f;
    public static final float SHADE_FRONT_BACK = 0.8f;
    public static final float SHADE_LEFT_RIGHT = 0.6f;
    public static final float SHADE_DOWN = 0.5f;

    // Shade multipliers for sky lighting
    public static final float SHADE_LIT = 1.0f;
    public static final float SHADE_SHADOW = 0.65f;

    private LightingConstants() {
        throw new AssertionError();
    }
}
