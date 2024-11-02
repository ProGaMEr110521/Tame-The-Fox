package org.progamer.tame_the_fox.events;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.progamer.tame_the_fox.EntityInit;
import org.progamer.tame_the_fox.Models.CustomFoxModel;
import org.progamer.tame_the_fox.Tame_the_fox;
import org.progamer.tame_the_fox.renderer.CustomFoxRenderer;

@Mod.EventBusSubscriber(modid = Tame_the_fox.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityInit.CUSTOM_FOX.get(), CustomFoxRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CustomFoxModel.LAYER_LOCATION, CustomFoxModel::createBodyLayer);
    }
}
