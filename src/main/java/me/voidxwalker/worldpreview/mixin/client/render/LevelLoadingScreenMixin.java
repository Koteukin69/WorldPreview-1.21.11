package me.voidxwalker.worldpreview.mixin.client.render;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    @Unique
    private boolean worldpreview_showMenu;

    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void worldpreview_init(WorldGenerationProgressTracker progressProvider, CallbackInfo ci) {
        WorldPreview.calculatedSpawn = true;
        WorldPreview.freezePreview = false;
        KeyBinding.unpressAll();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    public void worldpreview_stopBackgroundRender(LevelLoadingScreen instance, DrawContext context, int mouseX, int mouseY, float delta) {
        if (WorldPreview.camera == null) {
            instance.renderBackground(context, mouseX, mouseY, delta);
        }
    }

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 2)
    public int worldpreview_moveLoadingScreen(int i) {
        if (WorldPreview.camera == null) {
            return i;
        }
        return worldpreview_getChunkMapPos().x;
    }

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 3)
    public int moveLoadingScreen2(int i) {
        if (WorldPreview.camera == null) {
            return i;
        }
        return worldpreview_getChunkMapPos().y;
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void worldpreview_render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (WorldPreview.world != null && WorldPreview.clientWorld != null && WorldPreview.player != null && !WorldPreview.freezePreview) {
            if (((WorldRendererMixin) WorldPreview.worldRenderer).getWorld() == null && WorldPreview.calculatedSpawn) {
                ((WorldRendererMixin) WorldPreview.worldRenderer).setWorld(WorldPreview.clientWorld);
                WorldPreview.showMenu = true;
                this.worldpreview_showMenu = true;
                this.worldpreview_initWidgets();
            }

            if (((WorldRendererMixin) WorldPreview.worldRenderer).getWorld() != null) {
                KeyBinding.unpressAll();
                WorldPreview.kill = 0;

                if (this.worldpreview_showMenu != WorldPreview.showMenu) {
                    if (!WorldPreview.showMenu) {
                        this.clearChildren();
                    } else {
                        this.worldpreview_initWidgets();
                    }
                    this.worldpreview_showMenu = WorldPreview.showMenu;
                }

                MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().update(RenderTickCounter.ONE);

                if (WorldPreview.camera == null) {
                    WorldPreview.player.refreshPositionAndAngles(
                            WorldPreview.player.getX(),
                            WorldPreview.player.getY() + (WorldPreview.player.getBoundingBox().maxY - WorldPreview.player.getBoundingBox().minY),
                            WorldPreview.player.getZ(),
                            0.0F, 0.0F
                    );
                    WorldPreview.camera = new Camera();
                    Perspective perspective = this.client.options.getPerspective();
                    WorldPreview.camera.update(
                            WorldPreview.world,
                            WorldPreview.player,
                            !perspective.isFirstPerson(),
                            perspective.isFrontView(),
                            0.2F
                    );
                    WorldPreview.player.refreshPositionAndAngles(
                            WorldPreview.player.getX(),
                            WorldPreview.player.getY() - 1.5,
                            WorldPreview.player.getZ(),
                            0.0F, 0.0F
                    );
                    WorldPreview.inPreview = true;
                    WorldPreview.log("Starting Preview at (" + WorldPreview.player.getX() + ", " + Math.floor(WorldPreview.player.getY()) + ", " + WorldPreview.player.getZ() + ")");
                }

                // Render the world preview
                worldpreview_renderWorld(context, delta);

                // Render the pause menu on top
                worldpreview_renderPauseMenu(context, mouseX, mouseY, delta);
            }
        }
    }

    @Unique
    private void worldpreview_renderWorld(DrawContext context, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();

        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();

        Matrix4f projectionMatrix = new Matrix4f().perspective(
                (float) Math.toRadians(client.options.getFov().getValue()),
                (float) width / (float) height,
                0.05F,
                client.options.getViewDistance().getValue() * 16 * 4.0F
        );

        RenderTickCounter tickCounter = RenderTickCounter.ONE;
        WorldPreview.worldRenderer.render(
                tickCounter,
                false,
                WorldPreview.camera,
                client.gameRenderer,
                client.gameRenderer.getLightmapTextureManager(),
                projectionMatrix,
                context.getMatrices()
        );
    }

    @Unique
    private void worldpreview_renderPauseMenu(DrawContext context, int mouseX, int mouseY, float delta) {
        if (WorldPreview.showMenu) {
            for (var child : this.children()) {
                if (child instanceof ButtonWidget button) {
                    button.render(context, mouseX, mouseY, delta);
                }
            }
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("menu.paused"), this.width / 2, 10, 16777215);
        }
    }

    @Unique
    private Point worldpreview_getChunkMapPos() {
        switch (WorldPreview.chunkMapPos) {
            case 1:
                return new Point(this.width - 45, this.height - 75);
            case 2:
                return new Point(this.width - 45, 105);
            case 3:
                return new Point(45, 105);
            default:
                return new Point(45, this.height - 75);
        }
    }

    @Unique
    private void worldpreview_initWidgets() {
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.returnToGame"), (ignored) -> {
        }).dimensions(this.width / 2 - 102, this.height / 4 + 24 - 16, 204, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.advancements"), (ignored) -> {
        }).dimensions(this.width / 2 - 102, this.height / 4 + 48 - 16, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.stats"), (ignored) -> {
        }).dimensions(this.width / 2 + 4, this.height / 4 + 48 - 16, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.sendFeedback"), (ignored) -> {
        }).dimensions(this.width / 2 - 102, this.height / 4 + 72 - 16, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.reportBugs"), (ignored) -> {
        }).dimensions(this.width / 2 + 4, this.height / 4 + 72 - 16, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.options"), (ignored) -> {
        }).dimensions(this.width / 2 - 102, this.height / 4 + 96 - 16, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.shareToLan"), (ignored) -> {
        }).dimensions(this.width / 2 + 4, this.height / 4 + 96 - 16, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.returnToMenu"), (buttonWidgetX) -> {
            client.getSoundManager().stopAll();
            WorldPreview.kill = -1;
            buttonWidgetX.active = false;
        }).dimensions(this.width / 2 - 102, this.height / 4 + 120 - 16, 204, 20).build());
    }

    public void resize(MinecraftClient client, int width, int height) {
        this.init(client, width, height);
        this.worldpreview_initWidgets();
    }
}
