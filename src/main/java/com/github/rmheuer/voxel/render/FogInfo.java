package com.github.rmheuer.voxel.render;

import org.joml.Vector4f;

/**
 * Parameters for linear distance fog.
 */
public final class FogInfo {
    public final float minDistance;
    public final float maxDistance;
    public final Vector4f color;
    public final Vector4f tintColor;

    /**
     * @param minDistance distance at which the fog starts
     * @param maxDistance distance at which the fog is fully opaque
     * @param color RGBA color of the fog as floats
     * @param tintColor RGBA color to tint the level with
     */
    public FogInfo(float minDistance, float maxDistance, Vector4f color, Vector4f tintColor) {
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.color = color;
        this.tintColor = tintColor;
    }
}
