package mods.cybercat.gigeresque.common.block;

import mods.cybercat.gigeresque.client.particle.Particles;
import mods.cybercat.gigeresque.common.entity.AlienEntity;
import mods.cybercat.gigeresque.common.status.effect.GigStatusEffects;
import mods.cybercat.gigeresque.common.tags.GigTags;
import mods.cybercat.gigeresque.common.util.MathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AcidBlock extends FallingBlock implements SimpleWaterloggedBlock {
	private static final int MAX_THICKNESS = 4;
	public static final IntegerProperty THICKNESS = IntegerProperty.create("thickness", 0, MAX_THICKNESS);
	private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private int age = 0;

	AcidBlock(Properties settings) {
		super(settings);
		registerDefaultState((getStateDefinition().any().setValue(WATERLOGGED, false)).setValue(THICKNESS, MAX_THICKNESS));
	}

	private void scheduleTickIfNotScheduled(Level world, BlockPos pos) {
		if (!world.isClientSide && !world.getBlockTicks().hasScheduledTick(pos, this)) {
			age++;
			world.scheduleTick(pos, this, 40);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(THICKNESS, WATERLOGGED);
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
		if (age > MathUtil.clamp(world.random.nextInt(2) + 1, 1, 52)) {
			world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			age = -1;
		}
		scheduleTickIfNotScheduled(world, pos);
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	public boolean isPossibleToRespawnInThis() {
		return false;
	}

	@Override
	public float getExplosionResistance() {
		return 100.0f;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
		return true;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(StairBlock.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
			LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
		if (!world.isClientSide() && state.getValue(WATERLOGGED)) {
			world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		}
		return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
	}

	private int getThickness(BlockState state) {
		return state.getValue(THICKNESS);
	}

	@Override
	public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
	}

	@SuppressWarnings("unused")
	private void setThickness(ServerLevel world, BlockPos pos, BlockState state) {
		setThickness(world, pos, state, 1);
	}

	private void setThickness(ServerLevel world, BlockPos pos, BlockState state, int consume) {
		int newThickness = Math.max(getThickness(state) - consume, 0);
		BlockState newState = state.setValue(THICKNESS, newThickness);

		if (world.getBlockState(pos).getBlock() == Blocks.WATER) {
			newState = newState.setValue(WATERLOGGED, true);
		}

		world.setBlockAndUpdate(pos, newState);
	}

	@Override
	public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity,
			ItemStack stack) {
	}

	@Override
	public void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack stack,
			boolean dropExperience) {
	}

	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		for (int i = 0; i < (getThickness(state) * 2) + 1; i++) {
			double yOffset = state.getValue(WATERLOGGED) ? random.nextDouble() : 0.01;
			double d = pos.getX() + random.nextDouble();
			double e = pos.getY() + yOffset;
			double f = pos.getZ() + random.nextDouble();
			world.addAlwaysVisibleParticle(Particles.ACID, d, e, f, 0.0, 0.0, 0.0);
		}
		if (random.nextInt(5 * ((MAX_THICKNESS + 1) - getThickness(state))) == 0) {
			world.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS,
					0.2f + random.nextFloat() * 0.2f, 0.9f + random.nextFloat() * 0.15f, false);
		}
	}

	@Override
	public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		int currentThickness = getThickness(state);
		if (random.nextInt(8 - currentThickness) == 0) {
			boolean canGrief = world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
			BlockPos blockToEat = pos.below();
			if (currentThickness >= 1) {
				setThickness(world, pos, state, MathUtil.clamp(random.nextInt(2) + 1, 0, currentThickness));
				if (canGrief && !world.getBlockState(blockToEat).is(GigTags.ACID_RESISTANT)) {
					world.setBlockAndUpdate(blockToEat, Blocks.AIR.defaultBlockState());
					world.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_EXTINGUISH,
							SoundSource.BLOCKS, 0.2f + random.nextFloat() * 0.2f, 0.9f + random.nextFloat() * 0.15f,
							false);
				}
			}
		}
		super.tick(state, world, pos, random);
		scheduleTickIfNotScheduled(world, pos);
	}

	@Override
	public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		scheduleTickIfNotScheduled(world, pos);
	}

	public static boolean canFallThrough(BlockState state) {
		Material material = state.getMaterial();
		return (state.isAir() || state.is(BlockTags.FIRE) || material.isReplaceable()) && !material.isLiquid()
				&& !state.is(GigTags.ACID_RESISTANT) && state != GIgBlocks.ACID_BLOCK.defaultBlockState();
	}

	private void dealAcidDamage(BlockState state, Entity entity) {
		if (entity instanceof LivingEntity && !(entity instanceof AlienEntity) && !(entity instanceof WitherBoss))
			((LivingEntity) entity).addEffect(new MobEffectInstance(GigStatusEffects.ACID, 60, 0));
	}

	@Override
	public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
		this.dealAcidDamage(state, entity);
		super.stepOn(world, pos, state, entity);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
		return Block.box(0, 0, 0, 16, 2, 16);
	}

	@Override
	public void attack(BlockState state, Level world, BlockPos pos, Player player) {
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		return world.getBlockState(pos).isAir();
	}
}
