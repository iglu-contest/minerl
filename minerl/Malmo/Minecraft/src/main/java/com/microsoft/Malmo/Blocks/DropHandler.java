package com.microsoft.Malmo.Blocks;

import java.util.logging.Logger;
import java.util.logging.Level;
// import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.Block;
// import net.minecraft.util.text.StringTextComponent;
// import net.minecraftforge.common.ToolType;
// import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
// import net.minecraftforge.fml.common.Mod;

public class DropHandler
{
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event instanceof PlayerInteractEvent.LeftClickBlock)
        {
            Logger logger = Logger.getLogger("DropHandler   ");
            // Destroy block
            IBlockState iblockstate = event.getWorld().getBlockState(event.getPos());
            Block block = iblockstate.getBlock();
            String name = block.getLocalizedName().toString();
            if (!(name.contains("White") || name.contains("Grey"))) { // check for unbreakable blocks
                logger.log(Level.INFO, "destructing " + block.getLocalizedName().toString());
                event.getWorld().destroyBlock(event.getPos(), false);
            } else {
                event.setCanceled(true);
            }
        }
    }
}