package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.utils.SafeCloseable;

import java.util.Collections;
import java.util.List;

public final class SectionRenderData implements SafeCloseable {
    private final SectionRenderLayer opaque;
    private final SectionRenderLayer translucent;
    private List<BlockFace> translucentFaces;

    private boolean meshOutdated;

    public SectionRenderData() {
        opaque = new SectionRenderLayer();
        translucent = new SectionRenderLayer();
        translucentFaces = Collections.emptyList();

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

    public SectionRenderLayer getTranslucentLayer() {
        return translucent;
    }

    public boolean isMeshOutdated() {
        return meshOutdated;
    }

    public List<BlockFace> getTranslucentFaces() {
        return translucentFaces;
    }

    public void setTranslucentFaces(List<BlockFace> translucentFaces) {
        this.translucentFaces = translucentFaces;
    }

    @Override
    public void close() {
        opaque.close();
        translucent.close();
    }
}
