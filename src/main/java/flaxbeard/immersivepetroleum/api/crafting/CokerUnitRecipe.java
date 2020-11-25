package flaxbeard.immersivepetroleum.api.crafting;

import java.util.HashMap;
import java.util.Map;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPConfig;
import flaxbeard.immersivepetroleum.common.crafting.Serializers;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public class CokerUnitRecipe extends MultiblockRecipe{
	public static final IRecipeType<CokerUnitRecipe> TYPE = IRecipeType.register(ImmersivePetroleum.MODID + ":coking");
	
	public static Map<ResourceLocation, CokerUnitRecipe> recipes = new HashMap<>();
	
	public static CokerUnitRecipe findRecipe(ItemStack stack, FluidStack fluid){
		for(CokerUnitRecipe recipe:recipes.values()){
			if((recipe.inputItem != null && recipe.inputItem.test(stack)) && (recipe.inputFluid != null && recipe.inputFluid.test(fluid))){
				return recipe;
			}
		}
		
		return null;
	}
	
	public static boolean hasRecipeWithInput(FluidStack fluid){
		for(CokerUnitRecipe recipe:recipes.values()){
			if(recipe.inputFluid != null && recipe.inputFluid.test(fluid)){
				return true;
			}
		}
		return false;
	}
	
	public static boolean hasRecipeWithInput(ItemStack stack){
		for(CokerUnitRecipe recipe:recipes.values()){
			if(recipe.inputItem != null && recipe.inputItem.test(stack)){
				return true;
			}
		}
		return false;
	}
	
	// just a "Reference"
	// Water Input   -> FluidIn
	// Bitumen Input -> Item In
	// Coke Output   -> Item Out
	// Diesel Output -> Fluid Out
	
	public final ItemStack outputItem;
	public final FluidTagInput outputFluid;
	
	public final Ingredient inputItem;
	public final FluidTagInput inputFluid;
	
	protected int totalProcessTime;
	protected int totalProcessEnergy;
	
	public CokerUnitRecipe(ResourceLocation id, ItemStack outputItem, FluidTagInput outputFluid, Ingredient inputItem, FluidTagInput inputFluid, int energy){
		super(ItemStack.EMPTY, TYPE, id);
		this.inputFluid = inputFluid;
		this.inputItem = inputItem;
		this.outputFluid = outputFluid;
		this.outputItem = outputItem;
		
		this.totalProcessEnergy = (int) Math.floor(energy * IPConfig.REFINING.cokerUnit_energyModifier.get());
		this.totalProcessTime = (int) Math.floor(1 * IPConfig.REFINING.cokerUnit_timeModifier.get());
	}
	
	@Override
	public int getMultipleProcessTicks(){
		return 20;
	}
	
	@Override
	public int getTotalProcessTime(){
		return this.totalProcessTime;
	}
	
	@Override
	public int getTotalProcessEnergy(){
		return this.totalProcessEnergy;
	}
	
	@Override
	public NonNullList<ItemStack> getActualItemOutputs(TileEntity tile){
		NonNullList<ItemStack> list = NonNullList.create();
		list.add(this.outputItem);
		return list;
	}
	
	@Override
	protected IERecipeSerializer<CokerUnitRecipe> getIESerializer(){
		return Serializers.COKER_SERIALIZER.get();
	}
}