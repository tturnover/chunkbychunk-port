package xyz.immortius.chunkbychunk.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.immortius.chunkbychunk.client.GlitchMessage;
import xyz.immortius.chunkbychunk.client.WorldOptionsButton;
import xyz.immortius.chunkbychunk.client.particles.SpawnerArrowParticle;
import xyz.immortius.chunkbychunk.client.screens.BedrockChestScreen;
import xyz.immortius.chunkbychunk.client.screens.WorldForgeScreen;
import xyz.immortius.chunkbychunk.client.screens.WorldMenderScreen;
import xyz.immortius.chunkbychunk.client.screens.WorldScannerScreen;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.network.ConfigSyncPayload;
import xyz.immortius.chunkbychunk.common.network.GlitchMessagePayload;

/**
 * Client-only mod initialization
 */
public class ChunkByChunkClientMod implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(ChunkByChunkConstants.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Client Initializing");
        MenuScreens.register(ChunkByChunkMod.BEDROCK_CHEST_MENU, BedrockChestScreen::new);
        MenuScreens.register(ChunkByChunkMod.WORLD_FORGE_MENU, WorldForgeScreen::new);
        MenuScreens.register(ChunkByChunkMod.WORLD_SCANNER_MENU, WorldScannerScreen::new);
        MenuScreens.register(ChunkByChunkMod.WORLD_MENDER_MENU, WorldMenderScreen::new);

        ParticleProviderRegistry.getInstance().register(ChunkByChunkMod.SPAWNER_ARROW_PARTICLE, SpawnerArrowParticle.Provider::new);
        ClientTickEvents.END_CLIENT_TICK.register(SpawnerArrowParticle::clientTick);
        ClientTickEvents.END_CLIENT_TICK.register(GlitchMessage::clientTick);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            SpriteIconButton button = WorldOptionsButton.afterScreenInit(screen);
            if (button != null) {
                Screens.getWidgets(screen).add(button);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(GlitchMessagePayload.TYPE, (payload, context) -> payload.apply());

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
            LOGGER.info("Receiving config from server");
            payload.apply();
        });
    }

}
