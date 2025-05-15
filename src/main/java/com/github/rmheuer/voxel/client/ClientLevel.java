package com.github.rmheuer.voxel.client;

import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.client.render.LevelRenderData;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.level.LightMap;
import com.github.rmheuer.voxel.level.MapSection;

public final class ClientLevel implements SafeCloseable {
    private final BlockMap blockMap;
    private final LightMap lightMap;

    private final LevelRenderData renderData;
    // private final List<Particle> particles;

    public ClientLevel(int sizeX, int sizeY, int sizeZ, byte[] blockData) {
        if (sizeX % MapSection.SIZE != 0)
            throw new IllegalArgumentException("X dimension is not divisible by section size");
        if (sizeY % MapSection.SIZE != 0)
            throw new IllegalArgumentException("Y dimension is not divisible by section size");
        if (sizeZ % MapSection.SIZE != 0)
            throw new IllegalArgumentException("Z dimension is not divisible by section size");

        int sectionsX = sizeX / MapSection.SIZE;
        int sectionsY = sizeY / MapSection.SIZE;
        int sectionsZ = sizeZ / MapSection.SIZE;

        blockMap = new BlockMap(sectionsX, sectionsY, sectionsZ, blockData);
        lightMap = new LightMap(sizeX, sizeZ);
        lightMap.recalculateAll(blockMap);

        renderData = new LevelRenderData(sectionsX, sectionsY, sectionsZ);
    }

    public BlockMap getBlockMap() {
        return blockMap;
    }

    public LightMap getLightMap() {
        return lightMap;
    }

    public LevelRenderData getRenderData() {
        return renderData;
    }

    @Override
    public void close() {
        renderData.close();
    }
}
