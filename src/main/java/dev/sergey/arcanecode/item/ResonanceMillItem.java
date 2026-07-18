package dev.sergey.arcanecode.item;

import dev.sergey.arcanecode.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A compact manual mill. The ingredient is held in the opposite hand, so the
 * mill is damaged instead of being consumed by a crafting recipe.
 */
public final class ResonanceMillItem extends Item {
    public ResonanceMillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack mill = player.getItemInHand(hand);
        InteractionHand ingredientHand = hand == InteractionHand.MAIN_HAND
            ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack ingredient = player.getItemInHand(ingredientHand);

        if (ingredient.isEmpty()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.arcanecode.mill_hint"), true);
            }
            return InteractionResultHolder.sidedSuccess(mill, level.isClientSide());
        }

        ItemStack output;
        if (ingredient.is(Items.IRON_INGOT)) {
            output = ModItems.IRON_DUST.toStack(2);
        } else if (ingredient.is(Items.AMETHYST_SHARD)) {
            output = ModItems.AMETHYST_DUST.toStack(2);
        } else {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.arcanecode.mill_invalid"), true);
            }
            return InteractionResultHolder.sidedSuccess(mill, level.isClientSide());
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ingredient.consume(1, player);
            if (!serverPlayer.getInventory().add(output)) serverPlayer.drop(output, false);
            int nextDamage = mill.getDamageValue() + 1;
            if (nextDamage >= mill.getMaxDamage()) {
                mill.shrink(1);
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.85F);
            } else {
                mill.setDamageValue(nextDamage);
                level.playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.8F, 0.85F);
            }
            serverPlayer.displayClientMessage(Component.translatable("message.arcanecode.mill_success", output.getCount(), output.getHoverName()), true);
        }
        return InteractionResultHolder.sidedSuccess(mill, level.isClientSide());
    }
}
