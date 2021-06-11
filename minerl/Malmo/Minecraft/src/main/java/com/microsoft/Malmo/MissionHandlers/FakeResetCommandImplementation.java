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
                }
            });
            return null;
        }
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        if (verb.equals("fake_reset") && parameter.equals("1")) {
            // remove all blocks
            MalmoMod.network.sendToServer(new FakeResetCommandImplementation.ClearZoneMessage());
            // re-fill inventory
            // TODO: !!!
            // as we are currently increative mode, there is no need to re-fill the inventory
            // but in future we should handle this
            // teleport agent
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            player.setPositionAndRotation(x, y, z, yaw, pitch);
            player.onUpdate();
            return true;
        }
        return false;
    }
}
