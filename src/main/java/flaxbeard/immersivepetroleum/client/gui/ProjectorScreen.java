package flaxbeard.immersivepetroleum.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.client.gui.elements.GuiReactiveList;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.items.ProjectorItem;
import flaxbeard.immersivepetroleum.common.util.projector.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Lazy;

public class ProjectorScreen extends Screen{
	static final ResourceLocation GUI_TEXTURE = new ResourceLocation("immersivepetroleum", "textures/gui/projector.png");
	
	static final ITextComponent GUI_CONFIRM = translation("gui.immersivepetroleum.projector.button.confirm");
	static final ITextComponent GUI_CANCEL = translation("gui.immersivepetroleum.projector.button.cancel");
	static final ITextComponent GUI_MIRROR = translation("gui.immersivepetroleum.projector.button.mirror");
	static final ITextComponent GUI_ROTATE_CW = translation("gui.immersivepetroleum.projector.button.rcw");
	static final ITextComponent GUI_ROTATE_CCW = translation("gui.immersivepetroleum.projector.button.rccw");
	static final ITextComponent GUI_UP = translation("gui.immersivepetroleum.projector.button.up");
	static final ITextComponent GUI_DOWN = translation("gui.immersivepetroleum.projector.button.down");
	
	private Minecraft mc = Minecraft.getInstance();
	
	private int xSize = 256;
	private int ySize = 166;
	private int guiLeft;
	private int guiTop;
	
	private Lazy<List<IMultiblock>> multiblocks;
	
	private GuiReactiveList list;
	private String[] listEntries;
	
	private SearchField searchField;
	
	Settings settings;
	Hand hand;
	
	public ProjectorScreen(Hand hand, ItemStack projector){
		super(new StringTextComponent("projector"));
		this.settings = new Settings(projector);
		this.hand = hand;
	}
	
	@Override
	protected void init(){
		this.width = this.mc.getMainWindow().getScaledWidth();
		this.height = this.mc.getMainWindow().getScaledHeight();
		
		this.guiLeft = (this.width - this.xSize) / 2;
		this.guiTop = (this.height - this.ySize) / 2;
		
		this.multiblocks = Lazy.of(() -> MultiblockHandler.getMultiblocks());
		
		this.searchField = addButton(new SearchField(this.font, this.guiLeft + 23, this.guiTop + 11));
		
		addButton(new ConfirmButton(this.guiLeft + 132, this.guiTop + 5, but -> {
			ItemStack held=Minecraft.getInstance().player.getHeldItem(this.hand);
			this.settings.applyTo(held);
			this.settings.sendPacketToServer(this.hand);
			Minecraft.getInstance().currentScreen.closeScreen();
		}));
		addButton(new CancelButton(this.guiLeft + 153, this.guiTop + 5, but -> {
			Minecraft.getInstance().currentScreen.closeScreen();
		}));
		addButton(new MirrorButton(this.guiLeft + 174, this.guiTop + 5, but -> {
			this.settings.flip();
		}));
		addButton(new RotateLeftButton(this.guiLeft + 195, this.guiTop + 5, but -> {
			this.settings.rotateCCW();
		}));
		addButton(new RotateRightButton(this.guiLeft + 216, this.guiTop + 5, but -> {
			this.settings.rotateCW();
		}));
		
		updatelist();
	}
	
	private void listaction(Button button){
		GuiReactiveList l=(GuiReactiveList)button;
		String str=this.listEntries[l.selectedOption];
		IMultiblock mb=this.multiblocks.get().get(Integer.valueOf(str));
		
		ImmersivePetroleum.log.info(l.selectedOption+" -> \""+str+"\" -> "+mb.getUniqueName());
		this.settings.setMultiblock(mb);
	}
	
	private void updatelist(){
		boolean exists = this.buttons.contains(this.list);
		
		List<String> list = new ArrayList<>();
		for(int i = 0;i < this.multiblocks.get().size();i++){
			String name = this.multiblocks.get().get(i).getUniqueName().toString();
			if(!name.contains("feedthrough")){
				list.add(Integer.toString(i));
			}
		}
		
		// Lazy search based on content
		list.removeIf(str->{
			IMultiblock mb=this.multiblocks.get().get(Integer.valueOf(str));
			String name=I18n.format("desc.immersiveengineering.info.multiblock.IE:" + ProjectorItem.getActualMBName(mb));
			
			return !name.contains(this.searchField.getText());
		});
		
		this.listEntries = list.toArray(new String[0]);
		GuiReactiveList guilist = new GuiReactiveList(this, this.guiLeft + 7, this.guiTop + 26, 100, 133, button -> listaction(button), this.listEntries);
		guilist.setPadding(1, 1, 1, 1);
		guilist.setTranslationFunc(str -> {
			IMultiblock mb=this.multiblocks.get().get(Integer.valueOf(str));
			return I18n.format("desc.immersiveengineering.info.multiblock.IE:" + ProjectorItem.getActualMBName(mb));
		});
		
		if(!exists){
			this.list = addButton(guilist);
			return;
		}
		
		int a = this.buttons.indexOf(this.list);
		int b = this.children.indexOf(this.list);
		this.list = guilist;
		if(a != -1) this.buttons.set(a, this.list);
		if(b != -1) this.children.set(b, this.list);
	}
	
	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks){
		background(matrix, mouseX, mouseY, partialTicks);
		super.render(matrix, mouseX, mouseY, partialTicks);
		this.searchField.render(matrix, mouseX, mouseY, partialTicks);
		
		for(Widget widget:this.buttons){
			if(widget.isHovered()){
				widget.renderToolTip(matrix, mouseX, mouseY);
				break;
			}
		}
		
		if(this.settings.getMultiblock()!=null){
			// TODO Multiblock Preview Rendering
		}
	}
	
	private void background(MatrixStack matrix, int mouseX, int mouseY, float partialTicks){
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getInstance().getTextureManager().bindTexture(GUI_TEXTURE);
		blit(matrix, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers){
		return super.keyPressed(keyCode, scanCode, modifiers) || this.searchField.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public boolean charTyped(char codePoint, int modifiers){
		return super.charTyped(codePoint, modifiers) || this.searchField.charTyped(codePoint, modifiers);
	}
	
	@Override
	public boolean isPauseScreen(){
		return false;
	}
	
	// CLASSES
	
	class ConfirmButton extends ProjectorScreen.SpriteButton{
		public ConfirmButton(int x, int y, Consumer<PButton> action){
			super(x, y, 18, 18, 19, 185, action);
		}
		
		@Override
		public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY){
			ProjectorScreen.this.renderTooltip(matrixStack, GUI_CONFIRM, mouseX, mouseY);
		}
	}
	
	class CancelButton extends ProjectorScreen.SpriteButton{
		public CancelButton(int x, int y, Consumer<PButton> action){
			super(x, y, 18, 18, 37, 185, action);
		}
		
		@Override
		public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY){
			ProjectorScreen.this.renderTooltip(matrixStack, GUI_CANCEL, mouseX, mouseY);
		}
	}
	
	class MirrorButton extends ProjectorScreen.SpriteButton{
		public MirrorButton(int x, int y, Consumer<PButton> action){
			super(x, y, 18, 18, 1, 185, action);
		}
		
		@Override
		public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY){
			ProjectorScreen.this.renderTooltip(matrixStack, GUI_MIRROR, mouseX, mouseY);
		}
	}
	
	class RotateRightButton extends ProjectorScreen.SpriteButton{
		public RotateRightButton(int x, int y, Consumer<PButton> action){
			super(x, y, 18, 18, 55, 185, action);
		}
		
		@Override
		public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY){
			ProjectorScreen.this.renderTooltip(matrixStack, GUI_ROTATE_CW, mouseX, mouseY);
		}
	}
	
	class RotateLeftButton extends ProjectorScreen.SpriteButton{
		public RotateLeftButton(int x, int y, Consumer<PButton> action){
			super(x, y, 18, 18, 73, 185, action);
		}
		
		@Override
		public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY){
			ProjectorScreen.this.renderTooltip(matrixStack, GUI_ROTATE_CCW, mouseX, mouseY);
		}
	}
	
	class ScrollUpButton extends ProjectorScreen.SpriteButton{
		public ScrollUpButton(int x, int y, Consumer<PButton> action){
			super(x, y, 12, 12, 0, 202, action);
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partialTicks){
			Minecraft.getInstance().getTextureManager().bindTexture(GUI_TEXTURE);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			int i = this.xOverlay;
			if(isHovered()){
				i += this.width;
			}
			
			blit(matrix, this.x, this.y, i, this.yOverlay, this.width, this.height);
		}
		
		@Override
		protected void buttonOverlay(MatrixStack matrix){
		}
		
		@Override
		public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY){
			ProjectorScreen.this.renderTooltip(matrixStack, GUI_UP, mouseX, mouseY);
		}
	}
	
	class ScrollDownButton extends ProjectorScreen.SpriteButton{
		public ScrollDownButton(int x, int y, Consumer<PButton> action){
			super(x, y, 12, 12, 0, 215, action);
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partialTicks){
			Minecraft.getInstance().getTextureManager().bindTexture(GUI_TEXTURE);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			int i = this.xOverlay;
			if(isHovered()){
				i += this.width;
			}
			
			blit(matrix, this.x, this.y, i, this.yOverlay, this.width, this.height);
		}
		
		@Override
		protected void buttonOverlay(MatrixStack matrix){
		}
		
		@Override
		public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY){
			ProjectorScreen.this.renderTooltip(matrixStack, GUI_DOWN, mouseX, mouseY);
		}
	}
	
	class SearchField extends TextFieldWidget{
		public SearchField(FontRenderer font, int x, int y){
			super(font, x, y, 77, 14, translation("gui.immersivepetroleum.projector.search"));
			setMaxStringLength(50);
			setEnableBackgroundDrawing(false);
			setVisible(true);
			setTextColor(0xFFFFFF);
		}
		
		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers){
			String s = getText();
			if(super.keyPressed(keyCode, scanCode, modifiers)){
				if(!Objects.equals(s, getText())){
					ProjectorScreen.this.updatelist();
				}
				
				return true;
			}else{
				return isFocused() && getVisible() && keyCode != 256 ? true : super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		
		@Override
		public boolean charTyped(char codePoint, int modifiers){
			if(!isFocused()){
				changeFocus(true);
				setFocused2(true);
			}
			
			String s = getText();
			if(super.charTyped(codePoint, modifiers)){
				if(!Objects.equals(s, getText())){
					ProjectorScreen.this.updatelist();
				}
				
				return true;
			}else{
				return false;
			}
		}
	}
	
	// STATIC METHODS
	
	static ITextComponent translation(String key, Object... args){
		return new TranslationTextComponent(key, args);
	}
	
	static ITextComponent text(String str){
		return new StringTextComponent(str);
	}
	
	// STATIC CLASSES
	
	abstract static class PButton extends AbstractButton{
		protected boolean selected;
		protected int bgStartX = 0, bgStartY = 166;
		protected Consumer<PButton> action;
		public PButton(int x, int y, int width, int height, Consumer<PButton> action){
			super(x, y, width, height, StringTextComponent.EMPTY);
			this.action = action;
		}
		
		@Override
		public void onPress(){
			this.action.accept(this);
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partialTicks){
			Minecraft.getInstance().getTextureManager().bindTexture(GUI_TEXTURE);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			int i = this.bgStartX;
			if(!this.active){
				i += this.width * 2;
			}else if(this.selected){
				i += this.width * 1;
			}else if(isHovered()){
				i += this.width * 3;
			}
			
			blit(matrix, this.x, this.y, i, this.bgStartY, this.width, this.height);
			buttonOverlay(matrix);
		}
		
		protected abstract void buttonOverlay(MatrixStack matrix);
		
		public boolean isSelected(){
			return this.selected;
		}
		
		public void setSelected(boolean isSelected){
			this.selected = isSelected;
		}
	}
	
	abstract static class SpriteButton extends ProjectorScreen.PButton{
		protected int iconSize = 16;
		protected final int xOverlay, yOverlay;
		public SpriteButton(int x, int y, int width, int height, int overlayX, int overlayY, Consumer<PButton> action){
			super(x, y, width, height, action);
			this.xOverlay = overlayX;
			this.yOverlay = overlayY;
		}
		
		@Override
		protected void buttonOverlay(MatrixStack matrix){
			blit(matrix, this.x + 1, this.y + 1, this.xOverlay, this.yOverlay, this.iconSize, this.iconSize);
		}
	}
}
