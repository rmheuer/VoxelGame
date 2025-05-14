package com.github.rmheuer.voxel.block;

import com.github.rmheuer.voxel.client.render.AtlasSprite;

/**
 * Definitions of all block types.
 */
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

    /** Total number of blocks */
    public static final int BLOCK_COUNT = 50;

    private static final Block[] BLOCKS = new Block[BLOCK_COUNT];
    static {
        register(new Block(ID_AIR, new InvisibleShape())
                .setLightBlocking(false)
                .setSolid(false)
                .setInteractable(false));

        register(new Block(ID_STONE, CubeShape.all(new AtlasSprite(1, 0))));
        register(new Block(ID_GRASS, new CubeShape(new AtlasSprite(0, 0), new AtlasSprite(3, 0), new AtlasSprite(2, 0))));
        register(new Block(ID_DIRT, CubeShape.all(new AtlasSprite(2, 0))));
        register(new Block(ID_COBBLESTONE, CubeShape.all(new AtlasSprite(0, 1))));
        register(new Block(ID_PLANKS, CubeShape.all(new AtlasSprite(4, 0))));
        register(new Block(ID_SAPLING, new CrossShape(new AtlasSprite(15, 0)))
                .setLightBlocking(false)
                .setSolid(false));
        register(new Block(ID_BEDROCK, CubeShape.all(new AtlasSprite(1, 1))));
        register(new Block(ID_FLOWING_WATER, new LiquidShape(new AtlasSprite(14, 0), false, true))
                .setLiquid(Liquid.WATER)
                .setSolid(false)
                .setInteractable(false));
        register(new Block(ID_STILL_WATER, new LiquidShape(new AtlasSprite(14, 0), false, true))
                .setLiquid(Liquid.WATER)
                .setSolid(false)
                .setInteractable(false));
        register(new Block(ID_FLOWING_LAVA, new LiquidShape(new AtlasSprite(14, 1), true, false))
                .setLiquid(Liquid.LAVA)
                .setSolid(false)
                .setInteractable(false));
        register(new Block(ID_STILL_LAVA, new LiquidShape(new AtlasSprite(14, 1), true, false))
                .setLiquid(Liquid.LAVA)
                .setSolid(false)
                .setInteractable(false));
        register(new Block(ID_SAND, CubeShape.all(new AtlasSprite(2, 1))));
        register(new Block(ID_GRAVEL, CubeShape.all(new AtlasSprite(3, 1))));
        register(new Block(ID_GOLD_ORE, CubeShape.all(new AtlasSprite(0, 2))));
        register(new Block(ID_IRON_ORE, CubeShape.all(new AtlasSprite(1, 2))));
        register(new Block(ID_COAL_ORE, CubeShape.all(new AtlasSprite(2, 2))));
        register(new Block(ID_LOG, CubeShape.column(new AtlasSprite(5, 1), new AtlasSprite(4, 1))));
        register(new Block(ID_LEAVES, CubeShape.all(new AtlasSprite(6, 1)).setTransparencyType(CubeShape.TransparencyType.TRANSPARENT))
                .setLightBlocking(false)
                .setParticleGravityScale(0.4f));
        register(new Block(ID_SPONGE, CubeShape.all(new AtlasSprite(0, 3)))
                .setParticleGravityScale(0.9f));
        register(new Block(ID_GLASS, CubeShape.all(new AtlasSprite(1, 3)).setTransparencyType(CubeShape.TransparencyType.TRANSPARENT_OCCLUDE_SELF))
                .setLightBlocking(false));

        for (int color = 0; color < 16; color++) {
            register(new Block((byte) (ID_CLOTH + color), CubeShape.all(new AtlasSprite(color, 4))));
        }

        register(new Block(ID_YELLOW_FLOWER, new CrossShape(new AtlasSprite(13, 0)))
                .setLightBlocking(false)
                .setSolid(false));
        register(new Block(ID_RED_FLOWER, new CrossShape(new AtlasSprite(12, 0)))
                .setLightBlocking(false)
                .setSolid(false));
        register(new Block(ID_BROWN_MUSHROOM, new CrossShape(new AtlasSprite(13, 1)))
                .setLightBlocking(false)
                .setSolid(false));
        register(new Block(ID_RED_MUSHROOM, new CrossShape(new AtlasSprite(12, 1)))
                .setLightBlocking(false)
                .setSolid(false));
        register(new Block(ID_GOLD_BLOCK, new CubeShape(new AtlasSprite(8, 1), new AtlasSprite(8, 2), new AtlasSprite(8, 3))));
        register(new Block(ID_IRON_BLOCK, new CubeShape(new AtlasSprite(7, 1), new AtlasSprite(7, 2), new AtlasSprite(7, 3))));
        register(new Block(ID_DOUBLE_SLAB, CubeShape.column(new AtlasSprite(6, 0), new AtlasSprite(5, 0)))
                .setItemId(ID_SLAB));
        register(new Block(ID_SLAB, new SlabShape(new AtlasSprite(6, 0), new AtlasSprite(5, 0))));
        register(new Block(ID_BRICKS, CubeShape.all(new AtlasSprite(7, 0))));
        register(new Block(ID_TNT, new CubeShape(new AtlasSprite(9, 0), new AtlasSprite(8, 0), new AtlasSprite(10, 0))));
        register(new Block(ID_BOOKSHELF, CubeShape.column(new AtlasSprite(4, 0), new AtlasSprite(3, 2))));
        register(new Block(ID_MOSSY_STONE, CubeShape.all(new AtlasSprite(4, 2))));
        register(new Block(ID_OBSIDIAN, CubeShape.all(new AtlasSprite(5, 2))));
    }

    private static void register(Block block) {
        BLOCKS[block.getId()] = block;
    }

    /**
     * Gets the block properties for the specified block ID.
     *
     * @param id block ID to look up
     * @return properties of the block
     */
    public static Block getBlock(byte id) {
        return BLOCKS[id];
    }
}
