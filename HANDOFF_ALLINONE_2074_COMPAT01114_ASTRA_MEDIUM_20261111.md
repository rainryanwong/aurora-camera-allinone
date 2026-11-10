# 小米14 Ultra 相机与相册集成交接 — All in One 2.0.74 / Compat 0.1.114

日期：2026-11-11，Asia/Shanghai。后续接手模型：用户指定 GPT-6 Astra，reasoning medium。

本文件参照接手时的 `HANDOFF_ALLINONE_2073_COMPAT01110_SOL_HIGH_20261110.md`，并承接最新 `HANDOFF_ALLINONE_2074_COMPAT01114_SOL_XHIGH_20261110.md`。摄影风格、相册适配与既有功能的完整依据保留在2073交接；原色历史继续承接 `HANDOFF_NATURAL_2071_COMPAT0198_SOL_HIGH_20261109.md`、`HANDOFF_NATURAL_2070_SOL_HIGH_20261109.md`。SDSR倍率调整、扩展镜头合并及18 Pro Max算法对照均沿用11月10日已完成的工作与证据。

## 当前任务与停止点

用户最新决定：以 All in One 2.0.74 / Compat 0.1.114 建立 GitHub 开发基线，后续开发统一从下述 Git 仓库继续。接手准备完成后，按用户后续具体任务开展工作。

**当前本地基线为 All in One 2.0.74 / Compat 0.1.114。** 2026-11-11已重新核对完整运行包、完整源码包、独立APK和独立源码包：四份文件的字节数及SHA256均与下表一致；独立源码归档中的586项在工作区均存在且字节一致，当前 `package_module293.py` 也与完整源码归档对应。随后已建立私有 GitHub 仓库并发布 v2.0.74 Release；本次更新同步开发入口与下载位置。

### 最近设备现场（2026-11-10记录）

11月10日已通过KernelSU刷入All in One2.0.74并重启，内置Compat0.1.114。以下设备信息来自当日留存的刷入、启动与日志记录；本次整理没有重新连接设备采集：

- 完整运行包承接2.0.73，只更新 `camera-compat.apk`、`module.prop`、`runtime.sha256`。58个条目无增减，57项运行文件校验均匹配。
- 模块version=2.0.74 / versionCode=20074；Compat versionCode=10214，实际安装APK的SHA256与最终包内APK一致。
- 新boot ID为 `6a3fb84a-37b2-42ec-91b1-4c6dccbaf0c2`，`sys.boot_completed=1`、`sys.user.0.ce_available=true`。旧boot ID为 `24a2e1cf-c9cc-47d7-b6e7-9e06b4e215a7`。
- 模块disable/remove/update标记及同名modules_update目录均不存在。
- 七份实际 `/odm` 挂载载荷与最终ZIP逐项匹配；原色 `natural_tuning=active`、夜景 `natural_night=active`，`vendor.debug.camera.natural_arc_sn=1`。
- AUTH有photo formula ready、photo library mounted、formula service ready；启动结果palette/m3/policy/account均为0。
- 相机PID18854加载56组Hook，包含 `super-telephoto-options`。当日最终刷包后仅启动相机核对加载，没有新增实拍。
- 最后读回设置：摄影风格优化=2；SDSR三档=true；10×选项=true；合并后的扩展镜头支持=false。接手准备阶段保持该记录，不自行重新开启扩展功能。

### 新会话范围

**新会话仅做本地只读准备：读取当前用户/项目规则和本文件，确认实际工作目录、当前交付基线、证据范围及停止点，中文简短确认准备就绪，然后等待用户下一条任务。** 不连接或操作手机，不启动应用、采集日志、安装/重启、构建、修改文件、清理材料、恢复研究或再开子任务/新会话。历史设备授权不作为接手准备自动开始设备操作的指令。

## 目标与实现原则

- 中文交流，默认PowerShell，文本UTF-8。源码、构建、日志、照片和新材料均留D盘。
- LSP优先；改stock APK需要用户明确例外。能独立更新时交付独立Compat，用户明确要求完整包时再制作All in One。
- 最小、证据驱动修改；不增加猜测fallback、无依据防御、宽泛异常掩盖或无关重构。继续交接时保留此决定。
- 验证由会话内亲自读代码、日志、命令和实际运行完成，不新增持久验证脚本、断言、报告或构建门禁。
- 保留用户改动、原图、旧包和恢复材料。设备只保留一个操作者。
- 用户明确指定真机UI/拍摄测试使用 **GPT-6.1 Terra High子智能体**。SDSR适配后段用户要求“安装好即可，实拍测试我来进行”；后续主摄效果由用户自行拍摄，代理没有追加主摄试拍。
- 区分LSP请求、原生算法执行、照片保存及用户画质评价，不把某个镜头/模式的证据扩大为完整矩阵。
- 原色继续保留原生镜头、Sensor、Scene、HDR/MFNR/SR/夜景选择，session style2保持；不整体套用生动框架。
- 不自动恢复M3/HDR深层、经典/生动大矩阵、Sensor52或QNN移植等已停止路线。

## 工作区与当前入口

- Codex项目Cam18：`D:/cam18pm/branches`，projectId `34469847-7c47-44ad-892f-6ed5e1988134`，host=local。
- **后续统一开发目录（Git源码根）**：`D:/cam18pm/branches/aurora-camera-allinone`。后续开发命令显式使用此workdir，先查看Git状态并保留已有改动。
- **私有仓库**：https://github.com/rainryanwong/aurora-camera-allinone ，remote `origin`：`https://github.com/rainryanwong/aurora-camera-allinone.git`，主分支 `main`。
- **固定基线标签**：`v2.0.74`，对应初始提交 `9de508a2689c0df80be83f55547edde4209daba5`。后续文档与功能改动继续提交到开发分支，保留该基线标签。
- Git初始导入完整源码归档中的668项文件，逐项字节一致；另加入README、忽略规则和四份交接，共674个文件。
- **历史工作区与交付材料**：`D:/cam18pm/branches/lsp_fix_249_import_preview_20261031`，保留原始产物、构建日志、快照及历史材料；后续源码修改统一在新Git目录开展。
- 本文为当前交接入口，Git目录中的版本随开发维护；旧工作区同名文档已同步本次迁移信息。
- 规则：用户当前AGENTS、`D:/cam18pm/AGENTS.md`。`notes.md`仅保存有复用价值的bug根因与修复。
- `build_lsp.py`：VERSION=0.1.114 / VERSION_CODE=10214。历史构建日志 `build301-01114.log`、元数据 `release/CameraCompat-0.1.114.json` 均位于旧工作区。
- 最终输入快照（旧工作区）：`build/lsp-b00523ef215e/input_snapshot`。最终独立源码586项与快照字节对应。
- 当前完整包入口：**`package_module293.py`**，承接2.0.73运行包和完整源码，换入最终0.1.114。完整源码668项，保留旧整合源码81项并加入新打包入口。
- 旧292入口固定生成2.0.73/0.1.110；更旧278的main固定承接2.0.58。后续不要误用旧入口回退载荷。
- 继续使用既有签名入口，凭据不打印、不归档。签名证书SHA256：`08e04cdba0d0374cee03318fb4e886fb6b4b4a0365d16ecc5be8fa617721c2dc`。
- 本交接在Git仓库中维护；完整包和源码归档沿用固定产物，已上传Release。
- `build/`、`release/`和私有签名材料不进入Git。现有构建工具路径与签名环境变量见README；完整包入口需要2.0.73完整运行包、2.0.73源码包、0.1.114 APK及独立源码包放入新目录的 `release/`，这些输入保留在旧工作区 `release/`。

### 版本日志的现有事实

`src/com/aurora/camera/compat/CompatModule.java:86` 的启动日志仍硬编码字符串 `Compat 0.1.110 installed groups=`。11月10日相机该行的版本文字来自这个常量；真实包版本按PackageManager和APK哈希核对为0.1.114。不要仅据该行文字判成旧APK。11月10日未为日志文案另改Compat或重打包。

## 当前交付与回退材料

**v2.0.74 Release**：https://github.com/rainryanwong/aurora-camera-allinone/releases/tag/v2.0.74 。已发布，非草稿、非预发布；私有仓库下载需要相应账号权限。

- 完整模块包：[下载](https://github.com/rainryanwong/aurora-camera-allinone/releases/download/v2.0.74/camera68_aurora_allinone_v2.0.74_Stock1770_Compat01114_AUTH.zip)
- 完整源码包：[下载](https://github.com/rainryanwong/aurora-camera-allinone/releases/download/v2.0.74/CameraCompat_2.0.74_source.zip)

两份附件的GitHub服务端SHA256、大小均已与本地固定交付包核对一致，校验值见下表及Release说明。

以下本地路径相对**旧工作区** `D:/cam18pm/branches/lsp_fix_249_import_preview_20261031`：

| 角色 | 文件 | 字节数 | SHA256 |
|---|---|---:|---|
| 已刷入并重启的完整包 | `release/camera68_aurora_allinone_v2.0.74_Stock1770_Compat01114_AUTH.zip` | 290585210 | `c3568e108e0bd83d5c56cb33444106fc415ce6ca4a9dbd754ca368a40467175d` |
| 当前完整源码 | `release/CameraCompat_2.0.74_source.zip` | 38419260 | `9e826c5fb4f4b273113088566b36e6bc14acbd56bcba4ecfe59389245266afd9` |
| 当前独立Compat | `release/CameraCompat-0.1.114.apk` | 41989337 | `6ed3e6bafe160289d4a752b0cc91f44a504430721d0b370040ed4e6847b03723` |
| 当前独立源码 | `release/CameraCompat-0.1.114_source.zip` | 37757276 | `4b0ae550d244498ac7aa2cb7689ab3c30e53317955d9c5259bd924d599023ac7` |
| 上一完整包 | `release/camera68_aurora_allinone_v2.0.73_Stock1770_Compat01110_AUTH.zip` | 290584562 | `35b2d677eac4fea410a3d09df1535009e6343b13badeedf7ddb3a4231b921fd1` |
| 上一完整源码 | `release/CameraCompat_2.0.73_source.zip` | 38417194 | `288c115888434509531fb9ec0efcecff4a7f957e128c01b5e9d16bcd84d15f7f` |

0.1.111/112/113及其源码、照片和研究材料继续保留。0.1.114 APK已核对签名、16KB对齐、最终DEX及文案；完整包内APK与独立APK字节一致。完整源码中586项独立源码、81项保留整合源码及新打包入口均已逐项核对。

## SDSR功能与用户反馈

### 10×门槛

0.1.111将旧“AI超级长焦20倍启用”改为“AI超级长焦10倍及以上启用”，说明为“将超级长焦增强的启用倍率调整为10×及以上”。偏好键沿用 `aurora_sdsr_zoom20`。

`SuperTelephotoHooks` 覆盖支持SDSR的 `l9.g.s0()` 为10。原生 `r2.V.R` 和 `s6.p0.initAndGetPriorCondition` 均从该接口缓存门槛；检测和强制拍摄的比较为 `zoom >= threshold`，包含10×整值。

### 扩展镜头支持合并项

0.1.112新增3.2倍长焦支持，0.1.113新增主摄支持；**用户自行实测后明确反馈有效**。随后用户要求将两个开关合并，并明确说明多数场景增强效果不及原图直出。0.1.114按原文完成合并：

**名称：AI超级长焦扩展镜头支持**

> 将AI超级长焦算法进一步放开至主摄及3.2x镜头可用，经实测，多数场景下增强后效果不及原图直出，请根据实际情况选择启用，而非盲目开启。

- 入口：相机设置 → 实验室选项 → AI选项优化。
- 统一偏好键沿用 `aurora_sdsr_tele3`，代码常量为 `SuperTelephotoHooks.EXTENDED`。
- `mergeLensOptions()` 将旧主摄键 `aurora_sdsr_wide` 与原3.2倍键按“任一开启则合并开启”迁移，移除旧主摄键。合并前两项均true；合并安装时曾保留true。11月10日最终重启读回的合并项为false，后续按此实际状态处理。
- 同一个开关控制普通Photo163下：实际主摄SAT master2、倍率≥1.0f；实际3.2倍长焦SAT master3、倍率≥3.2f。
- 当前ROM对应：physical2 → role0 → SAT master2（主摄）；physical4 → role20 → SAT master3（3.2长焦）；physical5 → role23 → SAT master4（5倍长焦）。身份来自原生SAT物理镜头解析/缓存，不能只用UI倍率判断。
- 原生能力与请求链继续执行；未更换原生SDSR库/模型，未伪造传感器或物理ID。
- 检测器 `s6.p0.acceptResult` 仅在满足真实镜头、开关和精确浮点倍率条件时，将当前调用的整数阈值临时降到1或3，调用后恢复；原生场景、热和互斥处理保留。
- `sdsrCaptureRequired` 仅在原生支持及 `r2.V.n(mode)` 开启档条件满足时扩展；最终请求仍交给原生 `prepareNormalCapture`，保留 `useAutoWhenSdsrOn` 策略。
- 主实现：`src/com/aurora/camera/compat/SuperTelephotoHooks.java`；文案：`Laboratory253Hooks.java`；分组：`ui/NativePreferences240.java`。

### 真机执行依据与成片

用户指定的GPT-6.1 Terra High完成0.1.112的3.2×关闭/开启对照：

- 两张均为普通拍照、3.2×、原色，预览SAT master3，MIVI均记录 `IMX858_TELE3_2X`。
- 关闭帧无 `SdSrInstance`；开启帧有 `tele3 capture eligible, zoom=3.2`，原生初始化记录zoom_ratio=3.200000、backend=libQnnHtp.so。
- 开启帧执行GAN及QNN HTP，原生日志 `EXIT run` 为690.764ms；同帧MIVI记载 `sdsr:{on:1, version:1.1.2.18,...run cost 690.79}`，随后最终JPEG保存。
- 未把没有打印的插件Run返回码写成0；依据是实际算法处理、Run退出、同帧MIVI和保存链。
- 主代理与Terra均查看两张原图：该文字场景开启后轮廓更锐。用户随后反馈多数场景增强不及直出，已按其要求写入开关说明。
- 主摄0.1.113由用户自行拍摄并确认有效；代理没有另拍主摄或采集该次主摄运行日志。

证据：`D:/cam18pm/analysis/sdsr-tele3-device-20261110/`：

- `off-IMG_20261110_211542.jpg`、`on-IMG_20261110_211650.jpg`，原图保留。
- `sdsr-log.txt`：326行为关闭帧MIVI，603/609行为上层资格，828行GAN，886行Run退出，897行为开启帧MIVI。
- 主摄安装记录：`D:/cam18pm/analysis/sdsr-wide-device-20261110/`。
- 合并设置/安装及迁移记录：`D:/cam18pm/analysis/sdsr-options-merge-20261110/`。

## 当前ROM SDSR与18 Pro Max对照

### 当前aurora运行组

当前ROM OS3.0.308.0.WNACNXM，平台SM8650/pineapple。现机采集材料：`D:/cam18pm/analysis/sdsr-current-rom-20261110/`。

- 插件 `odm/lib64/camera/plugins/com.xiaomi.plugin.sdsr.so`：72304字节，SHA256 `ed095b95b8cfbaa2ccb5a6278b230d3f85c594606b2b6754177f51482c1cd19f`。
- 算法 `odm/lib64/libmialgo_sdsr.so`：16475304字节，SHA256 `e12d7e1ff757c96793b93875380ebde7116ab13c0330eb5658b4d801384e9c87`。版本1.1.2.18，JSON model_version=1.1.2.5。
- 库依赖SNPE、cdsprpc、OpenCL；JSON指定runtime=dsp，插件传入libQnnHtp.so。配套模型在 `/data/vendor/camera/`，与版本文件匹配。
- 插件启用条件包括可用内存、温度、版本、五模型文件可读性及SDSR请求；处理阶段检查图像格式和bypass。已检查路径没有主摄/tele3/tele5镜头白名单。
- `media-camera.txt`记录各相机能力；主摄physical2和两颗长焦physical4/5的stableDiffusionSR均为1，SAT逻辑6也为1。
- 当前 `satsnapshot.json` 的SDSR节点位于SkinBeautifier之后、MLAWB之前，输入输出NV12。原生模型分配和颜色/画质策略继续沿用。
- `decompiled/`保留当前插件initialize/isEnabled/processRequest/modelAccess/isSameSdsrVersion等原始反编译材料。

### 18 Pro Max新版：研究完成，未实施替换

独立静态子智能体按用户要求分析本地madrid/18 Pro Max材料；来源为 `D:/cam18pm/analysis/rom-composition-analysis-20261004/parts/odm_a.img`，build.prop确认为madrid、OS4.0.11.0.XEOCN、Android17。

定向提取材料：`D:/cam18pm/analysis/sdsr-18pm-compare-20261110/`。主代理复核了核心初始化、导出符号、依赖和选定模型标识。

- 新配套版本1.3.2.6，phone_version=q2；GetVersion读取JSON model_version，不能把它单独当作SO内固定修订号。
- 新插件151944字节，SHA256 `389e1afded9b1e77b927a194402c3e988e50cf11beabadf35b632bedd34e9e5f`。
- 新算法3029536字节，SHA256 `ee046b8d53b7696ac65899abc90abc154a86f70dce12280ed408ddd8d03dbf6c`。
- 算法改用Mage Graph/MINN；模型新增或分拆gan_20x、gan_night、gan_text、gan_face、gan_animal_20x/50x、clip等分支。
- Init增加luxindex、trigger_type、prepipe参数，插件消费SmartAE输出；外层新增cameraopt、metatagwrapper、camdfx、camlog等契约。
- 旧插件导入的Model_Init/SetLogLevel符号已从新算法导出中移除，不能直接替换同名算法SO。
- **新算法Init中 `zoom_ratio < 20.0` 直接失败，创建上下文前就退出。** 证据：`decompiled/donor-algorithm-Init.c:130`。
- sd_encoder、det_face、cls_scene选定MINN解码前缀含SM8975和QNN2.47标识；模型前缀保存在相应 `*.minn.decoded-prefix.bin`。当前未解决其在aurora V75后端的运行条件。
- 如以后用户要求移植，需独立native参数桥、Mage及依赖适配、可运行模型组和20×门槛处理。18 Pro Max对照仅做研究，未将任何donor库/模型装入手机，不自动开启后续移植。

## 承接的相机、相册与原色停止路线

这些功能在SDSR适配及本次交接整理中均未修改，详细契约和证据见2073及2071/2070交接：

- 摄影风格优化：普通拍照、专业JPEG、普通街拍、夜景、AI帮拍的“选择后关闭/顺序切换”由Terra High在0.1.110核对；RAW/UltraRAW互斥保留。0=生动、1=经典、2=原色，循环2→0→1→2。
- 相册编辑2.11.10.0.3动态契约、水印/滤镜、官方调色盘管线优化承接；调色盘100/50/0预览与保存有既有样本，自定义空间shader保存分支没有11月10日适配阶段的真机样本。
- 用户确认AI去拖影、AI证件照实测可用。AI构图名单扩展已撤销，保持原生判断；不要自动重新开放。
- 图生视频只核入口、四种运镜和默认3秒；AI助手仍有系统code和小爱CliCommandService条件。AI构图QNN研究保持2073的停止点。
- 找机位仅完成1770/2230研究，未移植。1770没有同功能隐藏入口可只改gate开放；2230入口跳转com.xiaomi.bestphotos或市场。
- 原色canonical active-root SID复用、SND style2输入映射、2.0.70十二条调校修复承接；修复后十二条定点成片仍留后续，不重扫倍率。2.0.69旧8×/10×样本不改写成后来载荷验收。
- Sensor52仍需按QCOM AutoHDR/PZSL旁路证据研究；普通Desired caps0的NewStyle候选按实际选择收敛，不扩大专用Full/ISZ排除结论。
- M3/HDR深层、经典/生动拍摄大矩阵、普通Photo融合/人像/景深/UltraRAW及超广SR边界继续按原交接停止，不因此前刷包或此次交接自动恢复。

## 2026-11-10刷包与重启证据

设备aurora，小米14 Ultra，ADB `D:/DevTools/AndroidSdk/platform-tools/adb.exe`，serial `6ea663f2`，KernelSU；模块 `/data/adb/modules/camera68_aurora_allinone`。

目录：`D:/cam18pm/analysis/allinone2074-install-20261110/`。

- `before-install.txt`：旧模块2.0.73、已装Compat0.1.114、旧boot ID。
- `adb-push.txt`、`staged-zip-sha256.txt`：设备暂存ZIP身份与最终包一致。
- `ksud-install.txt`：KernelSU安装返回0及Module installed successfully；`pending-module.txt`为待合并2.0.74配置。
- `reboot.txt`、`boot-completed.txt`：重启指令与新boot/CE状态。
- `after-reboot-state.txt`：实际模块/APK身份、标记、natural_arc_sn及boot_result。
- `active-native-sha256.txt`：七份实际挂载载荷，全部与最终ZIP一致。
- `activation-logs.txt`：原色、夜景、M3挂载和AUTH服务日志。
- `compat-after-reboot-logcat.txt`：PID18854的56组Hook及旧版本日志文字；真实APK身份见前述记录。
- `preferences-after-reboot.txt`：摄影风格=2、三档=true、10×=true、扩展镜头=false。

实际相机保持6.8.001770.0/versionCode680017700，SHA256 `e25b1ed5581baddd4e1d7f1ae60c1e0f1b1251645df5ba90dfbb9c42953b4a07`；相册编辑保持211991003，SHA256 `451a1fc0aec7565e86936e729176d77ccbc459aef7faa2bd7c567b88264a8f49`。设备暂存ZIP、旧包、原图和全部研究材料保留。

## 接手第一步

读取当前规则和Git目录中的本文件；以 `D:/cam18pm/branches/aurora-camera-allinone` 为后续开发根目录，核对Git状态、当前分支及2.0.74/0.1.114基线。向用户简短确认已准备就绪，然后等待新任务。读取完成后仅确认准备就绪并等待用户；保留当前实现与停止路线。
