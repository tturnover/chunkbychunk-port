package xyz.immortius.chunkbychunk.client.uielements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.config.ChunkByChunkConfig;
import xyz.immortius.chunkbychunk.config.system.*;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A widget providing a scrollable list of configuration options for ChunkByChunk
 */
public class SettingListWidget extends ContainerObjectSelectionList<SettingListWidget.SettingEntry> {

    private static final int VALID_TEXT_COLOR = 0xFFE0E0E0;
    private static final int INVALID_TEXT_COLOR = 0xFFFF0000;

    private int rowWidth;

    public SettingListWidget(Minecraft minecraft, Screen parent, int width, int top, int bottom, int rowWidth) {
        super(minecraft, width, bottom - top, top, 24);

        this.rowWidth = rowWidth;
        ConfigMetadata metadata = MetadataBuilder.build(ChunkByChunkConfig.class);
        ChunkByChunkConfig defaultConfig = new ChunkByChunkConfig();

        for (SectionMetadata section : metadata.getSections().values()) {
            Object defaultSection = section.getSectionObject(defaultConfig);
            Object configSection = section.getSectionObject(ChunkByChunkConfig.get());
            this.addEntry(new SectionTitleEntry(section.getDisplayName()));
            for (FieldMetadata<?> field : section.getFields().values()) {
                if (field instanceof BooleanFieldMetadata boolField) {
                    Boolean defaultValue = boolField.getValue(defaultSection);
                    this.addEntry(new BooleanEntry(field.getDisplayName(),
                            () -> boolField.getValue(configSection),
                            (x) -> boolField.setValue(configSection, x), defaultValue));
                } else if (field instanceof EnumFieldMetadata enumField) {
                    Enum<?> defaultValue = enumField.getValue(defaultSection);
                    this.addEntry(new EnumEntry(enumField.getDisplayName(), enumField.enumType(),
                            () -> enumField.getValue(configSection),
                            (x) -> enumField.setValue(configSection, x), defaultValue));
                } else if (field instanceof IntFieldMetadata intField) {
                    Integer defaultValue = intField.getValue(defaultSection);
                    if (intField.getMaxValue() - intField.getMinValue() > 256) {
                        this.addEntry(new ExtendedIntegerEntry(field.getDisplayName(), intField.getMinValue(), intField.getMaxValue(),
                                () -> intField.getValue(configSection),
                                (x) -> intField.setValue(configSection, x), defaultValue));
                    } else {
                        this.addEntry(new IntegerEntry(field.getDisplayName(), intField.getMinValue(), intField.getMaxValue(),
                                () -> intField.getValue(configSection),
                                (x) -> intField.setValue(configSection, x), defaultValue));
                    }
                } else if (field instanceof StringFieldMetadata stringField) {
                   this.addEntry(new StringEntry(field.getDisplayName(), () -> stringField.getValue(configSection), (x) -> stringField.setValue(configSection, x), stringField.getValue(defaultSection)));
                } else {
                    ChunkByChunkConstants.LOGGER.info("Skipping config option {} as type not supported", field.getName());
                }
            }
        }
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }

    @Override
    protected int scrollBarX() {
        return this.width / 2 + getRowWidth() / 2 + 4;
    }

    public void tick() {
        this.children().forEach(SettingEntry::tick);
    }

    public void reset() {
        children().forEach(SettingEntry::reset);
    }

    public class SectionTitleEntry extends SettingEntry {

        private final Component displayName;

        public SectionTitleEntry(Component displayName) {
            this.displayName = displayName;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            graphics.text(SettingListWidget.this.minecraft.font, this.displayName, getContentX() + 12, getContentY() + 8, 0xFFFFFFFF, true);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }
    }

    public class IntegerEntry extends AbstractWidgetEntry<IntegerSlider> {

        private final Integer defaultValue;
        private final Consumer<Integer> setter;

        public IntegerEntry(Component displayName, int min, int max, Supplier<Integer> getter, Consumer<Integer> setter, Integer defaultValue) {
            super(new IntegerSlider(0, 0, getRowWidth(), 20, displayName, min, max, getter, setter));
            this.setter = setter;
            this.defaultValue = defaultValue;
        }

        @Override
        public void reset() {
            setter.accept(defaultValue);
            widget.setValue(defaultValue);
        }
    }

    public class StringEntry extends LabelledWidgetEntry {

        private final Consumer<String> setter;
        private final String defaultValue;

        public StringEntry(Component displayName, Supplier<String> getter, Consumer<String> setter, String defaultValue) {
            super(displayName, new EditBox(SettingListWidget.this.minecraft.font, 0, 0, getRowWidth(), 20, displayName));
            this.defaultValue = defaultValue;
            this.setter = setter;
            widget.setValue(getter.get());
            widget.setEditable(true);
            widget.setResponder(setter);
        }

        @Override
        public void reset() {
            setter.accept(defaultValue);
            widget.setValue(defaultValue);
        }
    }

    public class BooleanEntry extends AbstractWidgetEntry<CycleButton<Boolean>> {

        private final Boolean defaultValue;
        private final Consumer<Boolean> setter;

        public BooleanEntry(Component displayName, Supplier<Boolean> getter, Consumer<Boolean> setter, Boolean defaultValue) {
            super(CycleButton.<Boolean>builder((x) -> Component.translatable((x) ? "gui.yes" : "gui.no"), getter)
                    .withValues(Boolean.FALSE, Boolean.TRUE)
                    .create(0, 0, getRowWidth(), 20, displayName, (cycleButton, value) -> setter.accept(value)));
            this.defaultValue = defaultValue;
            this.setter = setter;
        }

        @Override
        public void reset() {
            setter.accept(defaultValue);
            widget.setValue(defaultValue);
        }
    }

    public class ExtendedIntegerEntry extends LabelledWidgetEntry {
        private final Integer defaultValue;
        private final Consumer<Integer> setter;

        public ExtendedIntegerEntry(Component displayName, int min, int max, Supplier<Integer> getter, Consumer<Integer> setter, Integer defaultValue) {
            super(displayName, new EditBox(SettingListWidget.this.minecraft.font, 0, 0, getRowWidth(), 20, displayName));
            this.defaultValue = defaultValue;
            this.setter = setter;
            widget.setValue(getter.get().toString());
            widget.setEditable(true);
            widget.setResponder(value -> {
                if (value.isEmpty() || "-".equals(value)) {
                    widget.setTextColor(VALID_TEXT_COLOR);
                    if (0 >= min && 0 <= max) {
                        setter.accept(0);
                    }
                    return;
                }
                try {
                    int val = Integer.parseInt(value);
                    if (val >= min && val <= max) {
                        widget.setTextColor(VALID_TEXT_COLOR);
                        setter.accept(val);
                        return;
                    }
                } catch (NumberFormatException ignored) {
                }
                widget.setTextColor(INVALID_TEXT_COLOR);
            });
        }

        @Override
        public void reset() {
            setter.accept(defaultValue);
            widget.setValue(defaultValue.toString());
        }
    }

    public class EnumEntry extends AbstractWidgetEntry<CycleButton<Enum<?>>> {

        private final Enum<?> defaultValue;
        private final Consumer<Enum<?>> setter;

        public EnumEntry(Component displayName, Class<? extends Enum<?>> type, Supplier<Enum<?>> getter, Consumer<Enum<?>> setter, Enum<?> defaultValue) {
            super(CycleButton.<Enum<?>>builder((x) -> Component.translatable("enumvalue.chunkbychunk." + type.getSimpleName() + "." + x.name()), getter)
                    .withValues(type.getEnumConstants())
                    .create(0, 0, getRowWidth(), 20, displayName, (cycleButton, value) -> setter.accept(value)));
            this.defaultValue = defaultValue;
            this.setter = setter;
        }

        @Override
        public void reset() {
            setter.accept(defaultValue);
            widget.setValue(defaultValue);
        }
    }

    /**
     * An entry with a label to the left of an edit box
     */
    public abstract class LabelledWidgetEntry extends AbstractWidgetEntry<EditBox> {
        private final Component displayName;

        public LabelledWidgetEntry(Component displayName, EditBox widget) {
            super(widget);
            this.displayName = displayName;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int labelLength = SettingListWidget.this.minecraft.font.width(this.displayName);
            graphics.text(SettingListWidget.this.minecraft.font, this.displayName, getContentX(), getContentY() + 6, 0xFFFFFFFF, true);
            widget.setX(getContentX() + labelLength + 6);
            widget.setWidth(getRowWidth() - labelLength - 6);
            widget.setY(getContentY());
            widget.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    public abstract class AbstractWidgetEntry<T extends AbstractWidget> extends SettingEntry {

        protected final T widget;

        public AbstractWidgetEntry(T widget) {
            this.widget = widget;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            widget.setX(getContentX());
            widget.setY(getContentY());
            widget.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(widget);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(widget);
        }
    }

    public static abstract class SettingEntry extends Entry<SettingEntry> {

        public void tick() {
        }

        public void reset() {
        }
    }

}
