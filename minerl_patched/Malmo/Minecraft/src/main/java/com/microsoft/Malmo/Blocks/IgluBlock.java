package com.microsoft.Malmo.Blocks;

import net.minecraft.block.BlockFalling;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Standard, falling (obeys gravity) custom CwC block.
 * @author nrynchn2, artemZholus
 */
public class IgluBlock extends BlockFalling {

	public IgluBlock() { super(); }

	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.SOLID; }
}
