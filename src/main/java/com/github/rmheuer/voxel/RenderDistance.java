package com.github.rmheuer.voxel;

/**
 * How far it is possible to see away from the player.
 */
public enum RenderDistance {
    FAR(512),
    NORMAL(128),
    SHORT(32),
    TINY(8);
    
    private final float fogDistance;

    /**
     * @param fogDistance distance from the camera at which fog is fully opaque
     */
    RenderDistance(float fogDistance) {
        this.fogDistance = fogDistance;
    }

    public float getFogDistance() {
        return fogDistance;
    }
}
