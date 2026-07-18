package dev.sergey.arcanecode.spell;

import dev.sergey.arcanecode.item.RuneStaffItem;
import dev.sergey.arcanecode.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class MediaManager {
    public static final int MEDIA_PER_DUST = 1_000;

    private MediaManager() {}

    public static boolean consume(ServerPlayer player, ItemStack staff, int rawCost) {
        if (!(staff.getItem() instanceof RuneStaffItem runeStaff)) return false;
        int cost = Math.max(0, (int)Math.ceil(rawCost * runeStaff.efficiency()));
        int media = RuneProgramData.getMedia(staff);
        while (media < cost && media < runeStaff.capacity()) {
            int slot = findDust(player);
            if (slot < 0) break;
            player.getInventory().getItem(slot).shrink(1);
            media = Math.min(runeStaff.capacity(), media + MEDIA_PER_DUST);
        }
        if (media < cost) return false;
        RuneProgramData.setMedia(staff, media - cost);
        return true;
    }

    private static int findDust(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.ARCANE_DUST.get())) return slot;
        }
        return -1;
    }
}
