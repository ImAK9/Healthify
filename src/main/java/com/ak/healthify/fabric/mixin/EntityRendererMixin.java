package com.ak.healthify.fabric.mixin;

//? if fabric && <26.1 {
/*import com.ak.healthify.client.NameTags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Unique
    private static boolean healthify$redrawing;

    @Shadow
    protected abstract boolean shouldShowName(T entity);

    @Shadow
    protected abstract void renderNameTag(T entity, Component displayName, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick);

    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void healthify$nameTag(T entity, Component displayName, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick, CallbackInfo ci) {
        if (healthify$redrawing) return;
        switch (NameTags.handle(entity, displayName)) {
            case HIDE -> ci.cancel();
            case RESET -> {
                if (!shouldShowName(entity)) {
                    ci.cancel();
                    return;
                }
                Component original = entity.getDisplayName();
                if (!displayName.equals(original)) {
                    ci.cancel();
                    healthify$redrawing = true;
                    try {
                        renderNameTag(entity, original, poseStack, bufferSource, packedLight, partialTick);
                    } finally {
                        healthify$redrawing = false;
                    }
                }
            }
            case KEEP -> { }
        }
    }
}
*///?}
//? if fabric && >=26.1 && <26.3 {
/*import com.ak.healthify.client.NameTags;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Shadow
    protected abstract boolean shouldShowName(T entity, double distanceToCameraSq);

    @Shadow
    protected abstract Component getNameTag(T entity);

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void healthify$nameTag(T entity, S state, float partialTicks, CallbackInfo ci) {
        healthify$apply(entity, state, 4096.0);
    }

    @Unique
    private void healthify$apply(T entity, S state, double maxDistanceSq) {
        Component content = state.nameTag != null ? state.nameTag : getNameTag(entity);
        switch (NameTags.handle(entity, content)) {
            case HIDE -> state.nameTag = null;
            case RESET -> {
                if (state.nameTag == null) return;
                if (state.distanceToCameraSq < maxDistanceSq && shouldShowName(entity, state.distanceToCameraSq)) {
                    state.nameTag = getNameTag(entity);
                } else {
                    state.nameTag = null;
                }
            }
            case KEEP -> { }
        }
    }
}
*///?}
//? if fabric && >=26.3 {
/*import com.ak.healthify.client.NameTags;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Shadow
    protected abstract boolean shouldShowName(T entity, double distanceToCameraSq);

    @Shadow
    protected abstract Component getNameTag(T entity);

    @Inject(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At("TAIL"))
    private void healthify$nameTag(T entity, S state, float partialTicks, double nameTagDistance, double belowNameDistance, CallbackInfo ci) {
        healthify$apply(entity, state, nameTagDistance * nameTagDistance);
    }

    @Unique
    private void healthify$apply(T entity, S state, double maxDistanceSq) {
        Component content = state.nameTag != null ? state.nameTag : getNameTag(entity);
        switch (NameTags.handle(entity, content)) {
            case HIDE -> state.nameTag = null;
            case RESET -> {
                if (state.nameTag == null) return;
                if (state.distanceToCameraSq < maxDistanceSq && shouldShowName(entity, state.distanceToCameraSq)) {
                    state.nameTag = getNameTag(entity);
                } else {
                    state.nameTag = null;
                }
            }
            case KEEP -> { }
        }
    }
}
*///?}
