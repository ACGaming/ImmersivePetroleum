package flaxbeard.immersivepetroleum.common.blocks.tileentities;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableSet;

import blusunrize.immersiveengineering.api.DirectionalBlockPos;
import blusunrize.immersiveengineering.api.utils.shapes.CachedShapesWithTransform;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity;
import blusunrize.immersiveengineering.common.util.CapabilityReference;
import blusunrize.immersiveengineering.common.util.Utils;
import flaxbeard.immersivepetroleum.api.IPTags;
import flaxbeard.immersivepetroleum.api.crafting.CokerUnitRecipe;
import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.multiblocks.CokerUnitMultiblock;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class CokerUnitTileEntity extends PoweredMultiblockTileEntity<CokerUnitTileEntity, CokerUnitRecipe> implements IInteractionObjectIE, IBlockBounds{
	/** Do not Touch! Taken care of by {@link IPContent#registerTile(RegistryEvent.Register, Class, Block...)} */
	public static TileEntityType<CokerUnitTileEntity> TYPE;

	/** Input Fluid Tank<br> */
	public static final int TANK_INPUT = 0;
	
	/** Output Fluid Tank<br> */
	public static final int TANK_OUTPUT = 1;

	/** Inventory Item Input<br> */
	public static final int INV_INPUT = 0;
	
	/** Inventory Item Storage. (Left column)<br> */
	public static final int INV_STORAGE_A = 1;
	
	/** Inventory Item Storage. (Right column)<br> */
	public static final int INV_STORAGE_B = 2;
	
	/** Inventory Item Output<br> */
	public static final int INV_OUTPUT = 3;
	
	/** Inventory Fluid Input (Filled Bucket)<br> */
	public static final int INV_INPUT_FILLED = 4;
	
	/** Inventory Fluid Input (Empty Bucket)<br> */
	public static final int INV_INPUT_EMPTY = 5;
	
	/** Inventory Fluid Output (Empty Bucket)<br> */
	public static final int INV_OUTPUT_EMPTY = 6;
	
	/** Inventory Fluid Output (Filled Bucket)<br> */
	public static final int INV_OUTPUT_FILLED = 7;
	
	/** Template-Location of the Item Input Port for Chaining. (0 1 2)<br> */
	public static final BlockPos Chaining_IN = new BlockPos(0, 1, 2);
	
	/** Template-Location of the Item Output Port for Chaining. (8 1 2)<br>Serves as the normal Item Output aswell.<br> */
	public static final BlockPos Chaining_OUT = new BlockPos(8, 1, 2);
	
	/** Template-Location of the Fluid Input Port. (6 0 0)<br> */
	public static final BlockPos Fluid_IN = new BlockPos(6, 0, 0);
	
	/** Template-Location of the Fluid Output Port. (3 0 4)<br> */
	public static final BlockPos Fluid_OUT = new BlockPos(3, 0, 4);
	
	/** Template-Location of the Item Input Port. (4 0 0)<br> */
	public static final BlockPos Item_IN = new BlockPos(4, 0, 0);
	
	/**
	 * Template-Location of the Energy Input Ports.<br>
	 * <pre>1 1 0<br>2 1 0<br>3 1 0</pre><br>
	 */
	public static final Set<BlockPos> Energy_IN = ImmutableSet.of(new BlockPos(1, 1, 0), new BlockPos(2, 1, 0), new BlockPos(3, 1, 0));
	
	/** Template-Location of the Redstone Input Port. (6 1 4)<br> */
	public static final Set<BlockPos> Redstone_IN = ImmutableSet.of(new BlockPos(6, 1, 4));
	
	public final NonNullList<ItemStack> inventory = NonNullList.withSize(6, ItemStack.EMPTY);
	public final FluidTank[] tanks = new FluidTank[]{new FluidTank(12000), new FluidTank(12000)};
	public CokerUnitTileEntity(){
		super(CokerUnitMultiblock.INSTANCE, 16000, true, null);
	}
	
	@Override
	public TileEntityType<?> getType(){
		return TYPE;
	}
	
	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket){
		super.readCustomNBT(nbt, descPacket);
		
		for(int i = 0;i < this.tanks.length;i++){
			this.tanks[i].readFromNBT(nbt.getCompound("tank" + i));
		}
		
		if(!descPacket){
			readInventory(nbt.getCompound("inventory"));
		}
	}
	
	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket){
		super.writeCustomNBT(nbt, descPacket);
		
		for(int i = 0;i < this.tanks.length;i++){
			nbt.put("tank" + i, this.tanks[i].writeToNBT(new CompoundNBT()));
		}
		
		if(!descPacket){
			nbt.put("inventory", writeInventory(this.inventory));
		}
	}
	
	protected void readInventory(CompoundNBT nbt){
		NonNullList<ItemStack> list = NonNullList.create();
		ItemStackHelper.loadAllItems(nbt, list);
		
		for(int i = 0;i < this.inventory.size();i++){
			ItemStack stack = ItemStack.EMPTY;
			if(i < list.size()){
				stack = list.get(i);
			}
			
			this.inventory.set(i, stack);
		}
	}
	
	protected CompoundNBT writeInventory(NonNullList<ItemStack> list){
		return ItemStackHelper.saveAllItems(new CompoundNBT(), list);
	}
	
	@Override
	protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource){
		if(this.posInMultiblock.equals(Fluid_IN)){
			if(side == null || (getIsMirrored() ? (side == getFacing().rotateYCCW()) : (side == getFacing()))){
				CokerUnitTileEntity master = master();
				
				if(master != null && master.tanks[TANK_INPUT].getFluidAmount() < master.tanks[TANK_INPUT].getCapacity()){
					FluidStack copy0 = Utils.copyFluidStackWithAmount(resource, 1000, false);
					FluidStack copy1 = Utils.copyFluidStackWithAmount(master.tanks[TANK_INPUT].getFluid(), 1000, false);
					
					if(master.tanks[TANK_INPUT].getFluid() == FluidStack.EMPTY){
						return CokerUnitRecipe.hasRecipeWithInput(copy0);
					}else{
						FluidStack existing = master.tanks[TANK_INPUT].getFluid();
						boolean r0 = CokerUnitRecipe.hasRecipeWithInput(copy0);
						boolean r1 = CokerUnitRecipe.hasRecipeWithInput(copy1);
						return r0 == r1;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	protected boolean canDrainTankFrom(int iTank, Direction side){
		if(this.posInMultiblock.equals(Fluid_OUT) && (side == null || (getIsMirrored() ? (side == getFacing().rotateYCCW()) : (side == getFacing().getOpposite())))){
			CokerUnitTileEntity master = master();
			
			return master != null && master.tanks[TANK_OUTPUT].getFluidAmount() > 0;
		}
		return false;
	}
	
	@Override
	public void doGraphicalUpdates(int slot){
		updateMasterBlock(null, true);
	}
	
	@Override
	public CokerUnitRecipe findRecipeForInsertion(ItemStack inserting){
		return null;
	}
	
	@Override
	public boolean additionalCanProcessCheck(MultiblockProcess<CokerUnitRecipe> process){
		return false;
	}
	
	/** Output Capability Reference */
	private CapabilityReference<IItemHandler> output_capref = CapabilityReference.forTileEntity(this,
			() -> CokerUnitTileEntity.sup(this),
			CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
	
	private static DirectionalBlockPos sup(CokerUnitTileEntity te){
		Direction outputdir = (te.getIsMirrored() ? te.getFacing().rotateY() : te.getFacing().rotateYCCW());
		return new DirectionalBlockPos(te.getBlockPosForPos(Chaining_OUT).offset(outputdir), outputdir);
	}
	
	@Override
	public void doProcessOutput(ItemStack output){
		output = Utils.insertStackIntoInventory(this.output_capref, output, false);
		if(!output.isEmpty()){
			
		}
	}
	
	@Override
	public void doProcessFluidOutput(FluidStack output){
	}
	
	@Override
	public void onProcessFinish(MultiblockProcess<CokerUnitRecipe> process){
	}
	
	@Override
	public void tick(){
		checkForNeedlessTicking();
		
		if(this.world.isRemote || isDummy() || isRSDisabled()){
			return;
		}
		
		boolean update = false;
		
		if(this.energyStorage.getEnergyStored() > 0 && this.processQueue.size() < getProcessQueueMaxLength()){
			if(!this.inventory.get(INV_INPUT).isEmpty() || this.tanks[TANK_INPUT].getFluidAmount() > 0){
				CokerUnitRecipe recipe = CokerUnitRecipe.findRecipe(this.inventory.get(INV_INPUT), this.tanks[TANK_INPUT].getFluid());
				if(recipe != null && this.energyStorage.getEnergyStored() >= recipe.getTotalProcessEnergy()){
					if(recipe.inputItem != null && recipe.inputFluid != null && this.tanks[TANK_INPUT].getFluidAmount() >= recipe.inputFluid.getAmount()){
						MultiblockProcessInMachine<CokerUnitRecipe> process = new MultiblockProcessInMachine<CokerUnitRecipe>(recipe, INV_INPUT).setInputTanks(TANK_INPUT);
						
						if(addProcessToQueue(process, true)){
							addProcessToQueue(process, false);
							update = true;
						}
					}
				}
			}
		}
		
		super.tick();
		
		if(!this.inventory.get(INV_INPUT_FILLED).isEmpty() && this.tanks[TANK_INPUT].getFluidAmount() < this.tanks[TANK_INPUT].getCapacity()){
			ItemStack container = Utils.drainFluidContainer(this.tanks[TANK_INPUT], this.inventory.get(INV_INPUT_FILLED), this.inventory.get(INV_INPUT_EMPTY), null);
			if(!container.isEmpty()){
				if(!this.inventory.get(INV_INPUT_EMPTY).isEmpty() && ItemHandlerHelper.canItemStacksStack(this.inventory.get(INV_INPUT_EMPTY), container)){
					this.inventory.get(INV_INPUT_EMPTY).grow(container.getCount());
				}else if(this.inventory.get(INV_INPUT_EMPTY).isEmpty()){
					this.inventory.set(INV_INPUT_EMPTY, container.copy());
				}
				
				this.inventory.get(INV_INPUT_FILLED).shrink(1);
				if(this.inventory.get(INV_INPUT_FILLED).getCount() <= 0){
					this.inventory.set(INV_INPUT_FILLED, ItemStack.EMPTY);
				}
			}
		}
		
		if(this.tanks[TANK_OUTPUT].getFluidAmount() > 0){
			if(!this.inventory.get(INV_OUTPUT_EMPTY).isEmpty()){
				ItemStack filledContainer = Utils.fillFluidContainer(this.tanks[TANK_OUTPUT], this.inventory.get(INV_OUTPUT_EMPTY), this.inventory.get(INV_OUTPUT_FILLED), null);
				if(!filledContainer.isEmpty()){
					
					if(this.inventory.get(INV_OUTPUT_FILLED).getCount() == 1 && !Utils.isFluidContainerFull(filledContainer)){
						this.inventory.set(INV_OUTPUT_FILLED, filledContainer.copy());
					}else{
						if(!this.inventory.get(INV_OUTPUT_FILLED).isEmpty() && ItemHandlerHelper.canItemStacksStack(this.inventory.get(INV_OUTPUT_FILLED), filledContainer)){
							this.inventory.get(INV_OUTPUT_FILLED).grow(filledContainer.getCount());
						}else if(this.inventory.get(INV_OUTPUT_FILLED).isEmpty()){
							this.inventory.set(INV_OUTPUT_FILLED, filledContainer.copy());
						}
						
						this.inventory.get(INV_OUTPUT_EMPTY).shrink(1);
						if(this.inventory.get(INV_OUTPUT_EMPTY).getCount() <= 0){
							this.inventory.set(INV_OUTPUT_EMPTY, ItemStack.EMPTY);
						}
					}
					
					update = true;
				}
			}
			
			// TODO Fluid output to pipes
		}
		
		if(update){
			updateMasterBlock(null, true);
		}
	}
	
	@Override
	public NonNullList<ItemStack> getInventory(){
		return this.inventory;
	}
	
	@Override
	public int getSlotLimit(int slot){
		return 64;
	}
	
	@Override
	protected CokerUnitRecipe getRecipeForId(ResourceLocation id){
		return CokerUnitRecipe.recipes.get(id);
	}
	
	@Override
	public Set<BlockPos> getEnergyPos(){
		return Energy_IN;
	}
	
	@Override
	public Set<BlockPos> getRedstonePos(){
		return Redstone_IN;
	}
	
	@Override
	public IFluidTank[] getInternalTanks(){
		return this.tanks;
	}
	
	@Override
	public int[] getOutputSlots(){
		return new int[]{INV_OUTPUT};
	}
	
	@Override
	public int[] getOutputTanks(){
		return new int[]{TANK_OUTPUT};
	}
	
	@Override
	public int getMaxProcessPerTick(){
		return 1;
	}
	
	@Override
	public int getProcessQueueMaxLength(){
		return 2;
	}
	
	@Override
	public float getMinProcessDistance(MultiblockProcess<CokerUnitRecipe> process){
		return 1.0F;
	}
	
	@Override
	protected IFluidTank[] getAccessibleFluidTanks(Direction side){
		// TODO Fluid I/O
		CokerUnitTileEntity master = master();
		if(master != null){
			// Fluid Input
			if(this.posInMultiblock.equals(Fluid_IN)){
				if(side == null || (getIsMirrored() ? (side == getFacing().rotateYCCW()) : (side == getFacing()))){
					return new IFluidTank[]{master.tanks[TANK_INPUT]};
				}
			}
			
			// Fluid Output
			if(this.posInMultiblock.equals(Fluid_OUT)){
				if(side == null || (getIsMirrored() ? (side == getFacing().rotateYCCW()) : (side == getFacing().getOpposite()))){
					return new IFluidTank[]{master.tanks[TANK_OUTPUT]};
				}
			}
		}
		return new IFluidTank[0];
	}
	
	@Override
	public IInteractionObjectIE getGuiMaster(){
		return master();
	}
	
	@Override
	public boolean canUseGui(PlayerEntity player){
		return false;
	}
	
	@Override
	public boolean isInWorldProcessingMachine(){
		return false;
	}
	
	@Override
	public boolean isStackValid(int slot, ItemStack stack){
		return true;
	}
	
	public boolean isLadder(){
		// TODO
		return false;
	}
	
	private static CachedShapesWithTransform<BlockPos, Pair<Direction, Boolean>> SHAPES = CachedShapesWithTransform.createForMultiblock(CokerUnitTileEntity::getShape);
	
	@Override
	public VoxelShape getBlockBounds(ISelectionContext ctx){
		return SHAPES.get(this.posInMultiblock, Pair.of(getFacing(), getIsMirrored()));
	}
	
	private static List<AxisAlignedBB> getShape(BlockPos posInMultiblock){
		return Arrays.asList(new AxisAlignedBB(0.0625, 0.0625, 0.0625, 0.9375, 0.9375, 0.9375));
	}
}