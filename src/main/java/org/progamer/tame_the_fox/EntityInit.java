package org.progamer.tame_the_fox;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tame_the_fox.MODID);

    public static final RegistryObject<EntityType<CustomFox>> CUSTOM_FOX = ENTITIES.register("custom_fox", () -> EntityType.Builder.<CustomFox>of(CustomFox::new, MobCategory.CREATURE)
            .sized(0.6F, 0.7F)
            .clientTrackingRange(8)
            .immuneTo(Blocks.SWEET_BERRY_BUSH)
            .build(new ResourceLocation(Tame_the_fox.MODID, "custom_fox").toString())
    );
}
