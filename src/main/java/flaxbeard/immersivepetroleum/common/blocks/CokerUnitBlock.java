package flaxbeard.immersivepetroleum.common.blocks;

import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

public class CokerUnitBlock extends IPMetalMultiblock<CokerUnitTileEntity>{
	public CokerUnitBlock(){
		super("cokerunit", () -> CokerUnitTileEntity.TYPE);
	}
	
	@Override
	public boolean isLadder(BlockState state, IWorldReader world, BlockPos pos, LivingEntity entity){
		TileEntity te = world.getTileEntity(pos);
		if(te instanceof CokerUnitTileEntity){
			return ((CokerUnitTileEntity) te).isLadder();
		}
		return false;
	}
}
