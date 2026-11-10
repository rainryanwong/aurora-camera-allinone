package com.aurora.camera.compat;

import java.util.ArrayList;
import java.util.List;

/** The in-progress registry is deliberately explicit; full coverage is tracked separately. */
public final class FeatureRegistry {
    private FeatureRegistry() {}
    public static List<HookPlan> resolve(Host host) throws ReflectiveOperationException {
        List<HookPlan> plans = new ArrayList<>();
        com.aurora.camera.compat.filter.LutRuntime229 lut=new com.aurora.camera.compat.filter.LutRuntime229(host);
        plans.add(NaturalBeautyHooks.resolve(host));
        plans.add(LegendaryClickHooks.resolve(host));
        plans.add(AiIconHooks.resolve(host));
        plans.add(PhotoQualityHooks.resolve(host));
        plans.add(VlogSessionHooks.resolve(host));
        plans.add(TargetZoomHooks.resolve(host));
        plans.add(ModeCapabilityHooks.resolve(host));
        plans.add(MadridConfigHooks.resolve(host));
        plans.add(LatestPhotoHooks.resolve(host));
        plans.add(FilterUnlimitedHooks.resolve(host));
        plans.add(AmbilightHooks.resolve(host));
        plans.add(AiEffectResultHooks.resolve(host));
        plans.add(AiRequestHooks.resolve(host));
        plans.add(AiStyleLockHooks.resolve(host));
        plans.add(AiLifecycleHooks.resolve(host));
        plans.add(AiClickHooks.resolve(host));
        plans.add(AiEntryHooks.resolve(host));
        plans.add(NightVideoHooks.resolve(host));
        plans.add(ApertureHooks.resolve(host));
        plans.add(StyleHooks.resolve(host));
        plans.add(BottomEvHooks.resolve(host));
        plans.add(VideoSettingsHooks.resolve(host));
        plans.add(VideoWatermarkUiHooks.resolve(host));
        plans.add(CinematicHooks.resolve(host));
        plans.add(LensRouteHooks.resolve(host));
        // Its T() before callback supplies Night tokens before saved-menu migration.
        plans.add(Night211Hooks.resolve(host));
        plans.add(NightPaletteRenderHooks.resolve(host));
        plans.add(GripAndMenuHooks.resolve(host));
        plans.add(Settings211Hooks.resolve(host));
        plans.add(Hdr211Hooks.resolve(host));
        plans.add(HdrPresentation214Hooks.resolve(host));
        plans.add(Sound211Hooks.resolve(host));
        plans.add(FilterManager211Hooks.resolve(host,lut));
        plans.add(AiReset211Hooks.resolve(host));
        plans.add(Watermark217Hooks.resolve(host));
        plans.add(PhotoStyleSwitchHooks.resolve(host));
        plans.add(ZoomWheelHooks.resolve(host));
        plans.add(SuperTelephotoHooks.resolve(host));
        plans.add(WatermarkNameHooks.resolve(host,lut));
        plans.add(Street217Hooks.resolve(host));
        plans.add(RecipeSharing219Hooks.resolve(host));
        plans.add(StreetShutter229Hooks.resolve(host));
        plans.add(ShutterStyleHooks.resolve(host));
        plans.add(AlwaysShutter229Hooks.resolve(host));
        plans.add(Lut229Hooks.resolve(host,lut));
        plans.add(VideoLutQuality229Hooks.resolve(host));
        plans.add(Classic239Hooks.resolve(host));
        plans.add(TopControls239Hooks.resolve(host));
        plans.add(Brightness273Hooks.resolve(host));
        plans.add(Metering253Hooks.resolve(host));
        plans.add(AuthVignette239Hooks.resolve(host));
        plans.add(SoftLight245Hooks.resolve(host));
        plans.add(CameraLutShare246Hooks.resolve(host));
        plans.add(CameraWatermark247Hooks.resolve(host));
        plans.add(WatermarkGalleryHooks.resolve(host));
        plans.add(com.aurora.camera.compat.workspace.Workspace248Hooks.resolve(host));
        return plans;
    }
}
