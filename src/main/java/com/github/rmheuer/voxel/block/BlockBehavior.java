package com.github.rmheuer.voxel.block;

import com.github.rmheuer.voxel.level.LevelAccess;

@FunctionalInterface
public interface BlockBehavior {
    void doAction(LevelAccess level, int x, int y, int z, byte blockId);
}
