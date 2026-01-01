package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    public void worldpreview_getF3ESCKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (client.currentScreen instanceof LevelLoadingScreen && window == this.client.getWindow().getHandle() && WorldPreview.inPreview) {
            if (action != 0) {
                InputUtil.Key key2 = InputUtil.fromKeyCode(key, scancode);
                // ESC key handling - F3 + ESC hides menu, ESC alone shows menu
                if (key == 256 && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 292) && WorldPreview.showMenu) {
                    WorldPreview.showMenu = false;
                } else if (!WorldPreview.showMenu && key == 256) {
                    WorldPreview.showMenu = true;
                }

                boolean isF3Pressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 292);

                // Handle our custom keybindings
                if (WorldPreview.resetKey.matchesKey(key, scancode)
                        || WorldPreview.cycleChunkMapKey.matchesKey(key, scancode)
                        || WorldPreview.freezeKey.matchesKey(key, scancode)) {
                    if (isF3Pressed) {
                        KeyBinding.setKeyPressed(key2, false);
                    } else {
                        KeyBinding.setKeyPressed(key2, true);
                        KeyBinding.onKeyPressed(key2);
                    }
                }
            }
        }
    }
}
