package flaxbeard.immersivepetroleum.common.blocks;

import java.util.function.Supplier;

import blusunrize.immersiveengineering.common.blocks.metal.MetalMultiblockBlock;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;

public class IPMetalMultiblock extends MetalMultiblockBlock{
	public IPMetalMultiblock(String name, Supplier<TileEntityType<?>> te, IProperty<?>... additionalProperties){
		super(name, te, additionalProperties);
	}
	
	@Override
	public ResourceLocation createRegistryName(){
		return new ResourceLocation(ImmersivePetroleum.MODID, name);
	}
}