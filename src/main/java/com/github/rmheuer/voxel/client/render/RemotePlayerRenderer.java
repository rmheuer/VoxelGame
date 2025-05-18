package com.github.rmheuer.voxel.client.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.ActivePipeline;
import com.github.rmheuer.azalea.render.pipeline.PipelineInfo;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.client.ClientLevel;
import com.github.rmheuer.voxel.client.RemotePlayer;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.Collection;

public final class RemotePlayerRenderer implements SafeCloseable {
    private static final VertexLayout VERTEX_LAYOUT = new VertexLayout(
            AttribType.VEC3, // Position
            AttribType.VEC2, // UV
            AttribType.FLOAT // Shade
    );

    private final ShaderProgram shader;
    private final PipelineInfo pipeline;
    private final Texture2D skinTexture;
    private final Mesh mesh;

    public RemotePlayerRenderer(Renderer renderer) throws IOException {
        shader = renderer.createShaderProgram(
                ResourceUtil.readAsStream("shaders/player_vertex.glsl"),
                ResourceUtil.readAsStream("shaders/fragment.glsl")
        );
        pipeline = new PipelineInfo(shader)
                .setDepthTest(true);

        skinTexture = renderer.createTexture2D(ResourceUtil.readAsStream("char.png"));

        mesh = renderer.createMesh();
    }

    public void renderPlayers(Renderer renderer, ClientLevel level, Collection<RemotePlayer> players, Matrix4f view, Matrix4f proj, FogInfo fogInfo, float subtick) {
        try (MeshData data = new MeshData(VERTEX_LAYOUT, PrimitiveType.TRIANGLES)) {
            for (RemotePlayer player : players) {
                meshPlayer(level, player, data, subtick);
            }

            mesh.setData(data, DataUsage.STREAM);
        }

        try (ActivePipeline pipe = renderer.bindPipeline(pipeline)) {
            pipe.bindTexture(0, skinTexture);
            pipe.getUniform("u_View").setMat4(view);
            pipe.getUniform("u_Proj").setMat4(proj);
            pipe.getUniform("u_FogStart").setFloat(fogInfo.minDistance);
            pipe.getUniform("u_FogEnd").setFloat(fogInfo.maxDistance);
            pipe.getUniform("u_FogColor").setVec4(fogInfo.color);
            pipe.getUniform("u_TintColor").setVec4(fogInfo.tintColor);

            pipe.draw(mesh);
        }
    }

    private void meshPlayer(ClientLevel level, RemotePlayer player, MeshData data, float subtick) {
        int blockX = (int) Math.floor(player.getPosition().x);
        int blockY = (int) Math.floor(player.getPosition().y);
        int blockZ = (int) Math.floor(player.getPosition().z);
        float light = level.getLightMap().isLit(blockX, blockY, blockZ)
                ? LightingConstants.SHADE_LIT
                : LightingConstants.SHADE_SHADOW;

        Matrix4fStack stack = new Matrix4fStack(10);
        stack.translate(player.getSmoothedPosition(subtick));
        stack.scale(1 / 16.0f);

        stack.rotateY(player.getSmoothedYaw(subtick));

        // Body
        stack.pushMatrix();
        stack.translate(-4, 12, -2);
        meshCuboid(stack, data, 8, 12, 4, 16, 16, light);
        stack.popMatrix();

        // Head
        stack.pushMatrix();
        stack.translate(0, 24, 0);
        stack.rotateX(player.getSmoothedPitch(subtick));
        stack.translate(-4, 0, -4);
        meshCuboid(stack, data, 8, 8, 8, 0, 0, light);
        stack.popMatrix();

        float rotScale = player.getMovementScale(subtick);

        // Legs
        stack.pushMatrix();
        stack.translate(-4, 12, 0);
        stack.pushMatrix();
        stack.rotateX(rotScale * ((float) Math.random() * 2 - 1f) * (float) Math.PI / 2);
        stack.translate(0, -12, -2);
        meshCuboid(stack, data, 4, 12, 4, 0, 16, light);
        stack.popMatrix();
        stack.translate(4, 0, 0);
        stack.pushMatrix();
        stack.rotateX(rotScale * ((float) Math.random() * 2 - 1f) * (float) Math.PI / 2);
        stack.translate(0, -12, -2);
        meshCuboid(stack, data, 4, 12, 4, 0, 16, light);
        stack.popMatrix();
        stack.popMatrix();

        // Arms
        stack.pushMatrix();
        stack.translate(-6, 22, 0);
        stack.pushMatrix();
        stack.rotateZ(rotScale * (float) -(Math.random() * Math.PI / 2));
        stack.rotateX(rotScale * (float) ((Math.random() * 2 - 1f) * Math.PI / 2));
        stack.translate(-2, -10, -2);
        meshCuboid(stack, data, 4, 12, 4, 40, 16, light);
        stack.popMatrix();
        stack.translate(12, 0, 0);
        stack.pushMatrix();
        stack.rotateZ(rotScale * (float) (Math.random() * Math.PI / 2));
        stack.rotateX(rotScale * (float) ((Math.random() * 2 - 1f) * Math.PI / 2));
        stack.translate(-2, -10, -2);
        meshCuboid(stack, data, 4, 12, 4, 40, 16, light);
        stack.popMatrix();
        stack.popMatrix();
    }

    private void meshCuboid(Matrix4fStack stack, MeshData data, int w, int h, int d, int tx, int ty, float light) {
        Vector3f nnn = stack.transformPosition(new Vector3f(0, 0, 0));
        Vector3f nnp = stack.transformPosition(new Vector3f(0, 0, d));
        Vector3f npn = stack.transformPosition(new Vector3f(0, h, 0));
        Vector3f npp = stack.transformPosition(new Vector3f(0, h, d));
        Vector3f pnn = stack.transformPosition(new Vector3f(w, 0, 0));
        Vector3f pnp = stack.transformPosition(new Vector3f(w, 0, d));
        Vector3f ppn = stack.transformPosition(new Vector3f(w, h, 0));
        Vector3f ppp = stack.transformPosition(new Vector3f(w, h, d));

        data.putIndices(
                0, 1, 2, 1, 2, 3,
                2, 3, 4, 3, 4, 5,
                4, 5, 6, 5, 6, 7,
                6, 7, 8, 7, 8, 9,
                2, 4, 10, 4, 10, 11,
                12, 13, 14, 13, 14, 15
        );

        putVertex(data, ppp, tx, ty + d, light);
        putVertex(data, pnp, tx, ty + d + h, light);
        putVertex(data, ppn, tx + d, ty + d, light);
        putVertex(data, pnn, tx + d, ty + d + h, light);
        putVertex(data, npn, tx + d + w, ty + d, light);
        putVertex(data, nnn, tx + d + w, ty + d + h, light);
        putVertex(data, npp, tx + d * 2 + w, ty + d, light);
        putVertex(data, nnp, tx + d * 2 + w, ty + d + h, light);
        putVertex(data, ppp, tx + (d + w) * 2, ty + d, light);
        putVertex(data, pnp, tx + (d + w) * 2, ty + h + d, light);
        putVertex(data, ppp, tx + d, ty, 1);
        putVertex(data, npp, tx + d + w, ty, 1);
        putVertex(data, nnp, tx + d + w, ty, 1);
        putVertex(data, pnp, tx + d * 2 + w, ty, 1);
        putVertex(data, nnn, tx + d + w, ty + d, 1);
        putVertex(data, pnn, tx + d * 2 + w, ty + d, 1);
    }

    private void putVertex(MeshData data, Vector3f pos, int u, int v, float shade) {
        data.putVec3(pos);
        data.putVec2(u / 64.0f, v / 32.0f);
        data.putFloat(shade);
    }

    @Override
    public void close() {
        mesh.close();
        skinTexture.close();
        shader.close();
    }
}
