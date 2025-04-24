package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.render.mesh.AttribType;
import com.github.rmheuer.azalea.render.mesh.VertexData;
import com.github.rmheuer.azalea.render.mesh.VertexLayout;
import org.joml.Vector3f;

public final class BlockFace {
    public static final VertexLayout VERTEX_LAYOUT = new VertexLayout(
            AttribType.VEC3, // Position
            AttribType.VEC2, // UV
            AttribType.FLOAT // Shade
    );

    public final Vector3f v1, v2, v3, v4;
    public final AtlasSprite sprite;
    public final float shade;

    public BlockFace(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, AtlasSprite sprite, float shade) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.sprite = sprite;
        this.shade = shade;
    }

    public void addToMesh(VertexData out) {
        out.putVec3(v1); out.putVec2(sprite.u1, sprite.v1); out.putFloat(shade);
        out.putVec3(v2); out.putVec2(sprite.u1, sprite.v2); out.putFloat(shade);
        out.putVec3(v3); out.putVec2(sprite.u2, sprite.v2); out.putFloat(shade);
        out.putVec3(v4); out.putVec2(sprite.u2, sprite.v1); out.putFloat(shade);
    }

    public Vector3f getCenterPos(int ox, int oy, int oz) {
        return new Vector3f(v1).add(v2).add(v3).add(v4).div(4).add(ox, oy, oz);
    }

    public BlockFace makeBackFace() {
        return new BlockFace(v4, v3, v2, v1, sprite.flipHorizontally(), shade);
    }
}
