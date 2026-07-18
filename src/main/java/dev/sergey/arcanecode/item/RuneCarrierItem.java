package dev.sergey.arcanecode.item;

import dev.sergey.arcanecode.spell.RuneEngine;
import dev.sergey.arcanecode.spell.RuneProgramData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RuneCarrierItem extends Item {
    public RuneCarrierItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            var program = RuneProgramData.getProgram(stack);
            if (program.isEmpty()) {
                serverPlayer.displayClientMessage(Component.translatable("message.arcanecode.empty_program"), true);
            } else {
                ItemStack staff = RuneEngine.findStaff(serverPlayer);
                if (staff.isEmpty()) {
                    serverPlayer.displayClientMessage(Component.translatable("message.arcanecode.no_staff"), true);
                } else {
                    RuneEngine.cast(serverPlayer, staff, program);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
