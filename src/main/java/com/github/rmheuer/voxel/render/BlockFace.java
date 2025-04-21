package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.render.mesh.VertexData;
import org.joml.Vector3f;

public final class BlockFace {
    public final Vector3f v1, v2, v3, v4;
    public final int color;
    public final float shade;

    public BlockFace(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, int color, float shade) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.color = color;
        this.shade = shade;
    }

    public void addToMesh(VertexData out) {
        out.putVec3(v1); out.putColorRGBA(color); out.putFloat(shade);
        out.putVec3(v2); out.putColorRGBA(color); out.putFloat(shade);
        out.putVec3(v3); out.putColorRGBA(color); out.putFloat(shade);
        out.putVec3(v4); out.putColorRGBA(color); out.putFloat(shade);
    }

    public Vector3f getCenterPos(int ox, int oy, int oz) {
        return new Vector3f(v1).add(v2).add(v3).add(v4).div(4).add(ox, oy, oz);
    }
}
