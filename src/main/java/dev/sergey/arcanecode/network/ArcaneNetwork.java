package dev.sergey.arcanecode.network;

import dev.sergey.arcanecode.item.RuneStaffItem;
import dev.sergey.arcanecode.spell.RuneEngine;
import dev.sergey.arcanecode.spell.RuneLibrary;
import dev.sergey.arcanecode.spell.RuneProgramData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Arrays;
import java.util.List;

public final class ArcaneNetwork {
    public static final int SAVE = 0;
    public static final int CAST = 1;

    private ArcaneNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(RuneProgramPayload.TYPE, RuneProgramPayload.STREAM_CODEC, ArcaneNetwork::handle);
    }

    private static void handle(RuneProgramPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        List<String> program = Arrays.stream(payload.program().split(","))
            .filter(id -> !id.isBlank() && RuneLibrary.byId(id) != null)
            .limit(512)
            .toList();
        ItemStack staff = RuneEngine.findStaff(player);
        if (staff.isEmpty() || !(staff.getItem() instanceof RuneStaffItem)) {
            player.displayClientMessage(Component.translatable("message.arcanecode.no_staff"), false);
            return;
        }
        if (payload.action() == SAVE) {
            RuneProgramData.setProgram(staff, program);
            player.displayClientMessage(Component.translatable("message.arcanecode.program_saved", program.size()), false);
        } else if (payload.action() == CAST) {
            RuneEngine.cast(player, staff, program);
        }
    }
}
