package dev.sergey.arcanecode.guide;

import dev.sergey.arcanecode.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class StarterGuideEvents {
    private StarterGuideEvents() {}

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.ARCANE_CODEX.get())) return;
        }
        var codex = ModItems.ARCANE_CODEX.toStack();
        if (!player.getInventory().add(codex)) player.drop(codex, false);
        player.displayClientMessage(Component.translatable("message.arcanecode.codex_received"), false);
    }
}
