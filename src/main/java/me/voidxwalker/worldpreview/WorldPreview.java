package me.voidxwalker.worldpreview;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldPreview implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("WorldPreview");

    public static World world;
    public static ClientPlayerEntity player;
    public static ClientWorld clientWorld;
    public static boolean inPreview;
    public static BlockPos spawnPos;
    public static int kill = 0;
    public static int playerSpawn;
    public static Camera camera;
    public static WorldRenderer worldRenderer;
    public static boolean existingWorld;
    public static boolean showMenu;
    public static boolean calculatedSpawn;
    public static KeyBinding resetKey;
    public static KeyBinding freezeKey;
    public static KeyBinding cycleChunkMapKey;
    public static int chunkMapPos;
    public static boolean freezePreview;
    public static final Object lock = new Object();

    public static void log(String message) {
        LOGGER.info(message);
    }

    @Override
    public void onInitializeClient() {
        resetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.world_preview.reset",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.categories.world_preview"
        ));

        freezeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.world_preview.freeze",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "key.categories.world_preview"
        ));

        cycleChunkMapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.world_preview.cycle_chunkmap",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories.world_preview"
        ));
    }

    public static Text translate(String key, String fallback) {
        Text t = Text.translatable(key);
        if (t.getString().equals(key)) {
            return Text.literal(fallback);
        }
        return t;
    }
}
