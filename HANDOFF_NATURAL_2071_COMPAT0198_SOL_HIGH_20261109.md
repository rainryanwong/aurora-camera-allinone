# 小米14 Ultra 相机与原色集成交接 — All in One 2.0.71 / Compat 0.1.98

日期：2026-11-09，Asia/Shanghai。接手模型：GPT-6.1 Sol，reasoning high。
结构参考：`HANDOFF_NATURAL_2070_SOL_HIGH_20261109.md`。本文件承接其原色研究与停止路线，更新本轮 Compat 功能、验收、整合包和设备基线。

## 当前任务与停止点

用户要求：“参考你刚接手时读的交接文档，再写一份交接文档，随后新开一个6.1 Sol High的会话，准备接手后续任务”。

新会话本轮只读取本交接、用户/项目规则和必要源码，用中文确认当前基线、待办与准备就绪，然后等待下一项指令。不要自行开始手机连接、日志采集、拍摄、安装、重启、构建、改代码或新的研究。

**当前已安装 All in One 2.0.71，内置 Compat 0.1.98；本会话已通过 KernelSU 刷入并完成手机重启。**

- 2.0.71 相对 2.0.70 只更新 `camera-compat.apk`、`module.prop`、`runtime.sha256`。原色六文件、夜景桥、AUTH、启动脚本、stock 1770 相机和其余 APK 均逐字节保留。
- 重启后系统 `sys.boot_completed=1`，模块版本 2.0.71 / versionCode 20071，未见 disable/remove/update 标记。
- 已回读实际安装的 Compat 0.1.98 和 stock 相机 APK 哈希，与交付物一致；实际 `/odm` 下原色六文件及 AIASD 夜景桥共七文件哈希也全部与包内一致。
- 原色、夜景启动日志分别为 `natural_tuning=active`、`natural_night=active`；`vendor.debug.camera.natural_arc_sn=1`；AUTH 挂载日志显示 photo formula ready 和 photo library mounted。
- 本轮 Compat 界面复测在独立 0.1.98 阶段完成。整合包重启后完成版本、APK、原生载荷和启动挂载核对，未再打开相机或补拍。原色十二条新增查询的修复后成片复测仍待安排。

本会话已有相关真机 UI/截图/日志调试、Compat 覆盖安装与重开相机，以及 All in One 刷入和重启的明确授权。用户 AGENTS 的同会话授权沿用规则继续适用。新会话本轮委托仅为准备接手，不把旧会话授权当作自动启动新设备任务的指令。

## 目标与实现原则

- 原色继续采用用户选定的方案：保留相机原本镜头、Sensor、Feature、Scene、HDR/MFNR/SR/夜景等选择逻辑，按明确查询接入作者原色调校。不整体套用生动框架。
- 原色保持 session style2；指定色调查询映射为 `0→76、24→78、36→79、37→80`。色调模块为 CC/CV/Gamma/LTM/TDL。需要核对最终选中对象，effect76 正确本身只是路由证据。
- Sensor 是各物理镜头内部 mode index；Full/ISZ 请求尺寸不直接等于 UI 50MP 开关。
- 严格区分静态选择、登记补齐、实际载荷、算法执行、照片保存与视觉验收。八组高倍率拍摄发生于 2.0.69，不得改写为 2.0.70/71 修复后通过。
- **最小、证据驱动修改，不增加猜测 fallback、无依据防御、宽泛异常掩盖或全仓重构。** 用户要求必须在后续交接继续保留这条决定。
- 优先通过现有 LSP 实现；stock APK 修改需明确例外。本轮新增功能均在 Compat，未修改 stock APK。
- 能独立更新 Compat 时优先交付独立 APK；本轮用户随后明确要求 All in One，才制作 2.0.71。
- 验证在会话中亲自完成，不新增持久验证脚本、断言、报告或构建门禁。一次性探索不接入交付。
- 不恢复暂停的 M3/HDR 深层实验；不删除照片、旧包、恢复材料或证据。原色研究中的经典/生动拍摄回归矩阵此前已被用户取消，本轮摄影风格按钮 UI 复测不代表恢复该矩阵。

## 工作区与规则

- 项目 Cam18：`D:/cam18pm/branches`，projectId `34469847-7c47-44ad-892f-6ed5e1988134`，local，非 Git 项目。
- **实际源码：`D:/cam18pm/branches/lsp_fix_249_import_preview_20261031`，非 Git。每条 shell 命令显式指定该 workdir。** 保留已有用户改动，不把运行 ZIP 当源码。
- 中文沟通，默认 PowerShell，文本 UTF-8。新源码、构建、研究、日志、照片均留 D 盘。
- 遵循用户当前 AGENTS 和 `D:/cam18pm/AGENTS.md`。`notes.md` 仅记录可复用根因与修复，不记录验证绿灯或流水账。
- 本轮已使用 android-project-handoff、reverse-engineer-anything、android-device-evidence、apk-release-verification；后续按任务选择，不机械全套加载。
- REA/原生工具入口：`C:/Users/Ryan/.codex/REA-GHIDRA.md`。APK/JADX、WSL Ubuntu-24.04 Ghidra/JDK/REA 已有；新会话不假定仍有 active target。
- 不新开子任务或同时多代理操作同一手机。新的拍摄任务若涉及用户历史模型偏好，结合其新指令安排；本轮主会话串行 ADB 操作。

## 当前交付物

以下路径均相对实际源码目录。交接时重新计算了四份最新产物哈希。

| 角色 | 文件 | 字节数 | SHA256 |
|---|---|---:|---|
| 最新完整包，已刷入并重启 | `release/camera68_aurora_allinone_v2.0.71_Stock1770_Compat0198_AUTH.zip` | 290397661 | `4c2b6033739522c2c83e8805a416f4fbcb43ac6a300099bb0df443efbb0b8cec` |
| 完整包对应源码 | `release/CameraCompat_2.0.71_source.zip` | 38224971 | `db4db41aeb712fffb24458b5d822132126060ec75aedbce3f1eecda1f0d787b0` |
| 最终独立 Compat | `release/CameraCompat-0.1.98.apk` | 40854517 | `f9fb642eddad6fae1326399bcf282aa42359b8ad8be2c7976e2a2c4c2252c844` |
| 独立 Compat 对应源码 | `release/CameraCompat-0.1.98_source.zip` | 37565882 | `7d0ad287b0a0e1dc5ae3797d90ff9b3da1bcaedccc31d20787f5e7852bc79209` |

All in One versionCode=20071；Compat versionCode=10198。包版本与内部 APK 版本分别记录。

保留的上一完整包：`release/camera68_aurora_allinone_v2.0.70_Stock1770_Compat0195_AUTH.zip`，SHA256 `e9b1a348576fb48352378af948953a669790923d9520388156e344a8a3e6eb21`。
其源码 `release/CameraCompat_2.0.70_source.zip`，SHA256 `64ebcf96477c6ebb9669cdec797aaf15a305c98129ba53f7b4ab0a1c0993e0a2`。2.0.69 及更早包按旧交接保留。

Compat 0.1.96、0.1.97 为本轮中间版本，保留产物，不作为最终功能基线。0.1.96 自动关闭未生效；0.1.97 修好关闭和轮盘，但顺序切换内部值次序随后在 0.1.98 校正。

2.0.71 离线核对：运行包 58 条目，57 项 runtime 哈希全部匹配；无新增/删除运行条目，恰好上述三项变化。完整源码 656 条目，其中独立 Compat 源码的 577 条目逐字节对应，其余 78 条旧来源保留，加上新打包入口。

## 当前源码与构建入口

- `package_module290.py`：**2.0.71 完整包入口**。以 2.0.70 完整包/源码为基线，换入最终 `CameraCompat-0.1.98.apk`，合并其源码归档，保留原生载荷与激活链。
- `build_lsp.py`：当前 VERSION=0.1.98、VERSION_CODE=10198。默认构建当前完整 Compat 源码。使用项目既有签名，不打印或归档凭据。
- 最终构建信息：`release/CameraCompat-0.1.98.json`；构建目录 `build/lsp-3ca16ddd34ab`；实际输入快照 `build/lsp-3ca16ddd34ab/input_snapshot`；顶层构建日志 `build198-0198.log`。
- 签名证书 SHA256：`08e04cdba0d0374cee03318fb4e886fb6b4b4a0365d16ecc5be8fa617721c2dc`。与此前已安装 Compat 一致。最终 APK 签名、16KB zipalign、ZIP、元数据和输入快照对应均已核对。
- 新功能注册在 `src/com/aurora/camera/compat/FeatureRegistry.java`。
- 原色调校生产仍在 `native/natural_capture_profiles.py`、`package_module278.py`、`native/natural_lookup_model.py`、`native/mivi_capture.S`、`native/gs_capture.S` 等入口。
- `package_module289.py` 是 2.0.70 的原色增量入口，基线仍是 2.0.69 / Compat0195。后续做新的原色增量时应从当前 2.0.71 载荷承接，避免重跑旧脚本导致 Compat 回退。
- **不要直接运行旧 278 main 生成完整包**：其 BASE 固定为 2.0.58，早于夜景修复。调用其中通用函数时保留当前全部启动载荷和 canonical active-root SID 约定。
- ZIP 使用 `x` 创建，不覆盖旧产物。本交接独立保存；不要为了加入交接文档重新制作已刷入的源码归档。

## 本轮 Compat 改动及验收

### 摄影风格切换优化

设置位置：相机设置 → 拍照 → 水印机型下方。选项名称为“原生”“选择后关闭面板”“顺序切换”，默认原生。

源码：`src/com/aurora/camera/compat/PhotoStyleSwitchHooks.java`。配置存于相机进程 `aurora_features229` SharedPreferences，key `aurora_photo_style_switch`，0=原生、1=选择后关闭、2=顺序切换。

- 原生：沿用相机操作。
- 选择后关闭：在原生 VisualCheckBox 的触摸/无障碍点击处理后，识别 `top_choice_popup_group`（1770 资源 ID `0x7f0b0b62`），调用 `p5.u$a.a()` 原生关闭入口。
- 普通拍照的实际弹窗属于 `p5.u`，**不是** `CvTypeSecondMenu`；0.1.96 的错误父控件识别是未自动关闭的根因。已写入 `notes.md`。
- 顺序切换走 `X9.i0.X9(View)`、`R6.D.qd(String)` 和原生刷新/互斥处理。原生值是 **0=生动、1=经典、2=原色**，正确循环为 **2→0→1→2**。0.1.98 已修正上一版写反的次序，不能再按错误名称推导。
- 0.1.97 真机已分别确认三种风格及重复点当前选项都能关闭。最终 0.1.98 又确认原色/生动选择自动关闭，并以三次点击观察完成原色→生动→经典→原色循环。

### 自定义快门声的极暗夜景循环

源码：`src/com/aurora/camera/compat/Sound211Hooks.java`。

根因：`E1.g4.p(float,int)` 将事件6设为无限循环；原模块把其 `camera_click_motor_sound.wav` 映射为自定义 `camera_click.wav`，长曝光期间就反复播放快门声。

修复在 `com.android.camera.module.u.playCameraSound(int)` 的原生分发前：仅对普通拍照163/夜景173，且当前选中 hasselblad/laoda/xpan（哈苏、老大、XPAN）时，抑制事件6；完成回调的事件0沿用选中声效。其他事件与原生声效选择链保留。

用户安装 0.1.96 后明确反馈：除了自动关闭面板外，其他均已验证通过。该反馈包含本项；本会话未另做极暗长曝光补拍。

### 暗角着色器、小米 ASD 文案

- `AuthVignette239Hooks.java` 的说明已改为用户原文：`暗角着色器需搭配徕卡经典风格选项使用。“跟随原生”选项，着色器调节仅对自带暗角的滤镜生效，如需使用14 Ultra经典风格+全局暗角自定义调节，请在徕卡经典风格中选择13 Ultra 选项。`
- `classic/AsdSummary241.java` 说明为：`普通拍照AI场景分析，该选项默认为开启状态，仅在拍照模式的“AI相机”选项打开时作用`。已在最终 0.1.98 实验室页面亲自核实完整显示。
- ASD 本轮仅替换描述；`Classic239Hooks` 原有请求 override 实现未改。

### AI 超级长焦开关与分组

实验室分组由“AI帮拍”改名 **“AI选项优化”**。保留原有两个 AI 帮拍选项，再加两个独立开关，默认均关闭：

1. `AI超级长焦三档切换`，key `aurora_sdsr_three_states`。
2. `AI超级长焦20倍启用`，key `aurora_sdsr_zoom20`。

源码：`SuperTelephotoHooks.java`、`Laboratory253Hooks.java`、`ui/NativePreferences240.java`。

- 复用原生 SDSR 的 off/auto/on、图标、持久化、互斥和请求路径。
- 三档开关在原本支持 SDSR（`l9.h.P3(l9.g)`）时覆盖 `Q3`，使原生生成第三档并沿原生 `SDSR_MODE` 请求链处理。
- 20倍开关覆盖 `l9.g.s0()` 为20，统一菜单、检测器及拍摄判断使用的阈值。
- 偏好变化监听刷新已有 `r2.V` 配置与 `s6.p0` 检测器；`r2.V.R` **raw DEX 参数为 Object**，不要按反编译显示的 C 类型错误解析 Hook。
- 用户的 0.1.96“其他均验证通过”反馈覆盖这些功能。本轮没有追加 SDSR 原生算法或照片效果研究。

### 轮盘20倍数字与快捷跳转

源码：`ZoomWheelHooks.java`。在普通Photo163的 `J9.w.U()` 准备原生停靠点前，向 `J9.w.T0` 的既有10/30之间插入20，随后由原生完成数字绘制、角度和点击区域构建。未另画悬浮文字或模拟变焦。

现场原始列表为 `[0.5,1,2,3.2,5,10,30,60,120]`，新增后包含20。已在 0.1.97 与最终 0.1.98 上观察5×轮盘的10、20、30数字；点击20后实际UI为20×，ZoomManager日志为 `setZoomRatio(): 20.0`。

## 本轮证据目录与设备现场

UI、逆向和构建证据：`D:/cam18pm/analysis/compat197-ui-20261109/`。目录名沿用最初197，含最终198证据，不要由目录名认定版本。

- `initial-state.txt`、`style-selection-failed.png`：安装0.1.96和自动关闭失败现场。
- `activation198.txt`：最终独立Compat 0.1.98加载，56组Hook。
- `198-sequence-vivid.png`、`198-sequence-classic.png`、`198-sequence-natural.png`：三次顺序切换。
- `198-vivid-auto-close.png`、`198-natural-auto-close.png`：最终版选择后关闭。
- `198-wheel5-with20.png`、`198-tap20-confirmed.png`、`198-tap20-final-logcat.txt`：20数字与准确到20.0。
- `198-asd-description.png`：新ASD文案。
- `198-final-restored.png`、`198-final-identity.txt`：刷完整包前恢复原色1×、原有顺序切换偏好，APK哈希匹配。

APK静态材料：`D:/cam18pm/analysis/camera-ui-2071-20261109/`。

- `jadx/sources/` 来自当前1770 APK。全量JADX有153项反编译错误；本轮关键方法另通过REA/JADX及raw DEX核对，不能把反编译别名当原始描述符。
- `source-before/` 为本轮最初改动前选中文件副本；0.1.97改动前副本另在上面的UI目录 `source-before/`。
- 原始APK仍为 `D:/cam18pm/analysis/natural-photo-audit-20261108/current-camera.apk`，SHA256 `e25b1ed5581baddd4e1d7f1ae60c1e0f1b1251645df5ba90dfbb9c42953b4a07`。

完整包刷入与重启证据：`D:/cam18pm/analysis/allinone2071-install-20261109/`。

- `ksud-install.txt`：KernelSU成功，四个APK安装、LSP配置、账号检查均完成。
- `before-install.txt`：原2.0.70，旧boot id `6cf7aa9f-3a86-4eab-b009-33198d3363fd`。
- `after-reboot-state.txt`：新2.0.71，boot completed=1，新boot id **`0a3d52fa-5362-4bf0-8430-fa524a7d909f`**，原色/夜景active。
- `after-reboot-apks-auth.txt`：安装后的stock/Compat哈希与AUTH挂载日志。其中有一次 `cmd user is-user-unlocked` 不受本机支持的输出，不用该行推断解锁状态。
- `active-native-sha256.txt`：七个实际挂载文件与包内SHA256一致。
- `post-reboot-screen.png`：最后截取为黑屏；本轮未以它判断相机预览或解锁状态。刷包重启后未再开启相机。
- 设备暂存安装ZIP：`/data/local/tmp/camera68-aio-2071-20261109.zip`，保留未删除。

设备：ADB `D:/DevTools/AndroidSdk/platform-tools/adb.exe`，serial `6ea663f2`，aurora，KernelSU；模块 `/data/adb/modules/camera68_aurora_allinone`。本轮使用 `/data/adb/ksud module install <zip>`，成功后 `adb reboot`。

最终独立Compat复测时 app PID29278，属于**重启前旧boot**；新任务须重新取PID。当前仅承接上面的最后现场，不为写交接重新连接手机。

刷完整包前最后回读的 `aurora_features229`：摄影风格=2（顺序切换）；超级长焦三档=true、20倍=true；滤镜强度=true、视频LUT质量=true；始终开启快门声=false、预览60=false；亮度值3600。

实际挂载载荷（2.0.71重启后已核）：

| 文件 | 实际路径 | SHA256 |
|---|---|---|
| wide | `/odm/lib64/camera/com.qti.tuned.aurora_semco_LYT900_wide.bin` | `e2fb8251ddd9f492a4f97f6b5cf7bb08f6037ea4e52448a56e7aefa38fd558ac` |
| ultra | `/odm/lib64/camera/com.qti.tuned.aurora_semco_imx858_ultra.bin` | `6acea5bc18ccdd97ebd4dcb7a0561c1cd1dcd577c83fc3ee23ca82036d8cca60` |
| tele3 | `/odm/lib64/camera/com.qti.tuned.aurora_ofilm_imx858_tele3x.bin` | `0669bc0733e8a664bf8b3d710a8e1b7ebc521ac94a12490c446f8c23fd3f469c` |
| tele5 | `/odm/lib64/camera/com.qti.tuned.aurora_semco_imx858_tele5x.bin` | `04c14655cc168f503c6ac8a4c1689fd78892f0b2273a0eb0973bfd22ab625fca` |
| GS | `/odm/lib64/com.qti.feature2.gs.sm8650.so` | `6575c87bedc497e0eb5b620f06cb267758670fb2ebfafd00187dbcb3e16397b6` |
| MIVI | `/odm/lib64/camera/plugins/com.xiaomi.plugin.offlinepostproc.so` | `154b1a1be34a78ef886c1d3d12d5878b0172ad72283f8a6883d76fb937635ea7` |
| 夜景AIASD | `/odm/lib64/camera/components/com.mi.node.aiasd.so` | `30826d7aa3c620305cbf1468e01ba7d783c8e307b30a5a17e4f3cac0440c65a6` |

## 原色历史根因与必须保留的修复

详细证据、八组拍摄表和地址仍见旧交接 `HANDOFF_NATURAL_2070_SOL_HIGH_20261109.md` 对应章节。这里只保留继续工作直接需要的结论。

- 2.0.59启动失败：重复私有注册给相同数据分配独立source SID，造成provider CV/TDL加载时Scudo约6.5万maps/4GB RSS耗尽。2.0.60起同名称/offset/size的登记必须复用**已实际加载canonical active-root SID**，不能引用inactive作者对象冒充已加载。
- 2.0.61—67已扩普通HDR/MFHDR、超广RawHDR曝光帧、SR/HDRSR effect36、人脸Scene32、普通闪光44/91等明确查询。不要从已列绑定推断所有拍摄模式已穷尽。
- 2.0.68夜景：SND仅支持input0/1，session style2直传导致返回2、checker帧数0。桥只在指定后摄Photo/Night调用把SND输入2映射0，保持session style2及原触发选择。Photo163/op8001或9002、Night173/op800a，后摄role0..3。已于2.0.68实拍普通自动夜景7RAW、专用夜景9RAW并保存，证据 `D:/cam18pm/analysis/natural-night-2067-20261108/`。
- 2.0.69六条普通Sensor人脸gain：wide Sensor8、tele3 Sensor1、tele5 Sensor1各51/105、51/106，Scene32/source0→76。证据 `natural-remaining-2068-20261108/` 与 `natural-gain32-2069-20261108/`。
- 2.0.70十二条数据登记：wide Sensor4与ultra Sensor1各36/13、51/109四条MFNR；tele5 Sensor12/13/16/17各51/105、51/106八条人脸gain。主摄MFNR两项另补source0→76精确路由，超广只补登记；原Sensor/Scene/算法选择不改。
- MFNR的36/52与下游36/13是不同阶段；MFSR会按stage重写Feature2。缓存命中无新GetModule不等于未消费模块。HDRSR中随后被24/31覆盖的24/62不盲补。
- 八张8×/10×、3:4/16:9、RawSR/HDRSR实拍均发生于**2.0.69**，证据 `D:/cam18pm/analysis/natural-full-isz-2070-20261109/`，主图五类匹配作者，gain缺CC/CV/TDL的现场用于产生2.0.70修复。
- 2.0.70的离线核对为50866条绑定/254330项作者五类选择；原50854条/254270项保持。原始完整比较数量与八张逐项关联仍在旧交接，不能改写成2.0.71成片验收。

## 仍需推进的原色范围

1. **十二条新增查询修复后的定点复测**：当前2.0.71已确认实际载荷，后续任务按设备当时状态核身份后验证MFNR四项与高倍率八项。无需重扫全部倍率。此轮没有新增照片或原生探针。
2. **Sensor52保持不补**，继续收敛QCOM内部AutoHDR/PZSL提前配置旁路。
   - 正常Full请求RTMCX5a7b0→5d0d4 Base→override FindBest4456f0，5c9ac要求QCFA+QNo，现场拒52、接受53。
   - 剩余5b27c/5b2f4→5b910需physical HDR mode1、QCFA PZSL config1、hint bit45、ISZ!=2。hint来源为GS feature `isPseudoZSLSnapshot` bit18→capture bit13→GetFeatureHint的Hint+4 bit13。
   - SetDeviceHDRMode3ca988常规QCFA PZSL清mode0；3ca738提前路径可能绕过。H=HDRInfo+18，S=HDRInfo+1c，A=*(Extension+3f8)=StaticSettings+10548，U=Extension+5fcc。
   - SetAutoHDRCapability3c7f40读取QCOM EnableAutoHDR+isAutoHDRSupported；ConfigureHDRInformation3a1a30在A开启/session HDR vector为空时也能置S。不能只凭UI HDR或APK字符串排除此旁路。
   - 定点方案：3ca738读X23 H/S、session vector+20/+28、preferred+38/+40及X20 A/U；3cac00读X19 HDRDeviceInfo+20最终mode；RTMCX5b27c/5b910核同请求门槛。
   - 现有 `natural-remaining-2068-20261108/I_main15_hdr_pzsl.trace.txt` 为mode0、cfg1、hint0x82021、ISZ1，最终53。
3. **NewStyle普通Desired caps=0例外**：专用Full/ISZ SR的NewStyle变体已能排除，但不能扩写成所有物理mode全局排除。
   - override CheckNewStyleType4a3ee0读原session style2，GetMiDesireSensorMode4a6774取value&1置bit46；RTMCX GetNewStyleMode均cmp==1；selector444240/444270比较NewStyle能力位。tele5 Full14/15、ISZ18/19属专用NewStyle，12/13、16/17属普通原色。
   - IsMatchingMiCapability443fdc→4446ac在Desired caps0时仅检查candidate Normal位。带Normal的NewStyle候选：wide29/30/31/33/36/37/39/45/46/48，tele3 6/7，tele5/ultra 8/9。后续按尺寸/fps/排序及实际选择收敛。
4. 其他模式仍按实际问题逐项区分。普通Photo配置未启用融合拍照能力；专用人像/景深、UltraRaw不并入普通Photo清单。超广Photo SR阈值0、角色stream无phgType2/5，不盲加105/106；其RawHDR36/85/86及gain107人脸五类已在此前近距离拍摄匹配。

5× Full阈值7.5–9.99999、ISZ10–120，Lux65536、ShortGain16，仍受月亮Scene35等状态影响。Sensor12/13/16/17的八个人脸组合已经有2.0.69实际证据，不再作为“尚无可达性证据”的项目。

## 继续研究的输入与操作提示

- 作者四tuned：`D:/cam18pm/analysis/natural-tuning-v140-20261013/upstream-ksu/payload/`。
- camera.xiaomi.so、override、QCOM库：`D:/cam18pm/analysis/natural-tuning-v140-20261013/`；override SHA `43eb3c1ce976df4176c0579c753b456de7b8ea8a3334aeefbc071fdcea95fe15`；camera.xiaomi SHA `a58e66587c804fe572cfcb8da1f5ecba4f19afbeb039a6cb049cb217bff53ae3`。
- 原厂配置/sensormodule：`D:/cam18pm/analysis/natural-photo-audit-20261108/device/odm/`。
- Generic：`D:/cam18pm/analysis/natural224-owner-map-20261029/device/files/odm/lib64/com.qti.feature2.generic.so`，SHA `c17d7649a36545022f48193f7b654ab40083a38520bc2f0aa07c4fa8f076ba7c`。
- MFSR：`D:/cam18pm/analysis/natural-photo-audit-20261108/device/odm/lib64/com.qti.feature2.mfsr.so`，SHA `d2f8b9d6c328e6b625d70c54ace693217635c7d6c8fe6ad7a0f4aadd75055636`。
- RTMCX：`D:/cam18pm/analysis/natural-sensor52-selector-20261108/com.qti.feature2.rtmcx.so`，SHA `3a911f350d724ebb1b5d27b62d9f42fddd3a6b000263d7e5b41d8ca328b92446`。
- capability raw enum：38=CMS、39=DCG、41=QNo；NewStyle raw36→driver bit46。
- 原会话21个 `fullisz_1109a` 及 `gain32_1108a/b` 探针已撤除；本轮未建新探针。不要引用旧PID或把旧缓存行当本次请求。
- ADB root复合命令使用Python argv：`[adb,'-s',serial,'shell','su','-c',shlex.quote(command)]`；中文输出设置UTF-8，读取输出显式编码。
- 二进制用adb pull或exec-out，不用adb shell传二进制。截图物理1440×3200；坐标仅按已观察UI使用。轮盘会自动收起，截图后再点击旧坐标可能命中底部倍率按钮，测试时按实际显示时序操作。

## 接手第一步

读本文件和规则，必要时检查 `package_module290.py`、`PhotoStyleSwitchHooks.java`、`ZoomWheelHooks.java` 或当前原色profiles。向用户确认：All in One2.0.71/Compat0.1.98已安装，重启后七份载荷匹配；本轮UI功能已按上述范围复测；原色新增查询成片、Sensor52和普通caps0 NewStyle仍为后续事项。随后等待用户指令，不自行开展待办。
