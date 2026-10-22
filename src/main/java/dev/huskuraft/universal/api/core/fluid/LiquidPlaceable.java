package dev.huskuraft.universal.api.core.fluid;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.FluidState;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.World;

public interface LiquidPlaceable {

    boolean canPlaceLiquid(World world/*BlockGetter*/, Player player, BlockPosition blockPosition, BlockState blockState, Fluid fluid);

    boolean placeLiquid(World world, BlockPosition blockPosition, BlockState blockState, FluidState fluidState);

}
