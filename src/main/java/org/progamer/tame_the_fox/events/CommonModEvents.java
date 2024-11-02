package org.progamer.tame_the_fox.events;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.progamer.tame_the_fox.CustomFox;
import org.progamer.tame_the_fox.EntityInit;
import org.progamer.tame_the_fox.Tame_the_fox;

import java.util.Map;

@Mod.EventBusSubscriber(modid = Tame_the_fox.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEvents {
    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityInit.CUSTOM_FOX.get(), CustomFox.createAttributes().build());
    }

    @SubscribeEvent
    public static void SpawnPlacementRegisterEvent(SpawnPlacementRegisterEvent event) {
//        SpawnPlacements.register(EntityType.FOX, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
//                (entityType, level, spawnType, pos, random) -> false);
        event.register(
                EntityInit.CUSTOM_FOX.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.WORLD_SURFACE,
                CustomFox::checkFoxSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
    }
}
