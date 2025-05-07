package com.github.rmheuer.voxel.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.block.Blocks;
import org.joml.Vector2i;

import java.util.function.Consumer;

import static com.github.rmheuer.voxel.block.Blocks.*;

public final class BlockPickerUI {
    private static final String TITLE = "Select block";
    private static final byte[][] BLOCKS = {
            {ID_STONE, ID_COBBLESTONE, ID_BRICKS, ID_DIRT, ID_PLANKS, ID_LOG, ID_LEAVES, ID_GLASS, ID_SLAB},
            {ID_MOSSY_STONE, ID_SAPLING, ID_YELLOW_FLOWER, ID_RED_FLOWER, ID_BROWN_MUSHROOM, ID_RED_MUSHROOM, ID_SAND, ID_GRAVEL, ID_SPONGE},
            {ID_CLOTH, ID_CLOTH + 1, ID_CLOTH + 2, ID_CLOTH + 3, ID_CLOTH + 4, ID_CLOTH + 5, ID_CLOTH + 6, ID_CLOTH + 7, ID_CLOTH + 8},
            {ID_CLOTH + 9, ID_CLOTH + 10, ID_CLOTH + 11, ID_CLOTH + 12, ID_CLOTH + 13, ID_CLOTH + 14, ID_CLOTH + 15, ID_COAL_ORE, ID_IRON_ORE},
            {ID_GOLD_ORE, ID_IRON_BLOCK, ID_GOLD_BLOCK, ID_BOOKSHELF, ID_TNT, ID_OBSIDIAN}
    };

    private static final int BG_COLOR_1 = Colors.RGBA.fromInts(5, 5, 0, 96);
    private static final int BG_COLOR_2 = Colors.RGBA.fromInts(48, 48, 96, 160);
    private static final int HIGHLIGHT_COLOR = Colors.RGBA.fromInts(192, 192, 192);
    private static final int ITEM_SPACING = 28;

    private static final int WIDTH = ITEM_SPACING * (9 - 1) + 32;
    private static final int HEIGHT = ITEM_SPACING * (5 - 1) + 48;

    private final Consumer<Byte> blockPickedCallback;
    private int blocksOriginX, blocksOriginY;

    public BlockPickerUI(Consumer<Byte> blockPickedCallback) {
        this.blockPickedCallback = blockPickedCallback;
    }

    public void draw(UIDrawList draw, Vector2i mousePos) {
        int cornerX = draw.getWidth() / 2 - WIDTH / 2;
        int cornerY = (int) ((draw.getHeight() - HEIGHT) * 0.3);

        draw.drawRectVGradient(cornerX, cornerY, WIDTH, HEIGHT, BG_COLOR_1, BG_COLOR_2);
        draw.drawTextCentered(draw.getWidth() / 2, cornerY + 16, TITLE);

        blocksOriginX = cornerX + 8;
        blocksOriginY = cornerY + 24;
        for (int row = 0; row < 5; row++) {
            byte[] blocksInRow = BLOCKS[row];
            for (int col = 0; col < blocksInRow.length; col++) {
                Block block = Blocks.getBlock(blocksInRow[col]);

                int x = blocksOriginX + col * ITEM_SPACING;
                int y = blocksOriginY + row * ITEM_SPACING;
                int size = 16;

                boolean hovered = mousePos.x >= x - 4 && mousePos.x < x + size + 4
                        && mousePos.y >= y - 4 && mousePos.y < y + size + 4;
                if (hovered) {
                    size = 24;
                    x -= 4;
                    y -= 4;
                    draw.drawRect(x - 2, y - 2, 28, 28, HIGHLIGHT_COLOR);
                }

                draw.drawBlockAsItem(x, y, size, size, block);
            }
        }
    }

    public void mouseClicked(Vector2i mousePos) {
        for (int row = 0; row < 5; row++) {
            byte[] blocksInRow = BLOCKS[row];
            for (int col = 0; col < blocksInRow.length; col++) {
                int x = blocksOriginX + col * ITEM_SPACING;
                int y = blocksOriginY + row * ITEM_SPACING;
                int size = 16;

                boolean clicked = mousePos.x >= x - 4 && mousePos.x < x + size + 4
                        && mousePos.y >= y - 4 && mousePos.y < y + size + 4;

                if (clicked) {
                    blockPickedCallback.accept(blocksInRow[col]);
                    return;
                }
            }
        }
    }
}
