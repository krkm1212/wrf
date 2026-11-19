package com.solymod.smooth.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique private boolean smoothApplying = false;
    @Unique private double pendingX = 0.0;
    @Unique private double pendingY = 0.0;
    @Unique private long lastNanos = 0L;
    // time constant in seconds: smaller = more responsive, bigger = smoother
    @Unique private static final double TAU = 0.03;

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void smoothmove$smooth(double dx, double dy, CallbackInfo ci) {
        if (smoothApplying || !((Object) this instanceof ClientPlayerEntity)) return;
        long now = System.nanoTime();
        double dt = lastNanos == 0L ? 0.016 : (now - lastNanos) / 1.0e9;
        lastNanos = now;
        if (dt > 0.1) dt = 0.1;
        double factor = 1.0 - Math.exp(-dt / TAU);

        pendingX += dx;
        pendingY += dy;
        double outX = pendingX * factor;
        double outY = pendingY * factor;
        pendingX -= outX;
        pendingY -= outY;

        smoothApplying = true;
        try {
            ((Entity) (Object) this).changeLookDirection(outX, outY);
        } finally {
            smoothApplying = false;
        }
        ci.cancel();
    }
}
