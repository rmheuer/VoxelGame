package com.github.rmheuer.voxel;

import com.github.rmheuer.azalea.imgui.ImGuiBackend;
import com.github.rmheuer.azalea.input.keyboard.Key;
import com.github.rmheuer.azalea.input.keyboard.Keyboard;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.WindowSettings;
import com.github.rmheuer.azalea.render.camera.Camera;
import com.github.rmheuer.azalea.render.camera.PerspectiveProjection;
import com.github.rmheuer.azalea.runtime.BaseGame;
import com.github.rmheuer.voxel.level.Blocks;
import com.github.rmheuer.voxel.level.Level;
import com.github.rmheuer.voxel.render.LevelRenderData;
import com.github.rmheuer.voxel.render.LevelRenderer;
import imgui.ImGui;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.io.IOException;

public final class VoxelGame extends BaseGame {
    private static final WindowSettings WINDOW_SETTINGS =
            new WindowSettings(640, 480, "Voxel")
                .setVSync(false);

    private final ImGuiBackend imGuiBackend;
    private final LevelRenderer levelRenderer;

    private final Camera camera;
    private final Level level;
    private final LevelRenderData levelRenderData;

    public VoxelGame() throws IOException {
        super(WINDOW_SETTINGS);

        imGuiBackend = new ImGuiBackend(getWindow(), getEventBus());
        levelRenderer = new LevelRenderer(getRenderer());

        camera = new Camera(new PerspectiveProjection((float) Math.toRadians(90), 0.01f, 1000f));

        level = new Level(4, 4, 4);
        levelRenderData = new LevelRenderData(4, 4, 4);

        for (int y = 0; y < 24; y++) {
            for (int z = 0; z < 64; z++) {
                for (int x = 0; x < 64; x++) {
                    level.setBlockId(x, y, z, Blocks.ID_STONE);
                }
            }
        }
        for (int i = 0; i < 1024; i++) {
            int x = (int) (Math.random() * 64);
            int y = 24 + (int) (Math.random() * 4);
            int z = (int) (Math.random() * 64);
            level.setBlockId(x, y, z, Blocks.ID_STONE);
        }
    }

    @Override
    protected void tick(float dt) {
        moveCamera(imGuiBackend.getMaskedKeyboard(), dt);
    }

    private void moveCamera(Keyboard kb, float dt) {
        Vector3f pos = camera.getTransform().position;
        Vector3f rot = camera.getTransform().rotation;

        float move = dt * 20;
        Vector3f forward = new Vector3f(0, 0, -move).rotateY(rot.y);
        Vector3f right = new Vector3f(move, 0, 0).rotateY(rot.y);
        Vector3f up = new Vector3f(0, move, 0);

        if (kb.isKeyPressed(Key.W))
            pos.add(forward);
        if (kb.isKeyPressed(Key.S))
            pos.sub(forward);
        if (kb.isKeyPressed(Key.D))
            pos.add(right);
        if (kb.isKeyPressed(Key.A))
            pos.sub(right);
        if (kb.isKeyPressed(Key.SPACE))
            pos.add(up);
        if (kb.isKeyPressed(Key.LEFT_SHIFT))
            pos.sub(up);

        float turn = (float) (dt * Math.PI);
        if (kb.isKeyPressed(Key.LEFT))
            rot.y += turn;
        if (kb.isKeyPressed(Key.RIGHT))
            rot.y -= turn;
        if (kb.isKeyPressed(Key.UP))
            rot.x += turn;
        if (kb.isKeyPressed(Key.DOWN))
            rot.x -= turn;
    }

    @Override
    protected void render(Renderer renderer) {
        Vector2i windowSize = getWindow().getFramebufferSize();
        Matrix4f proj = camera.getProjectionMatrix(windowSize.x, windowSize.y);
        Matrix4f view = camera.getViewMatrix();
        Matrix4f viewProj = new Matrix4f(proj).mul(view);

        levelRenderer.renderLevel(renderer, level, levelRenderData, viewProj);

        imGuiBackend.beginFrame();
        imGuiBackend.endFrameAndRender();
    }

    @Override
    protected void cleanUp() {
        levelRenderData.close();
        levelRenderer.close();
        imGuiBackend.close();
    }

    public static void main(String[] args) {
        VoxelGame game;
        try {
            game = new VoxelGame();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resources", e);
        }

        game.run();
    }
}
