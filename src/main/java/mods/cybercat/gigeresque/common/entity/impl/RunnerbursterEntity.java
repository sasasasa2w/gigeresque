package mods.cybercat.gigeresque.common.entity.impl;

import mods.cybercat.gigeresque.Constants;
import mods.cybercat.gigeresque.common.block.GIgBlocks;
import mods.cybercat.gigeresque.common.config.ConfigAccessor;
import mods.cybercat.gigeresque.common.config.GigeresqueConfig;
import mods.cybercat.gigeresque.common.entity.AlienEntity;
import mods.cybercat.gigeresque.common.entity.Entities;
import mods.cybercat.gigeresque.common.entity.helper.Growable;
import mods.cybercat.gigeresque.common.sound.GigSounds;
import mods.cybercat.gigeresque.common.util.EntityUtils;
import mods.cybercat.gigeresque.interfacing.Eggmorphable;
import mods.cybercat.gigeresque.interfacing.Host;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation.LoopType;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RunnerbursterEntity extends ChestbursterEntity implements GeoEntity, Growable {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public RunnerbursterEntity(EntityType<? extends RunnerbursterEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 15.0).add(Attributes.ARMOR, 2.0)
				.add(Attributes.ARMOR_TOUGHNESS, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
				.add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.MOVEMENT_SPEED, 0.23000000417232513)
				.add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ATTACK_KNOCKBACK, 0.3);
	}

	@Override
	protected int getAcidDiameter() {
		return 1;
	}

	/*
	 * GROWTH
	 */

	@Override
	public float getGrowthMultiplier() {
		return GigeresqueConfig.runnerbursterGrowthMultiplier;
	}

	@Override
	public float getMaxGrowth() {
		return Constants.TPD / 2.0f;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isBirthed() == true && this.tickCount > 1200 && this.getGrowth() > 200) {
			this.setBirthStatus(false);
		}
	}

	@Override
	public LivingEntity growInto() {
		if (hostId == null) {
			return new ClassicAlienEntity(Entities.ALIEN, level);
		}

		var variantId = ConfigAccessor.getReversedMorphMappings().get(hostId);
		if (variantId == null) {
			return new ClassicAlienEntity(Entities.ALIEN, level);
		}
		var identifier = new ResourceLocation(variantId);
		var entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
		if (entityType == null) {
			return new ClassicAlienEntity(Entities.ALIEN, level);
		}
		var entity = entityType.create(level);

		if (hasCustomName()) {
			if (entity != null) {
				entity.setCustomName(getCustomName());
			}
		}

		return (LivingEntity) entity;
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, false));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
				entity -> !((entity instanceof AlienEntity || entity instanceof Warden || entity instanceof ArmorStand)
						|| (entity.getVehicle() != null
								&& entity.getVehicle().getSelfAndPassengers().anyMatch(AlienEntity.class::isInstance))
						|| (entity instanceof AlienEggEntity) || ((Host) entity).isBleeding()
						|| ((Eggmorphable) entity).isEggmorphing() || (EntityUtils.isFacehuggerAttached(entity))
						|| (entity.getFeetBlockState().getBlock() == GIgBlocks.NEST_RESIN_WEB_CROSS))
						&& !ConfigAccessor.isTargetBlacklisted(FacehuggerEntity.class, entity) && entity.isAlive()));
	}

	/*
	 * ANIMATIONS
	 */
	@Override
	public void registerControllers(AnimatableManager<?> manager) {
		manager.addController(new AnimationController<>(this, "livingController", 5, event -> {
			var velocityLength = this.getDeltaMovement().horizontalDistance();
			var isDead = this.dead || this.getHealth() < 0.01 || this.isDeadOrDying();
			if (velocityLength >= 0.000000001 && !isDead && animationSpeedOld > 0.15F) {
				if (animationSpeedOld >= 0.35F) {
					event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
					return PlayState.CONTINUE;
				} else {
					event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
					return PlayState.CONTINUE;
				}
			} else if (((this.getTarget() != null && this.doHurtTarget(getTarget()))
					|| (this.entityData.get(EAT) == true)) && !this.isDeadOrDying()) {
				event.getController().setAnimation(RawAnimation.begin().then("chomp", LoopType.PLAY_ONCE));
				return PlayState.CONTINUE;
			} else if (isDead) {
				event.getController().setAnimation(RawAnimation.begin().thenLoop("death"));
				return PlayState.CONTINUE;
			} else {
				if (this.entityData.get(BIRTHED) == true && this.tickCount < 60 && this.entityData.get(EAT) == false) {
					event.getController().setAnimation(RawAnimation.begin().then("birth", LoopType.PLAY_ONCE));
					return PlayState.CONTINUE;
				} else {
					event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
					return PlayState.CONTINUE;
				}
			}
		}).setSoundKeyframeHandler(event -> {
			if (event.getKeyframeData().getSound().matches("footstepSoundkey")) {
				if (this.level.isClientSide) {
					this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(),
							GigSounds.ALIEN_FOOTSTEP, SoundSource.HOSTILE, 0.5F, 1.0F, true);
				}
			}
			if (event.getKeyframeData().getSound().matches("handstepSoundkey")) {
				if (this.level.isClientSide) {
					this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(),
							GigSounds.ALIEN_HANDSTEP, SoundSource.HOSTILE, 0.5F, 1.0F, true);
				}
			}
			if (event.getKeyframeData().getSound().matches("idleSoundkey")) {
				if (this.level.isClientSide) {
					this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(),
							GigSounds.ALIEN_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.0F, true);
				}
			}
			if (event.getKeyframeData().getSound().matches("clawSoundkey")) {
				if (this.level.isClientSide) {
					this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(),
							GigSounds.ALIEN_CLAW, SoundSource.HOSTILE, 0.25F, 1.0F, true);
				}
			}
			if (event.getKeyframeData().getSound().matches("tailSoundkey")) {
				if (this.level.isClientSide) {
					this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(),
							GigSounds.ALIEN_TAIL, SoundSource.HOSTILE, 0.25F, 1.0F, true);
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
