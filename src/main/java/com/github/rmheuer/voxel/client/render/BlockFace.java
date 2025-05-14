package com.github.rmheuer.voxel.client.render;

import com.github.rmheuer.azalea.render.mesh.AttribType;
import com.github.rmheuer.azalea.render.mesh.VertexData;
import com.github.rmheuer.azalea.render.mesh.VertexLayout;
import org.joml.Vector3f;

/**
 * Represents one face of a block.
 */
public final class BlockFace {
    public static final VertexLayout VERTEX_LAYOUT = new VertexLayout(
            AttribType.VEC3, // Position
            AttribType.VEC2, // UV
            AttribType.FLOAT // Shade
    );

    public final Vector3f v1, v2, v3, v4;
    public final AtlasSprite sprite;
    public final float shade;

    /**
     * The corners should be specified in counterclockwise order, starting with
     * the top left corner.
     *
     * @param v1 corner 1
     * @param v2 corner 2
     * @param v3 corner 3
     * @param v4 corner 4
     * @param sprite sprite to show on the face
     * @param shade brightness of the face from 0-1
     */
    public BlockFace(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, AtlasSprite sprite, float shade) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.sprite = sprite;
        this.shade = shade;
    }

    /**
     * Adds this face to the mesh data of a section.
     */
    public void addToMesh(VertexData out) {
        out.putVec3(v1); out.putVec2(sprite.u1, sprite.v1); out.putFloat(shade);
        out.putVec3(v2); out.putVec2(sprite.u1, sprite.v2); out.putFloat(shade);
        out.putVec3(v3); out.putVec2(sprite.u2, sprite.v2); out.putFloat(shade);
        out.putVec3(v4); out.putVec2(sprite.u2, sprite.v1); out.putFloat(shade);
    }

    /**
     * Gets the position of the centroid point of the face.
     *
     * @param ox offset on X axis
     * @param oy offset on Y axis
     * @param oz offset on Z axis
     * @return center position with offset applied
     */
    public Vector3f getCenterPos(int ox, int oy, int oz) {
        return new Vector3f(v1).add(v2).add(v3).add(v4).div(4).add(ox, oy, oz);
    }

    /**
     * Creates a copy of this face that is the opposite side.
     *
     * @return back face of this face
     */
    public BlockFace makeBackFace() {
        return new BlockFace(v4, v3, v2, v1, sprite.flipHorizontally(), shade);
    }
}
