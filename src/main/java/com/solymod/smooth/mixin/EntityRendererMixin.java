package com.solymod.smooth.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Unique private static final Map<Integer, double[]> SMOOTH = new HashMap<>();
    // time constant in seconds: smaller = tighter to real position, bigger = smoother
    @Unique private static final double TAU = 0.04;

    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void smoothmove$smoothOpponent(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || entity == mc.player) return;

        long now = System.nanoTime();
        double[] s = SMOOTH.get(entity.getId());
        if (s == null) {
            SMOOTH.put(entity.getId(), new double[] { state.x, state.y, state.z, now });
            return;
        }
        double dx = state.x - s[0];
        double dy = state.y - s[1];
        double dz = state.z - s[2];
        if (dx * dx + dy * dy + dz * dz > 16.0) {
            s[0] = state.x; s[1] = state.y; s[2] = state.z; s[3] = now;
            return;
        }
        double dt = (now - (long) s[3]) / 1.0e9;
        if (dt <= 0.0) dt = 0.0;
        if (dt > 0.1) dt = 0.1;
        double f = 1.0 - Math.exp(-dt / TAU);
        s[0] += dx * f;
        s[1] += dy * f;
        s[2] += dz * f;
        s[3] = now;
        state.x = s[0];
        state.y = s[1];
        state.z = s[2];
    }
}
