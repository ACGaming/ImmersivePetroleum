package flaxbeard.immersivepetroleum.common.blocks;

import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity;

public class CokerUnitBlock extends IPMetalMultiblock<CokerUnitTileEntity>{
	public CokerUnitBlock(){
		super("coker", () -> CokerUnitTileEntity.TYPE);
	}
}
