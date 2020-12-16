package flaxbeard.immersivepetroleum.common.data;

import flaxbeard.immersivepetroleum.api.IPTags;
import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;

public class IPItemTags extends ItemTagsProvider{
	
	@SuppressWarnings("deprecation")
	public IPItemTags(DataGenerator generatorIn, BlockTagsProvider exhelper){
		super(generatorIn, exhelper);
	}
	
	@Override
	protected void registerTags(){
		IPTags.forAllBlocktags(this::copy);
		
		getOrCreateBuilder(IPTags.Items.bitumen).addItemEntry(IPContent.Items.bitumen);
		getOrCreateBuilder(IPTags.Items.petcoke).addItemEntry(IPContent.Items.petcoke);
		getOrCreateBuilder(IPTags.Items.petcokeStorage).addItemEntry(IPContent.Blocks.petcoke.asItem());
	}
}
