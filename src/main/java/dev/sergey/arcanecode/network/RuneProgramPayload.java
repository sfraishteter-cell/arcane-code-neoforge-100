package dev.sergey.arcanecode.network;

import dev.sergey.arcanecode.ArcaneCode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RuneProgramPayload(String program, int action) implements CustomPacketPayload {
    public static final Type<RuneProgramPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ArcaneCode.MOD_ID, "rune_program"));
    public static final StreamCodec<ByteBuf, RuneProgramPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(30_000), RuneProgramPayload::program,
        ByteBufCodecs.VAR_INT, RuneProgramPayload::action,
        RuneProgramPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
