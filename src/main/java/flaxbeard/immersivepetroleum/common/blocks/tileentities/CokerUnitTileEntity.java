package flaxbeard.immersivepetroleum.common.blocks.tileentities;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableSet;

import blusunrize.immersiveengineering.api.utils.shapes.CachedShapesWithTransform;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity;
import flaxbeard.immersivepetroleum.api.crafting.CokerUnitRecipe;
import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.multiblocks.CokerUnitMultiblock;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class CokerUnitTileEntity extends PoweredMultiblockTileEntity<CokerUnitTileEntity, CokerUnitRecipe> implements IInteractionObjectIE, IBlockBounds{
	/** Do not Touch! Taken care of by {@link IPContent#registerTile(RegistryEvent.Register, Class, Block...)} */
	public static TileEntityType<CokerUnitTileEntity> TYPE;
	
	/** Output Tank ID */
	public static final int TANK_OUTPUT = 0;
	
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
	
	protected FluidTank[] tanks = new FluidTank[]{new FluidTank(12000)};
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
		this.tanks[TANK_OUTPUT].readFromNBT(nbt.getCompound("tank0"));
	}
	
	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket){
		super.writeCustomNBT(nbt, descPacket);
		nbt.put("tank0", this.tanks[TANK_OUTPUT].writeToNBT(new CompoundNBT()));
	}
	
	@Override
	protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource){
		return false;
	}
	
	@Override
	protected boolean canDrainTankFrom(int iTank, Direction side){
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
	
//	/** Output Capability Reference */
//	private CapabilityReference<IItemHandler> output_capref = CapabilityReference.forTileEntity(this, () -> {
//		Direction outputdir = (getIsMirrored() ? getFacing().rotateY() : getFacing().rotateYCCW());
//		return new DirectionalBlockPos(getBlockPosForPos(Chaining_OUT).offset(outputdir), outputdir);
//	}, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
	
	@Override
	public void doProcessOutput(ItemStack output){
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
		
		super.tick();
	}
	
	@Override
	public NonNullList<ItemStack> getInventory(){
		return NonNullList.create();
	}
	
	@Override
	public int getSlotLimit(int slot){
		return 0;
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
		return null;
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
		return 1;
	}
	
	@Override
	public float getMinProcessDistance(MultiblockProcess<CokerUnitRecipe> process){
		return 1.0F;
	}
	
	@Override
	protected IFluidTank[] getAccessibleFluidTanks(Direction side){
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
		return false;
	}
	
	public boolean isLadder(){
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
