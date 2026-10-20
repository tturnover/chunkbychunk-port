package xyz.immortius.chunkbychunk.common.util;

import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.config.ChunkByChunkConfig;
import xyz.immortius.chunkbychunk.config.system.ConfigSystem;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility methods for working with Config
 */
public final class ConfigUtil {
    private ConfigUtil() {

    }

    private static final ConfigSystem system = new ConfigSystem();

    /**
     * Loads the default (not world specific) config.
     */
    public static void loadDefaultConfig() {
        synchronized (system) {
            system.synchConfig(Paths.get(ChunkByChunkConstants.DEFAULT_CONFIG_PATH).resolve(ChunkByChunkConstants.CONFIG_FILE), ChunkByChunkConfig.get());
        }
    }

    /**
     * Saves the default (not world specific) config.
     */
    public static void saveDefaultConfig() {
        synchronized (system) {
            system.write(Paths.get(ChunkByChunkConstants.DEFAULT_CONFIG_PATH).resolve(ChunkByChunkConstants.CONFIG_FILE), ChunkByChunkConfig.get());
        }
    }

    /**
     * Config chosen on the Create World screen for the world being created, applied when its server starts
     */
    private static volatile @Nullable String pendingWorldConfig;

    /**
     * Stores the current config as the config for the world being created.
     */
    public static void storePendingWorldConfig() {
        synchronized (system) {
            StringWriter writer = new StringWriter();
            system.write(writer, ChunkByChunkConfig.get());
            pendingWorldConfig = writer.toString();
        }
    }

    /**
     * Loads the config for the world being created, or the default config if none has been chosen.
     */
    public static void loadPendingWorldConfig() {
        String pending = pendingWorldConfig;
        if (pending == null) {
            loadDefaultConfig();
            return;
        }
        synchronized (system) {
            system.readInto(new BufferedReader(new StringReader(pending)), ChunkByChunkConfig.get());
        }
    }

    /**
     * Forgets any config chosen for a world being created.
     */
    public static void clearPendingWorldConfig() {
        pendingWorldConfig = null;
    }

    /**
     * Writes the config chosen for the world being created to the new world's config file, if it does not have one yet.
     * @param worldConfigFile The world's config file
     */
    public static void applyPendingWorldConfig(Path worldConfigFile) {
        String pending = pendingWorldConfig;
        pendingWorldConfig = null;
        if (pending == null || Files.exists(worldConfigFile)) {
            return;
        }
        try {
            Files.createDirectories(worldConfigFile.getParent());
            try (Writer writer = Files.newBufferedWriter(worldConfigFile)) {
                writer.write(pending);
            }
        } catch (IOException e) {
            ChunkByChunkConstants.LOGGER.error("Failed to write world config to {}", worldConfigFile, e);
        }
    }
}
