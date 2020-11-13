package flaxbeard.immersivepetroleum.common.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.blocks.IEBlocks;
import blusunrize.immersiveengineering.common.blocks.metal.ConveyorBeltTileEntity;
import blusunrize.immersiveengineering.common.blocks.metal.conveyors.BasicConveyor;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.event.SchematicPlaceBlockEvent;
import flaxbeard.immersivepetroleum.api.event.SchematicPlaceBlockPostEvent;
import flaxbeard.immersivepetroleum.api.event.SchematicRenderBlockEvent;
import flaxbeard.immersivepetroleum.client.ClientProxy;
import flaxbeard.immersivepetroleum.client.ShaderUtil;
import flaxbeard.immersivepetroleum.client.gui.ProjectorScreen;
import flaxbeard.immersivepetroleum.client.render.IPRenderTypes;
import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.IPContent.Items;
import flaxbeard.immersivepetroleum.common.util.projector.MultiblockProjection;
import flaxbeard.immersivepetroleum.common.util.projector.Settings;
import flaxbeard.immersivepetroleum.common.util.projector.Settings.Mode;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersivePetroleum.MODID)
public class ProjectorItem extends IPItemBase{
	public ProjectorItem(String name){
		super(name, new Item.Properties().maxStackSize(1));
	}
	
	@OnlyIn(Dist.CLIENT)
	public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn){
		Settings settings = getSettings(stack);
		if(settings.getMultiblock() != null){
			String name = getActualMBName(settings.getMultiblock());
			
			tooltip.add(new TranslationTextComponent("chat.immersivepetroleum.info.schematic.build0"));
			tooltip.add(new TranslationTextComponent("chat.immersivepetroleum.info.schematic.build1", new TranslationTextComponent("desc.immersiveengineering.info.multiblock.IE:" + name)));
			
			Vector3i size = settings.getMultiblock().getSize(worldIn);
			tooltip.add(new StringTextComponent(size.getX() + " x " + size.getY() + " x " + size.getZ()).mergeStyle(TextFormatting.DARK_GRAY));
			
			if(settings.getPos() != null){
				int x = settings.getPos().getX();
				int y = settings.getPos().getY();
				int z = settings.getPos().getZ();
				tooltip.add(new TranslationTextComponent("chat.immersivepetroleum.info.schematic.center", x, y, z).mergeStyle(TextFormatting.DARK_GRAY));
			}
			
			ITextComponent rotation = new TranslationTextComponent("chat.immersivepetroleum.info.projector.rotated." + Direction.byHorizontalIndex(settings.getRotation().ordinal()))
					.mergeStyle(TextFormatting.DARK_GRAY);
			tooltip.add(rotation);
			
			String flipped = I18n.format("chat.immersivepetroleum.info.projector.flipped." + (settings.isMirrored() ? "yes" : "no"));
			tooltip.add(new TranslationTextComponent("chat.immersivepetroleum.info.projector.flipped", flipped).mergeStyle(TextFormatting.DARK_GRAY));
			
			ITextComponent ctrl0 = new TranslationTextComponent("chat.immersivepetroleum.info.schematic.controls1").mergeStyle(TextFormatting.DARK_GRAY);
			ITextComponent ctrl1 = new TranslationTextComponent("chat.immersivepetroleum.info.schematic.controls2", ClientProxy.keybind_preview_flip.func_238171_j_()).mergeStyle(TextFormatting.DARK_GRAY);
			
			tooltip.add(ctrl0);
			tooltip.add(ctrl1);
			
		}else{
			tooltip.add(new StringTextComponent(TextFormatting.DARK_GRAY + I18n.format("chat.immersivepetroleum.info.schematic.noMultiblock")));
		}
	}
	
	@Override
	public ITextComponent getDisplayName(ItemStack stack){
		String selfKey = getTranslationKey(stack);
		if(stack.hasTag()){
			Settings settings = getSettings(stack);
			if(settings.getMultiblock() != null){
				String name = getActualMBName(settings.getMultiblock());
				
				return new TranslationTextComponent(selfKey + ".specific", I18n.format("desc.immersiveengineering.info.multiblock.IE:" + name));
			}
		}
		return new TranslationTextComponent(selfKey);
	}
	
	/** Name cache for {@link ProjectorItem#getActualMBName(IMultiblock)} */
	static final Map<Class<? extends IMultiblock>, String> nameCache=new HashMap<>();
	/** Gets the name of the class */
	public static String getActualMBName(IMultiblock multiblock){
		if(!nameCache.containsKey(multiblock.getClass())){
			String name=multiblock.getClass().getSimpleName();
			name=name.substring(0, name.indexOf("Multiblock"));
			
			switch(name){
				case "LightningRod": name="Lightningrod"; break;
				case "ImprovedBlastfurnace": name="BlastFurnaceAdvanced"; break;
			}
			
			nameCache.put(multiblock.getClass(), name);
			//System.out.println(multiblock.getClass().getSimpleName()+" -> "+name);
		}
		
		return nameCache.get(multiblock.getClass());
	}
	
	@Override
	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items){
		if(this.isInGroup(group)){
			items.add(new ItemStack(this, 1));
		}
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn){
		ItemStack held=playerIn.getHeldItem(handIn);
		
		boolean changeMode = false;
		Settings settings = getSettings(held);
		switch(settings.getMode()){
			case PROJECTION:{
				if(worldIn.isRemote){
					if(playerIn.isSneaking()){
						if(settings.getPos() != null){
							settings.setPos(null);
							settings.sendPacketToServer(handIn);
						}else{
							changeMode = true;
						}
					}
				}
				break;
			}
			case MULTIBLOCK_SELECTION:{
				if(worldIn.isRemote){
					if(!playerIn.isSneaking()){
						Minecraft.getInstance().displayGuiScreen(new ProjectorScreen(handIn, held));
					}else{
						changeMode = true;
					}
				}
				break;
			}
			default:break;
		}
		
		if(worldIn.isRemote && changeMode){
			int modeId = settings.getMode().ordinal() + 1;
			settings.setMode(Mode.values()[modeId >= Mode.values().length ? 0 : modeId]);
			settings.applyTo(held);
			settings.sendPacketToServer(handIn);
			playerIn.sendStatusMessage(settings.getMode().getTranslated(), true);
		}
		
		return ActionResult.resultSuccess(held);
	}
	
	@Override
	public ActionResultType onItemUse(ItemUseContext context){
		World world = context.getWorld();
		PlayerEntity playerIn = context.getPlayer();
		Hand hand = context.getHand();
		BlockPos pos = context.getPos();
		Direction facing = context.getFace();
		
		ItemStack stack = playerIn.getHeldItem(hand);
		final Settings settings = ProjectorItem.getSettings(stack);
		if(playerIn.isSneaking() && settings.getPos() != null){
			if(world.isRemote){
				settings.setPos(null);
				settings.applyTo(stack);
				settings.sendPacketToServer(hand);
			}
			
			return ActionResultType.SUCCESS;
		}
		
		if(settings.getMode() == Mode.PROJECTION && settings.getPos() == null && settings.getMultiblock() != null){
			BlockState state = world.getBlockState(pos);
			
			final Mutable hit = pos.toMutable();
			if(!state.getMaterial().isReplaceable() && facing == Direction.UP){
				hit.setAndOffset(hit, 0, 1, 0);
			}
			
			Vector3i size = settings.getMultiblock().getSize(world);
			hit.setPos(alignHit(hit, playerIn, settings.getRotation(), size, settings.isMirrored()));
			
			if(playerIn.isSneaking() && playerIn.isCreative()){
				if(!world.isRemote){
					if(settings.getMultiblock().getUniqueName().getPath().contains("excavator_demo") || settings.getMultiblock().getUniqueName().getPath().contains("bucket_wheel")){
						hit.setAndOffset(hit, 0, -2, 0);
					}
					
					Predicate<MultiblockProjection.Info> pred = layer -> {
						SchematicPlaceBlockEvent event = new SchematicPlaceBlockEvent(layer.multiblock, world, layer.tPos, layer.blockInfo.pos, layer.blockInfo.state, layer.blockInfo.nbt, settings.getRotation());
						if(!MinecraftForge.EVENT_BUS.post(event)){
							world.setBlockState(layer.tPos.add(hit), event.getState());
							
							SchematicPlaceBlockPostEvent postEvent = new SchematicPlaceBlockPostEvent(layer.multiblock, world, layer.tPos, layer.blockInfo.pos, event.getState(), layer.blockInfo.nbt, settings.getRotation());
							MinecraftForge.EVENT_BUS.post(postEvent);
						}
						
						return false; // Don't ever skip a step.
					};
					
					MultiblockProjection projection = new MultiblockProjection(world, settings.getMultiblock());
					projection.setFlip(settings.isMirrored());
					projection.setRotation(settings.getRotation());
					for(int i = 0;i < projection.getLayerCount();i++){
						projection.process(i, pred);
					}
				}
				
				return ActionResultType.SUCCESS;
				
			}else{
				if(world.isRemote){
					settings.setPos(hit);
					settings.applyTo(stack);
					settings.sendPacketToServer(hand);
				}
				
				return ActionResultType.SUCCESS;
			}
		}
		
		return ActionResultType.PASS;
	}
	
	public static Settings getSettings(@Nullable ItemStack stack){
		return new Settings(stack);
	}
	
	private static BlockPos alignHit(BlockPos hit, PlayerEntity playerIn, Rotation rotation, Vector3i multiblockSize, boolean flip){
		int xd = (rotation.ordinal() % 2 == 0) ? multiblockSize.getX() : multiblockSize.getZ();
		int zd = (rotation.ordinal() % 2 == 0) ? multiblockSize.getZ() : multiblockSize.getX();
		
		Direction look = playerIn.getHorizontalFacing();
		
		if(multiblockSize.getZ() > 1 && (look == Direction.NORTH || look == Direction.SOUTH)){
			int a = zd / 2;
			if(look == Direction.NORTH){
				a += 1;
			}
			hit = hit.add(0, 0, a);
		}else if(multiblockSize.getX() > 1 && (look == Direction.EAST || look == Direction.WEST)){
			int a = xd / 2;
			if(look == Direction.WEST){
				a += 1;
			}
			hit = hit.add(a, 0, 0);
		}
		
		if(multiblockSize.getZ() > 1 && look == Direction.NORTH){
			hit = hit.add(0, 0, -zd);
		}else if(multiblockSize.getX() > 1 && look == Direction.WEST){
			hit = hit.add(-xd, 0, 0);
		}
		
		return hit;
	}
	
	@SubscribeEvent
	public static void handleConveyorPlace(SchematicPlaceBlockPostEvent event){
		IMultiblock mb = event.getMultiblock();
		BlockState state = event.getState();
		String mbName = mb.getUniqueName().getPath();
		
		if(mbName.equals("multiblocks/auto_workbench") || mbName.equals("multiblocks/bottling_machine") || mbName.equals("multiblocks/assembler") || mbName.equals("multiblocks/metal_press")){
			if(state.getBlock() == IEBlocks.MetalDevices.CONVEYORS.get(BasicConveyor.NAME)){
				TileEntity te = event.getWorld().getTileEntity(event.getWorldPos());
				if(te instanceof ConveyorBeltTileEntity){
					
				}
			}
		}
	}
	
	// STATIC SUPPORT CLASSES
	
	/** Client Rendering Stuff */
	@Mod.EventBusSubscriber(modid = ImmersivePetroleum.MODID, value=Dist.CLIENT)
	public static class ClientRenderHandler{
		@SubscribeEvent
		public static void renderLast(RenderWorldLastEvent event){
			Minecraft mc = ClientUtils.mc();
			
			if(mc.player != null){
				MatrixStack matrix = event.getMatrixStack();
				matrix.push();
				{
					ItemStack secondItem = mc.player.getHeldItemOffhand();
					boolean off = !secondItem.isEmpty() && secondItem.getItem() == Items.projector && ItemNBTHelper.hasKey(secondItem, "multiblock");
					
					// Anti-Jiggle when moving
					Vector3d renderView = ClientUtils.mc().gameRenderer.getActiveRenderInfo().getProjectedView();
					matrix.translate(-renderView.x, -renderView.y, -renderView.z);
					
					for(int i = 0;i <= 10;i++){
						ItemStack stack = (i == 10 ? secondItem : mc.player.inventory.getStackInSlot(i));
						if(!stack.isEmpty() && stack.getItem() == Items.projector && ItemNBTHelper.hasKey(stack, "settings")){
							Settings settings = getSettings(stack);
							matrix.push();
							{
								renderSchematic(matrix, settings, mc.player, mc.player.world, event.getPartialTicks(), i == mc.player.inventory.currentItem || (i == 10 && off));
							}
							matrix.pop();
						}
					}
				}
				matrix.pop();
			}
		}
		
		static final Mutable FULL_MAX = new Mutable(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		public static void renderSchematic(MatrixStack matrix, Settings settings, PlayerEntity player, World world, float partialTicks, boolean shouldRenderMoving){
			if(settings.getMultiblock() == null) return;
			
			final Mutable hit = new Mutable(FULL_MAX.getX(), FULL_MAX.getY(), FULL_MAX.getZ());
			Vector3i size = settings.getMultiblock().getSize(world);
			boolean isPlaced = false;
			
			if(settings.getPos() != null){
				hit.setPos(settings.getPos());
				isPlaced = true;
				
			}else if(shouldRenderMoving && ClientUtils.mc().objectMouseOver != null && ClientUtils.mc().objectMouseOver.getType() == Type.BLOCK){
				BlockRayTraceResult blockRTResult = (BlockRayTraceResult) ClientUtils.mc().objectMouseOver;
				
				BlockPos pos = (BlockPos) blockRTResult.getPos();
				
				BlockState state = world.getBlockState(pos);
				if(state.getMaterial().isReplaceable() || blockRTResult.getFace() != Direction.UP){
					hit.setPos(pos);
				}else{
					hit.setAndOffset(pos, 0, 1, 0);
				}
				
				hit.setPos(alignHit(hit, ClientUtils.mc().player, settings.getRotation(), size, settings.isMirrored()));
			}
			
			if(!hit.equals(FULL_MAX)){
				if(settings.getMultiblock().getUniqueName().getPath().contains("excavator_demo") || settings.getMultiblock().getUniqueName().getPath().contains("bucket_wheel")){
					hit.setAndOffset(hit, 0, -2, 0);
				}
	
				final boolean placedCopy = isPlaced;
				final List<RenderInfo> toRender = new ArrayList<>();
				final MutableInt currentLayer = new MutableInt();
				final MutableInt badBlocks = new MutableInt();
				final MutableInt goodBlocks = new MutableInt();
				BiPredicate<Integer, MultiblockProjection.Info> bipred = (layer, info)->{
					// Slice handling
					if(badBlocks.getValue() == 0 && layer > currentLayer.getValue()){
						currentLayer.setValue(layer);
					}else if(layer != currentLayer.getValue()){
						return true; // breaks the internal loop
					}
					
					if(placedCopy){ // Render only slices when placed
						if(layer == currentLayer.getValue()){
							boolean skip = false;
							BlockState toCompare = world.getBlockState(info.tPos.add(hit));
							if(info.blockInfo.state.getBlock() == toCompare.getBlock()){
								toRender.add(new RenderInfo(RenderInfo.Layer.PERFECT, info.blockInfo, info.settings, info.tPos));
								goodBlocks.increment();
								skip = true;
							}else{
								// Making it this far only needs an air check, the other already proved to be false.
								if(toCompare.getBlock() != Blocks.AIR){
									toRender.add(new RenderInfo(RenderInfo.Layer.BAD, info.blockInfo, info.settings, info.tPos));
									skip = true;
								}
								badBlocks.increment();
							}
							
							if(!skip){
								toRender.add(new RenderInfo(RenderInfo.Layer.ALL, info.blockInfo, info.settings, info.tPos));
							}
						}
					}else{ // Render all when not placed
						toRender.add(new RenderInfo(RenderInfo.Layer.ALL, info.blockInfo, info.settings, info.tPos));
					}
					
					return false;
				};
				
				MultiblockProjection projection = new MultiblockProjection(world, settings.getMultiblock());
				projection.setRotation(settings.getRotation());
				projection.setFlip(settings.isMirrored());
				projection.processAll(bipred);
				
				boolean perfect = (goodBlocks.getValue() == projection.getBlockCount());
				
				Mutable min = new Mutable(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
				Mutable max = new Mutable(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
				float flicker = (world.rand.nextInt(10) == 0) ? 0.75F : (world.rand.nextInt(20) == 0 ? 0.5F : 1F);
				matrix.translate(hit.getX(), hit.getY(), hit.getZ());
				
				toRender.sort((a, b) -> {
					if(a.layer.ordinal() > b.layer.ordinal()){
						return 1;
					}else if(a.layer.ordinal() < b.layer.ordinal()){
						return -1;
					}
					return 0;
				});
				
//				ClientUtils.bindAtlas();
				ItemStack heldStack = player.getHeldItemMainhand();
				for(RenderInfo rInfo:toRender){
					switch(rInfo.layer){
						case ALL:{ // All / Slice
							Template.BlockInfo info = rInfo.blockInfo;
							float alpha = heldStack.getItem() == info.state.getBlock().asItem() ? 1.0F : .5F;
							
							matrix.push();
							{
								renderPhantom(matrix, settings.getMultiblock(), world, info, rInfo.worldPos, flicker, alpha, partialTicks, settings.isMirrored(), rInfo.settings.getRotation());
							}
							matrix.pop();
							break;
						}
						case BAD:{ // Bad block
							matrix.push();
							{
								matrix.translate(rInfo.worldPos.getX(), rInfo.worldPos.getY(), rInfo.worldPos.getZ());
								
								renderCenteredOutlineBox(matrix, 0xFF0000, flicker);
							}
							matrix.pop();
							break;
						}
						case PERFECT:{
							min.setPos(
									(rInfo.worldPos.getX() < min.getX() ? rInfo.worldPos.getX() : min.getX()),
									(rInfo.worldPos.getY() < min.getY() ? rInfo.worldPos.getY() : min.getY()),
									(rInfo.worldPos.getZ() < min.getZ() ? rInfo.worldPos.getZ() : min.getZ()));
							
							max.setPos(
									(rInfo.worldPos.getX() > max.getX() ? rInfo.worldPos.getX() : max.getX()),
									(rInfo.worldPos.getY() > max.getY() ? rInfo.worldPos.getY() : max.getY()),
									(rInfo.worldPos.getZ() > max.getZ() ? rInfo.worldPos.getZ() : max.getZ()));
							break;
						}
					}
				}
				
				if(perfect){
					// Multiblock Correctly Built
					matrix.push();
					{
						renderOutlineBox(matrix, min, max, 0x00BF00, flicker);
					}
					matrix.pop();
					
					// Debugging Stuff
					if(!player.getHeldItemOffhand().isEmpty() && player.getHeldItemOffhand().getItem() == IPContent.debugItem){
						matrix.push();
						{
							// Min (Magenta/Purple)
							matrix.translate(min.getX(), min.getY(), min.getZ());
							renderCenteredOutlineBox(matrix, 0xFF0000, flicker);
						}
						matrix.pop();
						
						matrix.push();
						{
							// Max (Yellow)
							matrix.translate(max.getX(), max.getY(), max.getZ());
							renderCenteredOutlineBox(matrix, 0x00FF00, flicker);
						}
						matrix.pop();
						
						matrix.push();
						{
							// Center (White)
							BlockPos center = min.toImmutable().add(max);
							matrix.translate(center.getX() / 2, center.getY() / 2, center.getZ() / 2);
							
							renderCenteredOutlineBox(matrix, 0x0000FF, flicker);
						}
						matrix.pop();
					}
				}
			}
		}
		
		private static void renderPhantom(MatrixStack matrix, IMultiblock multiblock, World world, Template.BlockInfo info, BlockPos wPos, float flicker, float alpha, float partialTicks, boolean flipXZ, Rotation rotation){
			BlockRendererDispatcher dispatcher = ClientUtils.mc().getBlockRendererDispatcher();
			BlockModelRenderer blockRenderer = dispatcher.getBlockModelRenderer();
			BlockColors blockColors = ClientUtils.mc().getBlockColors();
			
			matrix.translate(wPos.getX(), wPos.getY(), wPos.getZ()); // Centers the preview block
			IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
			
			SchematicRenderBlockEvent renderEvent = new SchematicRenderBlockEvent(multiblock, world, wPos, info.pos, info.state, info.nbt, rotation);
			if(!MinecraftForge.EVENT_BUS.post(renderEvent)){
				BlockState state = renderEvent.getState();
				
//				dispatcher.renderBlock(renderEvent.getState(), matrix, buffer, 0xF000F0, 0, EmptyModelData.INSTANCE);
				
				BlockRenderType blockrendertype = state.getRenderType();
				if(blockrendertype != BlockRenderType.INVISIBLE){
					if(blockrendertype == BlockRenderType.MODEL){
						IBakedModel ibakedmodel = dispatcher.getModelForState(state);
						int i = blockColors.getColor(state, (IBlockDisplayReader) null, (BlockPos) null, 0);
						float f = (float) (i >> 16 & 255) / 255.0F;
						float f1 = (float) (i >> 8 & 255) / 255.0F;
						float f2 = (float) (i & 255) / 255.0F;
						blockRenderer.renderModel(matrix.getLast(), buffer.getBuffer(RenderType.getTranslucent()), state, ibakedmodel, f, f1, f2, 0xF000F0, 0, EmptyModelData.INSTANCE);
//						blockRenderer.renderModel(matrix.getLast(), buffer.getBuffer(RenderTypeLookup.func_239220_a_(state, false)), state, ibakedmodel, f, f1, f2, 0xF000F0, 0, EmptyModelData.INSTANCE);
						
					}else if(blockrendertype == BlockRenderType.ENTITYBLOCK_ANIMATED){
						ItemStack stack = new ItemStack(state.getBlock());
						stack.getItem().getItemStackTileEntityRenderer().func_239207_a_(stack, ItemCameraTransforms.TransformType.NONE, matrix, buffer, 0xF000F0, 0);
					}
				}
			}
			
			ShaderUtil.alpha_static(flicker * alpha, ClientUtils.mc().player.ticksExisted + partialTicks);
			buffer.finish();
			ShaderUtil.releaseShader();
		}
		
		private static void renderOutlineBox(MatrixStack matrix, Vector3i min, Vector3i max, int rgb, float flicker){
			IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
			IVertexBuilder builder = buffer.getBuffer(IPRenderTypes.TRANSLUCENT_LINES);
			
			float alpha = 0.25F + (0.5F * flicker);
			
			float xMax = Math.abs(max.getX() - min.getX()) + 1;
			float yMax = Math.abs(max.getY() - min.getY()) + 1;
			float zMax = Math.abs(max.getZ() - min.getZ()) + 1;
			
			float r = ((rgb >> 16) & 0xFF) / 255.0F;
			float g = ((rgb >> 8) & 0xFF) / 255.0F;
			float b = ((rgb >> 0) & 0xFF) / 255.0F;
			
			matrix.translate(-1, 0, -1);
			matrix.scale(xMax, yMax, zMax);
			Matrix4f mat = matrix.getLast().getMatrix();
			
			builder.pos(mat, 0.0F, 1.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 1.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 1.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 1.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 1.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 0.0F, 1.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 0.0F, 1.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 0.0F, 1.0F, 0.0F).color(r, g, b, alpha).endVertex();
			
			builder.pos(mat, 0.0F, 1.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 0.0F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 1.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 0.0F, 1.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 0.0F, 0.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 1.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 0.0F, 1.0F).color(r, g, b, alpha).endVertex();
			
			builder.pos(mat, 0.0F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 0.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 1.0F, 0.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 0.0F, 0.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 0.0F, 0.0F, 1.0F).color(r, g, b, alpha).endVertex();
			builder.pos(mat, 0.0F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();
			
			buffer.finish();
		}
		
		private static void renderCenteredOutlineBox(MatrixStack matrix, int rgb, float flicker){
			IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
			IVertexBuilder builder = buffer.getBuffer(IPRenderTypes.TRANSLUCENT_LINES);
			
			matrix.translate(0.5, 0.5, 0.5);
			matrix.scale(1.01F, 1.01F, 1.01F);
			Matrix4f mat = matrix.getLast().getMatrix();
			
			float r = ((rgb >> 16) & 0xFF) / 255.0F;
			float g = ((rgb >> 8) & 0xFF) / 255.0F;
			float b = ((rgb >> 0) & 0xFF) / 255.0F;
			float alpha = .375F * flicker;
			float s = 0.5F;
			
			builder.pos(mat, -s, s, -s)	.color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, s, -s)	.color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, s, -s)	.color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, s,  s)	.color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, s,  s)	.color(r, g, b, alpha).endVertex();
			builder.pos(mat, -s, s,  s)	.color(r, g, b, alpha).endVertex();
			builder.pos(mat, -s, s,  s)	.color(r, g, b, alpha).endVertex();
			builder.pos(mat, -s, s, -s)	.color(r, g, b, alpha).endVertex();
			
			builder.pos(mat, -s,  s, -s).color(r, g, b, alpha).endVertex();
			builder.pos(mat, -s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s,  s, -s).color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.pos(mat, -s,  s,  s).color(r, g, b, alpha).endVertex();
			builder.pos(mat, -s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s,  s,  s).color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, -s,  s).color(r, g, b, alpha).endVertex();
			
			builder.pos(mat, -s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.pos(mat,  s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.pos(mat, -s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.pos(mat, -s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.pos(mat, -s, -s, -s).color(r, g, b, alpha).endVertex();
			
			buffer.finish();
		}
		
		@SubscribeEvent
		public static void handleConveyorsAndPipes(SchematicRenderBlockEvent event){
			BlockState state = event.getState();
			
			if(state.getBlock() == IEBlocks.MetalDevices.fluidPipe){
				event.setState(IPContent.Blocks.dummyPipe.getDefaultState());
			}else if(state.getBlock() == IEBlocks.MetalDevices.CONVEYORS.get(BasicConveyor.NAME)){
				// event.setState(IPContent.Blocks.dummyConveyor.getDefaultState());
			}
		}
	}
	
	/** Client Input Stuff */
	@Mod.EventBusSubscriber(modid = ImmersivePetroleum.MODID, value = Dist.CLIENT)
	public static class ClientInputHandler{
		static boolean shiftHeld = false;
		
		@SubscribeEvent
		public static void onPlayerTick(TickEvent.PlayerTickEvent event){
			if(event.side == LogicalSide.CLIENT && event.player != null && event.player == ClientUtils.mc().getRenderViewEntity()){
				if(event.phase == Phase.END){
					if(!ClientProxy.keybind_preview_flip.isInvalid() && ClientProxy.keybind_preview_flip.isPressed() && shiftHeld){
						doAFlip();
					}
				}
			}
		}
		
		@SubscribeEvent
		public static void handleScroll(InputEvent.MouseScrollEvent event){
			double delta = event.getScrollDelta();
			
			if(shiftHeld && delta != 0.0){
				PlayerEntity player = ClientUtils.mc().player;
				ItemStack mainItem = player.getHeldItemMainhand();
				ItemStack secondItem = player.getHeldItemOffhand();
				
				boolean main = !mainItem.isEmpty() && mainItem.getItem() == Items.projector && ItemNBTHelper.hasKey(mainItem, "settings", NBT.TAG_COMPOUND);
				boolean off = !secondItem.isEmpty() && secondItem.getItem() == Items.projector && ItemNBTHelper.hasKey(secondItem, "settings", NBT.TAG_COMPOUND);
				
				if(main || off){
					ItemStack target = main ? mainItem : secondItem;
					
					if(shiftHeld){
						Settings settings = getSettings(target);
						
						if(delta > 0){
							settings.rotateCCW();
						}else{
							settings.rotateCW();
						}
						
						settings.applyTo(target);
						settings.sendPacketToServer(main ? Hand.MAIN_HAND : Hand.OFF_HAND);
						
						Direction facing = Direction.byHorizontalIndex(settings.getRotation().ordinal());
						player.sendStatusMessage(new TranslationTextComponent("chat.immersivepetroleum.info.projector.rotated." + facing), true);
						
						event.setCanceled(true);
					}
				}
			}
		}
		
		@SubscribeEvent
		public static void handleKey(InputEvent.KeyInputEvent event){
			if(event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT || event.getKey() == GLFW.GLFW_KEY_LEFT_SHIFT){
				switch(event.getAction()){
					case GLFW.GLFW_PRESS:{
						shiftHeld = true;
						return;
					}
					case GLFW.GLFW_RELEASE:{
						shiftHeld = false;
						return;
					}
				}
			}
		}
		
		@SubscribeEvent
		public static void handleMouseInput(InputEvent.MouseInputEvent event){}
		
		private static void doAFlip(){
			PlayerEntity player = ClientUtils.mc().player;
			ItemStack mainItem = player.getHeldItemMainhand();
			ItemStack secondItem = player.getHeldItemOffhand();
			
			boolean main = !mainItem.isEmpty() && mainItem.getItem() == Items.projector && ItemNBTHelper.hasKey(mainItem, "settings", NBT.TAG_COMPOUND);
			boolean off = !secondItem.isEmpty() && secondItem.getItem() == Items.projector && ItemNBTHelper.hasKey(secondItem, "settings", NBT.TAG_COMPOUND);
			ItemStack target = main ? mainItem : secondItem;
			
			if(main || off){
				Settings settings = ProjectorItem.getSettings(target);
				
				settings.flip();
				settings.applyTo(target);
				settings.sendPacketToServer(main ? Hand.MAIN_HAND : Hand.OFF_HAND);
				
				String yesno = settings.isMirrored() ? I18n.format("chat.immersivepetroleum.info.projector.flipped.yes") : I18n.format("chat.immersivepetroleum.info.projector.flipped.no");
				player.sendStatusMessage(new TranslationTextComponent("chat.immersivepetroleum.info.projector.flipped", yesno), true);
			}
		}
	}
	
	private static class RenderInfo{
		public final Layer layer;
		public final Template.BlockInfo blockInfo;
		public final BlockPos worldPos;
		public final PlacementSettings settings;
		public RenderInfo(Layer layer, Template.BlockInfo blockInfo, PlacementSettings settings, BlockPos worldPos){
			this.layer = layer;
			this.blockInfo = blockInfo;
			this.worldPos = worldPos;
			this.settings = settings;
		}
		
		public static enum Layer{
			ALL, BAD, PERFECT;
		}
	}
}
