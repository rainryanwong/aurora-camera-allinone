package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;
import java.util.ArrayList;
import java.util.List;

/** Resolves the camera feature groups in their installation order. */
public final class FeatureRegistry {
    private FeatureRegistry() {}
    public static List<HookPlan> resolve(Host host, String cameraDataDir) throws ReflectiveOperationException {
        List<HookPlan> plans = new ArrayList<>();
        // Only the three filter consumers need this shared runtime.
        LutBindings lut = new LutBindings(host);
        add(plans, "NaturalBeautyHooks", "自然美颜", () -> NaturalBeautyHooks.resolve(host));
        add(plans, "LegendaryClickHooks", "传奇一瞬入口", () -> LegendaryClickHooks.resolve(host));
        add(plans, "AiIconHooks", "AI 图标", () -> AiIconHooks.resolve(host));
        add(plans, "PhotoQualityHooks", "照片画质档位", () -> PhotoQualityHooks.resolve(host));
        add(plans, "VlogSessionHooks", "Vlog 会话与防抖", () -> VlogSessionHooks.resolve(host));
        add(plans, "TargetZoomHooks", "目标变焦", () -> TargetZoomHooks.resolve(host));
        add(plans, "ModeCapabilityHooks", "模式能力", () -> ModeCapabilityHooks.resolve(host));
        add(plans, "CosmeticMirrorEntry", "化妆镜", () -> MoreModeEntry.mirror(host));
        add(plans, "MagicCloneEntry", "魔法分身", () -> MoreModeEntry.clone(host));
        add(plans, "DirectorModeEntry", "导演模式", () -> MoreModeEntry.director(host));
        add(plans, "DirectorRecording", "导演模式录像规格与辅助菜单", () -> DirectorRecording.prepare(host));
        add(plans, "DirectorMenuLayout", "导演模式菜单编辑", () -> DirectorMenuLayout.prepare(host));
        add(plans, "DirectorCapture", "导演模式镜头与光圈", () -> DirectorCapture.prepare(host));
        add(plans, "DirectorDisplay", "导演模式面板与焦段布局", () -> DirectorDisplay.prepare(host));
        add(plans, "DirectorVideoZoom", "导演与专业录像轮盘", () -> DirectorVideoZoom.prepare(host));
        add(plans, "DirectorPortrait", "导演模式竖屏布局", () -> DirectorPortrait.prepare(host));
        add(plans, "DirectorLandscape", "导演模式横屏监看栏对齐", () -> DirectorLandscape.prepare(host));
        add(plans, "MadridConfigHooks", "相机设备配置", () -> MadridConfigHooks.resolve(host));
        add(plans, "LatestPhotoHooks", "最近照片", () -> LatestPhotoHooks.resolve(host));
        add(plans, "FilterUnlimitedHooks", "滤镜限制", () -> FilterUnlimitedHooks.resolve(host));
        add(plans, "AmbilightHooks", "氛围灯资源", () -> AmbilightHooks.resolve(host));
        add(plans, "AiEffectResultHooks", "AI 推荐结果", () -> AiEffectResultHooks.resolve(host));
        add(plans, "AiRequestHooks", "AI 推荐请求", () -> AiRequestHooks.resolve(host));
        add(plans, "AiStyleLockHooks", "AI 推荐与摄影风格", () -> AiStyleLockHooks.resolve(host));
        add(plans, "AiLifecycleHooks", "AI 推荐生命周期", () -> AiLifecycleHooks.resolve(host));
        add(plans, "AiClickHooks", "AI 点击行为", () -> AiClickHooks.resolve(host));
        add(plans, "AiEntryHooks", "AI 入口", () -> AiEntryHooks.resolve(host));
        add(plans, "NightVideoHooks", "夜景录像", () -> NightVideoHooks.resolve(host));
        add(plans, "ApertureHooks", "光圈控制", () -> ApertureHooks.resolve(host));
        add(plans, "StyleHooks", "原色摄影风格", () -> StyleHooks.resolve(host));
        add(plans, "AiDebugMenu", "AI 帮拍菜单与调试界面", () -> StyleHooks.resolveAi(host));
        add(plans, "BottomEvHooks", "取景曝光补偿", () -> BottomEvHooks.resolve(host));
        add(plans, "VideoSettingsHooks", "录像设置与收音", () -> VideoSettingsHooks.resolve(host));
        add(plans, "VideoOpenGate", "录像片门全开", () -> new VideoOpenGate(host));
        add(plans, "VideoWatermarkHooks", "视频水印", () -> com.aurora.camera.videowm.VideoWatermarkHooks.resolve(host));
        add(plans, "CinematicHooks", "电影模式", () -> CinematicHooks.resolve(host));
        add(plans, "LensRouteHooks", "镜头路由", () -> LensRouteHooks.resolve(host));
        // Its T() before callback supplies Night tokens before saved-menu migration.
        add(plans, "Night211Hooks", "夜景光圈与滤镜", () -> Night211Hooks.resolve(host));
        add(plans, "NightPaletteRenderHooks", "夜景滤镜预览", () -> NightPaletteRenderHooks.resolve(host));
        add(plans, "GripAndMenuHooks", "手柄与相机菜单", () -> GripAndMenuHooks.resolve(host));
        add(plans, "Settings211Hooks", "实验室设置", () -> Settings211Hooks.resolve(host));
        add(plans, "ViewfinderDebugHooks", "取景调试选项", () -> ViewfinderDebugHooks.resolve(host));
        add(plans, "PreviewDiagnosticsHooks", "预览逐帧诊断", () -> PreviewDiagnosticsHooks.resolve(host, cameraDataDir));
        add(plans, "CameraCloudControls", "相机云控", () -> CameraCloudControls.resolve(host));
        add(plans, "RetiredLaboratoryOptions", "实验室选项迁移", () -> RetiredLaboratoryOptions.resolve(host));
        add(plans, "Hdr211Hooks", "HDR 拍照路径", () -> Hdr211Hooks.resolve(host));
        add(plans, "HdrPresentation214Hooks", "HDR 图标与提示", () -> HdrPresentation214Hooks.resolve(host));
        add(plans, "Sound211Hooks", "快门声音", () -> Sound211Hooks.resolve(host));
        add(plans, "FilterManager211Hooks", "摄影风格滤镜管理", () -> FilterManager211Hooks.resolve(host,lut.get()));
        add(plans, "AiReset211Hooks", "AI 复位与变焦", () -> AiReset211Hooks.resolve(host));
        add(plans, "Watermark217Hooks", "水印渲染", () -> Watermark217Hooks.resolve(host));
        add(plans, "PhotoStyleSwitchHooks", "摄影风格切换", () -> PhotoStyleSwitchHooks.resolve(host));
        add(plans, "ZoomWheelHooks", "变焦拨轮", () -> ZoomWheelHooks.resolve(host));
        add(plans, "SuperTelephotoHooks", "超级长焦", () -> SuperTelephotoHooks.resolve(host));
        add(plans, "MotionCaptureRetainHooks", "运动抓拍状态保留", () -> MotionCaptureRetainHooks.resolve(host));
        add(plans, "NearRangeLensHooks", "近距自适应镜头切换优化", () -> new NearRangeLensHooks(host));
        add(plans, "FocusDistance75mm", "中长焦镜头对焦距离", () -> new TelephotoFocusDistance(host));
        add(plans, "WatermarkNameHooks", "水印文件命名", () -> WatermarkNameHooks.resolve(host,lut.get()));
        add(plans, "Street217Hooks", "街拍与传奇一瞬界面", () -> Street217Hooks.resolve(host));
        add(plans, "RecipeSharing219Hooks", "摄影配方分享", () -> RecipeSharing219Hooks.resolve(host));
        add(plans, "StreetShutter229Hooks", "街拍快门材质", () -> StreetShutter229Hooks.resolve(host));
        add(plans, "ShutterStyleHooks", "快门样式", () -> ShutterStyleHooks.resolve(host));
        add(plans, "AlwaysShutter229Hooks", "耳机模式快门声", () -> AlwaysShutter229Hooks.resolve(host));
        add(plans, "Lut229Hooks", "导入 LUT", () -> Lut229Hooks.resolve(host,lut.get()));
        add(plans, "CamStyleBindingHooks", "摄影风格滤镜重新绑定", () -> new com.aurora.camera.compat.filter.CamStyleBindingHooks(host));
        add(plans, "PhotoFilterPerformance", "拍照滤镜加载与缓存", () -> new com.aurora.camera.compat.filter.PhotoFilterPerformance(host));
        add(plans, "VideoLutQuality229Hooks", "滤镜视频规格", () -> VideoLutQuality229Hooks.resolve(host));
        add(plans, "SlowMotionBatteryHooks", "解除低电量录像限制", () -> SlowMotionBatteryHooks.resolve(host));
        add(plans, "Classic239Hooks", "徕卡经典与场景滤镜", () -> Classic239Hooks.resolve(host));
        add(plans, "TopControls239Hooks", "顶栏、时钟与光圈", () -> TopControls239Hooks.resolve(host));
        add(plans, "Brightness273Hooks", "屏幕亮度提升", () -> Brightness273Hooks.resolve(host));
        add(plans, "Metering253Hooks", "测光模式", () -> Metering253Hooks.resolve(host));
        add(plans, "AuthVignette239Hooks", "AUTH 暗角", () -> AuthVignette239Hooks.resolve(host));
        add(plans, "SoftLight245Hooks", "柔光控制", () -> SoftLight245Hooks.resolve(host));
        add(plans, "CameraLutShare246Hooks", "相机与相册 LUT 共享", () -> CameraLutShare246Hooks.resolve(host));
        add(plans, "CameraWatermark247Hooks", "相机色彩签名水印", () -> CameraWatermark247Hooks.resolve(host));
        add(plans, "WatermarkGalleryHooks", "水印编辑缩略图", () -> WatermarkGalleryHooks.resolve(host));
        add(plans, "Workspace248Hooks", "拍摄工作区", () -> com.aurora.camera.compat.workspace.Workspace248Hooks.resolve(host));
        return plans;
    }

    private static final class LutBindings {
        private final Host host;
        private com.aurora.camera.compat.filter.LutRuntime229 runtime;
        private ReflectiveOperationException unavailable;

        LutBindings(Host host) { this.host = host; }

        com.aurora.camera.compat.filter.LutRuntime229 get() throws ReflectiveOperationException {
            if (unavailable != null) throw unavailable;
            if (runtime == null) {
                try { runtime = new com.aurora.camera.compat.filter.LutRuntime229(host); }
                catch (ReflectiveOperationException e) { unavailable = e; throw e; }
            }
            return runtime;
        }
    }

    @FunctionalInterface
    private interface Resolver {
        HookPlan resolve() throws ReflectiveOperationException;
    }

    private static void add(List<HookPlan> plans, String group, String title, Resolver resolver)
            throws ReflectiveOperationException {
        HookDiagnostics.resolving(group);
        HookPlan plan;
        try {
            plan = resolver.resolve();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            HookDiagnostics.resolutionFailed(failure);
            android.util.Log.e("AuroraCameraCompat", "Unable to resolve " + group, failure);
            return;
        }
        HookDiagnostics.resolved();
        plans.add(new HookPlan() {
            @Override public String id() { return plan.id(); }
            @Override public void install() { HookDiagnostics.install(group, plan::install); }
        });
    }
}
