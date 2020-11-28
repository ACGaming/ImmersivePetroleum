package flaxbeard.immersivepetroleum.client.gui;

import static flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity.CHAMBER_A;
import static flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity.CHAMBER_B;
import static flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity.TANK_INPUT;
import static flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity.TANK_OUTPUT;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity.CokingChamber;
import flaxbeard.immersivepetroleum.common.gui.CokerUnitContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.gui.GuiUtils;

public class CokerUnitScreen extends IEContainerScreen<CokerUnitContainer>{
	static final String GUI_TEXTURE = "immersivepetroleum:textures/gui/coker.png";
	
	CokerUnitTileEntity tile;
	public CokerUnitScreen(CokerUnitContainer inventorySlotsIn, PlayerInventory inv, ITextComponent title){
		super(inventorySlotsIn, inv, title);
		this.tile = container.tile;
		
		this.xSize = 200;
		this.ySize = 187;
	}
	
	@Override
	public void render(MatrixStack transform, int mx, int my, float partialTicks){
		this.renderBackground(transform);
		super.render(transform, mx, my, partialTicks);
		this.renderHoveredTooltip(transform, mx, my);
		
		List<ITextComponent> tooltip = new ArrayList<>();
		
		// Buffer tank displays
		ClientUtils.handleGuiTank(transform, tile.bufferTanks[TANK_INPUT], guiLeft + 32, guiTop + 14, 16, 47, 0, 0, 0, 0, mx, my, GUI_TEXTURE, tooltip);
		ClientUtils.handleGuiTank(transform, tile.bufferTanks[TANK_OUTPUT], guiLeft + 152, guiTop + 14, 16, 47, 0, 0, 0, 0, mx, my, GUI_TEXTURE, tooltip);
		
		// Chamber Stats
		chamberDisplay(transform, guiLeft + 74, guiTop + 24, 6, 38, CHAMBER_A, mx, my, partialTicks, tooltip);
		chamberDisplay(transform, guiLeft + 120, guiTop + 24, 6, 38, CHAMBER_B, mx, my, partialTicks, tooltip);
		
		// Power Stored
		if(mx > guiLeft + 167 && mx < guiLeft + 175 &&
			my > guiTop + 66 && my < guiTop + 88){
			tooltip.add(new StringTextComponent(tile.getEnergyStored(null) + "/" + tile.getMaxEnergyStored(null) + " RF"));
		}
		
		if(!tooltip.isEmpty()){
			GuiUtils.drawHoveringText(transform, tooltip, mx, my, width, height, -1, font);
		}
	}
	
	private void chamberDisplay(MatrixStack matrix, int x, int y, int w, int h, int chamberId, int mx, int my, float partialTicks, List<ITextComponent> tooltip){
		CokingChamber chamber = tile.chambers[chamberId];
		// TODO Chamber Visuals
		
		switch(chamberId){
			case CHAMBER_A:{
				ClientUtils.handleGuiTank(matrix, chamber.tank, x, y, 6, 38, 0, 0, 0, 0, mx, my, GUI_TEXTURE, null);
				break;
			}
			case CHAMBER_B:{
				ClientUtils.handleGuiTank(matrix, chamber.tank, x, y, 6, 38, 0, 0, 0, 0, mx, my, GUI_TEXTURE, null);
				break;
			}
			default:break;
		}
		
		// Debugging Tooltip
		if((mx >= x && mx < x + w) && (my >= y && my < y + h)){
			float completed = 100 * chamber.getCompleted();
			float remaining = 100 * chamber.getRemaining();
			
			tooltip.add(new StringTextComponent("State: ")
					.append(new StringTextComponent("Active").mergeStyle(chamber.isActive()?TextFormatting.GREEN:TextFormatting.RED))
					.appendString(" | ")
					.append(new StringTextComponent("Dumping").mergeStyle(chamber.isActive()?TextFormatting.GREEN:TextFormatting.RED)));
			
			tooltip.add(new StringTextComponent("Items: " + chamber.getTotalAmount() + " / " + chamber.getCapacity()));
			
			ITextComponent inString = new StringTextComponent("Input: ")
					.appendString(chamber.getInputItem().getDisplayName().getString());
			
			ITextComponent outString = new StringTextComponent("Output: ")
					.appendString(chamber.getOutputItem().getDisplayName().getString());
			
			tooltip.add(inString);
			tooltip.add(outString);
			tooltip.add(new StringTextComponent(String.format(Locale.US, "%.1f", completed) + "% Completed."));
			tooltip.add(new StringTextComponent(String.format(Locale.US, "%.1f", remaining) + "% Remaining."));
			
			tooltip.add(new StringTextComponent("-------------"));
			ClientUtils.handleGuiTank(matrix, chamber.tank, x, y, w, x, 0, 0, 0, 0, mx, my, GUI_TEXTURE, tooltip);
		}
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mx, int my){
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		ClientUtils.bindTexture(GUI_TEXTURE);
		this.blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize);
		
		ClientUtils.handleGuiTank(matrixStack, tile.bufferTanks[TANK_INPUT], guiLeft + 32, guiTop + 14, 16, 47, 202, 2, 16, 47, mx, my, GUI_TEXTURE, null);
		ClientUtils.handleGuiTank(matrixStack, tile.bufferTanks[TANK_OUTPUT], guiLeft + 152, guiTop + 14, 16, 47, 202, 2, 16, 47, mx, my, GUI_TEXTURE, null);
		
		int stored = (int) (46 * (tile.getEnergyStored(null) / (float) tile.getMaxEnergyStored(null)));
		ClientUtils.drawGradientRect(guiLeft + 168, guiTop + 67 + (46 - stored), guiLeft + 175, guiTop + 88, 0xffb51500, 0xff600b00);
	}
}
