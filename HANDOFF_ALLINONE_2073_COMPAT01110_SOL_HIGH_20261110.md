# 小米14 Ultra 相机与相册集成交接 — All in One 2.0.73 / Compat 0.1.110

日期：2026-11-10，Asia/Shanghai。后续接手模型：用户指定 GPT-6.1 Sol，reasoning high。
结构及原色历史承接 `HANDOFF_NATURAL_2071_COMPAT0198_SOL_HIGH_20261109.md`；原色深层证据继续参考 `HANDOFF_NATURAL_2070_SOL_HIGH_20261109.md`。本文件更新本会话新增的相册适配、摄影风格跨模式行为，以及最终整合包与设备现场。

## 当前任务与停止点

用户本轮要求：制作 All in One，刷入并重启；参照旧交接写新交接；随后新开一个 6.1 Sol High 会话，准备接手后续任务。

**All in One 2.0.73 已通过 KernelSU 刷入并重启，内置 Compat 0.1.110。**

- 运行包以 2.0.72 为基线，只变更 `camera-compat.apk`、`module.prop`、`runtime.sha256`。58 个条目没有增减，57 项运行文件哈希全部匹配。
- 手机模块 version=2.0.73 / versionCode=20073；Compat versionCode=10210。回读实际 Compat、stock1770 相机及相册编辑 APK 哈希均匹配。
- 新 boot ID 为 `fa5690ee-78c3-404a-a02a-309b1588bb75`，`sys.boot_completed=1`，`sys.user.0.ce_available=true`；未发现模块 disable/remove/update 标记及待合并的同名 modules_update。
- 七份实际 `/odm` 挂载载荷与最终包逐项一致；原色、夜景日志分别为 `natural_tuning=active`、`natural_night=active`，`vendor.debug.camera.natural_arc_sn=1`。AUTH 日志有 photo formula ready、photo library mounted、formula service ready，启动结果 palette/m3/policy/account 均为0。
- 重启后相机进程 PID21007 打印 `Compat 0.1.110 installed groups=56`，stock SHA 匹配。此 PID 和 boot ID 仅用于归属本轮证据，后续操作重新读取。
- 摄影风格五模式 UI 验证在独立0.1.110阶段由 Terra High 完成。整合后本轮完成版本、挂载、启动和 Hook 加载核对，没有新增拍照或图库云生成任务。

**新会话当前只需读取本交接、当前用户/项目规则和必要源码，中文确认基线与准备就绪，然后等待用户下一条任务。不要自行开展待办或操作手机。** 当前会话的设备操作授权不自动跨入新会话；用户后续的具体请求按其范围执行，不增加例行重复确认。

## 目标与实现原则

- 最小、证据驱动修改；不增加猜测 fallback、无依据防御、宽泛异常掩盖或无关重构。继续交接时保留这条决定。
- 优先通过 LSP 实现；改 stock APK 需要用户明确例外。可独立更新时优先交付独立 Compat；用户要求完整包时再制作 All in One。
- 验证由会话内读代码、命令、日志和实际运行完成；不为本任务新增持久验证脚本、断言、报告或构建门禁。一次性探索不进入交付。
- 中文交流，默认 PowerShell，文本 UTF-8，研究/构建/日志/照片留 D 盘。保留用户改动、原图、旧包与恢复材料。
- 设备控制只保留一个操作者；用户指定的相机/相册 UI 复测模型为 GPT-6.1 Terra High。是否委派静态分析按当前用户 AGENTS，不沿用旧文档中更窄的通用代理限制。
- 区分入口、实际 Hook 加载、算法执行、保存成片与用户验收。旧场景的结果不扩写成新场景通过。
- 原色仍保留原生镜头、Sensor、Scene、HDR/MFNR/SR/夜景选择；只在有证据的查询中接作者五类色调，不整体套用生动框架。session style2 保持。
- 不自动恢复已暂停的 M3/HDR 深层研究、经典/生动拍摄大矩阵、Sensor52或QNN移植；后续以用户新任务为准。

## 工作区与当前入口

- 项目 Cam18：`D:/cam18pm/branches`，projectId `34469847-7c47-44ad-892f-6ed5e1988134`，host=local，非 Git。
- 实际源码根：`D:/cam18pm/branches/lsp_fix_249_import_preview_20261031`。每条 shell 命令显式指定此 workdir，不把运行 ZIP 当源码，不虚构 Git 分支或 HEAD。
- 规则：用户当前 AGENTS、`D:/cam18pm/AGENTS.md`；`notes.md` 保存可复用根因与修复。
- `build_lsp.py`：VERSION=0.1.110 / VERSION_CODE=10210。实际输入快照 `build/lsp-e02612b3ef80/input_snapshot`，构建信息 `release/CameraCompat-0.1.110.json`，日志 `build297-01110.log`。
- `package_module292.py`：2.0.73 当前完整包入口，承接2.0.72的运行包/源码并换入最终0.1.110。不要运行旧278的 main重新生成完整包，它固定承接2.0.58，会回退后来载荷。
- 使用既有签名入口，凭据不打印、不归档；证书SHA256 `08e04cdba0d0374cee03318fb4e886fb6b4b4a0365d16ecc5be8fa617721c2dc`。
- 源码归档已固定，交接文档外置保存；不要为了把本交接塞入源码 ZIP而重打已经刷入的产物。

## 当前交付物与回退材料

以下路径相对源码根。当前完整源码667条目，含独立Compat源码586条目、原整合源码其余80条目及新打包入口。各自字节已核对。

| 角色 | 文件 | 字节数 | SHA256 |
|---|---|---:|---|
| 已刷入并重启的完整包 | `release/camera68_aurora_allinone_v2.0.73_Stock1770_Compat01110_AUTH.zip` | 290584562 | `35b2d677eac4fea410a3d09df1535009e6343b13badeedf7ddb3a4231b921fd1` |
| 当前完整源码 | `release/CameraCompat_2.0.73_source.zip` | 38417194 | `288c115888434509531fb9ec0efcecff4a7f957e128c01b5e9d16bcd84d15f7f` |
| 最终独立 Compat | `release/CameraCompat-0.1.110.apk` | 41989337 | `30b405d7fc6ea097295e84056f68b3cd27e76c89a61154d9539643c2896f20ca` |
| 独立源码 | `release/CameraCompat-0.1.110_source.zip` | 37768915 | `9a5def70f1bab1f790a7372d532f4878fdc9c4bebc37d3c8b56e382440c01fdb` |
| 上一完整包 | `release/camera68_aurora_allinone_v2.0.72_Stock1770_Compat01109_AUTH.zip` | 290584727 | `d4f1c05129bbe268eb39f41f5d4aabde9ed5b223c9dbf452bbd1d54372529962` |
| 上一完整源码 | `release/CameraCompat_2.0.72_source.zip` | 38415938 | `fb120601a7ed00db2c2e2f054509056490218c83ac1e7cd3ab5da79b966d706c` |

2.0.72/Compat0.1.109 阶段，用户对 AI 去拖影、AI证件照反馈“已实测可用，收尾交付包即可”。原2.0.71及更早回退包按旧交接保留。

## 摄影风格跨模式行为

源码：`src/com/aurora/camera/compat/PhotoStyleSwitchHooks.java`，注册于 `FeatureRegistry.java`。

设置仍为相机拍照设置中的“摄影风格切换优化”：0原生、1选择后关闭面板、2顺序切换；偏好为相机 `aurora_features229.xml` 的 `aurora_photo_style_switch`。

原代码在关闭与顺序切换两处限定mode163。0.1.110改为读取 `X9.i0.k` 的活动模式，使用原生 `r2.m.m(currentMode)` 计算下一项；清互斥、重新打开及原生 `R6.D.qd` 事务均针对当前模式。`getDisableReasonString(Context)` 保留 RAW/UltraRAW等原生限制；自动关闭仍仅识别真实 `top_choice_popup_group` 中 enabled 的选项，并调用原生 `p5.u$a.a()`，没有固定延时或模拟空白点击。

Terra High 在本会话已实测：

| 模式 | 选择后关闭（包含再点当前项） | 三次顺序切换闭环 |
|---|---|---|
| 普通拍照 | 完成 | 2→0→1→2 |
| 专业JPEG | 完成 | 1→2→0→1 |
| 普通街拍 | 完成 | 1→2→0→1 |
| 夜景 | 完成 | 2→0→1→2 |
| AI帮拍 | 完成 | 2→0→1→2 |

0=生动、1=经典、2=原色。相机实际保存值已随点击变化；当前人像没有该入口，未强加。专业RAW/UltraRAW时点击走原生面板且值保持1，随后恢复JPEG。复测后恢复普通拍照原色、顺序切换和原街拍UI。本轮重启后又读到 `aurora_photo_style_switch=2`、SDSR三档和20倍两个偏好均为true。

源码改动前副本：`D:/cam18pm/analysis/photo-style-modes-20261110/source-before/`。本轮合包后直接证据见下文2073安装目录；上述逐模式结果来自本会话 Terra High 的真机回报。

## 相册编辑 2.11 适配与当前取舍

目标相册编辑：`D:/Camera Files/小米相册-编辑-2.11.10.0.3(211991003).apk`，SHA256 `451a1fc0aec7565e86936e729176d77ccbc459aef7faa2bd7c567b88264a8f49`。ASCII引用副本 `D:/cam18pm/analysis/editor211991003-20261109/editor211991003.apk`。

### 动态契约、水印和滤镜

- 旧2.4契约 `assets/editor250/contract.tsv` 保留；新2.11使用 `assets/editor291/contract.tsv` 及 `aliases.tsv`。按结构指纹、调用边、字段/构造角色定位，不把JADX别名直接当raw DEX名称。
- 新版重排分类/滤镜构造参数、增加styleParams，并混淆ViewModel/云对象/部分AndroidX和miuix。当前实际DEX与包含新增字段类改名的副本，214项声明均已解析；新增Candy接口也在旧版中核对。
- `Editor245Hooks.java`、`EditorMore246Hooks.java`、`editor/EditorNativeCloud291.java`、`editor/EditorWatermarkEnvelope292.java`、`editor/EditorLeicaFilters292.java` 为主要实现。
- 新原生配置库检测XposedBridge/liblspd后，两个状态字节会让水印查询或云接口返回空。`native/editor291/environment.c` 在实际加载ELF中按指令关系定位并恢复这两个状态，再走原生解析/筛选/渲染；没有使用固定地址或替换空结果。桥库为 `libaurora_editor.so`。
- 新版徕卡水印的机型清单在原生过滤前处理；外层是Base64 protobuf封装，内部UTF-8末字节XOR，按原seed重新封装。旧版通过实际配置对象处理。旧/新版云徕卡滤镜的 `lcc_support_devices` 限制均有对应处理。
- 用户确认水印和特殊样式可用；本会话真机完成新增水印111/93/2和CHR滤镜的预览/保存。滤镜管理顺序、启用条目与更多分组沿既有动态同步保留；不要把资源CDN偶发超时归入机型或LSP检测。

### 调色盘官方管线

源码：`editor/more246/EditorRecipe248.java`、`StyleGpu248.java`、`EditorMore246Hooks.java`。

官方 `CandyAlgorithm` 只在选定滤镜时配置SDK/LUT，连续拖动使用updatePipeline + processBitmap。JNI只处理RGBA原始字节，AHB上传/输入shader/下载均不为Bitmap路径转换色域。当前实现移除额外逐帧Canvas P3往返，保留宿主色彩空间/gainmap；持久复用SDK、LUT和effect chain，结构或配方/LUT身份变化才重建。无自定义空间shader且强度100时合并滤镜、后置效果和调色盘；中间强度先混合配方再调盘，0强度可独立调盘。空间shader输出Bitmap沿源颜色空间。

同图/同手势、理光Positive、1440×1920预览：0.1.106的16次完整处理为146.93–235.76ms、中位160.67ms；0.1.108的39次为12.81–16.76ms、中位14.18ms。这是处理耗时。100/50/0三档均另存3072×4096，原图哈希不变；原生徕卡与More换滤镜/参数重置已核对。现成样本的algos均为0，自定义空间shader保存分支本轮没有真机样本。

证据：`D:/cam18pm/analysis/editor-palette-official-device-20261109/`，含 `more106-recipe.log`、`more108-recipe.log`、录屏及130555/130804/130919成片。原生库分析：`D:/cam18pm/analysis/editor-palette-latency-20261109/libMiFilterSDK.so`，SHA256 `4d66aaa632c223abd190672a8dcfea4b98882260e8d3a24a8d974b66537904d0`。

### AI功能状态

`editor/EditorFeatureDevices293.java` 定向给各自设备名单加入Build.DEVICE，保留地区、系统、登录和后端条件。

- 调色盘 `bt.a.d`；图生视频 `at.a.a`；AI修图助手 `at.b.a`。
- AI去拖影 `at.k.a / at.k.b`：国内/国际两套机型名单同时处理，目录及模型注册共用该判断。
- 新AI证件照 `at.j.a`：本地机型名单放开。用户自行实测后确认可用；Terra只核入口，未把普通测试图当人像完成生成。
- **AI构图 `at.d` 的扩展已按用户要求撤销，保持原生机型判定。不要自动重新开放。**
- 图生视频只核过入口、四种运镜和默认3秒，未提交云生成。AI助手仍有系统code≥4及实际小爱CliCommandService依赖；本机系统code3，本会话此前小爱版本缺该服务，仅改机型名单不能补齐。
- 画质超清本机已原生支持；此前读到API36、相册4.3.1.20/versionCode4030120、系统code3，满足新版增强分支的本地条件，未增加它的入口放行。

AI入口/条件研究目录：`D:/cam18pm/analysis/editor-ai-features-20261109/`、`editor-ai-unlock296-20261109/`、`editor-ai-unlock109-device-20261109/`。真实去拖影走云端create/getResult；证件照涉及本地FaceUnity检测、联网及云端人像/服装任务，不能把入口显示替代完整执行证据。

### AI构图QNN研究停止点

`D:/cam18pm/analysis/editor-qnn-rom-20261109/` 保留ROM选定库和模型材料。本地完整ROM为 madrid OS4.0.11 Android17：`D:/360极速浏览器X下载/madrid_images_OS4.0.11.0.XEOCNXM_20260925.0000.00_17.0_cn_fdc4296bdb.tgz`。

ROM有vendor QNN2.47.5/V85与system_ext LiteRT QNN2.39/V75两套。实际构图MINN头为qnn.v2.47.0，Mage走V2470的序列化context反序列化；模型内后端元数据有85标记。2.39 Host过不了当前executor的provider/版本检查；当前ROM没有“现有2.47模型+aurora V75”的现成整套组合。Prepare库不属于此context路径。没有移植系统DSP驱动/固件，也未把跨版本Host/Stub/Skel混装。后续若用户再要求，需匹配V75的模型及运行组；不要改版本头或强选低版本executor冒充兼容。

## 找机位研究结论与边界

用户只要求研究，没有要求实现或开放。已按相近文案、布局、调用链做1770/2230功能级对照。

- 当前1770的两种More界面没有附近拍摄点推荐入口；类似ScenicSpotsHandler按GPS矩形匹配输出景点打卡水印，未连More页或机位推荐App。
- 2230包：`D:/Camera Files/相机-6.8.002230.0(680022300).apk`，SHA256 `155d9012a46c980510802fcba88924a5772af228e8e6553633efde54fee49778`。
- 2230两种More布局均含 `find_best_photos_layout`，公共T4.g绑定。面板U4.g/f5y，Tab U4.i/f5z；Tab页在网格下方或页面底部显示同一pill，不是新Tab。编辑模式变淡至约0.3并停用点击。
- 显示gate v.C0()=Common.p3() + Pe.b.J() + moreModeFindBestPhotos。此版原生p3放行hongkong/madrid；J要求ro.product.mod_device不含_global；动态Boolean缺省true，显式false隐藏。
- 显示gate不检查账号、GPS权限、联网、是否首次进入、是否安装外部App。点击时若锁屏先请求解锁；能解析目标Activity则打开 `com.xiaomi.bestphotos/com.xiaomi.bestphotos.feature.home.HomeActivity`，extra enter_from=CAMERA_ENTRY，否则去小米市场。
- 当前1770若增加该功能，需要移植UI/点击逻辑并核对外部App，不存在一套同功能的隐藏入口可只改条件放出。本轮没有这项代码改动。

研究源码：`D:/cam18pm/analysis/stock-1770-vs-2230-20261023/decoded-2230/` 与 `decoded-1770-res/`；当前1770 JADX：`D:/cam18pm/analysis/camera-ui-2071-20261109/jadx/`。关键2230 smali为 `com/android/camera/data/data/v.smali` 的C0、`T4/g.smali` 的initView/Gs/zs、`U4/i.smali` 的布局和编辑状态处理。

## 设备现场及本次刷包证据

设备aurora，小米14Ultra，ADB `D:/DevTools/AndroidSdk/platform-tools/adb.exe`，serial `6ea663f2`，KernelSU；模块 `/data/adb/modules/camera68_aurora_allinone`。

证据目录：`D:/cam18pm/analysis/allinone2073-install-20261110/`。

- `before-install.txt`：2.0.72、Compat0.1.110及三个APK哈希；旧boot ID `1f50142b-03cc-4015-a897-22a91b1429e4`。
- `adb-push.txt`、`staged-zip-sha256.txt`：设备暂存包身份匹配。
- `ksud-install.txt`：`/data/adb/ksud module install /data/local/tmp/camera68-aio-2073-20261110.zip` 返回0及 Module installed successfully。
- `after-reboot-state.txt`：新boot ID、boot/CE完成、模块版本、标记检查、实际APK哈希和启动结果。
- `active-native-sha256.txt`：实际挂载七文件哈希。
- `activation-logs.txt`：原色/夜景active、AUTH挂载和服务ready。
- `compat-after-reboot-logcat.txt`：PID21007，Compat0.1.110加载56组。
- `preferences-after-reboot.txt`：摄影风格优化2、SDSR三档true、20倍true。

实际相机仍为6.8.001770.0/versionCode680017700，SHA256 `e25b1ed5581baddd4e1d7f1ae60c1e0f1b1251645df5ba90dfbb9c42953b4a07`；Editor仍为211991003及上述目标哈希。设备暂存ZIP保留，不删除原图或旧材料。

| 实际挂载文件 | SHA256 |
|---|---|
| LYT900 wide | `e2fb8251ddd9f492a4f97f6b5cf7bb08f6037ea4e52448a56e7aefa38fd558ac` |
| imx858 ultra | `6acea5bc18ccdd97ebd4dcb7a0561c1cd1dcd577c83fc3ee23ca82036d8cca60` |
| tele3x | `0669bc0733e8a664bf8b3d710a8e1b7ebc521ac94a12490c446f8c23fd3f469c` |
| tele5x | `04c14655cc168f503c6ac8a4c1689fd78892f0b2273a0eb0973bfd22ab625fca` |
| GS | `6575c87bedc497e0eb5b620f06cb267758670fb2ebfafd00187dbcb3e16397b6` |
| MIVI | `154b1a1be34a78ef886c1d3d12d5878b0172ad72283f8a6883d76fb937635ea7` |
| AIASD夜景桥 | `30826d7aa3c620305cbf1468e01ba7d783c8e307b30a5a17e4f3cac0440c65a6` |

## 保留的原色历史与后续范围

本轮没有改原色、MIVI/GS、夜景或AUTH载荷，继续承接2.0.70/71的以下事实：

- 私有调校登记必须复用已加载canonical active-root SID；重复独立SID曾导致Scudo maps/RSS耗尽。路由effect76本身不能代替CC/CV/Gamma/LTM/TDL对象匹配。
- 2.0.68 SND桥只在既定后摄Photo/Night调用把输入style2映射0，session style2和原触发不变；Photo163/op8001或9002，Night173/op800a，后摄role0..3。
- 2.0.70新增十二条：wide Sensor4与ultra Sensor1各36/13、51/109四项MFNR；tele5 Sensor12/13/16/17各51/105、51/106八项人脸gain。那八张8×/10×照片来自2.0.69，不能改写成后续修复后的成片验证。
- 这十二条的修复后定点成片复测仍留作后续任务；不用重扫全部倍率。
- Sensor52仍不盲补，继续研究QCOM AutoHDR/PZSL提前配置旁路；普通Desired caps0下带Normal位的NewStyle候选仍需按实际选择收敛，不能从专用Full/ISZ路径排除扩写为全局排除。
- 普通Photo融合、人像/景深、UltraRAW以及超广SR边界保持旧交接定义。M3/HDR深层与经典/生动大矩阵保持停止路线。

上述原色根因、输入路径、地址和待办细项以2071/2070交接的相应章节为历史参考；新任务先核对实际身份后再使用，不复用旧PID或旧缓存行。

## 接手第一步

读本文件和当前规则；必要时核对 `package_module292.py`、`PhotoStyleSwitchHooks.java`、`EditorFeatureDevices293.java`、`EditorRecipe248.java`。向用户确认：2.0.73/0.1.110已刷入重启，七份载荷匹配、56组相机Hook加载；摄影风格五模式和相册功能按上述范围有证据，AI构图撤销、找机位仅研究。随后等待下一项用户指令，不自行恢复任何待办。
