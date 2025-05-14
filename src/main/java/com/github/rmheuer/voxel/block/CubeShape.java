package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.client.render.AtlasSprite;
import com.github.rmheuer.voxel.client.render.LightingConstants;
import com.github.rmheuer.voxel.client.render.SectionContext;
import com.github.rmheuer.voxel.client.render.SectionGeometry;

/**
 * Standard cube shaped block.
 */
public final class CubeShape implements BlockShape {
    /**
     * Type of transparency this block has
     */
    public enum TransparencyType {
        /** Fully occludes blocks behind */
        OPAQUE,
        /** Never occludes blocks behind */
        TRANSPARENT,
        /** Only occludes neighbors if they are the same block */
        TRANSPARENT_OCCLUDE_SELF
    }

    private static final CubeFaceTemplate[] CUBE_TEMPLATES = {
            new CubeFaceTemplate(CubeFace.POS_X, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, LightingConstants.SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.NEG_X, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, LightingConstants.SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.POS_Y, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, LightingConstants.SHADE_UP),
            new CubeFaceTemplate(CubeFace.NEG_Y, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, LightingConstants.SHADE_DOWN),
            new CubeFaceTemplate(CubeFace.POS_Z, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, LightingConstants.SHADE_FRONT_BACK),
            new CubeFaceTemplate(CubeFace.NEG_Z, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, LightingConstants.SHADE_FRONT_BACK)
    };

    /**
     * Creates a cube shape with the same sprite on all faces.
     *
     * @param sprite sprite for all faces
     * @return new shape
     */
    public static CubeShape all(AtlasSprite sprite) {
        return new CubeShape(sprite, sprite, sprite);
    }

    /**
     * Creates a cube shape with the same sprite on top and bottom, with a
     * different sprite on the sides.
     *
     * @param face sprite for top and bottom faces
     * @param side sprite for side faces
     * @return new shape
     */
    public static CubeShape column(AtlasSprite face, AtlasSprite side) {
        return new CubeShape(face, side, face);
    }

    private final AtlasSprite topSprite, sideSprite, bottomSprite;
    private TransparencyType transparencyType;

    /**
     * Creates a cube shape with the specified sprites on each face. The side
     * sprite will be used for breaking particles.
     *
     * @param topSprite sprite for the top face
     * @param sideSprite sprite for the side faces
     * @param bottomSprite sprite for the bottom face
     */
    public CubeShape(AtlasSprite topSprite, AtlasSprite sideSprite, AtlasSprite bottomSprite) {
        this.topSprite = topSprite;
        this.sideSprite = sideSprite;
        this.bottomSprite = bottomSprite;
        transparencyType = TransparencyType.OPAQUE;
    }

    /**
     * Sets the type of transparency this block has. The default transparency is
     * {@link TransparencyType#OPAQUE}.
     *
     * @param transparencyType new transparency type.
     * @return this
     */
    public CubeShape setTransparencyType(TransparencyType transparencyType) {
        this.transparencyType = transparencyType;
        return this;
    }

    @Override
    public void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom) {
        for (CubeFaceTemplate faceTemplate : CUBE_TEMPLATES) {
            int nx = x + faceTemplate.face.x;
            int ny = y + faceTemplate.face.y;
            int nz = z + faceTemplate.face.z;

            Block neighbor = ctx.getSurroundingBlock(nx, ny, nz);
            if (neighbor == null && faceTemplate.face != CubeFace.POS_Y)
                continue;
            if (neighbor != null && neighbor.getOcclusion(faceTemplate.face.getReverse()) == OcclusionType.FULL)
                continue;
            if (neighbor == block && transparencyType == TransparencyType.TRANSPARENT_OCCLUDE_SELF)
                continue;

            boolean lit = ctx.isLit(nx, ny, nz);
            float lightShade = lit ? LightingConstants.SHADE_LIT : LightingConstants.SHADE_SHADOW;

            AtlasSprite sprite = sideSprite;
            if (faceTemplate.face == CubeFace.POS_Y)
                sprite = topSprite;
            else if (faceTemplate.face == CubeFace.NEG_Y)
                sprite = bottomSprite;

            geom.addFace(true, faceTemplate.makeFace(x, y, z, sprite, lightShade));
        }
    }

    @Override
    public OcclusionType getOcclusion(CubeFace face) {
        return transparencyType == TransparencyType.OPAQUE
                ? OcclusionType.FULL
                : OcclusionType.NONE;
    }

    @Override
    public AABB getDefaultBoundingBox() {
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    @Override
    public AtlasSprite getParticleSprite() {
        return sideSprite;
    }
}
