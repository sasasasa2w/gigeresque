package mods.cybercat.gigeresque.common.util;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public record GigVibrationInfo(GameEvent gameEvent, float distance, Vec3 pos, @Nullable UUID uuid,
		@Nullable UUID projectileOwnerUuid, @Nullable Entity entity) {

	public static final Codec<GigVibrationInfo> CODEC = RecordCodecBuilder.create(instance -> instance
			.group((BuiltInRegistries.GAME_EVENT.byNameCodec().fieldOf("game_event"))
					.forGetter(GigVibrationInfo::gameEvent),
					(Codec.floatRange(0.0f, Float.MAX_VALUE).fieldOf("distance")).forGetter(GigVibrationInfo::distance),
					(Vec3.CODEC.fieldOf("pos")).forGetter(GigVibrationInfo::pos),
					UUIDUtil.CODEC.optionalFieldOf("source")
							.forGetter(vibrationInfo -> Optional.ofNullable(vibrationInfo.uuid())),
					UUIDUtil.CODEC.optionalFieldOf("projectile_owner")
							.forGetter(vibrationInfo -> Optional.ofNullable(vibrationInfo.projectileOwnerUuid())))
			.apply(instance,
					(gameEvent, float_, vec3, optional, optional2) -> new GigVibrationInfo((GameEvent) gameEvent,
							float_.floatValue(), (Vec3) vec3, optional.orElse(null), optional2.orElse(null))));

	public GigVibrationInfo(GameEvent gameEvent, float f, Vec3 vec3, @Nullable UUID uUID, @Nullable UUID uUID2) {
		this(gameEvent, f, vec3, uUID, uUID2, null);
	}

	public GigVibrationInfo(GameEvent gameEvent, float f, Vec3 vec3, @Nullable Entity entity) {
		this(gameEvent, f, vec3, entity == null ? null : entity.getUUID(), GigVibrationInfo.getProjectileOwner(entity),
				entity);
	}

	@Nullable
	private static UUID getProjectileOwner(@Nullable Entity entity) {
		Projectile projectile;
		if (entity instanceof Projectile && (projectile = (Projectile) entity).getOwner() != null) {
			return projectile.getOwner().getUUID();
		}
		return null;
	}

	public Optional<Entity> getEntity(ServerLevel serverLevel) {
		return Optional.ofNullable(this.entity).or(() -> Optional.ofNullable(this.uuid).map(serverLevel::getEntity));
	}

	public Optional<Entity> getProjectileOwner(ServerLevel serverLevel) {
		return this.getEntity(serverLevel).filter(entity -> entity instanceof Projectile)
				.map(entity -> (Projectile) entity).map(Projectile::getOwner)
				.or(() -> Optional.ofNullable(this.projectileOwnerUuid).map(serverLevel::getEntity));
	}
}