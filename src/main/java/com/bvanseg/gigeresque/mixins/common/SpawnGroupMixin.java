package com.bvanseg.gigeresque.mixins.common;

import com.bvanseg.gigeresque.Constants;
import com.bvanseg.gigeresque.CustomSpawnGroup;
import com.bvanseg.gigeresque.common.config.GigeresqueConfig;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.SpawnGroup;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Boston Vanseghi
 */
@Mixin(SpawnGroup.class)
public class SpawnGroupMixin {
    @Invoker("<init>")
    private static SpawnGroup newSpawnGroup(
            String internalName,
            int internalId,
            String name,
            int spawnCap,
            boolean peaceful,
            boolean rare,
            int immediateDespawnRange
    ) {
        throw new AssertionError();
    }

    @Shadow
    private static @Final
    @Mutable
    SpawnGroup[] field_6301;

    @Inject(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC, target = "Lnet/minecraft/entity/SpawnGroup;field_6301:[Lnet/minecraft/entity/SpawnGroup;", shift = At.Shift.AFTER))
    private static void addCustomSpawnGroup(CallbackInfo ci) {
        var spawnGroups = new ArrayList<>(Arrays.asList(field_6301));
        var last = spawnGroups.get(spawnGroups.size() - 1);

        var configPath = FabricLoader.getInstance().getConfigDir().resolve("gigeresque.json");
        var config = configPath.toFile();
        var alienSpawnCap = Constants.ALIEN_SPAWN_CAP;

        if (config.exists()) {
            try {
                var gson = new GsonBuilder().create();
                var gigeresqueConfig = gson.fromJson(Files.readString(configPath, StandardCharsets.UTF_8), GigeresqueConfig.class);
                alienSpawnCap = gigeresqueConfig.miscellaneous.getAlienSpawnCap();
            } catch (Exception e) {
                // Do nothing
            }
        }

        // This means our code will still work if other mods or Mojang add more spawn groups!
        var alien = newSpawnGroup(
                "ALIEN",
                last.ordinal() + 1,
                "alien",
                alienSpawnCap,
                false,
                false,
                128
        );

        CustomSpawnGroup.ALIEN = alien;
        spawnGroups.add(alien);
        field_6301 = spawnGroups.toArray(new SpawnGroup[0]);
    }
}
