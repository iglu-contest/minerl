package com.microsoft.Malmo.Blocks;

import java.util.logging.Logger;
import java.util.logging.Level;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlacementHandler
{
    private final int x1 = -5;
    private final int y1 = 227;
    private final int z1 = -5;
    private final int x2 = 5;
    private final int y2 = 235;
    private final int z2 = 5;

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        BlockPos pos = event.getBlockSnapshot().getPos();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (!(x >= this.x1 && y >= this.y1 && z >= this.z1
                && x <= this.x2 && y <= this.y2 && z <= this.z2))
        {
            // if the block is going to be placed outside the building zone
            Block air = Block.getBlockFromName("minecraft:air");
            IBlockState air_type = air.getDefaultState();
            event.getWorld().setBlockState(pos, air_type);
            event.setCanceled(true);
        }
    }
}