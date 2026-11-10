# Aurora Camera All in One

小米 14 Ultra（aurora）相机与相册适配开发仓库。

## 开发基线

- All in One：2.0.74
- CameraCompat：0.1.114 / versionCode 10214
- 相机：6.8.001770.0
- Git 基线标签：`v2.0.74`

初始源码来自固定交付包 `CameraCompat_2.0.74_source.zip`，SHA256：
`9e826c5fb4f4b273113088566b36e6bc14acbd56bcba4ecfe59389245266afd9`。
归档中的 668 项文件保持原样，包含 Compat、资源、原生依赖及历史整合脚本。

## 构建与打包

独立 Compat 构建入口：`python build_lsp.py`。
现有构建使用 Python、JDK 21、Android SDK build-tools 37.0.0 和 android-37.0；工具路径在 `build_lsp.py` 中配置。
签名沿用外部密钥，通过 `AURORA_LSP_KEYSTORE`、`AURORA_LSP_KEY_ALIAS`、`AURORA_LSP_KS_PASS`、`AURORA_LSP_KEY_PASS` 配置。密钥和密码由本机私下提供。

2.0.74 完整包入口：`python package_module293.py`，需要在 `release/` 放置：

- `camera68_aurora_allinone_v2.0.73_Stock1770_Compat01110_AUTH.zip`
- `CameraCompat_2.0.73_source.zip`
- `CameraCompat-0.1.114.apk`
- `CameraCompat-0.1.114_source.zip`

构建输出位于 `build/` 和 `release/`。这两个目录不进入 Git。
本机原始交付与上述打包输入保留在 `D:/cam18pm/branches/lsp_fix_249_import_preview_20261031/release/`。

## 项目上下文

参阅 `HANDOFF_ALLINONE_2074_COMPAT01114_ASTRA_MEDIUM_20261111.md` 及其承接文档了解功能、历史设备证据与停止路线。
这些交接记录中的旧工作区路径用于定位历史材料；本仓库作为后续开发目录。
开发采用 LSP 优先和最小、证据驱动修改。保留原生链路及已有用户改动。
