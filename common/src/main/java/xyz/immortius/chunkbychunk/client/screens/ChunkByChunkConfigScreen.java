package xyz.immortius.chunkbychunk.client.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xyz.immortius.chunkbychunk.client.uielements.SettingListWidget;
import xyz.immortius.chunkbychunk.common.util.ConfigUtil;

/**
 * Screen for editing the config. Either edits the default config used by new worlds, or (from the Create World screen)
 * the config of the world being created.
 */
public class ChunkByChunkConfigScreen extends Screen {
    private final Screen lastScreen;
    private final boolean forNewWorld;
    private SettingListWidget settingsList;
    private Button cancelButton;
    private Button resetButton;
    private Button saveButton;

    public ChunkByChunkConfigScreen(Screen lastScreen) {
        this(lastScreen, false);
    }

    private ChunkByChunkConfigScreen(Screen lastScreen, boolean forNewWorld) {
        super(Component.translatable(forNewWorld ? "config.chunkbychunk.worldtitle" : "config.chunkbychunk.title"));
        this.lastScreen = lastScreen;
        this.forNewWorld = forNewWorld;
    }

    /**
     * @param lastScreen The Create World screen
     * @return A screen editing the config of the world being created
     */
    public static ChunkByChunkConfigScreen forNewWorld(Screen lastScreen) {
        ConfigUtil.loadPendingWorldConfig();
        return new ChunkByChunkConfigScreen(lastScreen, true);
    }

    @Override
    protected void init() {
        settingsList = new SettingListWidget(minecraft, this, width, 22, height - 44, (int) (0.9f * width));

        int w = (width / 3 - 60) / 2;
        resetButton = Button.builder(Component.translatable("controls.reset"), button -> {
            settingsList.reset();
        }).pos(w, height - 32).size(60, 20).build();
        cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> {
            ConfigUtil.loadDefaultConfig();
            this.minecraft.gui.setScreen(lastScreen);
        }).pos(width / 3 + w, height - 32).size(60, 20).build();
        saveButton = Button.builder(Component.translatable(forNewWorld ? "gui.done" : "selectWorld.edit.save"), button -> {
            if (forNewWorld) {
                ConfigUtil.storePendingWorldConfig();
                ConfigUtil.loadDefaultConfig();
            } else {
                ConfigUtil.saveDefaultConfig();
            }
            this.minecraft.gui.setScreen(lastScreen);
        }).pos(2 * width / 3 + w, height - 32).size(60, 20).build();

        this.addRenderableWidget(settingsList);
        this.addRenderableWidget(cancelButton);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(resetButton);
    }

    @Override
    public void onClose() {
        ConfigUtil.loadDefaultConfig();
        super.onClose();
        this.minecraft.gui.setScreen(lastScreen);
    }

    @Override
    public void tick() {
        settingsList.tick();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleWidth = font.width(title);
        int titleX = (width - titleWidth) / 2;
        graphics.text(font, title, titleX, 8, 0xFFFFFFFF, true);
    }
}
