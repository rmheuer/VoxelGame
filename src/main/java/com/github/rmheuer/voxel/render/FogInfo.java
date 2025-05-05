package com.github.rmheuer.voxel.render;

import org.joml.Vector4f;

public final class FogInfo {
    public final float minDistance;
    public final float maxDistance;
    public final Vector4f color;
    public final Vector4f tintColor;

    public FogInfo(float minDistance, float maxDistance, Vector4f color, Vector4f tintColor) {
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.color = color;
        this.tintColor = tintColor;
    }
}
