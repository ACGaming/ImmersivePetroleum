package flaxbeard.immersivepetroleum.common.util.projector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

/**
 * Class for handling preview/projection placement<br>
 * <br>
 * Flipping only supports {@link Mirror#NONE} and {@link Mirror#FRONT_BACK}
 * 
 * @author TwistedGate
 */
public class MultiblockProjection{
	final IMultiblock multiblock;
	final PlacementSettings settings = new PlacementSettings();
	final Int2ObjectMap<List<Template.BlockInfo>> layers = new Int2ObjectArrayMap<>();
	final int blockcount;
	Mutable offset;
	boolean isDirty = true;
	World world;
	public MultiblockProjection(World world, @Nonnull IMultiblock multiblock){
		Objects.requireNonNull(multiblock);
		this.world = world;
		this.multiblock = multiblock;
		
		List<Template.BlockInfo> blocks = multiblock.getStructure(this.world);
		this.blockcount = blocks.size();
		for(Template.BlockInfo info:blocks){
			List<Template.BlockInfo> list = this.layers.get(info.pos.getY());
			if(list == null){
				list = new ArrayList<>();
				this.layers.put(info.pos.getY(), list);
			}
			
			list.add(info);
		}
	}
	
	public MultiblockProjection setRotation(Rotation rotation){
		if(this.settings.getRotation() != rotation){
			this.settings.setRotation(rotation);
			this.isDirty=true;
		}
		
		return this;
	}
	
	/**
	 * Sets the mirrored state.
	 * 
	 * <pre>
	 * true = {@link Mirror#FRONT_BACK}
	 * 
	 * false = {@link Mirror#NONE}
	 * </pre>
	 */
	public MultiblockProjection setFlip(boolean mirror){
		Mirror m = mirror ? Mirror.FRONT_BACK : Mirror.NONE;
		if(this.settings.getMirror() != m){
			this.settings.setMirror(m);
			this.isDirty=true;
		}
		
		return this;
	}
	
	/**
	 * Toggles between {@link Mirror#NONE} and {@link Mirror#FRONT_BACK}
	 */
	public MultiblockProjection flip(){
		return setFlip(this.settings.getMirror() == Mirror.NONE);
	}
	
	/**
	 * <pre>
	 * 1 Clockwise rotation
	 * -1 Counter-Clockwise rotation
	 * </pre>
	 */
	public MultiblockProjection rotate(int direction){
		if(direction != 0){
			int i = (this.settings.getRotation().ordinal() + direction) % 4;
			while(i < 0){
				i += 4;
			}
			
			setRotation(Rotation.values()[i]);
		}
		
		return this;
	}
	
	/** Total amount of blocks present in the multiblock */
	public int getBlockCount(){
		return this.blockcount;
	}
	
	/** Amount of layers in this projection */
	public int getLayerCount(){
		return this.layers.size();
	}
	
	public int getLayerSize(int layer){
		if(layer < 0 || layer >= this.layers.size()){
			return 0;
		}
		
		return this.layers.get(layer).size();
	}
	
	public IMultiblock getMultiblock(){
		return this.multiblock;
	}
	
	/**
	 * Single-Layer based projection processing
	 * 
	 * @param layer The layer to work on
	 * @param predicate What to do per block
	 * @return true if it was interrupted
	 */
	public boolean process(int layer, Predicate<Info> predicate){
		updateData();
		
		List<Template.BlockInfo> blocks = this.layers.get(layer);
		for(Template.BlockInfo info:blocks){
			BlockPos transformedPos = Template.transformedBlockPos(this.settings, info.pos).subtract(this.offset);
			
			if(predicate.test(new Info(this.multiblock, this.settings, info, transformedPos))){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Multi-Layer based projection processing. (Do all at once)
	 * 
	 * @param predicate What to do per block
	 * @return true if it was stopped pre-maturely, false if it went through everything
	 */
	public boolean processAll(BiPredicate<Integer, Info> predicate){
		updateData();
		
		for(int layer = 0;layer < getLayerCount();layer++){
			List<Template.BlockInfo> blocks = this.layers.get(layer);
			for(Template.BlockInfo info:blocks){
				BlockPos transformedPos = Template.transformedBlockPos(this.settings, info.pos).subtract(this.offset);
				
				if(predicate.test(layer, new Info(this.multiblock, this.settings, info, transformedPos))){
					return true;
				}
			}
		}
		return false;
	}
	
	private void updateData(){
		if(!this.isDirty) return;
		this.isDirty=false;
		
		int mWidth = this.multiblock.getSize(this.world).getX();
		int mDepth = this.multiblock.getSize(this.world).getZ();
		
		// Determine if the dimensions are even (true) or odd (false)
		// Divide with float, Divide with int then subtract both and check for 0
		boolean evenWidth = ((mWidth / 2F) - (mWidth / 2)) == 0F;
		boolean evenDepth = ((mDepth / 2F) - (mDepth / 2)) == 0F;
		
		// Take even/odd-ness of multiblocks into consideration for rotation
		int xa = evenWidth ? 1 : 0;
		int za = evenDepth ? 1 : 0;
		
		if(this.settings.getMirror() == Mirror.FRONT_BACK){
			this.offset = new Mutable(-mWidth / 2, 0, mDepth / 2);
			this.settings.setCenterOffset(this.offset.toImmutable());
			
			switch(this.settings.getRotation()){
				case NONE:{
					this.offset.setAndOffset(this.offset, xa, 0, 0);
					break;
				}
				case CLOCKWISE_90:{
					this.offset.setAndOffset(this.offset, xa, 0, za);
					break;
				}
				case CLOCKWISE_180:{
					this.offset.setAndOffset(this.offset, 0, 0, za);
					break;
				}
				default:
					break;
			}
		}else{
			this.offset = new Mutable(mWidth / 2, 0, mDepth / 2);
			this.settings.setCenterOffset(this.offset.toImmutable());
			
			switch(this.settings.getRotation()){
				case CLOCKWISE_90:{
					this.offset.setAndOffset(this.offset, xa, 0, 0);
					break;
				}
				case CLOCKWISE_180:{
					this.offset.setAndOffset(this.offset, xa, 0, za);
					break;
				}
				case COUNTERCLOCKWISE_90:{
					this.offset.setAndOffset(this.offset, 0, 0, za);
					break;
				}
				default:
					break;
			}
		}
	}
	
	
	public static final class Info{
		/** raw information from the template */
		public final Template.BlockInfo blockInfo;
		
		/** Transformed Position */
		public final BlockPos tPos;
		
		/** Currently applied template transformation (tPos) */
		public final PlacementSettings settings;
		
		/** the multiblock in question */
		public final IMultiblock multiblock;
		
		public Info(IMultiblock multiblock, PlacementSettings settings, Template.BlockInfo info, BlockPos transformedPos){
			this.blockInfo = info;
			this.tPos = transformedPos;
			this.settings = settings;
			this.multiblock = multiblock;
		}
		
		/** The state with rotations in mind */
		public BlockState getState(){
			@SuppressWarnings("deprecation")
			BlockState rotated = this.blockInfo.state.rotate(this.settings.getRotation());
			return rotated;
		}
	}
}
