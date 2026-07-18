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

public final class RuneStaffItem extends Item {
    private final int capacity;
    private final double efficiency;

    public RuneStaffItem(Properties properties, int capacity, double efficiency) {
        super(properties);
        this.capacity = capacity;
        this.efficiency = efficiency;
    }

    public int capacity() {
        return capacity;
    }

    public double efficiency() {
        return efficiency;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                var program = RuneProgramData.getProgram(stack);
                if (program.isEmpty()) {
                    serverPlayer.displayClientMessage(Component.translatable("message.arcanecode.empty_program"), true);
                } else {
                    RuneEngine.cast(serverPlayer, stack, program);
                }
            }
        } else if (level.isClientSide()) {
            ClientBridge.openCaster(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
