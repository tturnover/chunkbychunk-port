package xyz.immortius.chunkbychunk.client.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.menus.WorldMenderMenu;
import xyz.immortius.chunkbychunk.common.util.SpiralIterator;

/**
 * Screen for the WorldMender.
 */
public class WorldMenderScreen extends AbstractContainerScreen<WorldMenderMenu> {
    public static final Identifier CONTAINER_TEXTURE = Identifier.parse(ChunkByChunkConstants.MOD_ID + ":textures/gui/container/worldmender.png");

    private static final int MAIN_TEXTURE_DIM = 512;
    private static final float TICKS_PER_FRAME = 4f;
    private static final int NUM_FRAMES = 8;
    private static final int HIGHLIGHT_SIZE = 128;
    private static final int HIGHLIGHT_INSET_X = 24;
    private static final int HIGHLIGHT_INSET_Y = 13;

    private static final int[][] NODE_OFFSETS = new int[][] {{3},{5,3},{6,5,3},{7,6,5,3},{7,7,7,6,4},{7,8,8,8,7,4},{7,8,9,9,9,7,5}};

    private float animCounter = 0.f;

    public WorldMenderScreen(WorldMenderMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, 176, 235);
        titleLabelX = 8;
        titleLabelY = 4;
        inventoryLabelX = 8;
        inventoryLabelY = 143;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        animCounter += delta;
        while (animCounter > TICKS_PER_FRAME * NUM_FRAMES) {
            animCounter -= TICKS_PER_FRAME * NUM_FRAMES;
        }

        int frame = Mth.floor(animCounter / TICKS_PER_FRAME);


        int highlightOffsetX = imageWidth + (frame / 4) * HIGHLIGHT_SIZE;
        int highlightOffsetY = HIGHLIGHT_SIZE * (frame % 4);

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight, MAIN_TEXTURE_DIM, MAIN_TEXTURE_DIM);

        SpiralIterator iterator = new SpiralIterator(0,0);
        for (int i = 0; i < menu.getChunksSpawned(); i++) {
            Pos blitPos = getChunkPos(iterator.getX(), iterator.getY());
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, HIGHLIGHT_INSET_X + leftPos + blitPos.x, HIGHLIGHT_INSET_Y + topPos + blitPos.y, highlightOffsetX + blitPos.x(), highlightOffsetY + blitPos.y(), 2, 2, MAIN_TEXTURE_DIM, MAIN_TEXTURE_DIM);
            iterator.next();
        }
    }

    private Pos getChunkPos(int chunkX, int chunkZ) {

        int absX = Mth.abs(chunkX);
        int absY = Mth.abs(chunkZ);
        int sigX = Mth.sign(chunkX);
        int sigY = Mth.sign(chunkZ);

        int xOffset = chunkX * 7 + 6 * sigX + 63;
        int yOffset = chunkZ * 7 + 6 * sigY + 63;

        if (absX > 0 && absY > absX) {
            xOffset -= sigX * NODE_OFFSETS[absY - 2][absX - 1];
        } else if (absY > 0 && absX > absY) {
            yOffset -= sigY * NODE_OFFSETS[absX - 2][absY - 1];
        }

        return new Pos(xOffset, yOffset);
    }

    private record Pos(int x, int y) {
    }

}
