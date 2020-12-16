package flaxbeard.immersivepetroleum.api.crafting;

import java.util.HashMap;

import net.minecraft.fluid.Fluid;
import net.minecraft.util.ResourceLocation;

public class LubricantHandler{
	static final HashMap<ResourceLocation, Integer> lubricantAmounts = new HashMap<>();
	
	/**
	 * Registers a lubricant to be used in the Lubricant Can and Automatic
	 * Lubricator
	 *
	 * @param lube The fluid to be used as lubricant
	 * @param amount mB of lubricant to spend every 4 ticks
	 */
	public static void registerLubricant(Fluid lube, int amount){
		if(lube == null)
			return;
		
		lubricantAmounts.put(lube.getRegistryName(), amount);
	}
	
	/**
	 * Gets amount of this Fluid that is used every four ticks for the Automatic
	 * Lubricator. 0 if not valid lube. 100 * this result is used for the
	 * Lubricant Can
	 *
	 * @param toCheck Fluid to check
	 * @return mB of this Fluid used to lubricate
	 */
	public static int getLubeAmount(Fluid toCheck){
		if(toCheck != null){
			ResourceLocation s = toCheck.getRegistryName();
			return lubricantAmounts.getOrDefault(s, 0);
		}
		return 0;
	}
	
	/**
	 * Whether or not the given Fluid is a valid lubricant
	 *
	 * @param toCheck Fluid to check
	 * @return Whether or not the Fluid is a lubricant
	 */
	public static boolean isValidLube(Fluid toCheck){
		if(toCheck != null){
			return lubricantAmounts.containsKey(toCheck.getRegistryName());
		}
		return false;
	}
}
