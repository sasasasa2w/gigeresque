package mods.cybercat.gigeresque.common.entity.impl;

import java.util.List;

import mods.cybercat.gigeresque.Constants;
import mods.cybercat.gigeresque.common.config.ConfigAccessor;
import mods.cybercat.gigeresque.common.entity.AlienEntity;
import mods.cybercat.gigeresque.common.entity.Entities;
import mods.cybercat.gigeresque.common.entity.helper.GigAnimationsDefault;
import mods.cybercat.gigeresque.common.sound.GigSounds;
import mods.cybercat.gigeresque.common.util.EntityUtils;
import mods.cybercat.gigeresque.interfacing.Eggmorphable;
import mods.cybercat.gigeresque.interfacing.Host;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AlienEggEntity extends AlienEntity implements GeoEntity {

	private static final EntityDataAccessor<Boolean> IS_HATCHING = SynchedEntityData.defineId(AlienEggEntity.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> IS_HATCHED = SynchedEntityData.defineId(AlienEggEntity.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> HAS_FACEHUGGER = SynchedEntityData.defineId(AlienEggEntity.class,
			EntityDataSerializers.BOOLEAN);
	private long hatchProgress = 0L;
	private long ticksOpen = 0L;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private static final long MAX_HATCH_PROGRESS = 50L;

	public AlienEggEntity(EntityType<? extends AlienEggEntity> type, Level world) {
		super(type, world);
	}

	@Override
	protected int getAcidDiameter() {
		return 1;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ARMOR, 1.0)
				.add(Attributes.ARMOR_TOUGHNESS, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
				.add(Attributes.FOLLOW_RANGE, 0.0).add(Attributes.MOVEMENT_SPEED, 0.0);
	}

	public boolean isHatching() {
		return entityData.get(IS_HATCHING);
	}

	public void setIsHatching(boolean value) {
		entityData.set(IS_HATCHING, value);
	}

	public boolean isHatched() {
		return entityData.get(IS_HATCHED);
	}

	public void setIsHatched(boolean value) {
		entityData.set(IS_HATCHED, value);
	}

	public boolean hasFacehugger() {
		return entityData.get(HAS_FACEHUGGER);
	}

	public void setHasFacehugger(boolean value) {
		entityData.set(HAS_FACEHUGGER, value);
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(IS_HATCHING, false);
		entityData.define(IS_HATCHED, false);
		entityData.define(HAS_FACEHUGGER, true);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putBoolean("isHatching", isHatching());
		nbt.putBoolean("isHatched", isHatched());
		nbt.putBoolean("hasFacehugger", hasFacehugger());
		nbt.putLong("hatchProgress", hatchProgress);
		nbt.putLong("ticksOpen", ticksOpen);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("isHatching")) {
			setIsHatching(nbt.getBoolean("isHatching"));
		}
		if (nbt.contains("isHatched")) {
			setIsHatched(nbt.getBoolean("isHatched"));
		}
		if (nbt.contains("hasFacehugger")) {
			setHasFacehugger(nbt.getBoolean("hasFacehugger"));
		}
		if (nbt.contains("hatchProgress")) {
			hatchProgress = nbt.getLong("hatchProgress");
		}
		if (nbt.contains("ticksOpen")) {
			ticksOpen = nbt.getLong("ticksOpen");
		}
	}

	@Override
	protected AABB makeBoundingBox() {
		return super.makeBoundingBox();
	}

	@Override
	public AABB getLocalBoundsForPose(Pose pose) {
		return this.getBoundingBox().inflate(1);
	}

	@Override
	public void tick() {
		super.tick();
		if (isHatching() && hatchProgress < MAX_HATCH_PROGRESS) {
			hatchProgress++;
		}

		if (hatchProgress == 15L) {
			if (!level.isClientSide) {
				this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(), GigSounds.EGG_OPEN,
						SoundSource.HOSTILE, 1.0F, 1.0F, true);
			}
		}

		if (hatchProgress >= MAX_HATCH_PROGRESS) {
			setIsHatching(false);
			setIsHatched(true);
			ticksOpen++;
		}

		if (isHatched() && hasFacehugger()) {
			ticksOpen++;
		}

		if (ticksOpen >= 3L * Constants.TPS && hasFacehugger() && !level.isClientSide) {
			var facehugger = new FacehuggerEntity(Entities.FACEHUGGER, level);
			facehugger.moveTo(blockPosition().above(), getYRot(), getXRot());
			facehugger.setDeltaMovement(0.0, 0.7, 0.0);
			facehugger.setEggSpawnState(true);
			level.addFreshEntity(facehugger);
			setHasFacehugger(false);
		}
	}

	/**
	 * Prevents entity collisions from moving the egg.
	 */
	@Override
	public void doPush(Entity entity) {
		if (!level.isClientSide && EntityUtils.isPotentialHost(entity)) {
			setIsHatching(true);
		}
		// this.doPush(entity);
	}

	@Override
	public boolean canBeCollidedWith() {
		return this.isAlive();
	}

	/**
	 * Prevents the egg from being pushed.
	 */
	@Override
	public boolean isPushable() {
		return false;
	}

	/**
	 * Prevents fluids from moving the egg.
	 */
	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	/**
	 * Prevents the egg from moving on its own.
	 */
	@Override
	public boolean shouldPassengersInheritMalus() {
		return false;
	}

	/**
	 * Prevents the egg moving when hit.
	 */
	@Override
	public void knockback(double strength, double x, double z) {
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.getDirectEntity() != null || source != DamageSource.IN_WALL && !this.isHatched()) {
			setIsHatching(true);
		}
		return source == DamageSource.IN_WALL ? false : super.hurt(source, amount);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		float q = 6.0F;
		int k = Mth.floor(this.getX() - (double) q - 1.0D);
		int l = Mth.floor(this.getX() + (double) q + 1.0D);
		int t = Mth.floor(this.getY() - (double) q - 1.0D);
		int u = Mth.floor(this.getY() + (double) q + 1.0D);
		int v = Mth.floor(this.getZ() - (double) q - 1.0D);
		int w = Mth.floor(this.getZ() + (double) q + 1.0D);
		List<Entity> list = this.level.getEntities(this,
				new AABB((double) k, (double) t, (double) v, (double) l, (double) u, (double) w));
		Vec3 vec3d1 = new Vec3(this.getX(), this.getY(), this.getZ());

		for (int x = 0; x < list.size(); ++x) {
			Entity entity = (Entity) list.get(x);
			double y = (double) (Mth.sqrt((float) entity.distanceToSqr(vec3d1)) / q);
			if (y <= 1.0D && !ConfigAccessor.isTargetBlacklisted(this, entity) && entity.isAlive()) {
				if (entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof AlienEntity)
						&& !(ConfigAccessor.isTargetBlacklisted(FacehuggerEntity.class, entity))) {
					if (((Host) entity).doesNotHaveParasite() && ((Eggmorphable) entity).isNotEggmorphing()
							&& !(entity instanceof AmbientCreature)
							&& ((LivingEntity) entity).getMobType() != MobType.UNDEAD) {
						if (EntityUtils.isPotentialHost(entity))
							setIsHatching(true);
					}
				}
				if (entity instanceof Player && !((Player) entity).isCreative() && !((Player) entity).isSpectator()) {
					setIsHatching(true);
				}
			}
		}
	}

	/**
	 * Prevents the egg from drowning.
	 */
	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return (this.isHatched() && !this.hasFacehugger()) ? false : super.requiresCustomPersistence();
	}

	@Override
	public void checkDespawn() {
		if (this.isHatched() && !this.hasFacehugger())
			super.checkDespawn();
	}

	/*
	 * ANIMATIONS
	 */
	@Override
	public void registerControllers(ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "livingController", 5, event -> {
			if (isHatched() && !this.isDeadOrDying()) {
				if (!hasFacehugger())
					return event.setAndContinue(GigAnimationsDefault.HATCHED_EMPTY);
				return event.setAndContinue(GigAnimationsDefault.HATCHED);
			}
			if (this.isDeadOrDying())
				return event.setAndContinue(GigAnimationsDefault.DEATH);
			if (isHatching() && !this.isDeadOrDying())
				event.getController().setAnimation(GigAnimationsDefault.HATCHING);
			return event.setAndContinue(GigAnimationsDefault.IDLE);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	public void onSignalReceive(ServerLevel var1, GameEventListener var2, BlockPos var3, GameEvent var4, Entity var5,
			Entity var6, float var7) {
		if (this.isDeadOrDying()) {
			return;
		}
	}
}
