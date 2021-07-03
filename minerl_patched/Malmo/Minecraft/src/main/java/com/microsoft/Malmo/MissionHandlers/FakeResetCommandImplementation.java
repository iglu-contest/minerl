// ---------------------------------------------------------
// Author: Artem Zholus 2021
// ---------------------------------------------------------

package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Blocks.BlocksHandler;
import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MissionHandlers.AbsoluteMovementCommandsImplementation;
import com.microsoft.Malmo.Schemas.MissionInit;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IThreadListener;
import net.minecraft.entity.MoverType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import com.microsoft.Malmo.Utils.TimeHelper;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class FakeResetCommandImplementation extends CommandBase {
    // initial position for the agent
    private final double x = 0.5;
    private final double y = 227.;
    private final double z = 0.5;
    private final float yaw = -90.f;
    private final float pitch = 0.f;

    public FakeResetCommandImplementation(){
    }

    @Override
    public void install(MissionInit missionInit) {  }

    @Override
    public void deinstall(MissionInit missionInit) {  }

    @Override
    public boolean isOverriding() {
        return false;
    }

    @Override
    public void setOverriding(boolean b) {
    }

    public static class ClearZoneMessage implements IMessage
    {
        @Override
        public void fromBytes(ByteBuf buf) { }

        @Override
        public void toBytes(ByteBuf buf) { }
    }

    public static class ClearZoneMessageHandler implements IMessageHandler<FakeResetCommandImplementation.ClearZoneMessage, IMessage>
    {
        @Override
        public IMessage onMessage(final FakeResetCommandImplementation.ClearZoneMessage message, final MessageContext ctx)
        {
            IThreadListener mainThread = null;
            if (ctx.side == Side.CLIENT)
                return null;

            mainThread = (WorldServer)ctx.getServerHandler().playerEntity.world;
            mainThread.addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    WorldServer world = (WorldServer) ctx.getServerHandler().playerEntity.world;
                    for (int y = 227; y <= 235; y++) {
                        for (int z = -5; z <= 5; z++) {
                            for (int x = -5; x <= 5; x++) {
                                BlockPos pos = new BlockPos(x, y, z);
                                Block block = world.getBlockState(pos).getBlock();
                                if (!block.getLocalizedName().toString().contains("Air")) {
                                    world.destroyBlock(pos, false);
                                }
                            }
                        }
                    }
                    String[] types = {
                            "malmomod:iglu_minecraft_blue_rn",
                            "malmomod:iglu_minecraft_yellow_rn",
                            "malmomod:iglu_minecraft_green_rn",
                            "malmomod:iglu_minecraft_orange_rn",
                            "malmomod:iglu_minecraft_purple_rn",
                            "malmomod:iglu_minecraft_red_rn"
                    };
                    InventoryPlayer inventory = ctx.getServerHandler().playerEntity.inventory;
                    for (int i = 0; i < 6; ++i) {
                        Block block = Block.getBlockFromName(types[i]);
                        ItemStack stack = new ItemStack(block, 20);
                        inventory.setInventorySlotContents(i, stack);
                    }
                }
            });
            return null;
        }
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        if (verb.equals("fake_reset") && parameter.equals("1")) {
            // remove all blocks
            // re-fill inventory
            MalmoMod.network.sendToServer(new FakeResetCommandImplementation.ClearZoneMessage());
            // teleport agent
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            player.setPositionAndRotation(x, y, z, yaw, pitch);
            player.onUpdate();
            return true;
        }
        return false;
    }
}
