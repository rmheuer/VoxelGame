package com.github.rmheuer.voxel.server;

import com.github.rmheuer.azalea.math.MathUtil;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.level.BlockMap;

public final class LevelGenerator {
    private static int getHeight(int x, int z, int size) {
        double dist = Math.sqrt(MathUtil.square(x - size/2f) + MathUtil.square(z - size/2f));
        double scaled = Math.min(dist / (size / 2.0), 1) * Math.PI;

        return (int) (32 + Math.cos(scaled) * 6 + Math.sin(scaled * 6.87) * 2);
    }

    public static BlockMap generateLevel(int sectionsXZ) {
        BlockMap map = new BlockMap(sectionsXZ, 4, sectionsXZ);

        for (int z = 0; z < map.getBlocksZ(); z++) {
            for (int x = 0; x < map.getBlocksX(); x++) {
                int height = getHeight(x, z, map.getBlocksX());

                for (int y = 0; y < height - 4; y++) {
                    map.setBlockId(x, y, z, Blocks.ID_STONE);
                }
                for (int y = height - 4; y < height - 1; y++) {
                    map.setBlockId(x, y, z, Blocks.ID_DIRT);
                }

                if (height < 32) {
                    map.setBlockId(x, height - 1, z, Blocks.ID_DIRT);
                    for (int y = height; y < 32; y++) {
                        map.setBlockId(x, y, z, Blocks.ID_STILL_WATER);
                    }
                } else {
                    map.setBlockId(x, height - 1, z, Blocks.ID_GRASS);
                }
            }
        }

        return map;
    }
}
