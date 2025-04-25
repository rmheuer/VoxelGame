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
    public static final byte ID_BEDROCK = 7;
    public static final byte ID_FLOWING_WATER = 8;
    public static final byte ID_STILL_WATER = 9;
    public static final byte ID_FLOWING_LAVA = 10;
    public static final byte ID_STILL_LAVA = 11;
    public static final byte ID_SAND = 12;
    public static final byte ID_GRAVEL = 13;
    public static final byte ID_GOLD_ORE = 14;
    public static final byte ID_IRON_ORE = 15;
    public static final byte ID_COAL_ORE = 16;
    public static final byte ID_LOG = 17;
    public static final byte ID_LEAVES = 18;
    public static final byte ID_SPONGE = 19;
    public static final byte ID_GLASS = 20;
    public static final byte ID_CLOTH = 21; // Add 0-15 for different colors
    public static final byte ID_YELLOW_FLOWER = 37;
    public static final byte ID_RED_FLOWER = 38;
    public static final byte ID_BROWN_MUSHROOM = 39;
    public static final byte ID_RED_MUSHROOM = 40;
    public static final byte ID_GOLD_BLOCK = 41;
    public static final byte ID_IRON_BLOCK = 42;
    public static final byte ID_DOUBLE_SLAB = 43;
    public static final byte ID_SLAB = 44;
    public static final byte ID_BRICKS = 45;
    public static final byte ID_TNT = 46;
    public static final byte ID_BOOKSHELF = 47;
    public static final byte ID_MOSSY_STONE = 48;
    public static final byte ID_OBSIDIAN = 49;

    public static final int BLOCK_COUNT = 50;

    private static final Block[] BLOCKS = new Block[BLOCK_COUNT];
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
        BLOCKS[ID_BEDROCK] = new Block(CubeShape.all(new AtlasSprite(1, 1)));

        Block water = new Block(new LiquidShape(new AtlasSprite(14, 0), false, true))
                .setLiquid(Liquid.WATER)
                .setLightBlocking(false)
                .setSolid(false);
        BLOCKS[ID_FLOWING_WATER] = water;
        BLOCKS[ID_STILL_WATER] = water;

        Block lava = new Block(new LiquidShape(new AtlasSprite(14, 1), true, false))
                .setLiquid(Liquid.LAVA)
                .setSolid(false);
        BLOCKS[ID_FLOWING_LAVA] = lava;
        BLOCKS[ID_STILL_LAVA] = lava;

        BLOCKS[ID_SAND] = new Block(CubeShape.all(new AtlasSprite(2, 1)));
        BLOCKS[ID_GRAVEL] = new Block(CubeShape.all(new AtlasSprite(3, 1)));
        BLOCKS[ID_GOLD_ORE] = new Block(CubeShape.all(new AtlasSprite(0, 2)));
        BLOCKS[ID_IRON_ORE] = new Block(CubeShape.all(new AtlasSprite(1, 2)));
        BLOCKS[ID_COAL_ORE] = new Block(CubeShape.all(new AtlasSprite(2, 2)));
        BLOCKS[ID_LOG] = new Block(CubeShape.column(new AtlasSprite(5, 1), new AtlasSprite(4, 1)));
        BLOCKS[ID_LEAVES] = new Block(CubeShape.all(new AtlasSprite(6, 1)).setTransparencyType(CubeShape.TransparencyType.TRANSPARENT))
                .setLightBlocking(false)
                .setParticleGravityScale(0.4f);
        BLOCKS[ID_SPONGE] = new Block(CubeShape.all(new AtlasSprite(0, 3)))
                .setParticleGravityScale(0.9f);
        BLOCKS[ID_GLASS] = new Block(CubeShape.all(new AtlasSprite(1, 3)).setTransparencyType(CubeShape.TransparencyType.TRANSPARENT_OCCLUDE_SELF))
                .setLightBlocking(false);

        for (int color = 0; color < 16; color++) {
            BLOCKS[ID_CLOTH + color] = new Block(CubeShape.all(new AtlasSprite(color, 4)));
        }

        BLOCKS[ID_YELLOW_FLOWER] = new Block(new CrossShape(new AtlasSprite(13, 0)))
                .setLightBlocking(false)
                .setSolid(false);
        BLOCKS[ID_RED_FLOWER] = new Block(new CrossShape(new AtlasSprite(12, 0)))
                .setLightBlocking(false)
                .setSolid(false);
        BLOCKS[ID_BROWN_MUSHROOM] = new Block(new CrossShape(new AtlasSprite(13, 1)))
                .setLightBlocking(false)
                .setSolid(false);
        BLOCKS[ID_RED_MUSHROOM] = new Block(new CrossShape(new AtlasSprite(12, 1)))
                .setLightBlocking(false)
                .setSolid(false);
        BLOCKS[ID_GOLD_BLOCK] = new Block(new CubeShape(new AtlasSprite(8, 1), new AtlasSprite(8, 2), new AtlasSprite(8, 3)));
        BLOCKS[ID_IRON_BLOCK] = new Block(new CubeShape(new AtlasSprite(7, 1), new AtlasSprite(7, 2), new AtlasSprite(7, 3)));
        BLOCKS[ID_DOUBLE_SLAB] = new Block(CubeShape.column(new AtlasSprite(6, 0), new AtlasSprite(5, 0)));
        BLOCKS[ID_SLAB] = new Block(new SlabShape(new AtlasSprite(6, 0), new AtlasSprite(5, 0)));
        BLOCKS[ID_BRICKS] = new Block(CubeShape.all(new AtlasSprite(7, 0)));
        BLOCKS[ID_TNT] = new Block(new CubeShape(new AtlasSprite(9, 0), new AtlasSprite(8, 0), new AtlasSprite(10, 0)));
        BLOCKS[ID_BOOKSHELF] = new Block(CubeShape.column(new AtlasSprite(4, 0), new AtlasSprite(3, 2)));
        BLOCKS[ID_MOSSY_STONE] = new Block(CubeShape.all(new AtlasSprite(4, 2)));
        BLOCKS[ID_OBSIDIAN] = new Block(CubeShape.all(new AtlasSprite(5, 2)));
    }

    public static Block getBlock(byte id) {
        return BLOCKS[id];
    }
}
