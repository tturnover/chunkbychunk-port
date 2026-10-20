package xyz.immortius.chunkbychunk.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import xyz.immortius.chunkbychunk.client.screens.ChunkByChunkConfigScreen;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.util.ConfigUtil;

import java.lang.ref.WeakReference;

/**
 * Adds a Chunk By Chunk settings button to the Create World screen, so the settings can be chosen per world.
 */
public final class WorldOptionsButton {

    private static final Identifier SPRITE = Identifier.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "world_options");
    private static final int SIZE = 20;

    private static WeakReference<@Nullable Screen> currentCreateWorldScreen = new WeakReference<>(null);

    private WorldOptionsButton() {
    }

    /**
     * Called after any screen initialises.
     * @param screen The screen
     * @return The button to add to the screen, or null if the screen is not the Create World screen
     */
    public static @Nullable SpriteIconButton afterScreenInit(Screen screen) {
        if (!(screen instanceof CreateWorldScreen)) {
            return null;
        }
        if (currentCreateWorldScreen.get() != screen) {
            // A new world is being set up: start again from the default config
            currentCreateWorldScreen = new WeakReference<>(screen);
            ConfigUtil.clearPendingWorldConfig();
        }
        SpriteIconButton button = SpriteIconButton.builder(Component.translatable("config.chunkbychunk.worldbutton"),
                        b -> Minecraft.getInstance().gui.setScreen(ChunkByChunkConfigScreen.forNewWorld(screen)), true)
                .sprite(SPRITE, 16, 16)
                .size(SIZE, SIZE)
                .withTootip()
                .build();
        // Bottom left, beside the Create New World / Cancel buttons
        button.setPosition(8, screen.height - SIZE - 6);
        return button;
    }
}
