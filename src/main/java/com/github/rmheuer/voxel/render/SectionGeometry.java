package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.render.mesh.VertexData;
import com.github.rmheuer.azalea.utils.SafeCloseable;

import java.util.List;

public final class SectionGeometry implements SafeCloseable {
    public final VertexData opaqueData;
    public final List<WaterFace> waterFaces;

    public SectionGeometry(VertexData opaqueData, List<WaterFace> waterFaces) {
        this.opaqueData = opaqueData;
        this.waterFaces = waterFaces;
    }

    public int getRequiredFaceCount() {
        return Math.max(opaqueData.getVertexCount() / 4, waterFaces.size());
    }

    @Override
    public void close() {
        opaqueData.close();
    }
}
