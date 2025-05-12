package com.github.rmheuer.voxel;

public enum RenderDistance {
    FAR(512),
    NORMAL(128),
    SHORT(32),
    TINY(8);
    
    private final float fogDistance;

    RenderDistance(float fogDistance) {
        this.fogDistance = fogDistance;
    }

    public float getFogDistance() {
        return fogDistance;
    }
}
