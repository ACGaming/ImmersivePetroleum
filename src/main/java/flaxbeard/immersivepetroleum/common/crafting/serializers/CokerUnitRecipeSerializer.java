package flaxbeard.immersivepetroleum.common.crafting.serializers;

import com.google.gson.JsonObject;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import flaxbeard.immersivepetroleum.api.crafting.CokerUnitRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class CokerUnitRecipeSerializer extends IERecipeSerializer<CokerUnitRecipe>{
	
	@Override
	public CokerUnitRecipe readFromJson(ResourceLocation recipeId, JsonObject json){
		return null;
	}
	
	@Override
	public CokerUnitRecipe read(ResourceLocation recipeId, PacketBuffer buffer){
		return null;
	}
	
	@Override
	public void write(PacketBuffer buffer, CokerUnitRecipe recipe){
	}
	
	@Override
	public ItemStack getIcon(){
		return ItemStack.EMPTY; // TODO Add coker recipe icon
	}
}
