package mods.cybercat.gigeresque.common.entity.impl;

import mods.cybercat.gigeresque.common.config.GigeresqueConfig;
import mods.cybercat.gigeresque.common.entity.AlienEntity;
import mods.cybercat.gigeresque.common.entity.ai.enums.AlienAttackType;
import mods.cybercat.gigeresque.common.entity.attribute.AlienEntityAttributes;
import mods.cybercat.gigeresque.common.entity.helper.GigAnimationsDefault;
import mods.cybercat.gigeresque.common.sound.GigSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RunnerAlienEntity extends AdultAlienEntity {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public RunnerAlienEntity(EntityType<? extends AlienEntity> type, Level world) {
		super(type, world);
	}

	@Override
	public void travel(Vec3 movementInput) {
		this.navigation = (this.isUnderWater() || this.isInWater()) ? swimNavigation : landNavigation;
		this.moveControl = (this.wasEyeInWater || this.isInWater()) ? swimMoveControl : landMoveControl;
		this.lookControl = (this.wasEyeInWater || this.isInWater()) ? swimLookControl : landLookControl;

		if (isEffectiveAi() && this.isInWater()) {
			moveRelative(getSpeed(), movementInput);
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.5));
			if (getTarget() == null) {
				setDeltaMovement(getDeltaMovement().add(0.0, -0.005, 0.0));
			}
		} else {
			super.travel(movementInput);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 80.0).add(Attributes.ARMOR, 4.0)
				.add(Attributes.ARMOR_TOUGHNESS, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
				.add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.MOVEMENT_SPEED, 0.13000000417232513)
				.add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ATTACK_KNOCKBACK, 1.0)
				.add(AlienEntityAttributes.INTELLIGENCE_ATTRIBUTE, 0.5);
	}

	@Override
	public void tick() {
		super.tick();

		// Attack logic

		if (attackProgress > 0) {
			attackProgress--;

			if (!level.isClientSide && attackProgress <= 0) {
				setCurrentAttackType(AlienAttackType.NONE);
			}
		}

		if (attackProgress == 0 && swinging) {
			attackProgress = 10;
		}

		if (!level.isClientSide && getCurrentAttackType() == AlienAttackType.NONE) {
			setCurrentAttackType(switch (random.nextInt(5)) {
			case 0 -> AlienAttackType.CLAW_LEFT_MOVING;
			case 1 -> AlienAttackType.CLAW_RIGHT_MOVING;
			case 2 -> AlienAttackType.TAIL_LEFT;
			case 3 -> AlienAttackType.TAIL_RIGHT;
			default -> AlienAttackType.CLAW_LEFT_MOVING;
			});
		}
	}

	@Override
	public float getGrowthMultiplier() {
		return GigeresqueConfig.runnerAlienGrowthMultiplier;
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 3.0, false));
	}

	/*
	 * ANIMATIONS
	 */
	@Override
	public void registerControllers(ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "livingController", 5, event -> {
			var isDead = this.dead || this.getHealth() < 0.01 || this.isDeadOrDying();
			if (event.isMoving() && !this.isCrawling() && this.isExecuting() == false && !isDead
					&& this.isStatis() == false && !this.swinging) {
				if (!this.isInWater() && this.isExecuting() == false)
					if (animationSpeedOld > 0.35F && this.getFirstPassenger() == null)
						return event.setAndContinue(GigAnimationsDefault.RUN);
					else if (this.isExecuting() == false && animationSpeedOld < 0.35F
							|| (!this.isCrawling() && !this.onGround))
						return event.setAndContinue(GigAnimationsDefault.WALK);
					else if (this.wasEyeInWater && this.isExecuting() == false && !this.isVehicle())
						if (this.isAggressive() && !this.isVehicle())
							return event.setAndContinue(GigAnimationsDefault.RUSH_SWIM);
						else
							return event.setAndContinue(GigAnimationsDefault.SWIM);
			} else if (isDead)
				return event.setAndContinue(GigAnimationsDefault.DEATH);
			else if (this.isStatis() == true || this.isNoAi() && !isDead)
				return event.setAndContinue(GigAnimationsDefault.STATIS_ENTER);
			else if (this.isStatis() == false && this.isInWater())
				return event.setAndContinue(GigAnimationsDefault.IDLE_WATER);
			else if (this.isStatis() == false && !this.isInWater())
				return event.setAndContinue(GigAnimationsDefault.IDLE_LAND);
			return event.setAndContinue(GigAnimationsDefault.IDLE_LAND);
		}).setSoundKeyframeHandler(event -> {
			if (event.getKeyframeData().getSound().matches("footstepSoundkey")) {
				if (this.level.isClientSide) {
					this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(),
							GigSounds.ALIEN_FOOTSTEP, SoundSource.HOSTILE, 0.5F, 1.0F, true);
				}
			}
			if (event.getKeyframeData().getSound().matches("idleSoundkey")) {
				if (this.level.isClientSide) {
					this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(),
							GigSounds.ALIEN_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.0F, true);
				}
			}
		})).add(new AnimationController<>(this, "attackController", 1, event -> {
			if (this.entityData.get(IS_BREAKING) == true && !this.isVehicle())
				return event.setAndContinue(GigAnimationsDefault.LEFT_CLAW);
			if (getCurrentAttackType() != AlienAttackType.NONE && attackProgress > 0 && !this.isVehicle()
					&& this.isExecuting() == false)
				return event.setAndContinue(RawAnimation.begin()
						.thenPlayXTimes(AlienAttackType.animationMappings.get(getCurrentAttackType()), 1));
			return PlayState.STOP;
		}).setSoundKeyframeHandler(event -> {
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
		})).add(new AnimationController<>(this, "hissController", 0, event -> {
			var isDead = this.dead || this.getHealth() < 0.01 || this.isDeadOrDying();
			if (this.entityData.get(IS_HISSING) == true && !this.isVehicle() && this.isExecuting() == false && !isDead
					&& !this.isInWater())
				return event.setAndContinue(GigAnimationsDefault.HISS);
			return PlayState.STOP;
		}).setSoundKeyframeHandler(event -> {
			if (event.getKeyframeData().getSound().matches("hissSoundkey")) {
				if (this.level.isClientSide) {
					this.getCommandSenderWorld().playLocalSound(this.getX(), this.getY(), this.getZ(),
							GigSounds.ALIEN_HISS, SoundSource.HOSTILE, 1.0F, 1.0F, true);
				}
			}
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
