package mods.cybercat.gigeresque.common.entity.impl;

import java.util.function.Predicate;

import mods.cybercat.gigeresque.Constants;
import mods.cybercat.gigeresque.common.config.GigeresqueConfig;
import mods.cybercat.gigeresque.common.entity.AlienEntity;
import mods.cybercat.gigeresque.common.entity.Entities;
import mods.cybercat.gigeresque.common.entity.ai.goal.FleeFireGoal;
import mods.cybercat.gigeresque.common.entity.ai.goal.busters.EatFoodGoal;
import mods.cybercat.gigeresque.common.entity.ai.pathing.CrawlerNavigation;
import mods.cybercat.gigeresque.common.entity.helper.GigAnimationsDefault;
import mods.cybercat.gigeresque.common.entity.helper.Growable;
import mods.cybercat.gigeresque.common.sound.GigSounds;
import mods.cybercat.gigeresque.common.tags.GigTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ChestbursterEntity extends AlienEntity implements GeoEntity, Growable {

	private static final EntityDataAccessor<Float> BLOOD = SynchedEntityData.defineId(ChestbursterEntity.class,
			EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> GROWTH = SynchedEntityData.defineId(ChestbursterEntity.class,
			EntityDataSerializers.FLOAT);
	public static final EntityDataAccessor<Boolean> BIRTHED = SynchedEntityData.defineId(ChestbursterEntity.class,
			EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> EAT = SynchedEntityData.defineId(ChestbursterEntity.class,
			EntityDataSerializers.BOOLEAN);
	public static final Predicate<ItemEntity> PICKABLE_DROP_FILTER = item -> {
		ItemStack itemStack = item.getItem();
		return itemStack.is(GigTags.BUSTER_FOOD) && item.isAlive() && !item.hasPickUpDelay();
	};
	public int bloodRendering = 0;
	private final GroundPathNavigation landNavigation = new CrawlerNavigation(this, level);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	protected String hostId = null;

	public ChestbursterEntity(EntityType<? extends ChestbursterEntity> type, Level world) {
		super(type, world);

		navigation = landNavigation;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 15.0).add(Attributes.ARMOR, 2.0)
				.add(Attributes.ARMOR_TOUGHNESS, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
				.add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.MOVEMENT_SPEED, 0.3300000041723251)
				.add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ATTACK_KNOCKBACK, 0.3);
	}

	public float getBlood() {
		return entityData.get(BLOOD);
	}

	public void setBlood(float growth) {
		entityData.set(BLOOD, growth);
	}

	@Override
	protected int getAcidDiameter() {
		return 1;
	}

	public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public boolean isBirthed() {
		return this.entityData.get(BIRTHED);
	}

	public void setBirthStatus(boolean birth) {
		this.entityData.set(BIRTHED, Boolean.valueOf(birth));
	}

	public boolean isEating() {
		return this.entityData.get(EAT);
	}

	public void setEatingStatus(boolean birth) {
		this.entityData.set(EAT, Boolean.valueOf(birth));
	}

	public float getGrowth() {
		return entityData.get(GROWTH);
	}

	public void setGrowth(float growth) {
		entityData.set(GROWTH, growth);
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(GROWTH, 0.0f);
		entityData.define(BLOOD, 0.0f);
		entityData.define(BIRTHED, false);
		entityData.define(EAT, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level.isClientSide && this.isAlive()) {
			setBlood(bloodRendering++);
			grow(this, 1 * getGrowthMultiplier());
		}
		if (this.isBirthed() == true && this.tickCount > 1200 && this.getGrowth() > 200) {
			this.setBirthStatus(false);
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putFloat("growth", getGrowth());
		if (hostId != null) {
			nbt.putString("hostId", hostId);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("growth")) {
			setGrowth(nbt.getFloat("growth"));
		}
		if (nbt.contains("hostId")) {
			hostId = nbt.getString("hostId");
		}
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
		this.goalSelector.addGoal(5, new FleeFireGoal<ChestbursterEntity>(this));
		this.goalSelector.addGoal(5, new EatFoodGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
	}

	/*
	 * GROWTH
	 */

	@Override
	public float getGrowthMultiplier() {
		return GigeresqueConfig.chestbursterGrowthMultiplier;
	}

	@Override
	public float getMaxGrowth() {
		return Constants.TPD / 2.0f;
	}

	@Override
	public LivingEntity growInto() {
		var entity = new RunnerbursterEntity(Entities.RUNNERBURSTER, level);
		entity.hostId = this.hostId;

		if (hasCustomName()) {
			entity.setCustomName(this.getCustomName());
		}

		return entity;
	}

	/*
	 * ANIMATIONS
	 */
	@Override
	public void registerControllers(ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "livingController", 5, event -> {
			var isDead = this.dead || this.getHealth() < 0.01 || this.isDeadOrDying();
			if (event.isMoving() && !isDead && animationSpeedOld > 0.15F)
				if (animationSpeedOld >= 0.35F)
					return event.setAndContinue(GigAnimationsDefault.RUSH_SLITHER);
				else
					return event.setAndContinue(GigAnimationsDefault.SLITHER);
			else if (this.entityData.get(EAT) == true && !this.isDeadOrDying())
				return event.setAndContinue(GigAnimationsDefault.CHOMP);
			else if (isDead)
				return event.setAndContinue(GigAnimationsDefault.DEATH);
			else if (this.tickCount < 60 && this.entityData.get(BIRTHED) == true)
				return event.setAndContinue(GigAnimationsDefault.BIRTH);
			else
				return event.setAndContinue(GigAnimationsDefault.IDLE);
		}).setSoundKeyframeHandler(event -> {
			if (event.getKeyframeData().getSound().matches("stepSoundkey")) {
				if (this.level.isClientSide) {
					this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(),
							GigSounds.BURSTER_CRAWL, SoundSource.HOSTILE, 0.25F, 1.0F, true);
				}
			}
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	public void onSignalReceive(ServerLevel var1, GameEventListener var2, BlockPos var3, GameEvent var4, Entity var5,
			Entity var6, float var7) {
		super.onSignalReceive(var1, var2, var3, var4, var5, var6, var7);
		this.getNavigation().moveTo(var3.getX(), var3.getY(), var3.getZ(), 1.2F);
	}
}
