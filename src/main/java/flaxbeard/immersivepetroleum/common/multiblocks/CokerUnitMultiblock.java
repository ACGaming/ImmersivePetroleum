package flaxbeard.immersivepetroleum.common.multiblocks;

import com.mojang.blaze3d.matrix.MatrixStack;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class CokerUnitMultiblock extends IETemplateMultiblock{
	public static final CokerUnitMultiblock INSTANCE = new CokerUnitMultiblock();
	
	public CokerUnitMultiblock(){
		super(new ResourceLocation(ImmersivePetroleum.MODID, "multiblocks/cokerunit"),
				new BlockPos(4, 0, 2), new BlockPos(4, 1, 4),
				() -> IPContent.Multiblock.cokerunit.getDefaultState());
	}
	
	@Override
	public float getManualScale(){
		return 0;
	}
	
	@Override
	public boolean canRenderFormedStructure(){
		return false;
	}
	
	@Override
	public void renderFormedStructure(MatrixStack transform, IRenderTypeBuffer buffer){
	}
}
