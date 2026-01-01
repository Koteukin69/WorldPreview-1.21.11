package me.voidxwalker.worldpreview.mixin.server;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Redirect(method = "moveToSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I"))
    private int worldpreview_setSpawnPos(Random random, int bound) {
        if (WorldPreview.spawnPos != null) {
            int value = WorldPreview.playerSpawn;
            WorldPreview.spawnPos = null;
            return value;
        }
        return random.nextInt(bound);
    }
}
