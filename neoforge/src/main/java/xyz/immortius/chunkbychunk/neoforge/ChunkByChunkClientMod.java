package xyz.immortius.chunkbychunk.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import xyz.immortius.chunkbychunk.client.GlitchMessage;
import xyz.immortius.chunkbychunk.client.WorldOptionsButton;
import xyz.immortius.chunkbychunk.client.particles.SpawnerArrowParticle;
import xyz.immortius.chunkbychunk.client.screens.BedrockChestScreen;
import xyz.immortius.chunkbychunk.client.screens.ChunkByChunkConfigScreen;
import xyz.immortius.chunkbychunk.client.screens.WorldForgeScreen;
import xyz.immortius.chunkbychunk.client.screens.WorldMenderScreen;
import xyz.immortius.chunkbychunk.client.screens.WorldScannerScreen;

/**
 * Client-only mod initialization
 */
public final class ChunkByChunkClientMod {

    private ChunkByChunkClientMod() {
    }

    public static void init(IEventBus eventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, screen) -> new ChunkByChunkConfigScreen(screen));
        eventBus.addListener(ChunkByChunkClientMod::registerScreens);
        eventBus.addListener(ChunkByChunkClientMod::registerParticles);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            SpawnerArrowParticle.clientTick(Minecraft.getInstance());
            GlitchMessage.clientTick(Minecraft.getInstance());
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            SpriteIconButton button = WorldOptionsButton.afterScreenInit(event.getScreen());
            if (button != null) {
                event.addListener(button);
            }
        });
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ChunkByChunkMod.SPAWNER_ARROW_PARTICLE.get(), SpawnerArrowParticle.Provider::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ChunkByChunkMod.BEDROCK_CHEST_MENU.get(), BedrockChestScreen::new);
        event.register(ChunkByChunkMod.WORLD_FORGE_MENU.get(), WorldForgeScreen::new);
        event.register(ChunkByChunkMod.WORLD_SCANNER_MENU.get(), WorldScannerScreen::new);
        event.register(ChunkByChunkMod.WORLD_MENDER_MENU.get(), WorldMenderScreen::new);
    }
}
