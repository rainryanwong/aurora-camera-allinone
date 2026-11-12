# Aurora Camera All in One

小米 14 Ultra（aurora）相机与相册适配开发仓库。

## 开发基线

- All in One：2.0.78
- CameraCompat：0.1.125 / versionCode 10225
- 实况运镜：0.5.2 / versionCode 107
- 相机：6.8.001770.0
- Git 发布基线：[v2.0.78](https://github.com/rainryanwong/aurora-camera-allinone/releases/tag/v2.0.78)

## 构建与打包

独立 Compat 构建入口：`python build_lsp.py`。
现有构建使用 Python、JDK 21、Android SDK build-tools 37.0.0 和 android-37.0；工具路径在 `build_lsp.py` 中配置。
签名沿用外部密钥，通过 `AURORA_LSP_KEYSTORE`、`AURORA_LSP_KEY_ALIAS`、`AURORA_LSP_KS_PASS`、`AURORA_LSP_KEY_PASS` 配置。密钥和密码由本机私下提供。

独立实况运镜构建入口：`python live-motion/build.py`，沿用相同的签名环境变量。
该脚本需要本机的原版 1770 APK，以及 `live-motion/inputs/Phoenix-Q0Fix-8.10-a810.apk` 作为参考输入。

2.0.78 完整包入口：`python package_module297.py`，读取下列输入：

- `release/camera68_aurora_allinone_v2.0.77_Stock1770_Compat01124_LiveMotion051_AUTH.zip`
- `release/CameraCompat-0.1.125.apk`
- `live-motion/release/6.8Cam-LiveMotion-0.5.2.apk`

编译输出、整合包与私有签名材料独立于 Git 源码管理。
本机原始交付与上述打包输入保留在 `D:/cam18pm/branches/lsp_fix_249_import_preview_20261031/release/`。

## 项目上下文

参阅 `HANDOFF_ALLINONE_2074_COMPAT01114_ASTRA_MEDIUM_20261111.md` 及其承接文档了解历史设备证据与停止路线。
这些交接记录中的旧工作区路径用于定位历史材料；本仓库作为后续开发目录。
开发采用 LSP 优先和最小、证据驱动修改。保留原生链路及已有用户改动。
GitHub 推送、Release 和附件上传按用户明确要求执行。
