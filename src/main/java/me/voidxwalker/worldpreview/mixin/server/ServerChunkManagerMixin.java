package me.voidxwalker.worldpreview.mixin.server;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.ClientChunkManagerMixin;
import me.voidxwalker.worldpreview.mixin.access.ClientChunkMapMixin;
import me.voidxwalker.worldpreview.mixin.access.ThreadedAnvilChunkStorageMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin {
    @Shadow
    @Final
    public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

    @Shadow
    public abstract @Nullable WorldChunk getWorldChunk(int chunkX, int chunkZ);

    @Inject(method = "tick()Z", at = @At(value = "TAIL"))
    public void worldpreview_getChunks(CallbackInfoReturnable<Boolean> cir) {
        synchronized (WorldPreview.lock) {
            if (WorldPreview.player != null
                    && WorldPreview.calculatedSpawn
                    && !WorldPreview.freezePreview
                    && MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen
                    && WorldPreview.clientWorld != null) {

                ClientChunkManager.ClientChunkMap map = ((ClientChunkManagerMixin) WorldPreview.clientWorld.getChunkManager()).getChunks();

                for (ChunkHolder holder : ((ThreadedAnvilChunkStorageMixin) this.threadedAnvilChunkStorage).getChunkHolders().values()) {
                    if (holder != null) {
                        int index = ((ClientChunkMapMixin) (Object) map).callGetIndex(holder.getPos().x, holder.getPos().z);
                        if (((ClientChunkMapMixin) (Object) map).callGetChunk(index) == null) {
                            WorldChunk chunk = this.getWorldChunk(holder.getPos().x, holder.getPos().z);
                            if (chunk != null) {
                                ((ClientChunkMapMixin) (Object) map).callSet(index, chunk);
                            }
                        }
                    }
                }
            }
        }
    }
}
