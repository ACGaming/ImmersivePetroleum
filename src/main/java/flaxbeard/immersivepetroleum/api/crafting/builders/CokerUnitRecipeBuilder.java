package flaxbeard.immersivepetroleum.api.crafting.builders;

import java.util.Objects;

import blusunrize.immersiveengineering.api.crafting.builders.IEFinishedRecipe;
import flaxbeard.immersivepetroleum.common.crafting.Serializers;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ITag;

// TODO
public class CokerUnitRecipeBuilder extends IEFinishedRecipe<CokerUnitRecipeBuilder>{
	// Required: ItemStack outputItem, ItemStack inputItem
	// Optional: FluidTagInput outputFluid, FluidTagInput inputFluid
	
	public static CokerUnitRecipeBuilder builder(ItemStack output){
		Objects.requireNonNull(output);
		if(output.isEmpty()) throw new IllegalArgumentException("Input stack cannot be empty.");
		
		return new CokerUnitRecipeBuilder().addResult(output);
	}
	
	private CokerUnitRecipeBuilder(){
		super(Serializers.COKER_SERIALIZER.get());
	}
	
	public CokerUnitRecipeBuilder addInputFluid(ITag.INamedTag<Fluid> fluidTag, int amount){
		return addFluidTag("inputfluid", fluidTag, amount);
	}
	
	public CokerUnitRecipeBuilder addOutputFluid(ITag.INamedTag<Fluid> fluidTag, int amount){
		return addFluidTag("resultfluid", fluidTag, amount);
	}
	
	/** Defaults to 1 when loading the recipe in-game (including reload) */
	@Override
	public CokerUnitRecipeBuilder setTime(int time){
		return super.setTime(time);
	}
	
	/** Defaults to 2048 when loading the recipe in-game (including reload) */
	@Override
	public CokerUnitRecipeBuilder setEnergy(int energy){
		return super.setEnergy(energy);
	}
}
