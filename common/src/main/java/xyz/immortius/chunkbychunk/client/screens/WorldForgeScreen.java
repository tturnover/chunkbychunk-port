package xyz.immortius.chunkbychunk.client.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.menus.WorldForgeMenu;

/**
 * Screen for the World Forge - this is a single input/output style furnace with animated progress arrow
 */
public class WorldForgeScreen extends AbstractContainerScreen<WorldForgeMenu> {
    public static final Identifier CONTAINER_TEXTURE = Identifier.parse(ChunkByChunkConstants.MOD_ID + ":textures/gui/container/worldforge.png");

    public static final float TICKS_PER_FRAME = 2f;
    public static final int NUM_FRAMES = 8;

    private float animCounter = 0.f;

    public WorldForgeScreen(WorldForgeMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        animCounter += delta;
        while (animCounter > TICKS_PER_FRAME * NUM_FRAMES) {
            animCounter -= TICKS_PER_FRAME * NUM_FRAMES;
        }
        int frame = Mth.floor(animCounter / TICKS_PER_FRAME);

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        if (menu.getProgress() > 0)
        {
            int completion = 0;
            int goal = menu.getGoal();
            if (goal > 0) {
                int progress = Math.min(goal, menu.getProgress());
                completion = 30 * progress / goal;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, leftPos + 78, topPos + 37, 176, frame * 11, completion, 11, 256, 256);
        }
    }
}
