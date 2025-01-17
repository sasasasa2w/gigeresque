package mods.cybercat.gigeresque.common.entity;

import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;

import mods.cybercat.gigeresque.Constants;
import mods.cybercat.gigeresque.common.block.AcidBlock;
import mods.cybercat.gigeresque.common.block.GIgBlocks;
import mods.cybercat.gigeresque.common.tags.GigTags;
import mods.cybercat.gigeresque.common.util.DamageSourceUtils;
import mods.cybercat.gigeresque.common.util.EntityUtils;
import mods.cybercat.gigeresque.common.util.GigVibrationListener;
import mods.cybercat.gigeresque.common.util.GigVibrationListener.GigVibrationListenerConfig;
import mods.cybercat.gigeresque.interfacing.Host;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public abstract class AlienEntity extends Monster implements GigVibrationListenerConfig {

	public static final EntityDataAccessor<Boolean> UPSIDE_DOWN = SynchedEntityData.defineId(AlienEntity.class,
			EntityDataSerializers.BOOLEAN);
	protected static final EntityDataAccessor<Integer> CLIENT_ANGER_LEVEL = SynchedEntityData
			.defineId(AlienEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(AlienEntity.class,
			EntityDataSerializers.INT);
	public static final Predicate<BlockState> NEST = state -> state.is(GIgBlocks.NEST_RESIN_WEB_CROSS);
	private static final Logger LOGGER = LogUtils.getLogger();
	public DynamicGameEventListener<GigVibrationListener> dynamicGameEventListener;
	private AngerManagement angerManagement = new AngerManagement(this::canTargetEntity, Collections.emptyList());

	protected AlienEntity(EntityType<? extends Monster> entityType, Level world) {
		super(entityType, world);
		setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0f);
		setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0f);
		if (navigation != null) {
			navigation.setCanFloat(true);
		}
		this.dynamicGameEventListener = new DynamicGameEventListener<GigVibrationListener>(
				new GigVibrationListener(new EntityPositionSource(this, this.getEyeHeight()), 48, this));
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime == 150) {
			this.remove(Entity.RemovalReason.KILLED);
			super.tickDeath();
			this.dropExperience();
		}
	}

	protected int getAcidDiameter() {
		return 3;
	}

	public boolean isUpsideDown() {
		return this.entityData.get(UPSIDE_DOWN);
	}

	public void setUpsideDown(boolean upsideDown) {
		this.entityData.set(UPSIDE_DOWN, Boolean.valueOf(upsideDown));
	}

	public int getAttckingState() {
		return this.entityData.get(STATE);
	}

	public void setAttackingState(int time) {
		this.entityData.set(STATE, time);
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(UPSIDE_DOWN, false);
		this.entityData.define(STATE, 0);
		this.entityData.define(CLIENT_ANGER_LEVEL, 0);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		GigVibrationListener.codec(this).encodeStart(NbtOps.INSTANCE, this.dynamicGameEventListener.getListener())
				.resultOrPartial(LOGGER::error).ifPresent(tag -> compound.put("listener", (Tag) tag));
		AngerManagement.codec(this::canTargetEntity).encodeStart(NbtOps.INSTANCE, this.angerManagement)
				.resultOrPartial(LOGGER::error).ifPresent(tag -> compound.put("anger", (Tag) tag));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("anger")) {
			AngerManagement.codec(this::canTargetEntity).parse(new Dynamic<Tag>(NbtOps.INSTANCE, compound.get("anger")))
					.resultOrPartial(LOGGER::error).ifPresent(angerManagement -> {
						this.angerManagement = angerManagement;
					});
			this.syncClientAngerLevel();
		}
		if (compound.contains("listener", 10)) {
			GigVibrationListener.codec(this).parse(new Dynamic<>(NbtOps.INSTANCE, compound.getCompound("listener")))
					.resultOrPartial(LOGGER::error).ifPresent(vibrationListener -> this.dynamicGameEventListener
							.updateListener((GigVibrationListener) vibrationListener, this.level));
		}
	}

	public int getClientAngerLevel() {
		return this.entityData.get(CLIENT_ANGER_LEVEL);
	}

	private void syncClientAngerLevel() {
		this.entityData.set(CLIENT_ANGER_LEVEL, this.getActiveAnger());
	}

	public AngerLevel getAngerLevel() {
		return AngerLevel.byAnger(this.getActiveAnger());
	}

	private int getActiveAnger() {
		return this.angerManagement.getActiveAnger(this.getTarget());
	}

	public void clearAnger(Entity entity) {
		this.angerManagement.clearAnger(entity);
	}

	@VisibleForTesting
	public AngerManagement getAngerManagement() {
		return this.angerManagement;
	}

	public Optional<LivingEntity> getEntityAngryAt() {
		if (this.getAngerLevel().isAngry()) {
			return this.angerManagement.getActiveEntity();
		}
		return Optional.empty();
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void customServerAiStep() {
		ServerLevel serverLevel = (ServerLevel) this.level;
		super.customServerAiStep();
		if (this.tickCount % 20 == 0) {
			this.angerManagement.tick(serverLevel, this::canTargetEntity);
			this.syncClientAngerLevel();
		}
	}

	@Override
	public void tick() {
		super.tick();

		Level level = this.level;
		if (level instanceof ServerLevel) {
			ServerLevel serverLevel = (ServerLevel) level;
			this.dynamicGameEventListener.getListener().tick(serverLevel);
		}

		if (!level.isClientSide && level.getBlockState(blockPosition()).getBlock() == GIgBlocks.NEST_RESIN
				&& this.tickCount % Constants.TPS == 0) {
			this.heal(0.0833f);
		}
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	public void checkDespawn() {
	}

	private void generateAcidPool(int xOffset, int zOffset) {
		BlockPos pos = this.blockPosition().offset(xOffset, 0, zOffset);
		BlockState posState = level.getBlockState(pos);

		BlockState newState = GIgBlocks.ACID_BLOCK.defaultBlockState();

		if (posState.getBlock() == Blocks.WATER) {
			newState = newState.setValue(BlockStateProperties.WATERLOGGED, true);
		}

		if (!(posState.getBlock() instanceof AirBlock)
				&& !(posState.getBlock() instanceof LiquidBlock && !(posState.is(GigTags.ACID_RESISTANT)))
				&& !(posState.getBlock() instanceof TorchBlock)) {
			return;
		}
		level.setBlockAndUpdate(pos, newState);
	}

	@Override
	public void die(DamageSource source) {
		if (DamageSourceUtils.isDamageSourceNotPuncturing(source)) {
			super.die(source);
			return;
		}
		if (source == DamageSource.OUT_OF_WORLD) {
			super.die(source);
			return;
		}

		if (!this.level.isClientSide) {
			if (source != DamageSource.OUT_OF_WORLD) {
				if (getAcidDiameter() == 1) {
					generateAcidPool(0, 0);
				} else {
					int radius = (getAcidDiameter() - 1) / 2;

					for (int x = -radius; x <= radius; x++) {
						for (int z = -radius; z <= radius; z++) {
							generateAcidPool(x, z);
						}
					}
				}
			}
		}
		super.die(source);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!this.level.isClientSide) {
			Entity attacker = source.getEntity();
			if (attacker != null) {
				if (attacker instanceof LivingEntity) {
					this.brain.setMemory(MemoryModuleType.ATTACK_TARGET, (LivingEntity) attacker);
				}
			}
		}

		if (DamageSourceUtils.isDamageSourceNotPuncturing(source))
			return super.hurt(source, amount);

		if (!this.level.isClientSide && source != DamageSource.OUT_OF_WORLD) {
			int acidThickness = this.getHealth() < (this.getMaxHealth() / 2) ? 1 : 0;

			if (this.getHealth() < (this.getMaxHealth() / 4)) {
				acidThickness += 1;
			}

			if (amount >= 5) {
				acidThickness += 1;
			}

			if (amount > (this.getMaxHealth() / 10)) {
				acidThickness += 1;
			}

			if (acidThickness == 0)
				return super.hurt(source, amount);

			BlockState newState = GIgBlocks.ACID_BLOCK.defaultBlockState().setValue(AcidBlock.THICKNESS, acidThickness);

			if (this.getFeetBlockState().getBlock() == Blocks.WATER) {
				newState = newState.setValue(BlockStateProperties.WATERLOGGED, true);
			}
			if (!this.getFeetBlockState().is(GigTags.ACID_RESISTANT))
				level.setBlockAndUpdate(this.blockPosition(), newState);
		}

		return super.hurt(source, amount);
	}

	@Override
	public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> biConsumer) {
		Level level = this.level;
		if (level instanceof ServerLevel) {
			ServerLevel serverLevel = (ServerLevel) level;
			biConsumer.accept(this.dynamicGameEventListener, serverLevel);
		}
	}

	@Override
	public boolean canTriggerAvoidVibration() {
		return true;
	}

	/*
	 * Enabled force condition propagation Lifted jumps to return sites
	 */
	@Contract(value = "null->false")
	public boolean canTargetEntity(@Nullable Entity entity) {
		if (!(entity instanceof LivingEntity))
			return false;
		LivingEntity livingEntity = (LivingEntity) entity;
		if (this.level != entity.level)
			return false;
		if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity))
			return false;
		if (((Host) livingEntity).hasParasite())
			return false;
		if (this.isVehicle())
			return false;
		if (this.isAlliedTo(entity))
			return false;
		if (livingEntity.getMobType() == MobType.UNDEAD)
			return false;
		if (livingEntity.getFeetBlockState().getBlock() == GIgBlocks.NEST_RESIN_WEB_CROSS)
			return false;
		if (livingEntity.getType() == EntityType.ARMOR_STAND)
			return false;
		if (livingEntity.getType() == EntityType.WARDEN)
			return false;
		if (livingEntity instanceof Bat)
			return false;
		if (EntityUtils.isFacehuggerAttached(livingEntity))
			return false;
		if (livingEntity.isInvulnerable())
			return false;
		if (livingEntity.isDeadOrDying())
			return false;
		if (!this.level.getWorldBorder().isWithinBounds(livingEntity.getBoundingBox()))
			return false;
		Stream<BlockState> list2 = livingEntity.level
				.getBlockStatesIfLoaded(livingEntity.getBoundingBox().inflate(2.0, 2.0, 2.0));
		if (list2.anyMatch(NEST))
			return false;
		if (livingEntity.getVehicle() != null
				&& livingEntity.getVehicle().getSelfAndPassengers().anyMatch(AlienEntity.class::isInstance))
			return false;
		if (livingEntity instanceof AlienEntity)
			return false;
		return true;
	}

	@Override
	public boolean shouldListen(ServerLevel var1, GameEventListener var2, BlockPos var3, GameEvent var4, Context var5) {
		@SuppressWarnings("unused")
		LivingEntity livingEntity;
		if (this.isNoAi() || this.isDeadOrDying() || !level.getWorldBorder().isWithinBounds(var3) || this.isRemoved()) {
			return false;
		}
		Entity entity = var5.sourceEntity();
		return !(entity instanceof LivingEntity);
	}

	@Override
	public void onSignalReceive(ServerLevel var1, GameEventListener var2, BlockPos var3, GameEvent var4, Entity var5,
			Entity var6, float var7) {
		if (this.isDeadOrDying()) {
			return;
		}
		if (this.isVehicle()) {
			return;
		}
		if (this.isAggressive()) {
			return;
		}
	}
}
