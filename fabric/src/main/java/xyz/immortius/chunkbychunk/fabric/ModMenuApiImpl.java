package xyz.immortius.chunkbychunk.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;
import xyz.immortius.chunkbychunk.client.screens.ChunkByChunkConfigScreen;

public class ModMenuApiImpl implements ModMenuApi {

    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return lastScreen -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.isLocalServer() || minecraft.getConnection() != null) {
                return null;
            } else {
                return new ChunkByChunkConfigScreen(lastScreen);
            }
        };
    }
}