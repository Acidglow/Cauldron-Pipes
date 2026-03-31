package acidglow.cauldron_pipes.client;

import acidglow.cauldron_pipes.common.CauldronPipes;
import acidglow.cauldron_pipes.common.registries.ModBlockEntityTypes;
import acidglow.cauldron_pipes.client.renderer.CauldronFluidRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CauldronPipes.MODID, value = Dist.CLIENT)
public final class ClientEvents
{
	private ClientEvents()
	{
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerBlockEntityRenderer(ModBlockEntityTypes.CAULDRON.get(), CauldronFluidRenderer::new);
	}

}
