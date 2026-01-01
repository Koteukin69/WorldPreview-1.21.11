package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    private @Nullable IntegratedServer server;

    @Shadow
    @Nullable
    public ClientWorld world;

    @Shadow
    @Nullable
    public Screen currentScreen;

    @Mutable
    @Shadow
    @Final
    public WorldRenderer worldRenderer;

    @Unique
    private int worldpreview_cycleCooldown;

    @Inject(method = "isFabulousGraphicsOrBetter", at = @At(value = "RETURN"), cancellable = true)
    private static void worldpreview_stopFabulous(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof LevelLoadingScreen && client.world == null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startIntegratedServer", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z"), cancellable = true)
    public void worldpreview_onHotKeyPressed(CallbackInfo ci) {
        if (WorldPreview.inPreview) {
            worldpreview_cycleCooldown++;
            if (WorldPreview.cycleChunkMapKey.wasPressed() && worldpreview_cycleCooldown > 10 && !WorldPreview.freezePreview) {
                worldpreview_cycleCooldown = 0;
                WorldPreview.chunkMapPos = WorldPreview.chunkMapPos < 5 ? WorldPreview.chunkMapPos + 1 : 1;
            }
            if (WorldPreview.resetKey.wasPressed() || WorldPreview.kill == -1) {
                WorldPreview.log("Leaving world generation");
                WorldPreview.kill = 1;
                while (WorldPreview.inPreview) {
                    Thread.yield();
                }
                if (this.server != null) {
                    this.server.stop(false);
                }
                MinecraftClient.getInstance().disconnect();
                WorldPreview.kill = 0;
                MinecraftClient.getInstance().setScreen(new TitleScreen());
                ci.cancel();
            }
            if (WorldPreview.freezeKey.wasPressed()) {
                WorldPreview.freezePreview = !WorldPreview.freezePreview;
                if (WorldPreview.freezePreview) {
                    WorldPreview.log("Freezing Preview");
                } else {
                    WorldPreview.log("Unfreezing Preview");
                }
            }
        }
    }

    @Redirect(method = "reset", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
    public void worldpreview_smoothTransition(MinecraftClient instance, Screen screen) {
        if (this.currentScreen instanceof LevelLoadingScreen
                && ((WorldRendererMixin) WorldPreview.worldRenderer).getWorld() != null
                && WorldPreview.world != null
                && WorldPreview.clientWorld != null
                && WorldPreview.player != null) {
            return;
        }
        instance.setScreen(screen);
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At(value = "HEAD"))
    public void worldpreview_reset(Screen screen, CallbackInfo ci) {
        synchronized (WorldPreview.lock) {
            WorldPreview.world = null;
            WorldPreview.player = null;
            WorldPreview.clientWorld = null;
            WorldPreview.camera = null;
            if (WorldPreview.worldRenderer != null) {
                ((WorldRendererMixin) WorldPreview.worldRenderer).setWorld(null);
            }
            worldpreview_cycleCooldown = 0;
        }
    }

    @Inject(method = "onInitFinished", at = @At("TAIL"))
    private void worldpreview_createPreviewRenderer(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        WorldPreview.worldRenderer = new WorldRenderer(client, client.getEntityRenderDispatcher(), client.getBlockEntityRenderDispatcher(), client.getBufferBuilders());
    }
}
