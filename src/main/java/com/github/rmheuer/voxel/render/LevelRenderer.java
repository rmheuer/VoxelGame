package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.math.CubeFace;
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
                .setFillMode(FillMode.FILLED);

        sharedIndexBuffer = new SharedIndexBuffer(
                renderer,
                PrimitiveType.TRIANGLES,
                4,
                0, 1, 2, 0, 2, 3
        );
    }

    private static final class WaterSection {
        public final int blockX, blockY, blockZ;
        public final SectionRenderLayer layer;

        public WaterSection(int blockX, int blockY, int blockZ, SectionRenderLayer layer) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.layer = layer;
        }
    }

    public void renderLevel(Renderer renderer, BlockMap blockMap, LightMap lightMap, LevelRenderData renderData, Vector3fc cameraPos, Matrix4fc viewProj, boolean wireframe) {
        int sectionsX = blockMap.getSectionsX();
        int sectionsY = blockMap.getSectionsY();
        int sectionsZ = blockMap.getSectionsZ();

        boolean cameraMoved = !renderData.getPrevCameraPos().equals(cameraPos);

        int updated = 0;
        for (int sectionY = 0; sectionY < sectionsY; sectionY++) {
            for (int sectionZ = 0; sectionZ < sectionsZ; sectionZ++) {
                for (int sectionX = 0; sectionX < sectionsX; sectionX++) {
                    SectionRenderData renderSection = renderData.getSection(sectionX, sectionY, sectionZ);

                    int ox = sectionX * MapSection.SIZE;
                    int oy = sectionY * MapSection.SIZE;
                    int oz = sectionZ * MapSection.SIZE;

                    boolean updateWater = cameraMoved;
                    if (renderSection.isMeshOutdated()) {
                        try (SectionGeometry geom = createSectionGeometry(blockMap, lightMap, sectionX, sectionY, sectionZ)) {
                            renderSection.getOpaqueLayer().updateMesh(renderer, geom.opaqueData);
                            renderSection.setWaterFaces(geom.waterFaces);
                            sharedIndexBuffer.ensureCapacity(geom.getRequiredFaceCount());
                            renderSection.clearOutdated();
                        }
                        updated++;
                        updateWater = true;
                    }

                    if (updateWater) {
                        List<WaterFace> waterFaces = renderSection.getWaterFaces();
                        waterFaces.sort(Comparator.comparingDouble((face) -> -cameraPos.distanceSquared(face.getCenterPos(ox, oy, oz))));

                        int color = Colors.RGBA.fromFloats(0.3f, 0.3f, 1.0f, 0.6f);
                        try (VertexData data = new VertexData(LAYOUT)) {
                            for (WaterFace face : waterFaces) {
                                meshFace(
                                        data,
                                        lightMap,
                                        face.x, face.y, face.z,
                                        ox + face.x, oy + face.y, oz + face.z,
                                        face.face,
                                        face.botH,
                                        face.topH,
                                        face.depth,
                                        color
                                );
                            }
                            renderSection.getWaterLayer().updateMesh(renderer, data);
                        }
                    }
                }
            }
        }
        if (updated > 0) {
            System.out.println("Updated " + updated + " section mesh(es)");
        }

        pipeline.setFillMode(wireframe ? FillMode.WIREFRAME : FillMode.FILLED);

        List<WaterSection> waterToRender = new ArrayList<>();
        try (ActivePipeline pipe = renderer.bindPipeline(pipeline.setCullMode(CullMode.BACK))) {
            pipe.getUniform("u_ViewProj").setMat4(viewProj);

            ShaderUniform offsetUniform = pipe.getUniform("u_SectionOffset");
            IndexBuffer indexBuffer = sharedIndexBuffer.getIndexBuffer();

            for (int sectionY = 0; sectionY < sectionsY; sectionY++) {
                for (int sectionZ = 0; sectionZ < sectionsZ; sectionZ++) {
                    for (int sectionX = 0; sectionX < sectionsX; sectionX++) {
                        SectionRenderData section = renderData.getSection(sectionX, sectionY, sectionZ);
                        SectionRenderLayer opaque = section.getOpaqueLayer();
                        SectionRenderLayer water = section.getWaterLayer();

                        if (water.getElementCount() > 0) {
                            waterToRender.add(new WaterSection(
                                    sectionX * MapSection.SIZE,
                                    sectionY * MapSection.SIZE,
                                    sectionZ * MapSection.SIZE,
                                    water
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
        }

        try (ActivePipeline pipe = renderer.bindPipeline(pipeline.setCullMode(CullMode.OFF))) {
            pipe.getUniform("u_ViewProj").setMat4(viewProj);

            ShaderUniform offsetUniform = pipe.getUniform("u_SectionOffset");
            IndexBuffer indexBuffer = sharedIndexBuffer.getIndexBuffer();

            int halfSz = MapSection.SIZE / 2;
            waterToRender.sort(Comparator.comparingDouble((section) -> -cameraPos.distanceSquared(
                    section.blockX + halfSz,
                    section.blockY + halfSz,
                    section.blockZ + halfSz
            )));
            for (WaterSection section : waterToRender) {
                offsetUniform.setVec3(section.blockX, section.blockY, section.blockZ);
                pipe.draw(section.layer.getVertexBuffer(), indexBuffer, 0, section.layer.getElementCount());
            }
        }

        renderData.setPrevCameraPos(cameraPos);
    }

    private boolean shouldDraw(byte block, byte neighbor) {
        return neighbor != block && neighbor != Blocks.ID_SOLID;
    }

    private SectionGeometry createSectionGeometry(BlockMap blockMap, LightMap lightMap, int sectionX, int sectionY, int sectionZ) {
        MapSection section = blockMap.getSection(sectionX, sectionY, sectionZ);
        VertexData opaqueData = new VertexData(LAYOUT);
        List<WaterFace> waterFaces = new ArrayList<>();

        if (section.isEmpty())
            return new SectionGeometry(opaqueData, waterFaces);

        MapSection sectionNX = sectionX > 0 ? blockMap.getSection(sectionX - 1, sectionY, sectionZ) : null;
        MapSection sectionNY = sectionY > 0 ? blockMap.getSection(sectionX, sectionY - 1, sectionZ) : null;
        MapSection sectionNZ = sectionZ > 0 ? blockMap.getSection(sectionX, sectionY, sectionZ - 1) : null;
        MapSection sectionPX = sectionX < blockMap.getSectionsX() - 1 ? blockMap.getSection(sectionX + 1, sectionY, sectionZ) : null;
        MapSection sectionPY = sectionY < blockMap.getSectionsY() - 1 ? blockMap.getSection(sectionX, sectionY + 1, sectionZ) : null;
        MapSection sectionPZ = sectionZ < blockMap.getSectionsZ() - 1 ? blockMap.getSection(sectionX, sectionY, sectionZ + 1) : null;

        MapSection sectionPYPX = sectionPY != null && sectionPX != null ? blockMap.getSection(sectionX + 1, sectionY + 1, sectionZ) : null;
        MapSection sectionPYPZ = sectionPY != null && sectionPZ != null ? blockMap.getSection(sectionX, sectionY + 1, sectionZ + 1) : null;
        MapSection sectionPYNX = sectionPY != null && sectionNX != null ? blockMap.getSection(sectionX - 1, sectionY + 1, sectionZ) : null;
        MapSection sectionPYNZ = sectionPY != null && sectionNZ != null ? blockMap.getSection(sectionX, sectionY + 1, sectionZ - 1) : null;

        int ox = sectionX * MapSection.SIZE;
        int oy = sectionY * MapSection.SIZE;
        int oz = sectionZ * MapSection.SIZE;

        for (int y = 0; y < MapSection.SIZE; y++) {
            for (int z = 0; z < MapSection.SIZE; z++) {
                for (int x = 0; x < MapSection.SIZE; x++) {
                    byte block = section.getBlockId(x, y, z);
                    if (block == Blocks.ID_AIR)
                        continue;

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

                    boolean drawNX = blockNX != null && shouldDraw(block, blockNX);
                    boolean drawNY = blockNY != null && shouldDraw(block, blockNY);
                    boolean drawNZ = blockNZ != null && shouldDraw(block, blockNZ);
                    boolean drawPX = blockPX != null && shouldDraw(block, blockPX);
                    boolean drawPY = blockPY == null || shouldDraw(block, blockPY);
                    boolean drawPZ = blockPZ != null && shouldDraw(block, blockPZ);

                    if (block == Blocks.ID_SOLID) {
                        int color = Colors.RGBA.WHITE;

                        if (drawNX)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.NEG_X, color);
                        if (drawPX)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.POS_X, color);
                        if (drawNY)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.NEG_Y, color);
                        if (drawPY)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.POS_Y, color);
                        if (drawNZ)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.NEG_Z, color);
                        if (drawPZ)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.POS_Z, color);
                    } else if (block == Blocks.ID_WATER) {
                        float shortHeight = 14 / 16.0f;

                        boolean tall = blockPY != null && blockPY == Blocks.ID_WATER;

                        if (drawNY)
                            waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_Y, 0, 1, 0));
                        if (!tall)
                            waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_Y, 0, 1, 1 - shortHeight));

                        Byte blockPYPX;
                        if (y < MapSection.SIZE - 1) {
                            blockPYPX = x < MapSection.SIZE - 1
                                    ? Byte.valueOf(section.getBlockId(x + 1, y + 1, z))
                                    : (sectionPX != null ? sectionPX.getBlockId(0, y + 1, z) : null);
                        } else {
                            blockPYPX = x < MapSection.SIZE - 1
                                    ? Byte.valueOf(section.getBlockId(x + 1, 0, z))
                                    : (sectionPYPX != null ? sectionPYPX.getBlockId(0, 0, z) : null);
                        }
                        if (blockPX != null && blockPX != Blocks.ID_SOLID) {
                            if (blockPX == Blocks.ID_WATER) {
                                boolean neighborTall = blockPYPX != null && blockPYPX == Blocks.ID_WATER;

                                if (tall && !neighborTall) {
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_X, shortHeight, 1, 0));
                                }
                            } else {
                                if (tall)
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_X, 0, 1, 0));
                                else
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_X, 0, shortHeight, 0));
                            }
                        }

                        Byte blockPYNX;
                        if (y < MapSection.SIZE - 1) {
                            blockPYNX = x > 0
                                    ? Byte.valueOf(section.getBlockId(x - 1, y + 1, z))
                                    : (sectionNX != null ? sectionNX.getBlockId(MapSection.SIZE - 1, y + 1, z) : null);
                        } else {
                            blockPYNX = x > 0
                                    ? Byte.valueOf(section.getBlockId(x - 1, 0, z))
                                    : (sectionPYNX != null ? sectionPYNX.getBlockId(MapSection.SIZE - 1, 0, z) : null);
                        }
                        if (blockNX != null && blockNX != Blocks.ID_SOLID) {
                            if (blockNX == Blocks.ID_WATER) {
                                boolean neighborTall = blockPYNX != null && blockPYNX == Blocks.ID_WATER;

                                if (tall && !neighborTall) {
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_X, shortHeight, 1, 0));
                                }
                            } else {
                                if (tall)
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_X, 0, 1, 0));
                                else
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_X, 0, shortHeight, 0));
                            }
                        }

                        Byte blockPYPZ;
                        if (y < MapSection.SIZE - 1) {
                            blockPYPZ = z < MapSection.SIZE - 1
                                    ? Byte.valueOf(section.getBlockId(x, y + 1, z + 1))
                                    : (sectionPZ != null ? sectionPZ.getBlockId(x, y + 1, 0) : null);
                        } else {
                            blockPYPZ = z < MapSection.SIZE - 1
                                    ? Byte.valueOf(section.getBlockId(x, 0, z + 1))
                                    : (sectionPYPZ != null ? sectionPYPZ.getBlockId(x, 0, 0) : null);
                        }
                        if (blockPZ != null && blockPZ != Blocks.ID_SOLID) {
                            if (blockPZ == Blocks.ID_WATER) {
                                boolean neighborTall = blockPYPZ != null && blockPYPZ == Blocks.ID_WATER;

                                if (tall && !neighborTall) {
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_Z, shortHeight, 1, 0));
                                }
                            } else {
                                if (tall)
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_Z, 0, 1, 0));
                                else
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_Z, 0, shortHeight, 0));
                            }
                        }

                        Byte blockPYNZ;
                        if (y < MapSection.SIZE - 1) {
                            blockPYNZ = z > 0
                                    ? Byte.valueOf(section.getBlockId(x, y + 1, z - 1))
                                    : (sectionNZ != null ? sectionNZ.getBlockId(x, y + 1, MapSection.SIZE - 1) : null);
                        } else {
                            blockPYNZ = z > 0
                                    ? Byte.valueOf(section.getBlockId(x, 0, z - 1))
                                    : (sectionPYNZ != null ? sectionPYNZ.getBlockId(x, 0, MapSection.SIZE - 1) : null);
                        }
                        if (blockNZ != null && blockNZ != Blocks.ID_SOLID) {
                            if (blockNZ == Blocks.ID_WATER) {
                                boolean neighborTall = blockPYNZ != null && blockPYNZ == Blocks.ID_WATER;

                                if (tall && !neighborTall) {
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_Z, shortHeight, 1, 0));
                                }
                            } else {
                                if (tall)
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_Z, 0, 1, 0));
                                else
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_Z, 0, shortHeight, 0));
                            }
                        }
                    }
                }
            }
        }

        return new SectionGeometry(opaqueData, waterFaces);
    }

    private void meshFace(VertexData data, LightMap lightMap, int x, int y, int z, int blockX, int blockY, int blockZ, CubeFace face, int color) {
        meshFace(data, lightMap, x, y, z, blockX, blockY, blockZ, face, 0, 1, 0, color);
    }

    // TODO: Make this method not horrible
    private void meshFace(VertexData data, LightMap lightMap, int x, int y, int z, int blockX, int blockY, int blockZ, CubeFace face, float botH, float topH, float depth, int color) {
        float x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;
        float faceShade;
        switch (face) {
            case POS_X:
                x1 = x + 1 - depth; y1 = y + topH; z1 = z + 1;
                x2 = x + 1 - depth; y2 = y + botH; z2 = z + 1;
                x3 = x + 1 - depth; y3 = y + botH; z3 = z;
                x4 = x + 1 - depth; y4 = y + topH; z4 = z;
                faceShade = SHADE_LEFT_RIGHT;
                break;
            case NEG_X:
                x1 = x + depth; y1 = y + topH; z1 = z;
                x2 = x + depth; y2 = y + botH; z2 = z;
                x3 = x + depth; y3 = y + botH; z3 = z + 1;
                x4 = x + depth; y4 = y + topH; z4 = z + 1;
                faceShade = SHADE_LEFT_RIGHT;
                break;
            case POS_Y:
                x1 = x;     y1 = y + 1 - depth; z1 = z;
                x2 = x;     y2 = y + 1 - depth; z2 = z + 1;
                x3 = x + 1; y3 = y + 1 - depth; z3 = z + 1;
                x4 = x + 1; y4 = y + 1 - depth; z4 = z;
                faceShade = SHADE_UP;
                break;
            case NEG_Y:
                x1 = x + 1; y1 = y + depth; z1 = z;
                x2 = x + 1; y2 = y + depth; z2 = z + 1;
                x3 = x;     y3 = y + depth; z3 = z + 1;
                x4 = x;     y4 = y + depth; z4 = z;
                faceShade = SHADE_DOWN;
                break;
            case POS_Z:
                x1 = x;     y1 = y + topH; z1 = z + 1 - depth;
                x2 = x;     y2 = y + botH; z2 = z + 1 - depth;
                x3 = x + 1; y3 = y + botH; z3 = z + 1 - depth;
                x4 = x + 1; y4 = y + topH; z4 = z + 1 - depth;
                faceShade = SHADE_FRONT_BACK;
                break;
            case NEG_Z:
                x1 = x + 1; y1 = y + topH; z1 = z + depth;
                x2 = x + 1; y2 = y + botH; z2 = z + depth;
                x3 = x;     y3 = y + botH; z3 = z + depth;
                x4 = x;     y4 = y + topH; z4 = z + depth;
                faceShade = SHADE_FRONT_BACK;
                break;
            default:
                throw new IllegalArgumentException();
        }

        float lightShade = lightMap.isLit(blockX + face.x, blockY + face.y, blockZ + face.z)
                ? SHADE_LIT
                : SHADE_SHADOW;

        float shade = faceShade * lightShade;
        data.putVec3(x1, y1, z1); data.putColorRGBA(color); data.putFloat(shade);
        data.putVec3(x2, y2, z2); data.putColorRGBA(color); data.putFloat(shade);
        data.putVec3(x3, y3, z3); data.putColorRGBA(color); data.putFloat(shade);
        data.putVec3(x4, y4, z4); data.putColorRGBA(color); data.putFloat(shade);
    }

    @Override
    public void close() {
        shader.close();
        sharedIndexBuffer.close();
    }
}
