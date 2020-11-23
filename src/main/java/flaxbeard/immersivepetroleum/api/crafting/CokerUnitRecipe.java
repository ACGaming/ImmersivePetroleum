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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

public class CokerUnitRecipe extends MultiblockRecipe{
	public static final IRecipeType<CokerUnitRecipe> TYPE = IRecipeType.register(ImmersivePetroleum.MODID + ":coking");
	
	public static Map<ResourceLocation, CokerUnitRecipe> recipes = new HashMap<>();
	
	protected final FluidTagInput inputFluid;
	protected final ItemStack inputItem;
	protected final ItemStack outputItem;
	
	protected int totalProcessTime;
	protected int totalProcessEnergy;
	
	protected CokerUnitRecipe(ResourceLocation id, ItemStack outputItem, FluidTagInput inputFluid, ItemStack inputItem, int energy, int time){
		super(ItemStack.EMPTY, TYPE, id);
		this.outputItem = outputItem;
		this.inputItem = inputItem;
		this.inputFluid = inputFluid;
		
		this.totalProcessEnergy = (int) Math.floor(energy * IPConfig.REFINING.cokerUnit_energyModifier.get());
		this.totalProcessTime = (int) Math.floor(time * IPConfig.REFINING.cokerUnit_timeModifier.get());
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
	
	public ItemStack getInputItem(){
		return this.inputItem;
	}
	
	public FluidTagInput getInputFluid(){
		return this.inputFluid;
	}
	
	@Override
	protected IERecipeSerializer<CokerUnitRecipe> getIESerializer(){
		return Serializers.COKER_SERIALIZER.get();
	}
}
