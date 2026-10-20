package xyz.immortius.chunkbychunk.client.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.menus.BedrockChestMenu;

/**
 * Screen for the Bedrock Chest
 */
public class BedrockChestScreen extends AbstractContainerScreen<BedrockChestMenu> {
    private static final Identifier CONTAINER_TEXTURE = Identifier.parse(ChunkByChunkConstants.MOD_ID + ":textures/gui/container/bedrockchest.png");

    public BedrockChestScreen(BedrockChestMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }
}
