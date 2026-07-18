package dev.sergey.arcanecode.registry;

import dev.sergey.arcanecode.ArcaneCode;
import dev.sergey.arcanecode.block.ArcaneBarrierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcaneCode.MOD_ID);

    public static final DeferredBlock<Block> ARCANE_BARRIER = BLOCKS.registerBlock(
        "arcane_barrier",
        ArcaneBarrierBlock::new,
        BlockBehaviour.Properties.of()
            .strength(-1.0F, 3_600_000.0F)
            .sound(SoundType.AMETHYST)
            .noOcclusion()
            .lightLevel(state -> 11)
    );

    private ModBlocks() {}
}
