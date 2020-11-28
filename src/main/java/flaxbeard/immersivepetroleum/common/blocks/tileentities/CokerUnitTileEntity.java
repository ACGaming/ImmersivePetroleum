package flaxbeard.immersivepetroleum.common.blocks.tileentities;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableSet;

import blusunrize.immersiveengineering.api.DirectionalBlockPos;
import blusunrize.immersiveengineering.api.utils.shapes.CachedShapesWithTransform;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity;
import blusunrize.immersiveengineering.common.util.CapabilityReference;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
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
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class CokerUnitTileEntity extends PoweredMultiblockTileEntity<CokerUnitTileEntity, CokerUnitRecipe> implements IInteractionObjectIE, IBlockBounds{
	/** Do not Touch! Taken care of by {@link IPContent#registerTile(RegistryEvent.Register, Class, Block...)} */
	public static TileEntityType<CokerUnitTileEntity> TYPE;
	
	public enum Inventory{
		/** Inventory Item Input */
		INPUT,
		/** Inventory Fluid Input (Filled Bucket) */
		INPUT_FILLED,
		/** Inventory Fluid Input (Empty Bucket) */
		INPUT_EMPTY,
		/** Inventory Fluid Output (Empty Bucket) */
		OUTPUT_EMPTY,
		/** Inventory Fluid Output (Filled Bucket) */
		OUTPUT_FILLED;
		
		public int id(){
			return ordinal();
		}
	}
	
	/** Input Fluid Tank<br> */
	public static final int TANK_INPUT = 0;
	
	/** Output Fluid Tank<br> */
	public static final int TANK_OUTPUT = 1;
	
	/** Coker Chamber A<br> */
	public static final int CHAMBER_A = 0;
	
	/** Coker Chamber B<br> */
	public static final int CHAMBER_B = 1;
	
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
	
	public final NonNullList<ItemStack> inventory = NonNullList.withSize(Inventory.values().length, ItemStack.EMPTY);
	public final FluidTank[] bufferTanks = {new FluidTank(16000), new FluidTank(16000)};
	public final CokingChamber[] chambers = {new CokingChamber(64, 8000), new CokingChamber(64, 8000)};
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
		
		this.bufferTanks[TANK_INPUT].readFromNBT(nbt.getCompound("tank0"));
		this.bufferTanks[TANK_OUTPUT].readFromNBT(nbt.getCompound("tank1"));
		
		this.chambers[CHAMBER_A].readFromNBT(nbt.getCompound("chamber0"));
		this.chambers[CHAMBER_B].readFromNBT(nbt.getCompound("chamber1"));
		
		if(!descPacket){
			readInventory(nbt.getCompound("inventory"));
		}
	}
	
	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket){
		super.writeCustomNBT(nbt, descPacket);
		
		nbt.put("tank0", this.bufferTanks[TANK_INPUT].writeToNBT(new CompoundNBT()));
		nbt.put("tank1", this.bufferTanks[TANK_OUTPUT].writeToNBT(new CompoundNBT()));
		
		nbt.put("chamber0", this.chambers[CHAMBER_A].writeToNBT(new CompoundNBT()));
		nbt.put("chamber1", this.chambers[CHAMBER_B].writeToNBT(new CompoundNBT()));
		
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
				
				if(master != null && master.bufferTanks[TANK_INPUT].getFluidAmount() < master.bufferTanks[TANK_INPUT].getCapacity()){
					if(master.bufferTanks[TANK_INPUT].getFluid() == FluidStack.EMPTY){
						return CokerUnitRecipe.hasRecipeWithInput(resource, true);
					}else{
						return resource.isFluidEqual(master.bufferTanks[TANK_INPUT].getFluid());
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
			
			return master != null && master.bufferTanks[TANK_OUTPUT].getFluidAmount() > 0;
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
	
	private LazyOptional<IItemHandler> insertionHandler = registerConstantCap(
			new IEInventoryHandler(5, this, 0, new boolean[]{true, false, false, false, false}, new boolean[8])
	);
	
	@Override
	public <C> LazyOptional<C> getCapability(@Nonnull Capability<C> capability, @Nullable Direction facing){
		if((facing == null || this.posInMultiblock.equals(Item_IN)) && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
			CokerUnitTileEntity master = master();
			if(master != null){
				return master.insertionHandler.cast();
			}
		}
		return super.getCapability(capability, facing);
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
		
		ItemStack inputStack = getInventory(Inventory.INPUT);
		FluidStack inputFluid = this.bufferTanks[TANK_INPUT].getFluid();
		
		if(!inputStack.isEmpty() && inputFluid.getAmount() > 0 && CokerUnitRecipe.hasRecipeWithInput(inputStack, inputFluid)){
			CokerUnitRecipe recipe = CokerUnitRecipe.findRecipe(inputStack, inputFluid);
			
			if(recipe != null && inputStack.getCount() >= recipe.inputItem.getCount() && inputFluid.getAmount() >= recipe.inputFluid.getAmount()){
				for(int i = 0;i < this.chambers.length;i++){
					CokingChamber chamber = this.chambers[i];
					boolean skipNext = false;
					
					if(!chamber.isDumping()){
						if(chamber.getRecipe() == null){
							chamber.setRecipe(recipe);
							update = true;
						}
						
						int acceptedFluid = chamber.tank.fill(copyFluid(inputFluid, chamber.getRecipe().inputFluid.getAmount()), FluidAction.SIMULATE);
						int acceptedStack = chamber.addStack(copyStack(inputStack, recipe.inputItem.getCount()), true);
						
						if(acceptedStack >= chamber.getRecipe().inputItem.getCount() && acceptedFluid >= chamber.getRecipe().inputFluid.getAmount()){
							acceptedStack = Math.min(acceptedStack, inputStack.getCount());
							acceptedFluid = Math.min(acceptedFluid, inputFluid.getAmount());
							
							chamber.addStack(copyStack(inputStack, acceptedStack), false);
							inputStack.shrink(acceptedStack);
							
							chamber.tank.fill(copyFluid(inputFluid, acceptedFluid), FluidAction.EXECUTE);
							this.bufferTanks[TANK_INPUT].drain(copyFluid(inputFluid, acceptedFluid), FluidAction.EXECUTE);
							
							skipNext = true;
							update = true;
						}
					}
					
					if(skipNext){
						break;
					}
				}
			}
		}
		
		for(int i = 0;i < this.chambers.length;i++){
			update |= this.chambers[i].tick(this);
		}
		
		if(!getInventory(Inventory.INPUT_FILLED).isEmpty() && this.bufferTanks[TANK_INPUT].getFluidAmount() < this.bufferTanks[TANK_INPUT].getCapacity()){
			ItemStack container = Utils.drainFluidContainer(this.bufferTanks[TANK_INPUT], getInventory(Inventory.INPUT_FILLED), getInventory(Inventory.INPUT_EMPTY), null);
			if(!container.isEmpty()){
				if(!getInventory(Inventory.INPUT_EMPTY).isEmpty() && ItemHandlerHelper.canItemStacksStack(getInventory(Inventory.INPUT_EMPTY), container)){
					getInventory(Inventory.INPUT_EMPTY).grow(container.getCount());
				}else if(getInventory(Inventory.INPUT_EMPTY).isEmpty()){
					setInventory(Inventory.INPUT_EMPTY, container.copy());
				}
				
				getInventory(Inventory.INPUT_FILLED).shrink(1);
				if(getInventory(Inventory.INPUT_FILLED).getCount() <= 0){
					setInventory(Inventory.INPUT_FILLED, ItemStack.EMPTY);
				}
				
				update = true;
			}
		}
		
		if(this.bufferTanks[TANK_OUTPUT].getFluidAmount() > 0){
			if(!getInventory(Inventory.OUTPUT_EMPTY).isEmpty()){
				ItemStack filledContainer = Utils.fillFluidContainer(this.bufferTanks[TANK_OUTPUT], getInventory(Inventory.OUTPUT_EMPTY), getInventory(Inventory.OUTPUT_FILLED), null);
				if(!filledContainer.isEmpty()){
					
					if(getInventory(Inventory.OUTPUT_FILLED).getCount() == 1 && !Utils.isFluidContainerFull(filledContainer)){
						setInventory(Inventory.OUTPUT_FILLED, filledContainer.copy());
					}else{
						if(!getInventory(Inventory.OUTPUT_FILLED).isEmpty() && ItemHandlerHelper.canItemStacksStack(getInventory(Inventory.OUTPUT_FILLED), filledContainer)){
							getInventory(Inventory.OUTPUT_FILLED).grow(filledContainer.getCount());
						}else if(getInventory(Inventory.OUTPUT_FILLED).isEmpty()){
							setInventory(Inventory.OUTPUT_FILLED, filledContainer.copy());
						}
						
						getInventory(Inventory.OUTPUT_EMPTY).shrink(1);
						if(getInventory(Inventory.OUTPUT_EMPTY).getCount() <= 0){
							setInventory(Inventory.OUTPUT_EMPTY, ItemStack.EMPTY);
						}
					}
					
					update = true;
				}
			}
			
			update |= FluidUtil.getFluidHandler(this.world, getBlockPosForPos(Fluid_OUT).offset(getFacing().getOpposite()), getFacing().getOpposite()).map(out -> {
				if(this.bufferTanks[TANK_OUTPUT].getFluidAmount() > 0){
					FluidStack fs = copyFluid(this.bufferTanks[TANK_OUTPUT].getFluid(), 100);
					int accepted = out.fill(fs, FluidAction.SIMULATE);
					if(accepted > 0){
						int drained = out.fill(copyFluid(fs, Math.min(fs.getAmount(), accepted)), FluidAction.EXECUTE);
						this.bufferTanks[TANK_OUTPUT].drain(copyFluid(fs, drained), FluidAction.EXECUTE);
						return true;
					}
				}
				return false;
			}).orElse(false);
		}
		
		if(update){
			updateMasterBlock(null, true);
		}
	}
	
	private FluidStack copyFluid(FluidStack fluid, int amount){
		FluidStack copy = fluid.copy();
		copy.setAmount(amount);
		return copy;
	}
	
	private ItemStack copyStack(ItemStack stack, int amount){
		ItemStack copy = stack.copy();
		copy.setCount(amount);
		return copy;
	}
	
	public ItemStack getInventory(Inventory inv){
		return this.inventory.get(inv.id());
	}
	
	public ItemStack setInventory(Inventory inv, ItemStack stack){
		return this.inventory.set(inv.id(), stack);
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
		return this.bufferTanks;
	}
	
	@Override
	public int[] getOutputSlots(){
		return new int[0];
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
		// TODO Fluid I/O with rotation and mirror
		CokerUnitTileEntity master = master();
		if(master != null){
			// Fluid Input
			if(this.posInMultiblock.equals(Fluid_IN)){
				if(side == null || (getIsMirrored() ? (side == getFacing().rotateYCCW()) : (side == getFacing()))){
					return new IFluidTank[]{master.bufferTanks[TANK_INPUT]};
				}
			}
			
			// Fluid Output
			if(this.posInMultiblock.equals(Fluid_OUT)){
				if(side == null || (getIsMirrored() ? (side == getFacing().rotateYCCW()) : (side == getFacing().getOpposite()))){
					return new IFluidTank[]{master.bufferTanks[TANK_OUTPUT]};
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
		return this.formed;
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
		return Arrays.asList(new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
	}
	
	public class CokingProcess extends MultiblockProcessInMachine<CokerUnitRecipe>{
		public CokingProcess(CokerUnitRecipe recipe){
			super(recipe, Inventory.INPUT.id());
			setInputTanks(TANK_INPUT);
		}
		
		@Override
		public boolean canProcess(PoweredMultiblockTileEntity<?, CokerUnitRecipe> multiblock){
			return super.canProcess(multiblock);
		}
		
		@Override
		public void doProcessTick(PoweredMultiblockTileEntity<?, CokerUnitRecipe> multiblock){
			super.doProcessTick(multiblock);
		}
	}
	
	// TODO Do the cycle stuff
	public static class CokingChamber{
		@Nullable
		protected CokerUnitRecipe recipe = null;
		public final FluidTank tank;
		
		/** Total capacity. inputAmount + outputAmount, should not go above this */
		protected final int capacity;
		protected int inputAmount = 0;
		protected int outputAmount = 0;
		
		protected boolean active = false;
		protected boolean dumping = false;
		
		public CokingChamber(int itemCapacity, int fluidCapacity){
			this.capacity = itemCapacity;
			this.tank = new FluidTank(fluidCapacity);
		}
		
		public void setRecipe(@Nullable CokerUnitRecipe recipe){
			this.recipe = recipe;
		}
		
		public int addStack(@Nonnull ItemStack stack, boolean simulate){
			if(!stack.isEmpty()){
				if(this.recipe != null && this.recipe.inputItem.test(stack)){
					if(simulate){
						return Math.min(this.capacity - getTotalAmount(), stack.getCount());
					}
					
					int filled = this.capacity - getTotalAmount();
					if(stack.getCount() < filled){
						this.inputAmount += stack.getCount();
						filled = stack.getCount();
					}else{
						this.inputAmount += filled;
					}
					
					return filled;
				}
			}else if(simulate){
				return Math.min(this.capacity, stack.getCount());
			}
			
			return 0;
		}
		
		/** Marks this chamber as done and starts the dumping process */
		public void markForDump(){
			this.dumping = true;
		}
		
		/** Get the next stack. Returns {@link ItemStack#EMPTY} once the chamber is empty */
		public ItemStack next(boolean peek){
			if(this.dumping && this.outputAmount > 0){
				ItemStack stack = getInputItem().copy();
				stack.setCount(this.outputAmount >= 64 ? 64 : this.outputAmount);
				
				if(!peek){
					this.outputAmount -= stack.getCount();
				}
				
				return stack;
			}
			
			return ItemStack.EMPTY;
		}
		
		/**
		 * Pulls the Requested amount of Items out of the Chamber.
		 * 
		 * @param amount
		 * @return Stack with size <= <i>amount</i>, or {@link ItemStack#EMPTY} if chamber has been emptied out.
		 */
		public ItemStack pull(int amount){
			if(this.dumping && this.outputAmount > 0){
				int count = this.outputAmount >= 64 ? 64 : this.outputAmount;
				
				ItemStack stack = getInputItem().copy();
				stack.setCount(count);
				
				this.outputAmount -= count;
				
				return stack;
			}
			
			return ItemStack.EMPTY;
		}
		
		int timer = 0;
		public boolean tick(CokerUnitTileEntity cokerunit){
			if(this.recipe == null){
				return false;
			}
			
			if(!this.dumping){
				// TODO Coking Process
				
				if(!getInputItem().isEmpty() && this.inputAmount > 0 && !this.tank.isEmpty()){
					if(cokerunit.energyStorage.getEnergyStored() >= this.recipe.getTotalProcessEnergy()){
						cokerunit.energyStorage.extractEnergy(this.recipe.getTotalProcessEnergy(), false);
						
						this.timer++;
						if(this.timer >= 10){
							this.timer = 0;
							
							this.tank.drain(Utils.copyFluidStackWithAmount(this.tank.getFluid(), this.recipe.inputFluid.getAmount(), false), FluidAction.EXECUTE);
							this.inputAmount--;
							this.outputAmount++;
							
							if(this.inputAmount <= 0){
								this.dumping = true;
							}
						}
						
						return true; // Tells the coker to update
					}
				}
				
			}else{
				// TODO Dumping Process
				// Dumping should not cost energy, because gravity is a thing too..
				
			}
			
			return false;
		}
		
		public CokingChamber readFromNBT(CompoundNBT nbt){
			this.tank.readFromNBT(nbt.getCompound("tank"));
			
			this.inputAmount = nbt.getInt("input");
			this.outputAmount = nbt.getInt("output");
			this.timer = nbt.getInt("timer");
			
			this.active = nbt.getBoolean("active");
			this.dumping = nbt.getBoolean("dumping");
			
			if(nbt.contains("recipe", NBT.TAG_STRING)){
				try{
					this.recipe = CokerUnitRecipe.recipes.get(new ResourceLocation(nbt.getString("recipe")));
				}catch(ResourceLocationException e){
					ImmersivePetroleum.log.error("Tried to load a coking recipe with an invalid name", e);
				}
			}else{
				this.recipe = null;
			}
			
			return this;
		}
		
		public CompoundNBT writeToNBT(CompoundNBT nbt){
			nbt.put("tank", this.tank.writeToNBT(new CompoundNBT()));
			
			nbt.putInt("input", this.inputAmount);
			nbt.putInt("output", this.outputAmount);
			nbt.putInt("timer", this.timer);
			
			nbt.putBoolean("active", this.active);
			nbt.putBoolean("dumping", this.dumping);
			
			
			if(this.recipe != null){
				nbt.putString("recipe", this.recipe.getId().toString());
			}
			
			return nbt;
		}
		
		public boolean isActive(){
			return this.active;
		}
		
		public boolean isDumping(){
			return this.dumping;
		}
		
		/** returns the total chamber capacity*/
		public int getCapacity(){
			return this.capacity;
		}
		
		/** returns the combined I/O Amount */
		public int getTotalAmount(){
			return this.inputAmount + this.outputAmount;
		}
		
		public float getRemaining(){
			if(getTotalAmount() > 0){
				return this.inputAmount / (float) getTotalAmount();
			}
			
			return 0.0F;
		}
		
		public float getCompleted(){
			if(getTotalAmount() > 0){
				return this.outputAmount / (float) getTotalAmount();
			}
			
			return 0.0F;
		}
		
		@Nullable
		public CokerUnitRecipe getRecipe(){
			return this.recipe;
		}
		
		/** Expected input. For displaying purposes. <b>Do not alter in any way.</b> */
		public ItemStack getInputItem(){
			if(this.recipe == null){
				return ItemStack.EMPTY;
			}
			return this.recipe.inputItem.getMatchingStacks()[0];
		}
		
		/** Expected output. For displaying purposes. <b>Do not alter in any way.</b> */
		public ItemStack getOutputItem(){
			if(this.recipe == null){
				return ItemStack.EMPTY;
			}
			
			return this.recipe.outputItem;
		}
	}
}
