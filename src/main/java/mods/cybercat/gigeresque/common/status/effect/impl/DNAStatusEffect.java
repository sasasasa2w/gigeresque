package mods.cybercat.gigeresque.common.status.effect.impl;

import java.awt.Color;
import java.util.SplittableRandom;

import mods.cybercat.gigeresque.common.block.GIgBlocks;
import mods.cybercat.gigeresque.common.config.ConfigAccessor;
import mods.cybercat.gigeresque.common.entity.AlienEntity;
import mods.cybercat.gigeresque.common.entity.Entities;
import mods.cybercat.gigeresque.common.entity.impl.AlienEggEntity;
import mods.cybercat.gigeresque.common.source.GigDamageSources;
import mods.cybercat.gigeresque.common.status.effect.GigStatusEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class DNAStatusEffect extends MobEffect {
	private BlockPos lightBlockPos = null;

	public DNAStatusEffect() {
		super(MobEffectCategory.HARMFUL, Color.darkGray.getRGB());
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean isInstantenous() {
		return false;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		super.applyEffectTick(entity, amplifier);
		if (this == GigStatusEffects.DNA)
			entity.heal(0);
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		SplittableRandom random = new SplittableRandom();
		int randomPhase = random.nextInt(0, 50);
		if (!(entity instanceof AlienEntity)) {
			if (randomPhase > 25) {
				if (entity instanceof Player
						&& !(((Player) entity).isCreative() || ((Player) entity).isSpectator())) {
					AlienEggEntity egg = new AlienEggEntity(Entities.EGG, entity.level);
					egg.moveTo(entity.blockPosition(), entity.getYRot(), entity.getXRot());
					entity.level.addFreshEntity(egg);
					entity.hurt(GigDamageSources.DNA, Integer.MAX_VALUE);
					return;
				} else if (entity instanceof Creeper) {
					return;
				} else if (!(entity instanceof Player) && !(ConfigAccessor.isTargetDNAImmune(entity))) {
					AlienEggEntity egg = new AlienEggEntity(Entities.EGG, entity.level);
					egg.moveTo(entity.blockPosition(), entity.getYRot(), entity.getXRot());
					entity.level.addFreshEntity(egg);
					entity.hurt(GigDamageSources.DNA, Integer.MAX_VALUE);
					return;
				}
			} else {
				if (entity instanceof Player
						&& !(((Player) entity).isCreative() || ((Player) entity).isSpectator())) {
					entity.hurt(GigDamageSources.DNA, Integer.MAX_VALUE);
					boolean isInsideWaterBlock = entity.level.isWaterAt(entity.blockPosition());
					spawnGoo(entity, isInsideWaterBlock);
					return;
				} else if (entity instanceof Creeper) {
					return;
				} else if (!(entity instanceof Player) && !(ConfigAccessor.isTargetDNAImmune(entity))) {
					entity.hurt(GigDamageSources.DNA, Integer.MAX_VALUE);
					boolean isInsideWaterBlock = entity.level.isWaterAt(entity.blockPosition());
					spawnGoo(entity, isInsideWaterBlock);
					return;
				}
			}
		}
		super.removeAttributeModifiers(entity, attributes, amplifier);
	}

	private void spawnGoo(LivingEntity entity, boolean isInWaterBlock) {
		if (lightBlockPos == null) {
			lightBlockPos = findFreeSpace(entity.level, entity.blockPosition(), 1);
			if (lightBlockPos == null)
				return;
			AreaEffectCloud areaEffectCloudEntity = new AreaEffectCloud(entity.level, entity.getX(), entity.getY(), entity.getZ());
            areaEffectCloudEntity.setRadius(2.0F);
            areaEffectCloudEntity.setDuration(300);
            areaEffectCloudEntity.setRadiusPerTick(0);
            areaEffectCloudEntity.addEffect(new MobEffectInstance(GigStatusEffects.DNA, 600, 0));
            entity.level.addFreshEntity(areaEffectCloudEntity);
		} else
			lightBlockPos = null;
	}

	private BlockPos findFreeSpace(Level world, BlockPos blockPos, int maxDistance) {
		if (blockPos == null)
			return null;

		int[] offsets = new int[maxDistance * 2 + 1];
		offsets[0] = 0;
		for (int i = 2; i <= maxDistance * 2; i += 2) {
			offsets[i - 1] = i / 2;
			offsets[i] = -i / 2;
		}
		for (int x : offsets)
			for (int y : offsets)
				for (int z : offsets) {
					BlockPos offsetPos = blockPos.offset(x, y, z);
					BlockState state = world.getBlockState(offsetPos);
					if (state.isAir() || state.getBlock().equals(GIgBlocks.NEST_RESIN_WEB_CROSS))
						return offsetPos;
				}

		return null;
	}

}