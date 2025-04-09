package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.utils.SafeCloseable;

public final class SectionRenderData implements SafeCloseable {
    private final SectionRenderLayer opaque;
    private final SectionRenderLayer translucent;

    private boolean meshOutdated;

    public SectionRenderData() {
        opaque = new SectionRenderLayer();
        translucent = new SectionRenderLayer();

        meshOutdated = true;
    }

    public void markOutdated() {
        meshOutdated = true;
    }

    public void updateMeshes(Renderer renderer, SectionMeshes meshes) {
        opaque.updateMesh(renderer, meshes.opaqueData);
        translucent.updateMesh(renderer, meshes.translucentData);
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

    @Override
    public void close() {
        opaque.close();
        translucent.close();
    }
}
