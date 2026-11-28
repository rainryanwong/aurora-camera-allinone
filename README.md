# Aurora Camera All in One

小米 14 Ultra（aurora）相机与相册适配开发仓库。

## 2.1.15（20116）

适用于小米 14 Ultra（aurora），相机 6.8.001770.0，CameraCompat 0.3.16（10317），实况运镜 0.5.3。完整安装包见 [GitHub Release v2.1.15](https://github.com/rainryanwong/aurora-camera-allinone/releases/tag/v2.1.15)。

修复照片质量选择「超高」后仍按「高」压缩的问题。四颗后摄的普通 1250 万和超清 5000 万均经 JPEG 原片验证。「高」对应质量 96，「超高」对应 100。

## 构建

独立 Compat 的构建入口为 `python build_lsp.py`。使用 JDK 21、Android SDK 37、build-tools 37.0.0 和脚本指定的 NDK。使用自己的签名材料配置 `AURORA_LSP_KEYSTORE`、`AURORA_LSP_KEY_ALIAS`、`AURORA_LSP_KS_PASS`、`AURORA_LSP_KEY_PASS`。

整包入口为 `python package_module330.py`。在 `release/` 目录准备 2.1.14 正式整包及 CameraCompat-0.3.16.apk。打包程序沿用基包的原厂相机及设备载荷，更新 Compat、模块版本、WebUI 说明和文件校验清单。原厂 APK、设备调校及私有签名材料需另行提供。
