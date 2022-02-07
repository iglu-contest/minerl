// ---------------------------------------------------------
// Author: Artem Zholus 2021
// ---------------------------------------------------------

package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Blocks.BlocksHandler;
import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MissionHandlers.AbsoluteMovementCommandsImplementation;
import com.microsoft.Malmo.Schemas.MissionInit;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FakeResetCommandImplementation extends CommandBase {
    // initial position for the agent
    private final double default_x = 0.5;
    private final double default_y = 227.;
    private final double default_z = 0.5;
    private final float default_yaw = -90.f;
    private final float default_pitch = 0.f;

    private double x = 0.5;
    private double y = 227.;
    private double z = 0.5;
    private float yaw = -90.f;
    private float pitch = 0.f;
    private static final Pattern pPos = Pattern.compile(
            "(-?\\d+(\\.\\d+)?),(-?\\d+(\\.\\d+)?),(-?\\d+(\\.\\d+)?),(-?\\d+(\\.\\d+)?),(-?\\d+(\\.\\d+)?)"
    );
    private static final Pattern pBlock = Pattern.compile(
            "(-?\\d+),(-?\\d+),(-?\\d+),(-?\\d+)"
    );

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
        List<BlockPos> blocksCoord;
        List<Integer> blockTypes;

        public ClearZoneMessage() { }

        public ClearZoneMessage(List<BlockPos> blocksCoord, List<Integer> blockTypes) {
            this.blocksCoord = blocksCoord;
            this.blockTypes = blockTypes;

        }
        @Override
        public void fromBytes(ByteBuf buf) {
            this.blocksCoord = new ArrayList<>();
            this.blockTypes = new ArrayList<>();
            int length = buf.readInt();
            for (int i = 0; i < length; ++i) {
                this.blocksCoord.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
                this.blockTypes.add(buf.readInt());
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(this.blocksCoord.size());
            for (int i = 0; i < this.blocksCoord.size(); ++i) {
                buf.writeInt(this.blocksCoord.get(i).getX());
                buf.writeInt(this.blocksCoord.get(i).getY());
                buf.writeInt(this.blocksCoord.get(i).getZ());
                buf.writeInt(this.blockTypes.get(i));
            }
        }
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
                    int[] counts = {20, 20, 20, 20, 20, 20};
                    for (int i = 0; i < message.blocksCoord.size(); ++i) {
                        BlockPos pos = message.blocksCoord.get(i);
                        String kind = types[message.blockTypes.get(i) - 1];
                        counts[message.blockTypes.get(i) - 1]--;
                        IBlockState type = Block.getBlockFromName(kind).getDefaultState();
                        world.setBlockState(pos, type);
                    }

                    InventoryPlayer inventory = ctx.getServerHandler().playerEntity.inventory;
                    for (int i = 0; i < 6; ++i) {
                        Block block = Block.getBlockFromName(types[i]);
                        ItemStack stack = new ItemStack(block, counts[i]);
                        inventory.setInventorySlotContents(i, stack);
                    }
                }
            });
            return null;
        }
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        if (verb.equals("fake_reset") && !parameter.equals("0")) {
            // remove all blocks
            // re-fill inventor
            List<BlockPos> blocks = new ArrayList<>();
            List<Integer> types = new ArrayList<>();
            if (!parameter.equals("1")) {
                try {
                    String[] splits = parameter.split(";", 2);
                    String sPos = splits[0];
                    String sBlocks = "";
                    if (splits.length > 1) {
                        sBlocks = splits[1];
                    }
                    Matcher m = pPos.matcher(sPos);
                    if (m.find()) {
                        this.x = Double.parseDouble(m.group(1));
                        this.y = Double.parseDouble(m.group(3));
                        this.z = Double.parseDouble(m.group(5));
                        this.pitch = Float.parseFloat(m.group(7));
                        this.yaw = Float.parseFloat((m.group(9)));
                    }
                    for (String block : sBlocks.split(";")) {
                        m = pBlock.matcher(block);
                        if (m.find()) {
                            blocks.add(new BlockPos(
                                    Integer.parseInt(m.group(1)),
                                    Integer.parseInt(m.group(2)),
                                    Integer.parseInt(m.group(3))
                            ));
                            types.add(Integer.parseInt(m.group(4)));
                        }
                    }
                } catch (IllegalStateException e) {
                    return false;
                    // lol
                }
            } else {
                this.x = default_x;
                this.y = default_y;
                this.z = default_z;
                this.pitch = default_pitch;
                this.yaw = default_yaw;
            }

            MalmoMod.network.sendToServer(new FakeResetCommandImplementation.ClearZoneMessage(
                blocks, types
            ));
            // teleport agent
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            player.setPositionAndRotation(x, y, z, yaw, pitch);
            player.onUpdate();
            return true;
        }
        return false;
    }
}
