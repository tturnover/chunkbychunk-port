package xyz.immortius.chunkbychunk.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Message above the hotbar, framed by glitching symbols, whose text decodes from fully glitched to readable over
 * {@link #DECODE_TICKS} ticks, one character at a time in a random order.
 */
public final class GlitchMessage {

    private static final int DECODE_TICKS = 40;
    private static final String FRAME = "**";
    private static final Style GLITCHED = Style.EMPTY.withObfuscated(true).withColor(ChatFormatting.DARK_RED);
    private static final Style DECODED = Style.EMPTY.withObfuscated(false).withColor(ChatFormatting.RED);

    private static @Nullable String text;
    private static int @Nullable [] revealOrder;
    private static int ticks;

    private GlitchMessage() {
    }

    /**
     * Starts showing a message
     * @param translationKey The translation key of the message text
     */
    public static void show(String translationKey) {
        String message = I18n.get(translationKey);
        RandomSource random = RandomSource.create();
        // revealOrder[i] is the step at which character i decodes; spaces are never glitched
        List<Integer> characters = new ArrayList<>();
        for (int i = 0; i < message.length(); i++) {
            if (!Character.isWhitespace(message.charAt(i))) {
                characters.add(i);
            }
        }
        int[] order = new int[message.length()];
        for (int step = 0; !characters.isEmpty(); step++) {
            order[characters.remove(random.nextInt(characters.size()))] = step;
        }
        text = message;
        revealOrder = order;
        ticks = 0;
        update(Minecraft.getInstance());
    }

    /**
     * Called at the end of each client tick to advance the animation.
     */
    public static void clientTick(Minecraft minecraft) {
        if (text != null) {
            ticks++;
            update(minecraft);
        }
    }

    private static void update(Minecraft minecraft) {
        if (text == null || revealOrder == null) {
            return;
        }
        int visibleCharacters = (int) text.chars().filter(c -> !Character.isWhitespace(c)).count();
        int revealed = Math.min(visibleCharacters, ticks * visibleCharacters / DECODE_TICKS);
        // Siblings inherit their parent's style, so start from an unstyled root
        MutableComponent message = Component.empty().append(Component.literal(FRAME).withStyle(GLITCHED)).append(Component.literal(" "));
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean decoded = Character.isWhitespace(c) || revealOrder[i] < revealed;
            message.append(Component.literal(String.valueOf(c)).withStyle(decoded ? DECODED : GLITCHED));
        }
        message.append(Component.literal(" ")).append(Component.literal(FRAME).withStyle(GLITCHED));
        minecraft.gui.hud.setOverlayMessage(message, false);
        if (ticks >= DECODE_TICKS) {
            // Fully decoded: leave the final message to fade out normally
            text = null;
            revealOrder = null;
        }
    }
}
