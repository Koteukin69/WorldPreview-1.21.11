package me.voidxwalker.worldpreview.mixin.client.render;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.BuiltChunkStorageMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow
    private ClientWorld world;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private BuiltChunkStorage chunks;

    @Shadow
    private ChunkBuilder chunkBuilder;

    @Shadow
    private int viewDistance;

    @Shadow
    public abstract void reload();

    @Unique
    private boolean worldpreview_isPreviewRenderer() {
        return this == WorldPreview.worldRenderer && client.currentScreen instanceof LevelLoadingScreen;
    }

    @Redirect(method = "renderWeather", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld(MinecraftClient instance) {
        if (worldpreview_isPreviewRenderer()) {
            return this.world;
        }
        return instance.world;
    }

    @Redirect(method = "tickRainSplashing", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld2(MinecraftClient instance) {
        if (worldpreview_isPreviewRenderer()) {
            return this.world;
        }
        return instance.world;
    }

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity(MinecraftClient instance) {
        if (instance.getCameraEntity() == null && worldpreview_isPreviewRenderer()) {
            return WorldPreview.player;
        }
        return instance.getCameraEntity();
    }

    @Inject(method = "reload", at = @At(value = "TAIL"))
    public void worldpreview_reload(CallbackInfo ci) {
        if (this.world != null && worldpreview_isPreviewRenderer() && this.chunkBuilder != null) {
            this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, this.client.options.getViewDistance().getValue(), (WorldRenderer) (Object) this);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getViewDistance()F"))
    public float worldpreview_getViewDistance(GameRenderer instance) {
        if (worldpreview_isPreviewRenderer()) {
            return client.options.getViewDistance().getValue() * 16;
        }
        return instance.getViewDistance();
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;targetedEntity:Lnet/minecraft/entity/Entity;", opcode = Opcodes.GETFIELD))
    public Entity worldpreview_getCorrectTargetedPlayerEntity(MinecraftClient instance) {
        if (instance.player == null && worldpreview_isPreviewRenderer()) {
            return WorldPreview.player;
        }
        return instance.targetedEntity;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld3(MinecraftClient instance) {
        if (worldpreview_isPreviewRenderer()) {
            return this.world;
        }
        return instance.world;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSpectator()Z"))
    public boolean worldpreview_spectator(ClientPlayerEntity instance) {
        if (worldpreview_isPreviewRenderer() && instance == null) {
            return false;
        }
        return instance != null && instance.isSpectator();
    }

    @Redirect(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld4(MinecraftClient instance) {
        if (instance.world == null && worldpreview_isPreviewRenderer()) {
            return this.world;
        }
        return instance.world;
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getCamera()Lnet/minecraft/client/render/Camera;"))
    public Camera worldpreview_getCamera(GameRenderer instance) {
        if (instance.getCamera() == null && worldpreview_isPreviewRenderer()) {
            return WorldPreview.camera;
        }
        return instance.getCamera();
    }

    @Redirect(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
    public ClientPlayerEntity worldpreview_getCorrectPlayer3(MinecraftClient instance) {
        if (instance.player == null && worldpreview_isPreviewRenderer()) {
            return WorldPreview.player;
        }
        return instance.player;
    }
}
