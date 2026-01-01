package me.voidxwalker.worldpreview.mixin.server;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.SpawnLocatingMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.ServerTask;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.function.Supplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantThreadExecutor<ServerTask> {
    public MinecraftServerMixin(String string) {
        super(string);
    }

    @Shadow
    public abstract @Nullable ServerWorld getWorld(RegistryKey<World> key);

    @Shadow
    public abstract ServerWorld getOverworld();

    @Shadow
    public abstract Iterable<ServerWorld> getWorlds();

    @Shadow
    @Final
    protected LevelStorage.Session session;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public abstract @Nullable ServerNetworkIo getNetworkIo();

    @Shadow
    public abstract Thread getThread();

    @Shadow
    public abstract int getSpawnRadius(@Nullable ServerWorld world);

    @Inject(method = "prepareStartRegion", at = @At(value = "HEAD"))
    public void worldpreview_getWorld(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        WorldPreview.calculatedSpawn = false;
        synchronized (WorldPreview.lock) {
            if (!WorldPreview.existingWorld) {
                ServerWorld serverWorld = this.getOverworld();
                WorldPreview.spawnPos = serverWorld.getSpawnPos();
                WorldPreview.freezePreview = false;
                WorldPreview.world = this.getWorld(World.OVERWORLD);

                MinecraftClient client = MinecraftClient.getInstance();
                RegistryKey<World> registryKey = World.OVERWORLD;

                RegistryEntry<DimensionType> dimensionTypeEntry = serverWorld.getDimensionEntry();

                ClientWorld.Properties properties = new ClientWorld.Properties(Difficulty.NORMAL, WorldPreview.world.getLevelProperties().isHardcore(), false);
                Supplier<Profiler> profilerSupplier = client::getProfiler;

                WorldPreview.clientWorld = new ClientWorld(
                        null,
                        properties,
                        registryKey,
                        dimensionTypeEntry,
                        client.options.getClampedViewDistance(),
                        client.options.getSimulationDistance().getValue(),
                        profilerSupplier,
                        null,
                        false,
                        ((ServerWorld) WorldPreview.world).getSeed()
                );

                WorldPreview.player = new ClientPlayerEntity(
                        client,
                        WorldPreview.clientWorld,
                        new ClientPlayNetworkHandler(
                                client,
                                client.currentScreen,
                                null,
                                null,
                                false,
                                null,
                                null
                        ),
                        null,
                        false,
                        false
                );

                worldpreview_calculateSpawn(serverWorld);
                WorldPreview.calculatedSpawn = true;
            }
            WorldPreview.existingWorld = false;
        }
    }

    @Unique
    private void worldpreview_calculateSpawn(ServerWorld serverWorld) {
        BlockPos blockPos = WorldPreview.spawnPos;
        int i = Math.max(0, this.getSpawnRadius((ServerWorld) WorldPreview.world));
        int j = MathHelper.floor(WorldPreview.world.getWorldBorder().getDistanceInsideBorder(blockPos.getX(), blockPos.getZ()));
        if (j < i) {
            i = j;
        }
        if (j <= 1) {
            i = 1;
        }
        long l = i * 2L + 1;
        long m = l * l;
        int k = m > 2147483647L ? Integer.MAX_VALUE : (int) m;
        int n = worldpreview_calculateSpawnOffsetMultiplier(k);
        int o = Random.create().nextInt(k);
        WorldPreview.playerSpawn = o;
        for (int p = 0; p < k; ++p) {
            int q = (o + n * p) % k;
            int r = q % (i * 2 + 1);
            int s = q / (i * 2 + 1);
            BlockPos blockPos2 = SpawnLocatingMixin.callFindOverworldSpawn(serverWorld, blockPos.getX() + r - i, blockPos.getZ() + s - i);
            if (blockPos2 != null) {
                WorldPreview.player.refreshPositionAndAngles(blockPos2, 0.0F, 0.0F);
                if (serverWorld.doesNotIntersectEntities(WorldPreview.player)) {
                    break;
                }
            }
        }
    }

    @Unique
    private int worldpreview_calculateSpawnOffsetMultiplier(int horizontalSpawnArea) {
        return horizontalSpawnArea <= 16 ? horizontalSpawnArea - 1 : 17;
    }

    @Inject(method = "shutdown", at = @At(value = "HEAD"), cancellable = true)
    public void worldpreview_kill(CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen && Thread.currentThread().getId() != this.getThread().getId()) {
            worldpreview_shutdownWithoutSave();
            ci.cancel();
        }
    }

    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;setupServer()Z", shift = At.Shift.AFTER), cancellable = true)
    public void worldpreview_kill2(CallbackInfo ci) {
        WorldPreview.inPreview = false;
        if (WorldPreview.kill == 1) {
            ci.cancel();
        }
    }

    @Unique
    public void worldpreview_shutdownWithoutSave() {
        LOGGER.info("Stopping server");
        if (this.getNetworkIo() != null) {
            this.getNetworkIo().stop();
        }
        for (ServerWorld serverWorld : this.getWorlds()) {
            if (serverWorld != null) {
                serverWorld.savingDisabled = false;
            }
        }
        for (ServerWorld serverWorld : this.getWorlds()) {
            if (serverWorld != null) {
                try {
                    serverWorld.getChunkManager().threadedAnvilChunkStorage.close();
                } catch (IOException ignored) {
                }
            }
        }
        try {
            this.session.close();
        } catch (IOException var4) {
            LOGGER.error("Failed to unlock level {}", this.session.getDirectoryName(), var4);
        }
    }

    @Inject(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;getTotalChunksLoadedCount()I", shift = At.Shift.AFTER), cancellable = true)
    public void worldpreview_killDuringGeneration(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        if (WorldPreview.kill == 1) {
            ci.cancel();
        }
    }
}
