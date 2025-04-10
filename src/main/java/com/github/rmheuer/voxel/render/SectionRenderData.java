package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.utils.SafeCloseable;

import java.util.Collections;
import java.util.List;

public final class SectionRenderData implements SafeCloseable {
    private final SectionRenderLayer opaque;
    private final SectionRenderLayer water;
    private List<WaterFace> waterFaces;

    private boolean meshOutdated;

    public SectionRenderData() {
        opaque = new SectionRenderLayer();
        water = new SectionRenderLayer();
        waterFaces = Collections.emptyList();

        meshOutdated = true;
    }

    public void markOutdated() {
        meshOutdated = true;
    }

    public void clearOutdated() {
        meshOutdated = false;
    }

    public SectionRenderLayer getOpaqueLayer() {
        return opaque;
    }

    public SectionRenderLayer getWaterLayer() {
        return water;
    }

    public boolean isMeshOutdated() {
        return meshOutdated;
    }

    public List<WaterFace> getWaterFaces() {
        return waterFaces;
    }

    public void setWaterFaces(List<WaterFace> waterFaces) {
        this.waterFaces = waterFaces;
    }

    @Override
    public void close() {
        opaque.close();
        water.close();
    }
}
