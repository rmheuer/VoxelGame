package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.client.render.AtlasSprite;
import com.github.rmheuer.voxel.client.render.SectionContext;
import com.github.rmheuer.voxel.client.render.SectionGeometry;

/**
 * Represents the physical shape of a block.
 */
public interface BlockShape {
    /**
     * Adds this block to a render section mesh at the specified position. This
     * should handle occlusion culling of each block face if possible.
     *
     * @param ctx surrounding context of the block
     * @param block the block itself
     * @param x x coordinate of the block within the section
     * @param y y coordinate of the block within the section
     * @param z z coordinate of the block within the section
     * @param geom section mesh geometry to add faces into
     */
    void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom);

    /**
     * Gets the type of occlusion for the specified face.
     *
     * @param face face to get occlusion for
     * @return type of occlusion for that face
     */
    OcclusionType getOcclusion(CubeFace face);

    /**
     * Gets the default bounding box for blocks with this shape.
     *
     * @return default bounding box
     */
    AABB getDefaultBoundingBox();

    /**
     * Gets the sprite from the block atlas that should be used for breaking
     * particles.
     */
    AtlasSprite getParticleSprite();
}
