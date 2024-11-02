package org.progamer.tame_the_fox;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import io.netty.channel.ChannelHandler;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.progamer.tame_the_fox.goals.FoxSitWhenOrderedToGoal;
import org.spongepowered.asm.mixin.Shadow;

public class CustomFox extends TamableAnimal{
    private static final float START_HEALTH = 8.0F;
    private static final float TAME_HEALTH = 20.0F;

    private static final EntityDataAccessor<Integer> DATA_TYPE_ID = SynchedEntityData.defineId(CustomFox.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(CustomFox.class, EntityDataSerializers.BYTE);
    private static final int FLAG_SITTING = 1;
    public static final int FLAG_CROUCHING = 4;
    public static final int FLAG_INTERESTED = 8;
    public static final int FLAG_POUNCING = 16;
    private static final int FLAG_SLEEPING = 32;
    private static final int FLAG_FACEPLANTED = 64;
    private static final int FLAG_DEFENDING = 128;
    private static final EntityDataAccessor<Optional<UUID>> DATA_TRUSTED_ID_0 = SynchedEntityData.defineId(CustomFox.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_TRUSTED_ID_1 = SynchedEntityData.defineId(CustomFox.class, EntityDataSerializers.OPTIONAL_UUID);
    static final Predicate<ItemEntity> ALLOWED_ITEMS = (p_289438_) -> {
        return !p_289438_.hasPickUpDelay() && p_289438_.isAlive();
    };
    private static final Predicate<Entity> TRUSTED_TARGET_SELECTOR = (p_287481_) -> {
        if (!(p_287481_ instanceof LivingEntity livingentity)) {
            return false;
        } else {
            return livingentity.getLastHurtMob() != null && livingentity.getLastHurtMobTimestamp() < livingentity.tickCount + 600;
        }
    };
    static final Predicate<Entity> STALKABLE_PREY = (p_28498_) -> {
        return p_28498_ instanceof Chicken || p_28498_ instanceof Rabbit;
    };
    private static final Predicate<Entity> AVOID_PLAYERS = (p_28463_) -> {
        return !p_28463_.isDiscrete() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p_28463_);
    };
    private static final int MIN_TICKS_BEFORE_EAT = 600;
    private Goal landTargetGoal;
    private Goal turtleEggTargetGoal;
    private Goal fishTargetGoal;
    private float interestedAngle;
    private float interestedAngleO;
    float crouchAmount;
    float crouchAmountO;
    private int ticksSinceEaten;

    public CustomFox(EntityType<? extends CustomFox> p_28451_, Level p_28452_) {
        super(p_28451_, p_28452_);
        this.setTame(false);
        this.lookControl = new CustomFox.FoxLookControl();
        this.moveControl = new CustomFox.FoxMoveControl();
        this.setPathfindingMalus(BlockPathTypes.DANGER_OTHER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, 0.0F);
        this.setCanPickUpLoot(true);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TRUSTED_ID_0, Optional.empty());
        this.entityData.define(DATA_TRUSTED_ID_1, Optional.empty());
        this.entityData.define(DATA_TYPE_ID, 0);
        this.entityData.define(DATA_FLAGS_ID, (byte)0);
    }

    protected void registerGoals() {
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(6, new NonTameRandomTargetGoal<>(this, Turtle.class, false, Turtle.BABY_ON_LAND_SELECTOR));
//        this.goalSelector.addGoal(14, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));

        this.goalSelector.addGoal(25, new FoxSitWhenOrderedToGoal(this));

        this.landTargetGoal = new NearestAttackableTargetGoal<>(this, Animal.class, 10, false, false, (p_28604_) -> {
            return p_28604_ instanceof Chicken || p_28604_ instanceof Rabbit;
        });
        this.turtleEggTargetGoal = new NearestAttackableTargetGoal<>(this, Turtle.class, 10, false, false, Turtle.BABY_ON_LAND_SELECTOR);
        this.fishTargetGoal = new NearestAttackableTargetGoal<>(this, AbstractFish.class, 20, false, false, (p_28600_) -> {
            return p_28600_ instanceof AbstractSchoolingFish;
        });
        this.goalSelector.addGoal(0, new CustomFox.FoxFloatGoal());
        this.goalSelector.addGoal(0, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(1, new CustomFox.FaceplantGoal());
        this.goalSelector.addGoal(2, new CustomFox.FoxPanicGoal(2.2D));
        this.goalSelector.addGoal(3, new CustomFox.FoxBreedGoal(1.0D));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 16.0F, 1.6D, 1.4D, (player) -> {
            return AVOID_PLAYERS.test(player) && !this.trusts(player.getUUID()) && !this.isDefending();
        }));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Wolf.class, 8.0F, 1.6D, 1.4D, (p_289439_) -> {
            return !((Wolf)p_289439_).isTame() && !this.isDefending();
        }));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, PolarBear.class, 8.0F, 1.6D, 1.4D, (p_28585_) -> {
            return !this.isDefending();
        }));
        this.goalSelector.addGoal(5, new CustomFox.StalkPreyGoal());
        this.goalSelector.addGoal(6, new CustomFox.FoxPounceGoal());
        this.goalSelector.addGoal(6, new CustomFox.SeekShelterGoal(1.25D));
        this.goalSelector.addGoal(7, new CustomFox.FoxMeleeAttackGoal((double)1.2F, true));
        this.goalSelector.addGoal(7, new CustomFox.SleepGoal());
        this.goalSelector.addGoal(8, new CustomFox.FoxFollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(9, new CustomFox.FoxStrollThroughVillageGoal(32, 200));
        this.goalSelector.addGoal(10, new CustomFox.FoxEatBerriesGoal((double)1.2F, 12, 1));
        this.goalSelector.addGoal(10, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(11, new CustomFox.FoxSearchForItemsGoal());
        this.goalSelector.addGoal(15, new CustomFox.FoxLookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(13, new CustomFox.PerchAndSearchGoal());
        this.targetSelector.addGoal(3, new CustomFox.DefendTrustedTargetGoal(LivingEntity.class, false, false, (p_289440_) -> {
            return TRUSTED_TARGET_SELECTOR.test(p_289440_) && !this.trusts(p_289440_.getUUID());
        }));
    }

    public InteractionResult mobInteract(Player player, InteractionHand p_30413_) {
        ItemStack itemstack = player.getItemInHand(p_30413_);
        Item item = itemstack.getItem();
        if (this.level().isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || itemstack.is(Items.SWEET_BERRIES) && !this.isTame();
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else if (this.isTame()) {
            if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                this.heal((float)itemstack.getFoodProperties(this).getNutrition());
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }

                this.gameEvent(GameEvent.EAT, this);
                return InteractionResult.SUCCESS;
            } else {
                InteractionResult interactionresult = super.mobInteract(player, p_30413_);
                if ((!interactionresult.consumesAction() || this.isBaby()) && this.isOwnedBy(player)) {
//                    this.setSitting(!this.getFlag(1));
                    this.jumping = !this.jumping;
                    this.navigation.stop();
                    this.setOrderedToSit(!this.isOrderedToSit());
//                    this.setTarget((LivingEntity)null);
                    return InteractionResult.SUCCESS;
                } else {
                    return interactionresult;
                }
            }
        } else if (itemstack.is(Items.SWEET_BERRIES)) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            if (this.random.nextInt(3) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)) {
                this.tame(player);
                this.navigation.stop();
                this.setTarget((LivingEntity)null);
//                this.setOrderedToSit(true);
                this.setSitting(true);
                this.addTrustedUUID(player.getUUID());
                this.level().broadcastEntityEvent(this, (byte)7);
            } else {
                this.level().broadcastEntityEvent(this, (byte)6);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(player, p_30413_);
        }
    }

    public SoundEvent getEatingSound(ItemStack p_28540_) {
        return SoundEvents.FOX_EAT;
    }


    public void aiStep() {
        if (!this.level().isClientSide && this.isAlive() && this.isEffectiveAi()) {
            ++this.ticksSinceEaten;
            ItemStack itemstack = this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (this.canEat(itemstack)) {
                if (this.ticksSinceEaten > 600) {
                    ItemStack itemstack1 = itemstack.finishUsingItem(this.level(), this);
                    if (!itemstack1.isEmpty()) {
                        this.setItemSlot(EquipmentSlot.MAINHAND, itemstack1);
                    }

                    this.ticksSinceEaten = 0;
                } else if (this.ticksSinceEaten > 560 && this.random.nextFloat() < 0.1F) {
                    this.playSound(this.getEatingSound(itemstack), 1.0F, 1.0F);
                    this.level().broadcastEntityEvent(this, (byte)45);
                }
            }

            LivingEntity livingentity = this.getTarget();
            if (livingentity == null || !livingentity.isAlive()) {
                this.setIsCrouching(false);
                this.setIsInterested(false);
            }
        }

        if (this.isSleeping() || this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        }

        super.aiStep();
        if (this.isDefending() && this.random.nextFloat() < 0.05F) {
            this.playSound(SoundEvents.FOX_AGGRO, 1.0F, 1.0F);
        }

    }

    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    private boolean canEat(ItemStack p_28598_) {
        return p_28598_.getItem().isEdible() && this.getTarget() == null && this.onGround() && !this.isSleeping();
    }

    protected void populateDefaultEquipmentSlots(RandomSource p_218171_, DifficultyInstance p_218172_) {
        if (p_218171_.nextFloat() < 0.2F) {
            float f = p_218171_.nextFloat();
            ItemStack itemstack;
            if (f < 0.05F) {
                itemstack = new ItemStack(Items.EMERALD);
            } else if (f < 0.2F) {
                itemstack = new ItemStack(Items.EGG);
            } else if (f < 0.4F) {
                itemstack = p_218171_.nextBoolean() ? new ItemStack(Items.RABBIT_FOOT) : new ItemStack(Items.RABBIT_HIDE);
            } else if (f < 0.6F) {
                itemstack = new ItemStack(Items.WHEAT);
            } else if (f < 0.8F) {
                itemstack = new ItemStack(Items.LEATHER);
            } else {
                itemstack = new ItemStack(Items.FEATHER);
            }

            this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
        }

    }

    public void handleEntityEvent(byte p_28456_) {
        if (p_28456_ == 45) {
            ItemStack itemstack = this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!itemstack.isEmpty()) {
                for(int i = 0; i < 8; ++i) {
                    Vec3 vec3 = (new Vec3(((double)this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)).xRot(-this.getXRot() * ((float)Math.PI / 180F)).yRot(-this.getYRot() * ((float)Math.PI / 180F));
                    this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, itemstack), this.getX() + this.getLookAngle().x / 2.0D, this.getY(), this.getZ() + this.getLookAngle().z / 2.0D, vec3.x, vec3.y + 0.05D, vec3.z);
                }
            }
        } else {
            super.handleEntityEvent(p_28456_);
        }

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, (double)0.3F).add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Nullable
    public CustomFox getBreedOffspring(ServerLevel p_148912_, AgeableMob p_148913_) {
//        CustomFox fox = EntityType.FOX.create(p_148912_);
        CustomFox fox = EntityInit.CUSTOM_FOX.get().create(p_148912_);
        if (fox != null) {
            fox.setVariant(this.random.nextBoolean() ? this.getVariant() : ((CustomFox)p_148913_).getVariant());
        }

        return fox;
    }

    public static boolean checkFoxSpawnRules(EntityType<CustomFox> p_218176_, LevelAccessor p_218177_, MobSpawnType p_218178_, BlockPos p_218179_, RandomSource p_218180_) {
        return p_218177_.getBlockState(p_218179_.below()).is(BlockTags.FOXES_SPAWNABLE_ON) && isBrightEnoughToSpawn(p_218177_, p_218179_);
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_28487_, DifficultyInstance p_28488_, MobSpawnType p_28489_, @Nullable SpawnGroupData p_28490_, @Nullable CompoundTag p_28491_) {
        Holder<Biome> holder = p_28487_.getBiome(this.blockPosition());
        CustomFox.Type fox$type = CustomFox.Type.byBiome(holder);
        boolean flag = false;
        if (p_28490_ instanceof CustomFox.FoxGroupData fox$foxgroupdata) {
            fox$type = fox$foxgroupdata.type;
            if (fox$foxgroupdata.getGroupSize() >= 2) {
                flag = true;
            }
        } else {
            p_28490_ = new CustomFox.FoxGroupData(fox$type);
        }

        this.setVariant(fox$type);
        if (flag) {
            this.setAge(-24000);
        }

        if (p_28487_ instanceof ServerLevel) {
            this.setTargetGoals();
        }

        this.populateDefaultEquipmentSlots(p_28487_.getRandom(), p_28488_);
        return super.finalizeSpawn(p_28487_, p_28488_, p_28489_, p_28490_, p_28491_);
    }

    private void setTargetGoals() {
        if (this.getVariant() == CustomFox.Type.RED) {
            this.targetSelector.addGoal(4, this.landTargetGoal);
            this.targetSelector.addGoal(4, this.turtleEggTargetGoal);
            this.targetSelector.addGoal(6, this.fishTargetGoal);
        } else {
            this.targetSelector.addGoal(4, this.fishTargetGoal);
            this.targetSelector.addGoal(6, this.landTargetGoal);
            this.targetSelector.addGoal(6, this.turtleEggTargetGoal);
        }

    }

    protected void usePlayerItem(Player p_148908_, InteractionHand p_148909_, ItemStack p_148910_) {
        if (this.isFood(p_148910_)) {
            this.playSound(this.getEatingSound(p_148910_), 1.0F, 1.0F);
        }

        super.usePlayerItem(p_148908_, p_148909_, p_148910_);
    }

    protected float getStandingEyeHeight(Pose p_28500_, EntityDimensions p_28501_) {
        return this.isBaby() ? p_28501_.height * 0.85F : 0.4F;
    }

    public CustomFox.Type getVariant() {
        return CustomFox.Type.byId(this.entityData.get(DATA_TYPE_ID));
    }

    public void setVariant(CustomFox.Type p_28465_) {
        this.entityData.set(DATA_TYPE_ID, p_28465_.getId());
    }

    List<UUID> getTrustedUUIDs() {
        List<UUID> list = Lists.newArrayList();
        list.add(this.entityData.get(DATA_TRUSTED_ID_0).orElse((UUID)null));
        list.add(this.entityData.get(DATA_TRUSTED_ID_1).orElse((UUID)null));
        return list;
    }

    void addTrustedUUID(@Nullable UUID p_28516_) {
        if (this.entityData.get(DATA_TRUSTED_ID_0).isPresent()) {
            this.entityData.set(DATA_TRUSTED_ID_1, Optional.ofNullable(p_28516_));
        } else {
            this.entityData.set(DATA_TRUSTED_ID_0, Optional.ofNullable(p_28516_));
        }

    }

    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        List<UUID> list = this.getTrustedUUIDs();
        ListTag listtag = new ListTag();

        for(UUID uuid : list) {
            if (uuid != null) {
                listtag.add(NbtUtils.createUUID(uuid));
            }
        }

        compoundTag.put("Trusted", listtag);
        compoundTag.putBoolean("Sleeping", this.isSleeping());
        compoundTag.putString("Type", this.getVariant().getSerializedName());
        compoundTag.putBoolean("Sitting", this.isSitting());
        compoundTag.putBoolean("Crouching", this.isCrouching());
    }

    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        ListTag listtag = compoundTag.getList("Trusted", 11);

        for(int i = 0; i < listtag.size(); ++i) {
            this.addTrustedUUID(NbtUtils.loadUUID(listtag.get(i)));
        }

        this.setSleeping(compoundTag.getBoolean("Sleeping"));
        this.setVariant(CustomFox.Type.byName(compoundTag.getString("Type")));
        this.setSitting(compoundTag.getBoolean("Sitting"));
        this.setIsCrouching(compoundTag.getBoolean("Crouching"));
        if (this.level() instanceof ServerLevel) {
            this.setTargetGoals();
        }

    }

    public boolean isSitting() {
        return this.getFlag(1);
    }

    public void setSitting(boolean p_28611_) {
        this.setFlag(1, p_28611_);
    }

    public boolean isFaceplanted() {
        return this.getFlag(64);
    }

    void setFaceplanted(boolean p_28619_) {
        this.setFlag(64, p_28619_);
    }

    boolean isDefending() {
        return this.getFlag(128);
    }

    void setDefending(boolean p_28623_) {
        this.setFlag(128, p_28623_);
    }

    public boolean isSleeping() {
        return this.getFlag(32);
    }

    void setSleeping(boolean p_28627_) {
        this.setFlag(32, p_28627_);
    }

    private void setFlag(int p_28533_, boolean p_28534_) {
        if (p_28534_) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | p_28533_));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~p_28533_));
        }

    }

    private boolean getFlag(int p_28609_) {
        return (this.entityData.get(DATA_FLAGS_ID) & p_28609_) != 0;
    }

    public boolean canTakeItem(ItemStack p_28552_) {
        EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(p_28552_);
        if (!this.getItemBySlot(equipmentslot).isEmpty()) {
            return false;
        } else {
            return equipmentslot == EquipmentSlot.MAINHAND && super.canTakeItem(p_28552_);
        }
    }

    public boolean canHoldItem(ItemStack p_28578_) {
        Item item = p_28578_.getItem();
        ItemStack itemstack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return itemstack.isEmpty() || this.ticksSinceEaten > 0 && item.isEdible() && !itemstack.getItem().isEdible();
    }

    private void spitOutItem(ItemStack p_28602_) {
        if (!p_28602_.isEmpty() && !this.level().isClientSide) {
            ItemEntity itementity = new ItemEntity(this.level(), this.getX() + this.getLookAngle().x, this.getY() + 1.0D, this.getZ() + this.getLookAngle().z, p_28602_);
            itementity.setPickUpDelay(40);
            itementity.setThrower(this.getUUID());
            this.playSound(SoundEvents.FOX_SPIT, 1.0F, 1.0F);
            this.level().addFreshEntity(itementity);
        }
    }

    private void dropItemStack(ItemStack p_28606_) {
        ItemEntity itementity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), p_28606_);
        this.level().addFreshEntity(itementity);
    }

    protected void pickUpItem(ItemEntity p_28514_) {
        ItemStack itemstack = p_28514_.getItem();
        if (this.canHoldItem(itemstack)) {
            int i = itemstack.getCount();
            if (i > 1) {
                this.dropItemStack(itemstack.split(i - 1));
            }

            this.spitOutItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
            this.onItemPickup(p_28514_);
            this.setItemSlot(EquipmentSlot.MAINHAND, itemstack.split(1));
            this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            this.take(p_28514_, itemstack.getCount());
            p_28514_.discard();
            this.ticksSinceEaten = 0;
        }

    }

    public void tick() {
        super.tick();
//        if (this.isOrderedToSit()) {
//            this.goalSelector.setNewGoalRate(0);
//        }
        if (this.isEffectiveAi()) {
            boolean flag = this.isInWater();
            if (flag || this.getTarget() != null || this.level().isThundering()) {
                this.wakeUp();
            }

            if (flag || this.isSleeping()) {
                this.setSitting(false);
            }

            if (this.isFaceplanted() && this.level().random.nextFloat() < 0.2F) {
                BlockPos blockpos = this.blockPosition();
                BlockState blockstate = this.level().getBlockState(blockpos);
                this.level().levelEvent(2001, blockpos, Block.getId(blockstate));
            }
        }

        this.interestedAngleO = this.interestedAngle;
        if (this.isInterested()) {
            this.interestedAngle += (1.0F - this.interestedAngle) * 0.4F;
        } else {
            this.interestedAngle += (0.0F - this.interestedAngle) * 0.4F;
        }

        this.crouchAmountO = this.crouchAmount;
        if (this.isCrouching()) {
            this.crouchAmount += 0.2F;
            if (this.crouchAmount > 3.0F) {
                this.crouchAmount = 3.0F;
            }
        } else {
            this.crouchAmount = 0.0F;
        }

    }

    public boolean isFood(ItemStack p_28594_) {
        return p_28594_.is(ItemTags.FOX_FOOD);
    }

    protected void onOffspringSpawnedFromEgg(Player p_28481_, Mob p_28482_) {
        ((CustomFox)p_28482_).addTrustedUUID(p_28481_.getUUID());
    }

    public boolean isPouncing() {
        return this.getFlag(16);
    }

    public void setIsPouncing(boolean p_28613_) {
        this.setFlag(16, p_28613_);
    }

    public boolean isJumping() {
        return this.jumping;
    }

    public boolean isFullyCrouched() {
        return this.crouchAmount == 3.0F;
    }

    public void setIsCrouching(boolean p_28615_) {
        this.setFlag(4, p_28615_);
    }

    public boolean isCrouching() {
        return this.getFlag(4);
    }

    public void setIsInterested(boolean p_28617_) {
        this.setFlag(8, p_28617_);
    }

    public boolean isInterested() {
        return this.getFlag(8);
    }

    public float getHeadRollAngle(float p_28621_) {
        return Mth.lerp(p_28621_, this.interestedAngleO, this.interestedAngle) * 0.11F * (float)Math.PI;
    }

    public float getCrouchAmount(float p_28625_) {
        return Mth.lerp(p_28625_, this.crouchAmountO, this.crouchAmount);
    }

    public void setTarget(@Nullable LivingEntity p_28574_) {
        if (this.isDefending() && p_28574_ == null) {
            this.setDefending(false);
        }

        super.setTarget(p_28574_);
    }

    protected int calculateFallDamage(float p_28545_, float p_28546_) {
        return Mth.ceil((p_28545_ - 5.0F) * p_28546_);
    }

    void wakeUp() {
        this.setSleeping(false);
    }

    void clearStates() {
        this.setIsInterested(false);
        this.setIsCrouching(false);
        this.setSitting(false);
        this.setSleeping(false);
        this.setDefending(false);
        this.setFaceplanted(false);
    }

    boolean canMove() {
        return !this.isSleeping() && !this.isSitting() && !this.isFaceplanted();
    }

    public void playAmbientSound() {
        SoundEvent soundevent = this.getAmbientSound();
        if (soundevent == SoundEvents.FOX_SCREECH) {
            this.playSound(soundevent, 2.0F, this.getVoicePitch());
        } else {
            super.playAmbientSound();
        }

    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        if (this.isSleeping()) {
            return SoundEvents.FOX_SLEEP;
        } else {
            if (!this.level().isDay() && this.random.nextFloat() < 0.1F) {
                List<Player> list = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D, 16.0D, 16.0D), EntitySelector.NO_SPECTATORS);
                if (list.isEmpty()) {
                    return SoundEvents.FOX_SCREECH;
                }
            }

            return SoundEvents.FOX_AMBIENT;
        }
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource p_28548_) {
        return SoundEvents.FOX_HURT;
    }

    @Nullable
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    public void setTame(boolean p_30443_) {
        super.setTame(p_30443_);
        if (p_30443_) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0D);
            this.setHealth(20.0F);
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0D);
        }

        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0D);
    }

    boolean trusts(UUID p_28530_) {
        return this.getTrustedUUIDs().contains(p_28530_);
    }

    protected void dropAllDeathLoot(DamageSource p_28536_) {
        super.dropAllDeathLoot(p_28536_);
    }
    protected void dropEquipment() { // Forge: move extra drops to dropEquipment to allow them to be captured by LivingDropsEvent
        super.dropEquipment();
        ItemStack itemstack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!itemstack.isEmpty()) {
            this.spawnAtLocation(itemstack);
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }

    }

    public static boolean isPathClear(CustomFox p_28472_, LivingEntity p_28473_) {
        double d0 = p_28473_.getZ() - p_28472_.getZ();
        double d1 = p_28473_.getX() - p_28472_.getX();
        double d2 = d0 / d1;
        int i = 6;

        for(int j = 0; j < 6; ++j) {
            double d3 = d2 == 0.0D ? 0.0D : d0 * (double)((float)j / 6.0F);
            double d4 = d2 == 0.0D ? d1 * (double)((float)j / 6.0F) : d3 / d2;

            for(int k = 1; k < 4; ++k) {
                if (!p_28472_.level().getBlockState(BlockPos.containing(p_28472_.getX() + d4, p_28472_.getY() + (double)k, p_28472_.getZ() + d3)).canBeReplaced()) {
                    return false;
                }
            }
        }

        return true;
    }

    public Vec3 getLeashOffset() {
        return new Vec3(0.0D, (double)(0.55F * this.getEyeHeight()), (double)(this.getBbWidth() * 0.4F));
    }

    class DefendTrustedTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        @Nullable
        private LivingEntity trustedLastHurtBy;
        @Nullable
        private LivingEntity trustedLastHurt;
        private int timestamp;

        public DefendTrustedTargetGoal(Class<LivingEntity> p_28634_, boolean p_28635_, boolean p_28636_, @Nullable Predicate<LivingEntity> p_28637_) {
            super(CustomFox.this, p_28634_, 10, p_28635_, p_28636_, p_28637_);
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
                    return false;
                } else {
                    for (UUID uuid : CustomFox.this.getTrustedUUIDs()) {
                        if (uuid != null && CustomFox.this.level() instanceof ServerLevel) {
                            Entity entity = ((ServerLevel) CustomFox.this.level()).getEntity(uuid);
                            if (entity instanceof LivingEntity) {
                                LivingEntity livingEntity = (LivingEntity) entity;
                                this.trustedLastHurt = livingEntity;
                                this.trustedLastHurtBy = livingEntity.getLastHurtByMob();
                                int i = livingEntity.getLastHurtByMobTimestamp();
                                return i != this.timestamp && this.canAttack(this.trustedLastHurtBy, this.targetConditions);
                            }
                        }
                    }

                    return false;
                }
            }
            return false;
        }

        public void start() { // ТУТ ПИЗДА
            if (!((CustomFox)this.mob).isOrderedToSit()) {
                this.setTarget(this.trustedLastHurtBy);
                this.target = this.trustedLastHurtBy;
                if (this.trustedLastHurt != null) {
                    this.timestamp = this.trustedLastHurt.getLastHurtByMobTimestamp();
                }

                CustomFox.this.playSound(SoundEvents.FOX_AGGRO, 1.0F, 1.0F);
                CustomFox.this.setDefending(true);
                CustomFox.this.wakeUp();
                super.start();
            }
        }
    }

    class FaceplantGoal extends Goal {
        int countdown;

        public FaceplantGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                return CustomFox.this.isFaceplanted();
            }
            return false;
        }

        public boolean canContinueToUse() {
            return this.canUse() && this.countdown > 0;
        }

        public void start() {
            this.countdown = this.adjustedTickDelay(40);
        }

        public void stop() {
            CustomFox.this.setFaceplanted(false);
        }

        public void tick() {
            --this.countdown;
        }
    }

    public class FoxAlertableEntitiesSelector implements Predicate<LivingEntity> {
        public boolean test(LivingEntity p_28653_) {
            if (p_28653_ instanceof CustomFox) {
                return false;
            } else if (!(p_28653_ instanceof Chicken) && !(p_28653_ instanceof Rabbit) && !(p_28653_ instanceof Monster)) {
                if (p_28653_ instanceof TamableAnimal) {
                    return !((TamableAnimal)p_28653_).isTame();
                } else if (!(p_28653_ instanceof Player) || !p_28653_.isSpectator() && !((Player)p_28653_).isCreative()) {
                    if (CustomFox.this.trusts(p_28653_.getUUID())) {
                        return false;
                    } else {
                        return !p_28653_.isSleeping() && !p_28653_.isDiscrete();
                    }
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    abstract class FoxBehaviorGoal extends Goal {
        private final TargetingConditions alertableTargeting = TargetingConditions.forCombat().range(12.0D).ignoreLineOfSight().selector(CustomFox.this.new FoxAlertableEntitiesSelector());

        protected boolean hasShelter() {
            BlockPos blockpos = BlockPos.containing(CustomFox.this.getX(), CustomFox.this.getBoundingBox().maxY, CustomFox.this.getZ());
            return !CustomFox.this.level().canSeeSky(blockpos) && CustomFox.this.getWalkTargetValue(blockpos) >= 0.0F;
        }

        protected boolean alertable() {
            return !CustomFox.this.level().getNearbyEntities(LivingEntity.class, this.alertableTargeting, CustomFox.this, CustomFox.this.getBoundingBox().inflate(12.0D, 6.0D, 12.0D)).isEmpty();
        }
    }

    class FoxBreedGoal extends BreedGoal {
        public FoxBreedGoal(double p_28668_) {
            super(CustomFox.this, p_28668_);
        }

        public void start() { //ТУТ ПИЗДА
            if (!CustomFox.this.isOrderedToSit()) {
                ((CustomFox) this.animal).clearStates();
                ((CustomFox) this.partner).clearStates();
                super.start();
            }
        }

        protected void breed() {
            ServerLevel serverlevel = (ServerLevel)this.level;
            CustomFox fox = (CustomFox)this.animal.getBreedOffspring(serverlevel, this.partner);
            final net.minecraftforge.event.entity.living.BabyEntitySpawnEvent event = new net.minecraftforge.event.entity.living.BabyEntitySpawnEvent(animal, partner, fox);
            final boolean cancelled = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            fox = (CustomFox) event.getChild();
            if (cancelled) {
                //Reset the "inLove" state for the animals
                this.animal.setAge(6000);
                this.partner.setAge(6000);
                this.animal.resetLove();
                this.partner.resetLove();
                return;
            }
            if (fox != null) {
                ServerPlayer serverplayer = this.animal.getLoveCause();
                ServerPlayer serverplayer1 = this.partner.getLoveCause();
                ServerPlayer serverplayer2 = serverplayer;
                if (serverplayer != null) {
                    fox.addTrustedUUID(serverplayer.getUUID());
                } else {
                    serverplayer2 = serverplayer1;
                }

                if (serverplayer1 != null && serverplayer != serverplayer1) {
                    fox.addTrustedUUID(serverplayer1.getUUID());
                }

                if (serverplayer2 != null) {
                    serverplayer2.awardStat(Stats.ANIMALS_BRED);
                    CriteriaTriggers.BRED_ANIMALS.trigger(serverplayer2, this.animal, this.partner, fox);
                }

                this.animal.setAge(6000);
                this.partner.setAge(6000);
                this.animal.resetLove();
                this.partner.resetLove();
                fox.setAge(-24000);
                fox.moveTo(this.animal.getX(), this.animal.getY(), this.animal.getZ(), 0.0F, 0.0F);
                serverlevel.addFreshEntityWithPassengers(fox);
                this.level.broadcastEntityEvent(this.animal, (byte)18);
                if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                    this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), this.animal.getRandom().nextInt(7) + 1));
                }

            }
        }
    }

    public class FoxEatBerriesGoal extends MoveToBlockGoal {
        private static final int WAIT_TICKS = 40;
        protected int ticksWaited;

        public FoxEatBerriesGoal(double p_28675_, int p_28676_, int p_28677_) {
            super(CustomFox.this, p_28675_, p_28676_, p_28677_);
        }

        public double acceptedDistance() {
            return 2.0D;
        }

        public boolean shouldRecalculatePath() {
            return this.tryTicks % 100 == 0;
        }

        protected boolean isValidTarget(LevelReader p_28680_, BlockPos p_28681_) {
            BlockState blockstate = p_28680_.getBlockState(p_28681_);
            return blockstate.is(Blocks.SWEET_BERRY_BUSH) && blockstate.getValue(SweetBerryBushBlock.AGE) >= 2 || CaveVines.hasGlowBerries(blockstate);
        }

        public void tick() {
            if (this.isReachedTarget()) {
                if (this.ticksWaited >= 40) {
                    this.onReachedTarget();
                } else {
                    ++this.ticksWaited;
                }
            } else if (!this.isReachedTarget() && CustomFox.this.random.nextFloat() < 0.05F) {
                CustomFox.this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.0F);
            }

            super.tick();
        }

        protected void onReachedTarget() {
            if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(CustomFox.this.level(), CustomFox.this)) {
                BlockState blockstate = CustomFox.this.level().getBlockState(this.blockPos);
                if (blockstate.is(Blocks.SWEET_BERRY_BUSH)) {
                    this.pickSweetBerries(blockstate);
                } else if (CaveVines.hasGlowBerries(blockstate)) {
                    this.pickGlowBerry(blockstate);
                }

            }
        }

        private void pickGlowBerry(BlockState p_148927_) {
            CaveVines.use(CustomFox.this, p_148927_, CustomFox.this.level(), this.blockPos);
        }

        private void pickSweetBerries(BlockState p_148929_) {
            int i = p_148929_.getValue(SweetBerryBushBlock.AGE);
            p_148929_.setValue(SweetBerryBushBlock.AGE, Integer.valueOf(1));
            int j = 1 + CustomFox.this.level().random.nextInt(2) + (i == 3 ? 1 : 0);
            ItemStack itemstack = CustomFox.this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (itemstack.isEmpty()) {
                CustomFox.this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
                --j;
            }

            if (j > 0) {
                Block.popResource(CustomFox.this.level(), this.blockPos, new ItemStack(Items.SWEET_BERRIES, j));
            }

            CustomFox.this.playSound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
            CustomFox.this.level().setBlock(this.blockPos, p_148929_.setValue(SweetBerryBushBlock.AGE, Integer.valueOf(1)), 2);
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                return !CustomFox.this.isSleeping() && super.canUse();
            }
            return false;
        }

        public void start() {
            this.ticksWaited = 0;
            CustomFox.this.setSitting(false);
            super.start();
        }
    }

    class FoxFloatGoal extends FloatGoal {
        public FoxFloatGoal() {
            super(CustomFox.this);
        }

        public void start() {
            super.start();
            CustomFox.this.clearStates();
        }

        public boolean canUse() {
            return CustomFox.this.isInWater() && CustomFox.this.getFluidHeight(FluidTags.WATER) > 0.25D || CustomFox.this.isInLava() || CustomFox.this.isInFluidType((fluidType, height) -> CustomFox.this.canSwimInFluidType(fluidType) && height > 0.25D);
        }
    }

    class FoxFollowParentGoal extends FollowParentGoal {
        private final CustomFox fox;

        public FoxFollowParentGoal(CustomFox p_28696_, double p_28697_) {
            super(p_28696_, p_28697_);
            this.fox = p_28696_;
            Tame_the_fox.LOGGER.info("FoxFollowParentGoal");
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                return !this.fox.isDefending() && super.canUse();
            }
            return false;
        }

        public boolean canContinueToUse() {
            return !this.fox.isDefending() && super.canContinueToUse();
        }

        public void start() {
            this.fox.clearStates();
            super.start();
        }
    }

    public static class FoxGroupData extends AgeableMob.AgeableMobGroupData {
        public final CustomFox.Type type;

        public FoxGroupData(CustomFox.Type p_28703_) {
            super(false);
            this.type = p_28703_;
        }
    }

    class FoxLookAtPlayerGoal extends LookAtPlayerGoal {
        public FoxLookAtPlayerGoal(Mob p_28707_, Class<? extends LivingEntity> p_28708_, float p_28709_) {
            super(p_28707_, p_28708_, p_28709_);
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                return super.canUse() && !CustomFox.this.isFaceplanted() && !CustomFox.this.isInterested();
            }
            return false;
        }

        public boolean canContinueToUse() {
            return super.canContinueToUse() && !CustomFox.this.isFaceplanted() && !CustomFox.this.isInterested();
        }
    }

    public class FoxLookControl extends LookControl {
        public FoxLookControl() {
            super(CustomFox.this);
        }

        public void tick() {
            if (!CustomFox.this.isSleeping()) {
                super.tick();
            }

        }

        protected boolean resetXRotOnTick() {
            return !CustomFox.this.isPouncing() && !CustomFox.this.isCrouching() && !CustomFox.this.isInterested() && !CustomFox.this.isFaceplanted();
        }
    }

    class FoxMeleeAttackGoal extends MeleeAttackGoal {
        public FoxMeleeAttackGoal(double p_28720_, boolean p_28721_) {
            super(CustomFox.this, p_28720_, p_28721_);
        }

        protected void checkAndPerformAttack(LivingEntity p_28724_, double p_28725_) {
            double d0 = this.getAttackReachSqr(p_28724_);
            if (p_28725_ <= d0 && this.isTimeToAttack()) {
                this.resetAttackCooldown();
                this.mob.doHurtTarget(p_28724_);
                CustomFox.this.playSound(SoundEvents.FOX_BITE, 1.0F, 1.0F);
            }

        }

        public void start() {
            CustomFox.this.setIsInterested(false);
            super.start();
        }

        public boolean canUse() {
            return !CustomFox.this.isSitting() && !CustomFox.this.isSleeping() && !CustomFox.this.isCrouching() && !CustomFox.this.isFaceplanted() && super.canUse();
        }
    }

    class FoxMoveControl extends MoveControl {
        public FoxMoveControl() {
            super(CustomFox.this);
        }

        public void tick() {
            if (CustomFox.this.canMove()) {
                super.tick();
            }

        }
    }

    class FoxPanicGoal extends PanicGoal {
        public FoxPanicGoal(double p_28734_) {
            super(CustomFox.this, p_28734_);
        }

        public boolean shouldPanic() {
            return !CustomFox.this.isDefending() && super.shouldPanic();
        }
    }

    public class FoxPounceGoal extends JumpGoal {
        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                if (!CustomFox.this.isFullyCrouched()) {
                    return false;
                } else {
                    LivingEntity livingentity = CustomFox.this.getTarget();
                    if (livingentity != null && livingentity.isAlive()) {
                        if (livingentity.getMotionDirection() != livingentity.getDirection()) {
                            return false;
                        } else {
                            boolean flag = CustomFox.isPathClear(CustomFox.this, livingentity);
                            if (!flag) {
                                CustomFox.this.getNavigation().createPath(livingentity, 0);
                                CustomFox.this.setIsCrouching(false);
                                CustomFox.this.setIsInterested(false);
                            }

                            return flag;
                        }
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }

        public boolean canContinueToUse() {
            LivingEntity livingentity = CustomFox.this.getTarget();
            if (livingentity != null && livingentity.isAlive()) {
                double d0 = CustomFox.this.getDeltaMovement().y;
                return (!(d0 * d0 < (double)0.05F) || !(Math.abs(CustomFox.this.getXRot()) < 15.0F) || !CustomFox.this.onGround()) && !CustomFox.this.isFaceplanted();
            } else {
                return false;
            }
        }

        public boolean isInterruptable() {
            return false;
        }

        public void start() {
            CustomFox.this.setJumping(true);
            CustomFox.this.setIsPouncing(true);
            CustomFox.this.setIsInterested(false);
            LivingEntity livingentity = CustomFox.this.getTarget();
            if (livingentity != null) {
                CustomFox.this.getLookControl().setLookAt(livingentity, 60.0F, 30.0F);
                Vec3 vec3 = (new Vec3(livingentity.getX() - CustomFox.this.getX(), livingentity.getY() - CustomFox.this.getY(), livingentity.getZ() - CustomFox.this.getZ())).normalize();
                CustomFox.this.setDeltaMovement(CustomFox.this.getDeltaMovement().add(vec3.x * 0.8D, 0.9D, vec3.z * 0.8D));
            }

            CustomFox.this.getNavigation().stop();
        }

        public void stop() {
            CustomFox.this.setIsCrouching(false);
            CustomFox.this.crouchAmount = 0.0F;
            CustomFox.this.crouchAmountO = 0.0F;
            CustomFox.this.setIsInterested(false);
            CustomFox.this.setIsPouncing(false);
        }

        public void tick() {
            LivingEntity livingentity = CustomFox.this.getTarget();
            if (livingentity != null) {
                CustomFox.this.getLookControl().setLookAt(livingentity, 60.0F, 30.0F);
            }

            if (!CustomFox.this.isFaceplanted()) {
                Vec3 vec3 = CustomFox.this.getDeltaMovement();
                if (vec3.y * vec3.y < (double)0.03F && CustomFox.this.getXRot() != 0.0F) {
                    CustomFox.this.setXRot(Mth.rotLerp(0.2F, CustomFox.this.getXRot(), 0.0F));
                } else {
                    double d0 = vec3.horizontalDistance();
                    double d1 = Math.signum(-vec3.y) * Math.acos(d0 / vec3.length()) * (double)(180F / (float)Math.PI);
                    CustomFox.this.setXRot((float)d1);
                }
            }

            if (livingentity != null && CustomFox.this.distanceTo(livingentity) <= 2.0F) {
                CustomFox.this.doHurtTarget(livingentity);
            } else if (CustomFox.this.getXRot() > 0.0F && CustomFox.this.onGround() && (float) CustomFox.this.getDeltaMovement().y != 0.0F && CustomFox.this.level().getBlockState(CustomFox.this.blockPosition()).is(Blocks.SNOW)) {
                CustomFox.this.setXRot(60.0F);
                CustomFox.this.setTarget((LivingEntity)null);
                CustomFox.this.setFaceplanted(true);
            }

        }
    }

    class FoxSearchForItemsGoal extends Goal {
        public FoxSearchForItemsGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                if (!CustomFox.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                    return false;
                } else if (CustomFox.this.getTarget() == null && CustomFox.this.getLastHurtByMob() == null) {
                    if (!CustomFox.this.canMove()) {
                        return false;
                    } else if (CustomFox.this.getRandom().nextInt(reducedTickDelay(10)) != 0) {
                        return false;
                    } else {
                        List<ItemEntity> list = CustomFox.this.level().getEntitiesOfClass(ItemEntity.class, CustomFox.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), CustomFox.ALLOWED_ITEMS);
                        return !list.isEmpty() && CustomFox.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
                    }
                } else {
                    return false;
                }
            }
            return false;
        }

        public void tick() {
            List<ItemEntity> list = CustomFox.this.level().getEntitiesOfClass(ItemEntity.class, CustomFox.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), CustomFox.ALLOWED_ITEMS);
            ItemStack itemstack = CustomFox.this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (itemstack.isEmpty() && !list.isEmpty()) {
                CustomFox.this.getNavigation().moveTo(list.get(0), (double)1.2F);
            }

        }

        public void start() {
            List<ItemEntity> list = CustomFox.this.level().getEntitiesOfClass(ItemEntity.class, CustomFox.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), CustomFox.ALLOWED_ITEMS);
            if (!list.isEmpty()) {
                CustomFox.this.getNavigation().moveTo(list.get(0), (double)1.2F);
            }

        }
    }

    class FoxStrollThroughVillageGoal extends StrollThroughVillageGoal {
        public FoxStrollThroughVillageGoal(int p_28754_, int p_28755_) {
            super(CustomFox.this, p_28755_);
        }

        public void start() {
            CustomFox.this.clearStates();
            super.start();
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                return super.canUse() && this.canFoxMove();
            }
            return false;
        }

        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.canFoxMove();
        }

        private boolean canFoxMove() {
            return !CustomFox.this.isSleeping() && !CustomFox.this.isSitting() && !CustomFox.this.isDefending() && CustomFox.this.getTarget() == null;
        }
    }

    class PerchAndSearchGoal extends CustomFox.FoxBehaviorGoal {
        private double relX;
        private double relZ;
        private int lookTime;
        private int looksRemaining;

        public PerchAndSearchGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                return CustomFox.this.getLastHurtByMob() == null && CustomFox.this.getRandom().nextFloat() < 0.02F && !CustomFox.this.isSleeping() && CustomFox.this.getTarget() == null && CustomFox.this.getNavigation().isDone() && !this.alertable() && !CustomFox.this.isPouncing() && !CustomFox.this.isCrouching();
            }
            return false;
        }

        public boolean canContinueToUse() {
            return this.looksRemaining > 0;
        }

        public void start() {
            this.resetLook();
            this.looksRemaining = 2 + CustomFox.this.getRandom().nextInt(3);
            CustomFox.this.setSitting(true);
            CustomFox.this.getNavigation().stop();
        }

        public void stop() {
            CustomFox.this.setSitting(false);
        }

        public void tick() {
            --this.lookTime;
            if (this.lookTime <= 0) {
                --this.looksRemaining;
                this.resetLook();
            }

            CustomFox.this.getLookControl().setLookAt(CustomFox.this.getX() + this.relX, CustomFox.this.getEyeY(), CustomFox.this.getZ() + this.relZ, (float) CustomFox.this.getMaxHeadYRot(), (float) CustomFox.this.getMaxHeadXRot());
        }

        private void resetLook() {
            double d0 = (Math.PI * 2D) * CustomFox.this.getRandom().nextDouble();
            this.relX = Math.cos(d0);
            this.relZ = Math.sin(d0);
            this.lookTime = this.adjustedTickDelay(80 + CustomFox.this.getRandom().nextInt(20));
        }
    }

    class SeekShelterGoal extends FleeSunGoal {
        private int interval = reducedTickDelay(100);

        public SeekShelterGoal(double p_28777_) {
            super(CustomFox.this, p_28777_);
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                if (!CustomFox.this.isSleeping() && this.mob.getTarget() == null) {
                    if (CustomFox.this.level().isThundering() && CustomFox.this.level().canSeeSky(this.mob.blockPosition())) {
                        return this.setWantedPos();
                    } else if (this.interval > 0) {
                        --this.interval;
                        return false;
                    } else {
                        this.interval = 100;
                        BlockPos blockpos = this.mob.blockPosition();
                        return CustomFox.this.level().isDay() && CustomFox.this.level().canSeeSky(blockpos) && !((ServerLevel) CustomFox.this.level()).isVillage(blockpos) && this.setWantedPos();
                    }
                } else {
                    return false;
                }
            }
            return false;
        }

        public void start() {
            CustomFox.this.clearStates();
            super.start();
        }
    }

    class SleepGoal extends CustomFox.FoxBehaviorGoal {
        private static final int WAIT_TIME_BEFORE_SLEEP = reducedTickDelay(140);
        private int countdown = CustomFox.this.random.nextInt(WAIT_TIME_BEFORE_SLEEP);

        public SleepGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        public boolean canUse() {
            if (!CustomFox.this.isOrderedToSit()) {
                if (CustomFox.this.xxa == 0.0F && CustomFox.this.yya == 0.0F && CustomFox.this.zza == 0.0F) {
                    return this.canSleep() || CustomFox.this.isSleeping();
                } else {
                    return false;
                }
            }
            return false;
        }

        public boolean canContinueToUse() {
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return CustomFox.this.level().isDay() && this.hasShelter() && !this.alertable() && !CustomFox.this.isInPowderSnow;
            }
        }

        public void stop() {
            this.countdown = CustomFox.this.random.nextInt(WAIT_TIME_BEFORE_SLEEP);
            CustomFox.this.clearStates();
        }

        public void start() {
            CustomFox.this.setSitting(false);
            CustomFox.this.setIsCrouching(false);
            CustomFox.this.setIsInterested(false);
            CustomFox.this.setJumping(false);
            CustomFox.this.setSleeping(true);
            CustomFox.this.getNavigation().stop();
            CustomFox.this.getMoveControl().setWantedPosition(CustomFox.this.getX(), CustomFox.this.getY(), CustomFox.this.getZ(), 0.0D);
        }
    }

    class StalkPreyGoal extends Goal {
        public StalkPreyGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        public boolean canUse() {
            if (CustomFox.this.isSleeping() && CustomFox.this.isOrderedToSit()) {
                return false;
            } else {
                LivingEntity livingentity = CustomFox.this.getTarget();
                return livingentity != null && livingentity.isAlive() && CustomFox.STALKABLE_PREY.test(livingentity) && CustomFox.this.distanceToSqr(livingentity) > 36.0D && !CustomFox.this.isCrouching() && !CustomFox.this.isInterested() && !CustomFox.this.jumping;
            }
        }

        public void start() {
            CustomFox.this.setSitting(false);
            CustomFox.this.setFaceplanted(false);
        }

        public void stop() {
            LivingEntity livingentity = CustomFox.this.getTarget();
            if (livingentity != null && CustomFox.isPathClear(CustomFox.this, livingentity)) {
                CustomFox.this.setIsInterested(true);
                CustomFox.this.setIsCrouching(true);
                CustomFox.this.getNavigation().stop();
                CustomFox.this.getLookControl().setLookAt(livingentity, (float) CustomFox.this.getMaxHeadYRot(), (float) CustomFox.this.getMaxHeadXRot());
            } else {
                CustomFox.this.setIsInterested(false);
                CustomFox.this.setIsCrouching(false);
            }

        }

        public void tick() {
            LivingEntity livingentity = CustomFox.this.getTarget();
            if (livingentity != null) {
                CustomFox.this.getLookControl().setLookAt(livingentity, (float) CustomFox.this.getMaxHeadYRot(), (float) CustomFox.this.getMaxHeadXRot());
                if (CustomFox.this.distanceToSqr(livingentity) <= 36.0D) {
                    CustomFox.this.setIsInterested(true);
                    CustomFox.this.setIsCrouching(true);
                    CustomFox.this.getNavigation().stop();
                } else {
                    CustomFox.this.getNavigation().moveTo(livingentity, 1.5D);
                }

            }
        }
    }

    public static enum Type implements StringRepresentable {
        RED(0, "red"),
        SNOW(1, "snow");

        public static final StringRepresentable.EnumCodec<CustomFox.Type> CODEC = StringRepresentable.fromEnum(CustomFox.Type::values);
        private static final IntFunction<CustomFox.Type> BY_ID = ByIdMap.continuous(CustomFox.Type::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        private final int id;
        private final String name;

        private Type(int p_196658_, String p_196659_) {
            this.id = p_196658_;
            this.name = p_196659_;
        }

        public String getSerializedName() {
            return this.name;
        }

        public int getId() {
            return this.id;
        }

        public static CustomFox.Type byName(String p_28817_) {
            return CODEC.byName(p_28817_, RED);
        }

        public static CustomFox.Type byId(int p_28813_) {
            return BY_ID.apply(p_28813_);
        }

        public static CustomFox.Type byBiome(Holder<Biome> p_204063_) {
            return p_204063_.is(BiomeTags.SPAWNS_SNOW_FOXES) ? SNOW : RED;
        }
    }
}
