package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.*;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.shader.ShaderUniform;
import com.github.rmheuer.azalea.render.utils.SharedIndexBuffer;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.level.Blocks;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.level.LightMap;
import com.github.rmheuer.voxel.level.MapSection;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LevelRenderer implements SafeCloseable {
    private static final VertexLayout LAYOUT = new VertexLayout(
            AttribType.VEC3, // Position
            AttribType.COLOR_RGBA, // Color
            AttribType.FLOAT // Shade
    );

    private static final float SHADE_UP = 1.0f;
    private static final float SHADE_FRONT_BACK = 0.9f;
    private static final float SHADE_LEFT_RIGHT = 0.8f;
    private static final float SHADE_DOWN = 0.7f;

    private static final float SHADE_LIT = 1.0f;
    private static final float SHADE_SHADOW = 0.7f;

    private final ShaderProgram shader;
    private final PipelineInfo pipeline;
    private final SharedIndexBuffer sharedIndexBuffer;

    public LevelRenderer(Renderer renderer) throws IOException {
        shader = renderer.createShaderProgram(
                ResourceUtil.readAsStream("vertex.glsl"),
                ResourceUtil.readAsStream("fragment.glsl")
        );
        pipeline = new PipelineInfo(shader)
                .setDepthTest(true)
                .setWinding(FaceWinding.CCW_FRONT)
                .setCullMode(CullMode.BACK)
                .setFillMode(FillMode.FILLED);

        sharedIndexBuffer = new SharedIndexBuffer(
                renderer,
                PrimitiveType.TRIANGLES,
                4,
                0, 1, 2, 0, 2, 3
        );
    }

    private static final class TranslucentSection {
        public final int blockX, blockY, blockZ;
        public final SectionRenderLayer layer;

        public TranslucentSection(int blockX, int blockY, int blockZ, SectionRenderLayer layer) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.layer = layer;
        }
    }

    public void renderLevel(Renderer renderer, BlockMap blockMap, LightMap lightMap, LevelRenderData renderData, Vector3fc cameraPos, Matrix4fc viewProj) {
        int sectionsX = blockMap.getSectionsX();
        int sectionsY = blockMap.getSectionsY();
        int sectionsZ = blockMap.getSectionsZ();

        int updated = 0;
        for (int sectionY = 0; sectionY < sectionsY; sectionY++) {
            for (int sectionZ = 0; sectionZ < sectionsZ; sectionZ++) {
                for (int sectionX = 0; sectionX < sectionsX; sectionX++) {
                    SectionRenderData renderSection = renderData.getSection(sectionX, sectionY, sectionZ);

                    if (renderSection.isMeshOutdated()) {
                        try (SectionMeshes meshes = createSectionMesh(blockMap, lightMap, sectionX, sectionY, sectionZ)) {
                            renderSection.updateMeshes(renderer, meshes);
                            sharedIndexBuffer.ensureCapacity(meshes.getRequiredFaceCount());
                        }
                        updated++;
                    }
                }
            }
        }
        if (updated > 0) {
            System.out.println("Updated " + updated + " section mesh(es)");
        }

        try (ActivePipeline pipe = renderer.bindPipeline(pipeline)) {
            pipe.getUniform("u_ViewProj").setMat4(viewProj);

            ShaderUniform offsetUniform = pipe.getUniform("u_SectionOffset");
            IndexBuffer indexBuffer = sharedIndexBuffer.getIndexBuffer();

            List<TranslucentSection> translucentToRender = new ArrayList<>();
            for (int sectionY = 0; sectionY < sectionsY; sectionY++) {
                for (int sectionZ = 0; sectionZ < sectionsZ; sectionZ++) {
                    for (int sectionX = 0; sectionX < sectionsX; sectionX++) {
                        SectionRenderData section = renderData.getSection(sectionX, sectionY, sectionZ);
                        SectionRenderLayer opaque = section.getOpaqueLayer();
                        SectionRenderLayer translucent = section.getTranslucentLayer();

                        if (translucent.getElementCount() > 0) {
                            translucentToRender.add(new TranslucentSection(
                                    sectionX * MapSection.SIZE,
                                    sectionY * MapSection.SIZE,
                                    sectionZ * MapSection.SIZE,
                                    translucent
                            ));
                        }

                        if (opaque.getElementCount() > 0) {
                            offsetUniform.setVec3(
                                    sectionX * MapSection.SIZE,
                                    sectionY * MapSection.SIZE,
                                    sectionZ * MapSection.SIZE
                            );
                            pipe.draw(opaque.getVertexBuffer(), indexBuffer, 0, opaque.getElementCount());
                        }
                    }
                }
            }

            int halfSz = MapSection.SIZE / 2;
            translucentToRender.sort(Comparator.comparingDouble((section) -> -cameraPos.distanceSquared(
                    section.blockX + halfSz,
                    section.blockY + halfSz,
                    section.blockZ + halfSz
            )));
            for (TranslucentSection section : translucentToRender) {
                offsetUniform.setVec3(section.blockX, section.blockY, section.blockZ);
                pipe.draw(section.layer.getVertexBuffer(), indexBuffer, 0, section.layer.getElementCount());
            }
        }
    }

    private SectionMeshes createSectionMesh(BlockMap blockMap, LightMap lightMap, int sectionX, int sectionY, int sectionZ) {
        MapSection section = blockMap.getSection(sectionX, sectionY, sectionZ);
        VertexData opaqueData = new VertexData(LAYOUT);
        VertexData translucentData = new VertexData(LAYOUT);

        if (section.isEmpty())
            return new SectionMeshes(opaqueData, translucentData);

        MapSection sectionNX = sectionX > 0 ? blockMap.getSection(sectionX - 1, sectionY, sectionZ) : null;
        MapSection sectionNY = sectionY > 0 ? blockMap.getSection(sectionX, sectionY - 1, sectionZ) : null;
        MapSection sectionNZ = sectionZ > 0 ? blockMap.getSection(sectionX, sectionY, sectionZ - 1) : null;
        MapSection sectionPX = sectionX < blockMap.getSectionsX() - 1 ? blockMap.getSection(sectionX + 1, sectionY, sectionZ) : null;
        MapSection sectionPY = sectionY < blockMap.getSectionsY() - 1 ? blockMap.getSection(sectionX, sectionY + 1, sectionZ) : null;
        MapSection sectionPZ = sectionZ < blockMap.getSectionsZ() - 1 ? blockMap.getSection(sectionX, sectionY, sectionZ + 1) : null;

        int ox = sectionX * MapSection.SIZE;
        int oy = sectionY * MapSection.SIZE;
        int oz = sectionZ * MapSection.SIZE;

        for (int y = 0; y < MapSection.SIZE; y++) {
            for (int z = 0; z < MapSection.SIZE; z++) {
                for (int x = 0; x < MapSection.SIZE; x++) {
                    byte block = section.getBlockId(x, y, z);
                    if (block == Blocks.ID_AIR)
                        continue;

                    int color = block == Blocks.ID_SOLID
                            ? Colors.RGBA.WHITE
                            : Colors.RGBA.fromFloats(0.0f, 0.0f, 1.0f, 0.2f);

                    Byte blockNX = x > 0
                            ? Byte.valueOf(section.getBlockId(x - 1, y, z))
                            : (sectionNX != null ? sectionNX.getBlockId(MapSection.SIZE - 1, y, z) : null);
                    Byte blockNY = y > 0
                            ? Byte.valueOf(section.getBlockId(x, y - 1, z))
                            : (sectionNY != null ? sectionNY.getBlockId(x, MapSection.SIZE - 1, z) : null);
                    Byte blockNZ = z > 0
                            ? Byte.valueOf(section.getBlockId(x, y, z - 1))
                            : (sectionNZ != null ? sectionNZ.getBlockId(x, y, MapSection.SIZE - 1) : null);
                    Byte blockPX = x < MapSection.SIZE - 1
                            ? Byte.valueOf(section.getBlockId(x + 1, y, z))
                            : (sectionPX != null ? sectionPX.getBlockId(0, y, z) : null);
                    Byte blockPY = y < MapSection.SIZE - 1
                            ? Byte.valueOf(section.getBlockId(x, y + 1, z))
                            : (sectionPY != null ? sectionPY.getBlockId(x, 0, z) : null);
                    Byte blockPZ = z < MapSection.SIZE - 1
                            ? Byte.valueOf(section.getBlockId(x, y, z + 1))
                            : (sectionPZ != null ? sectionPZ.getBlockId(x, y, 0) : null);

                    int blockX = ox + x;
                    int blockY = oy + y;
                    int blockZ = oz + z;

                    VertexData data = block == Blocks.ID_SOLID
                            ? opaqueData
                            : translucentData;

                    if (blockNX != null && blockNX != block) {
                        float light = lightMap.isLit(blockX - 1, blockY, blockZ) ? SHADE_LIT : SHADE_SHADOW;
                        data.putVec3(x, y + 1, z); data.putColorRGBA(color); data.putFloat(SHADE_LEFT_RIGHT * light);
                        data.putVec3(x, y, z); data.putColorRGBA(color); data.putFloat(SHADE_LEFT_RIGHT * light);
                        data.putVec3(x, y, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_LEFT_RIGHT * light);
                        data.putVec3(x, y + 1, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_LEFT_RIGHT * light);
                    }
                    if (blockNY != null && blockNY != block) {
                        // Bottom face is always in shadow
                        data.putVec3(x + 1, y, z); data.putColorRGBA(color); data.putFloat(SHADE_DOWN * SHADE_SHADOW);
                        data.putVec3(x + 1, y, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_DOWN * SHADE_SHADOW);
                        data.putVec3(x, y, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_DOWN * SHADE_SHADOW);
                        data.putVec3(x, y, z); data.putColorRGBA(color); data.putFloat(SHADE_DOWN * SHADE_SHADOW);
                    }
                    if (blockNZ != null && blockNZ != block) {
                        float light = lightMap.isLit(blockX, blockY, blockZ - 1) ? SHADE_LIT : SHADE_SHADOW;
                        data.putVec3(x + 1, y + 1, z); data.putColorRGBA(color); data.putFloat(SHADE_FRONT_BACK * light);
                        data.putVec3(x + 1, y, z); data.putColorRGBA(color); data.putFloat(SHADE_FRONT_BACK * light);
                        data.putVec3(x, y, z); data.putColorRGBA(color); data.putFloat(SHADE_FRONT_BACK * light);
                        data.putVec3(x, y + 1, z); data.putColorRGBA(color); data.putFloat(SHADE_FRONT_BACK * light);
                    }
                    if (blockPX != null && blockPX != block) {
                        float light = lightMap.isLit(blockX + 1, blockY, blockZ) ? SHADE_LIT : SHADE_SHADOW;
                        data.putVec3(x + 1, y + 1, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_LEFT_RIGHT * light);
                        data.putVec3(x + 1, y, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_LEFT_RIGHT * light);
                        data.putVec3(x + 1, y, z); data.putColorRGBA(color); data.putFloat(SHADE_LEFT_RIGHT * light);
                        data.putVec3(x + 1, y + 1, z); data.putColorRGBA(color); data.putFloat(SHADE_LEFT_RIGHT * light);
                    }
                    if (blockPY == null || blockPY != block) {
                        float light = blockPY == null || lightMap.isLit(blockX, blockY + 1, blockZ) ? SHADE_LIT : SHADE_SHADOW;
                        data.putVec3(x, y + 1, z); data.putColorRGBA(color); data.putFloat(SHADE_UP * light);
                        data.putVec3(x, y + 1, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_UP * light);
                        data.putVec3(x + 1, y + 1, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_UP * light);
                        data.putVec3(x + 1, y + 1, z); data.putColorRGBA(color); data.putFloat(SHADE_UP * light);
                    }
                    if (blockPZ != null && blockPZ != block) {
                        float light = lightMap.isLit(blockX, blockY, blockZ + 1) ? SHADE_LIT : SHADE_SHADOW;
                        data.putVec3(x, y + 1, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_FRONT_BACK * light);
                        data.putVec3(x, y, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_FRONT_BACK * light);
                        data.putVec3(x + 1, y, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_FRONT_BACK * light);
                        data.putVec3(x + 1, y + 1, z + 1); data.putColorRGBA(color); data.putFloat(SHADE_FRONT_BACK * light);
                    }
                }
            }
        }

        return new SectionMeshes(opaqueData, translucentData);
    }

    @Override
    public void close() {
        shader.close();
        sharedIndexBuffer.close();
    }
}
