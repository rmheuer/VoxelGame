package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.ActivePipeline;
import com.github.rmheuer.azalea.render.pipeline.CullMode;
import com.github.rmheuer.azalea.render.pipeline.FaceWinding;
import com.github.rmheuer.azalea.render.pipeline.PipelineInfo;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.utils.SharedIndexBuffer;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.level.Blocks;
import com.github.rmheuer.voxel.level.Level;
import com.github.rmheuer.voxel.level.LevelSection;
import org.joml.Matrix4fc;

import java.io.IOException;

public final class LevelRenderer implements SafeCloseable {
    private static final VertexLayout LAYOUT = new VertexLayout(
            AttribType.VEC3, // Position
            AttribType.FLOAT // Shade
    );

    private static final float SHADE_UP = 1.0f;
    private static final float SHADE_FRONT_BACK = 0.9f;
    private static final float SHADE_LEFT_RIGHT = 0.8f;
    private static final float SHADE_DOWN = 0.7f;

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
                .setCullMode(CullMode.BACK);

        sharedIndexBuffer = new SharedIndexBuffer(
                renderer,
                PrimitiveType.TRIANGLES,
                4,
                0, 1, 2, 0, 2, 3
        );
    }

    public void renderLevel(Renderer renderer, Level level, LevelRenderData renderData, Matrix4fc viewProj) {
        int sectionsX = level.getSectionsX();
        int sectionsY = level.getSectionsY();
        int sectionsZ = level.getSectionsZ();

        for (int sectionY = 0; sectionY < sectionsY; sectionY++) {
            for (int sectionZ = 0; sectionZ < sectionsZ; sectionZ++) {
                for (int sectionX = 0; sectionX < sectionsX; sectionX++) {
                    SectionRenderData renderSection = renderData.getSection(sectionX, sectionY, sectionZ);

                    if (renderSection.isMeshOutdated()) {
                        try (VertexData data = createSectionMesh(level, sectionX, sectionY, sectionZ)) {
                            renderSection.updateMesh(renderer, data);
                            sharedIndexBuffer.ensureCapacity(data.getVertexCount() / 4);
                        }
                    }
                }
            }
        }

        try (ActivePipeline pipe = renderer.bindPipeline(pipeline)) {
            pipe.getUniform("u_ViewProj").setMat4(viewProj);

            IndexBuffer indexBuffer = sharedIndexBuffer.getIndexBuffer();
            for (int sectionY = 0; sectionY < sectionsY; sectionY++) {
                for (int sectionZ = 0; sectionZ < sectionsZ; sectionZ++) {
                    for (int sectionX = 0; sectionX < sectionsX; sectionX++) {
                        SectionRenderData section = renderData.getSection(sectionX, sectionY, sectionZ);

                        int elementCount = section.getElementCount();
                        if (elementCount > 0) {
                            pipe.draw(section.getVertexBuffer(), indexBuffer, 0, elementCount);
                        }
                    }
                }
            }
        }
    }

    private VertexData createSectionMesh(Level level, int sectionX, int sectionY, int sectionZ) {
        LevelSection section = level.getSection(sectionX, sectionY, sectionZ);
        VertexData data = new VertexData(LAYOUT);

        if (section.isEmpty())
            return data;

        LevelSection sectionNX = sectionX > 0 ? level.getSection(sectionX - 1, sectionY, sectionZ) : null;
        LevelSection sectionNY = sectionY > 0 ? level.getSection(sectionX, sectionY - 1, sectionZ) : null;
        LevelSection sectionNZ = sectionZ > 0 ? level.getSection(sectionX, sectionY, sectionZ - 1) : null;
        LevelSection sectionPX = sectionX < level.getSectionsX() - 1 ? level.getSection(sectionX + 1, sectionY, sectionZ) : null;
        LevelSection sectionPY = sectionY < level.getSectionsY() - 1 ? level.getSection(sectionX, sectionY + 1, sectionZ) : null;
        LevelSection sectionPZ = sectionZ < level.getSectionsZ() - 1 ? level.getSection(sectionX, sectionY, sectionZ + 1) : null;

        for (int y = 0; y < LevelSection.SIZE; y++) {
            for (int z = 0; z < LevelSection.SIZE; z++) {
                for (int x = 0; x < LevelSection.SIZE; x++) {
                    byte block = section.getBlockId(x, y, z);
                    if (block == Blocks.ID_AIR)
                        continue;

                    Byte blockNX = x > 0
                            ? Byte.valueOf(section.getBlockId(x - 1, y, z))
                            : (sectionNX != null ? sectionNX.getBlockId(LevelSection.SIZE - 1, y, z) : null);
                    Byte blockNY = y > 0
                            ? Byte.valueOf(section.getBlockId(x, y - 1, z))
                            : (sectionNY != null ? sectionNY.getBlockId(x, LevelSection.SIZE - 1, z) : null);
                    Byte blockNZ = z > 0
                            ? Byte.valueOf(section.getBlockId(x, y, z - 1))
                            : (sectionNZ != null ? sectionNZ.getBlockId(x, y, LevelSection.SIZE - 1) : null);
                    Byte blockPX = x < LevelSection.SIZE - 1
                            ? Byte.valueOf(section.getBlockId(x + 1, y, z))
                            : (sectionPX != null ? sectionPX.getBlockId(0, y, z) : null);
                    Byte blockPY = y < LevelSection.SIZE - 1
                            ? Byte.valueOf(section.getBlockId(x, y + 1, z))
                            : (sectionPY != null ? sectionPY.getBlockId(x, 0, z) : null);
                    Byte blockPZ = z < LevelSection.SIZE - 1
                            ? Byte.valueOf(section.getBlockId(x, y, z + 1))
                            : (sectionPZ != null ? sectionPZ.getBlockId(x, y, 0) : null);

                    if (blockNX != null && blockNX == Blocks.ID_AIR) {
                        data.putVec3(x, y + 1, z); data.putFloat(SHADE_LEFT_RIGHT);
                        data.putVec3(x, y, z); data.putFloat(SHADE_LEFT_RIGHT);
                        data.putVec3(x, y, z + 1); data.putFloat(SHADE_LEFT_RIGHT);
                        data.putVec3(x, y + 1, z + 1); data.putFloat(SHADE_LEFT_RIGHT);
                    }
                    if (blockNY != null && blockNY == Blocks.ID_AIR) {
                        data.putVec3(x + 1, y, z); data.putFloat(SHADE_DOWN);
                        data.putVec3(x + 1, y, z + 1); data.putFloat(SHADE_DOWN);
                        data.putVec3(x, y, z + 1); data.putFloat(SHADE_DOWN);
                        data.putVec3(x, y, z); data.putFloat(SHADE_DOWN);
                    }
                    if (blockNZ != null && blockNZ == Blocks.ID_AIR) {
                        data.putVec3(x + 1, y + 1, z); data.putFloat(SHADE_FRONT_BACK);
                        data.putVec3(x + 1, y, z); data.putFloat(SHADE_FRONT_BACK);
                        data.putVec3(x, y, z); data.putFloat(SHADE_FRONT_BACK);
                        data.putVec3(x, y + 1, z); data.putFloat(SHADE_FRONT_BACK);
                    }
                    if (blockPX != null && blockPX == Blocks.ID_AIR) {
                        data.putVec3(x + 1, y + 1, z + 1); data.putFloat(SHADE_LEFT_RIGHT);
                        data.putVec3(x + 1, y, z + 1); data.putFloat(SHADE_LEFT_RIGHT);
                        data.putVec3(x + 1, y, z); data.putFloat(SHADE_LEFT_RIGHT);
                        data.putVec3(x + 1, y + 1, z); data.putFloat(SHADE_LEFT_RIGHT);
                    }
                    if (blockPY != null && blockPY == Blocks.ID_AIR) {
                        data.putVec3(x, y + 1, z); data.putFloat(SHADE_UP);
                        data.putVec3(x, y + 1, z + 1); data.putFloat(SHADE_UP);
                        data.putVec3(x + 1, y + 1, z + 1); data.putFloat(SHADE_UP);
                        data.putVec3(x + 1, y + 1, z); data.putFloat(SHADE_UP);
                    }
                    if (blockPZ != null && blockPZ == Blocks.ID_AIR) {
                        data.putVec3(x, y + 1, z + 1); data.putFloat(SHADE_FRONT_BACK);
                        data.putVec3(x, y, z + 1); data.putFloat(SHADE_FRONT_BACK);
                        data.putVec3(x + 1, y, z + 1); data.putFloat(SHADE_FRONT_BACK);
                        data.putVec3(x + 1, y + 1, z + 1); data.putFloat(SHADE_FRONT_BACK);
                    }
                }
            }
        }

        return data;
    }

    @Override
    public void close() {
        shader.close();
        sharedIndexBuffer.close();
    }
}
