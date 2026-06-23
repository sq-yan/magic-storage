package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class HorizontalDirectionalBlock extends Block {
    public static final EnumProperty<Direction> f_54117_ = BlockStateProperties.f_61374_;

    protected HorizontalDirectionalBlock(BlockBehaviour.Properties p_54120_) {
        super(p_54120_);
    }

    @Override
    protected abstract MapCodec<? extends HorizontalDirectionalBlock> m_304657_();

    @Override
    protected BlockState m_6843_(BlockState p_54125_, Rotation p_54126_) {
        return p_54125_.m_61124_(f_54117_, p_54126_.m_55954_(p_54125_.m_61143_(f_54117_)));
    }

    @Override
    protected BlockState m_6943_(BlockState p_54122_, Mirror p_54123_) {
        return p_54122_.m_60717_(p_54123_.m_54846_(p_54122_.m_61143_(f_54117_)));
    }
}
