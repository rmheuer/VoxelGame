package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.render.mesh.VertexData;
import com.github.rmheuer.azalea.utils.SafeCloseable;

public final class SectionMeshes implements SafeCloseable {
    public final VertexData opaqueData;
    public final VertexData translucentData;

    public SectionMeshes(VertexData opaqueData, VertexData translucentData) {
        this.opaqueData = opaqueData;
        this.translucentData = translucentData;
    }

    public int getRequiredFaceCount() {
        return Math.max(opaqueData.getVertexCount(), translucentData.getVertexCount()) / 4;
    }

    @Override
    public void close() {
        opaqueData.close();
        translucentData.close();
    }
}
