package dev.sergey.arcanecode.registry;

import dev.sergey.arcanecode.ArcaneCode;
import dev.sergey.arcanecode.item.ArcaneCodexItem;
import dev.sergey.arcanecode.item.RuneCarrierItem;
import dev.sergey.arcanecode.item.ResonanceMillItem;
import dev.sergey.arcanecode.item.RuneStaffItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneCode.MOD_ID);

    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> AMETHYST_DUST = ITEMS.registerSimpleItem("amethyst_dust");
    public static final DeferredItem<Item> ARCANE_DUST = ITEMS.registerSimpleItem(
        "arcane_dust", new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> RESONANCE_MILL = ITEMS.registerItem(
        "resonance_mill", ResonanceMillItem::new,
        new Item.Properties().stacksTo(1).durability(256).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<Item> ARCANE_CODEX = ITEMS.registerItem(
        "arcane_codex", ArcaneCodexItem::new,
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> SPARK_STAFF = ITEMS.registerItem(
        "spark_staff", props -> new RuneStaffItem(props, 2_000, 1.0),
        new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> RESONANCE_STAFF = ITEMS.registerItem(
        "resonance_staff", props -> new RuneStaffItem(props, 8_000, 0.85),
        new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> ARCHITECT_STAFF = ITEMS.registerItem(
        "architect_staff", props -> new RuneStaffItem(props, 32_000, 0.70),
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> SOVEREIGN_STAFF = ITEMS.registerItem(
        "sovereign_staff", props -> new RuneStaffItem(props, 128_000, 0.55),
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final DeferredItem<Item> RUNE_SCROLL = ITEMS.registerItem(
        "rune_scroll", RuneCarrierItem::new,
        new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> FOCUS_PRISM = ITEMS.registerItem(
        "focus_prism", RuneCarrierItem::new,
        new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> WOVEN_ARTIFACT = ITEMS.registerItem(
        "woven_artifact", RuneCarrierItem::new,
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final DeferredItem<BlockItem> ARCANE_BARRIER_ITEM = ITEMS.registerSimpleBlockItem(
        ModBlocks.ARCANE_BARRIER, new Item.Properties().rarity(Rarity.EPIC));

    private ModItems() {}
}
