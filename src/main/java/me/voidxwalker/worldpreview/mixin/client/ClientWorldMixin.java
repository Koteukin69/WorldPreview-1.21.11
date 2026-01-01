package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Mutable
    @Shadow
    @Final
    private ClientChunkManager chunkManager;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;getRegistryManager()Lnet/minecraft/registry/DynamicRegistryManager$Immutable;"))
    public DynamicRegistryManager.Immutable worldpreview_stopLag(ClientPlayNetworkHandler instance) {
        if (instance == null) {
            return DynamicRegistryManager.EMPTY;
        }
        return instance.getRegistryManager();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void worldpreview_initChunkManager(ClientPlayNetworkHandler networkHandler, ClientWorld.Properties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimensionTypeEntry, int loadDistance, int simulationDistance, Supplier profiler, net.minecraft.client.render.WorldRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci) {
        if (WorldPreview.camera == null && WorldPreview.world != null && WorldPreview.spawnPos != null) {
            this.chunkManager = worldpreview_getChunkManager(loadDistance);
        }
    }

    @Unique
    private ClientChunkManager worldpreview_getChunkManager(int loadDistance) {
        return new ClientChunkManager((ClientWorld) (Object) this, loadDistance);
    }
}
