package xyz.immortius.chunkbychunk.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xyz.immortius.chunkbychunk.client.GlitchMessage;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;

/**
 * Payload sent from server to client to show a translated message above the hotbar that decodes out of glitched text.
 * The animation is done on the client because it needs the text in the client's language.
 */
public record GlitchMessagePayload(String translationKey) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GlitchMessagePayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "glitch_message"));

    public static final StreamCodec<ByteBuf, GlitchMessagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GlitchMessagePayload::translationKey,
            GlitchMessagePayload::new);

    /**
     * Shows the message. Client only.
     */
    public void apply() {
        GlitchMessage.show(translationKey);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
