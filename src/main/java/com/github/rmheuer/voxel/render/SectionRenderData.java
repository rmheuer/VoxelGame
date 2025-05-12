package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.utils.SafeCloseable;

import java.util.Collections;
import java.util.List;

public final class SectionRenderData implements SafeCloseable {
    private final SectionRenderLayer opaque;
    private final SectionRenderLayer translucent;
    private List<BlockFace> translucentFaces;
    private SectionVisibility visibility;

    private boolean meshOutdated;

    public SectionRenderData() {
        opaque = new SectionRenderLayer();
        translucent = new SectionRenderLayer();
        translucentFaces = Collections.emptyList();
        visibility = null;

        meshOutdated = true;
    }

    public void markOutdated() {
        meshOutdated = true;
    }

    public void clearOutdated() {
        meshOutdated = false;
    }

    public void markVisibilityOutdated() {
        visibility = null;
    }

    public void updateVisibility(SectionVisibility visibility) {
        this.visibility = visibility;
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

    public boolean isVisibilityOutdated() {
        return visibility == null;
    }

    public List<BlockFace> getTranslucentFaces() {
        return translucentFaces;
    }

    public void setTranslucentFaces(List<BlockFace> translucentFaces) {
        this.translucentFaces = translucentFaces;
    }

    public SectionVisibility getVisibility() {
        return visibility;
    }

    @Override
    public void close() {
        opaque.close();
        translucent.close();
    }
}
