package dev.huskuraft.universal.api.core.fluid;

import java.util.Optional;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.sound.Sound;

public interface BucketCollectable {

    ItemStack pickupBlock(World world, Player player, BlockPosition blockPosition, BlockState blockState);

    Optional<Sound> getPickupSound();

}
