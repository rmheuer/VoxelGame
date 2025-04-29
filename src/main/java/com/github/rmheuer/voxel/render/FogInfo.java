package com.github.rmheuer.voxel.render;

import org.joml.Vector4f;

public final class FogInfo {
    public final float minDistance;
    public final float maxDistance;
    public final Vector4f color;

    public FogInfo(float minDistance, float maxDistance, Vector4f color) {
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.color = color;
    }
}
