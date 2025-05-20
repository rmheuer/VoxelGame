package com.github.rmheuer.voxel.level;

public interface LevelAccess {
    byte getBlockId(int x, int y, int z);

    void setBlockId(int x, int y, int z, byte id);

    void setBlockIdNoNeighborUpdates(int x, int y, int z, byte id);

    void updateNeighbors(int x, int y, int z);
}
