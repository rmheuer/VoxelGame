package com.github.rmheuer.voxel;

import com.github.rmheuer.azalea.input.keyboard.Key;
import com.github.rmheuer.azalea.input.keyboard.KeyPressEvent;
import com.github.rmheuer.azalea.input.keyboard.Keyboard;
import com.github.rmheuer.azalea.input.mouse.MouseButton;
import com.github.rmheuer.azalea.input.mouse.MouseButtonPressEvent;
import com.github.rmheuer.azalea.input.mouse.MouseMoveEvent;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.WindowSettings;
import com.github.rmheuer.azalea.render.camera.Camera;
import com.github.rmheuer.azalea.render.camera.PerspectiveProjection;
import com.github.rmheuer.azalea.render.utils.DebugLineRenderer;
import com.github.rmheuer.azalea.runtime.BaseGame;
import com.github.rmheuer.voxel.level.Blocks;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.level.LightMap;
import com.github.rmheuer.voxel.level.MapSection;
import com.github.rmheuer.voxel.physics.Raycast;
import com.github.rmheuer.voxel.render.LevelRenderData;
import com.github.rmheuer.voxel.render.LevelRenderer;
import org.joml.*;

import java.io.IOException;
import java.lang.Math;

public final class VoxelGame extends BaseGame {
    private static final WindowSettings WINDOW_SETTINGS =
            new WindowSettings(640, 480, "Voxel")
                .setVSync(false);

    private final LevelRenderer levelRenderer;
    private final DebugLineRenderer lineRenderer;

    private final Camera camera;
    private final BlockMap blockMap;
    private final LightMap lightMap;
    private final LevelRenderData levelRenderData;

    private boolean mouseCaptured;
    private Raycast.Result raycastResult;

    private boolean drawSectionBoundaries;
    private boolean drawLightHeights;

    public VoxelGame() throws IOException {
        super(WINDOW_SETTINGS);

        levelRenderer = new LevelRenderer(getRenderer());
        lineRenderer = new DebugLineRenderer(getRenderer());

        camera = new Camera(new PerspectiveProjection((float) Math.toRadians(90), 0.01f, 1000f));

        blockMap = new BlockMap(4, 4, 4);
        lightMap = new LightMap(blockMap.getBlocksX(), blockMap.getBlocksZ());
        levelRenderData = new LevelRenderData(4, 4, 4);

        for (int y = 0; y < 24; y++) {
            for (int z = 0; z < 64; z++) {
                for (int x = 0; x < 64; x++) {
                    blockMap.setBlockId(x, y, z, Blocks.ID_SOLID);
                }
            }
        }
        for (int i = 0; i < 1024; i++) {
            int x = (int) (Math.random() * 64);
            int y = 24 + (int) (Math.random() * 4);
            int z = (int) (Math.random() * 64);
            blockMap.setBlockId(x, y, z, Blocks.ID_SOLID);
        }

        lightMap.recalculateAll(blockMap);

        setMouseCaptured(true);
        getEventBus().addHandler(KeyPressEvent.class, this::keyPressed);
        getEventBus().addHandler(MouseMoveEvent.class, this::mouseMoved);
        getEventBus().addHandler(MouseButtonPressEvent.class, this::mousePressed);

        drawSectionBoundaries = false;
        drawLightHeights = false;
    }

    private void setMouseCaptured(boolean mouseCaptured) {
        this.mouseCaptured = mouseCaptured;
        getWindow().getMouse().setCursorCaptured(mouseCaptured);
    }

    private void keyPressed(KeyPressEvent event) {
        if (event.getKey() == Key.ESCAPE) {
            setMouseCaptured(!mouseCaptured);
        } else if (event.getKey() == Key.F1) {
            drawSectionBoundaries = !drawSectionBoundaries;
        } else if (event.getKey() == Key.F2) {
            drawLightHeights = !drawLightHeights;
        }
    }

    private void mouseMoved(MouseMoveEvent event) {
        if (!mouseCaptured)
            return;

        final float sensitivity = 0.0025f;

        Vector2d delta = event.getCursorDelta();
        float deltaPitch = (float) -delta.y * sensitivity;
        float deltaYaw = (float) -delta.x * sensitivity;

        camera.getTransform().rotation.add(deltaPitch, deltaYaw, 0);
    }

    private void setBlock(Vector3i pos, byte blockId) {
        byte prevId = blockMap.setBlockId(pos.x, pos.y, pos.z, blockId);
        levelRenderData.blockChanged(pos.x, pos.y, pos.z);

        LightMap.Change lightChange = lightMap.blockChanged(blockMap, pos.x, pos.y, pos.z, prevId, blockId);
        if (lightChange != null)
            levelRenderData.lightChanged(pos.x, pos.z, lightChange.prevHeight, lightChange.newHeight);
    }

    private void mousePressed(MouseButtonPressEvent event) {
        if (!mouseCaptured)
            return;

        if (event.getButton() == MouseButton.LEFT) {
            if (raycastResult != null) {
                setBlock(raycastResult.blockPos, Blocks.ID_AIR);
            }
        } else if (event.getButton() == MouseButton.RIGHT) {
            if (raycastResult != null && raycastResult.hitFace != null) {
                Vector3i pos = new Vector3i(raycastResult.blockPos)
                        .add(raycastResult.hitFace.getDirection());

                setBlock(pos, Blocks.ID_SOLID);
            }
        }
    }

    @Override
    protected void tick(float dt) {
        moveCamera(getWindow().getKeyboard(), dt);

        Vector3f pos = camera.getTransform().position;
        Vector3f dir = camera.getTransform().getForward();
        raycastResult = Raycast.raycast(blockMap, pos, dir, 32);
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

        levelRenderer.renderLevel(renderer, blockMap, lightMap, levelRenderData, viewProj);

        if (raycastResult != null) {
            {
                int col = Colors.RGBA.BLUE;
                int x = raycastResult.blockPos.x;
                int y = raycastResult.blockPos.y;
                int z = raycastResult.blockPos.z;
                lineRenderer.addLine(x, y, z, x + 1, y, z, col);
                lineRenderer.addLine(x, y, z, x, y + 1, z, col);
                lineRenderer.addLine(x, y, z, x, y, z + 1, col);
                lineRenderer.addLine(x + 1, y, z, x + 1, y + 1, z, col);
                lineRenderer.addLine(x + 1, y, z, x + 1, y, z + 1, col);
                lineRenderer.addLine(x, y + 1, z, x + 1, y + 1, z, col);
                lineRenderer.addLine(x, y + 1, z, x, y + 1, z + 1, col);
                lineRenderer.addLine(x, y, z + 1, x + 1, y, z + 1, col);
                lineRenderer.addLine(x, y, z + 1, x, y + 1, z + 1, col);
                lineRenderer.addLine(x + 1, y + 1, z + 1, x, y + 1, z + 1, col);
                lineRenderer.addLine(x + 1, y + 1, z + 1, x + 1, y, z + 1, col);
                lineRenderer.addLine(x + 1, y + 1, z + 1, x + 1, y + 1, z, col);
            }
            {
                int col = Colors.RGBA.RED;
                float x = raycastResult.hitPos.x;
                float y = raycastResult.hitPos.y;
                float z = raycastResult.hitPos.z;
                float s = 0.2f;
                lineRenderer.addLine(x - s, y, z, x + s, y, z, col);
                lineRenderer.addLine(x, y - s, z, x, y + s, z, col);
                lineRenderer.addLine(x, y, z - s, x, y, z + s, col);
            }
        }

        if (drawSectionBoundaries) {
            int col = Colors.RGBA.YELLOW;

            int s = MapSection.SIZE;
            int nx = blockMap.getSectionsX() * s;
            int ny = blockMap.getSectionsY() * s;
            int nz = blockMap.getSectionsZ() * s;

            for (int x = 0; x <= nx; x += s) {
                for (int y = 0; y <= ny; y += s) {
                    lineRenderer.addLine(x, y, 0, x, y, nz, col);
                }
            }
            for (int x = 0; x <= nx; x += s) {
                for (int z = 0; z <= nz; z += s) {
                    lineRenderer.addLine(x, 0, z, x, ny, z, col);
                }
            }
            for (int y = 0; y <= ny; y += s) {
                for (int z = 0; z <= nz; z += s) {
                    lineRenderer.addLine(0, y, z, nx, y, z, col);
                }
            }
        }

        if (drawLightHeights) {
            int col = Colors.RGBA.GREEN;
            for (int z = 0; z < blockMap.getBlocksZ(); z++) {
                for (int x = 0; x < blockMap.getBlocksX(); x++) {
                    int lightHeight = lightMap.getLightHeight(x, z);
                    lineRenderer.addLine(x, lightHeight, z, x + 1, lightHeight, z + 1, col);
                    lineRenderer.addLine(x + 1, lightHeight, z, x, lightHeight, z + 1, col);
                }
            }
        }

        lineRenderer.flush(renderer, viewProj);
    }

    @Override
    protected void cleanUp() {
        levelRenderData.close();
        levelRenderer.close();
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
