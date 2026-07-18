package dev.sergey.arcanecode.registry;

import dev.sergey.arcanecode.ArcaneCode;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcaneCode.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARCANE_CODE =
        TABS.register("arcane_code", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.arcanecode.arcane_code"))
            .icon(() -> new ItemStack(ModItems.SOVEREIGN_STAFF.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.ARCANE_CODEX.get());
                output.accept(ModItems.RESONANCE_MILL.get());
                output.accept(ModItems.IRON_DUST.get());
                output.accept(ModItems.AMETHYST_DUST.get());
                output.accept(ModItems.ARCANE_DUST.get());
                output.accept(ModItems.SPARK_STAFF.get());
                output.accept(ModItems.RESONANCE_STAFF.get());
                output.accept(ModItems.ARCHITECT_STAFF.get());
                output.accept(ModItems.SOVEREIGN_STAFF.get());
                output.accept(ModItems.RUNE_SCROLL.get());
                output.accept(ModItems.FOCUS_PRISM.get());
                output.accept(ModItems.WOVEN_ARTIFACT.get());
            })
            .build());

    private ModCreativeTabs() {}
}
