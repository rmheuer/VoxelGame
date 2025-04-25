package com.github.rmheuer.voxel.block;

import com.github.rmheuer.voxel.render.AtlasSprite;

public final class Blocks {
    public static final byte ID_AIR = 0;
    public static final byte ID_STONE = 1;
    public static final byte ID_GRASS = 2;
    public static final byte ID_DIRT = 3;
    public static final byte ID_COBBLESTONE = 4;
    public static final byte ID_PLANKS = 5;
    public static final byte ID_SAPLING = 6;

    public static final byte ID_LOG = 17;
    public static final byte ID_LEAVES = 18;

    public static final byte ID_GLASS = 20;

    private static final Block[] BLOCKS = new Block[256];
    static {
        BLOCKS[ID_AIR] = new Block(new InvisibleShape())
                .setLightBlocking(false)
                .setSolid(false)
                .setInteractable(false);
        BLOCKS[ID_STONE] = new Block(CubeShape.all(new AtlasSprite(1, 0)));
        BLOCKS[ID_GRASS] = new Block(new CubeShape(new AtlasSprite(0, 0), new AtlasSprite(3, 0), new AtlasSprite(2, 0)));
        BLOCKS[ID_DIRT] = new Block(CubeShape.all(new AtlasSprite(2, 0)));
        BLOCKS[ID_COBBLESTONE] = new Block(CubeShape.all(new AtlasSprite(0, 1)));
        BLOCKS[ID_PLANKS] = new Block(CubeShape.all(new AtlasSprite(4, 0)));
        BLOCKS[ID_SAPLING] = new Block(new CrossShape(new AtlasSprite(15, 0)))
                .setLightBlocking(false)
                .setSolid(false);
        BLOCKS[ID_LOG] = new Block(CubeShape.column(new AtlasSprite(5, 1), new AtlasSprite(4, 1)));
        BLOCKS[ID_LEAVES] = new Block(CubeShape.all(new AtlasSprite(6, 1)).setTransparencyType(CubeShape.TransparencyType.TRANSPARENT))
                .setLightBlocking(false)
                .setParticleGravityScale(0.4f);
        BLOCKS[ID_GLASS] = new Block(CubeShape.all(new AtlasSprite(1, 3)).setTransparencyType(CubeShape.TransparencyType.TRANSPARENT_OCCLUDE_SELF))
                .setLightBlocking(false);
    }

    public static Block getBlock(byte id) {
        return BLOCKS[id];
    }
}
