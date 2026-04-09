package acidglow.cauldron_pipes.mixin;

import java.util.Map;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CauldronInteraction.Dispatcher.class)
public interface CauldronInteractionDispatcherAccessor
{
	@Accessor("items")
	Map<Item, CauldronInteraction> cauldron_pipes$getItems();
}
