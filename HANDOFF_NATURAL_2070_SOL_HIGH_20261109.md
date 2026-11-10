# 小米14 Ultra 原色集成交接 — 2.0.70

日期：2026-11-09，Asia/Shanghai。接手模型：GPT-6.1 Sol，reasoning high。
结构参考：`HANDOFF_NATURAL_2058_SOL_HIGH_20261108.md`。本文件更新其版本、设备状态、修复范围和待办；历史证据仍保留。

## 当前任务与停止点

用户要求：“参考你刚接手时读的交接文档，再写一份交接文档，随后新开一个6.1 Sol High的会话，准备接手后续任务。”

新会话当前只读取交接、必要规则和必要源码，确认准备就绪，等待用户下一项具体任务。不要自行启动补拍、安装、重启或新实验。

最新用户反馈：**已手动安装2.0.70**。这是当前安装版本的用户确认。交接时尝试只读核对，ADB报`device '6ea663f2' not found`，因此本次没有取得2.0.70安装后的重启、实际载荷生效或新增分支成片复测证据。最后一次成功的设备核对和八张拍摄是在2.0.69上，不能写成2.0.70修复后复测。

原会话已有设备文件提取、临时探针、相机UI/拍摄、相关安装重启等明确授权；当前新会话的具体委托是准备接手，先等后续指令。用户AGENTS中的同会话授权沿用规则继续适用，跨会话按用户新指令理解，不机械逐命令审批。

## 目标与实现原则

- 用户选定第一种方案：保留相机原本的镜头、Sensor、Feature、Scene、HDR/MFNR/SR/夜景等选择逻辑，按明确查询接入作者原色调校。不采用“借原色名称，整体换成徕卡生动框架”的方案。
- 原色保持session style2；识别原色后将指定色调查询映射到私有effect。`0→76、24→78、36→79、37→80`。实际五类模块为CC/CV/Gamma/LTM/TDL，并非后期滤镜。
- effect76到达只是路由证据，还必须核对最终查询选择的数据。已多次出现effect76正确但CC/CV/TDL仍是stock的情况。
- Sensor是各物理镜头内部的mode index，不是镜头编号或固定焦段。Full/ISZ请求尺寸不能直接等同UI的50MP开关。
- 先区分实际可达查询、静态条件、数据登记、算法执行、照片保存和视觉验收；不要把任一层代替另一层。
- 做实际问题的最小修改，不做猜测修复、宽泛fallback、无依据防御或全仓重构。优先现有独立LSP；本轮已批准的原色原生载荷修复继续按实际边界做，未修改stock APK。
- 新检查在会话中完成，不新增持久验证脚本、断言、构建门禁或流水账报告。必要一次性探索可放`_verification_scripts/`，不进交付。
- 不恢复暂停的M3/HDR深层实验，不删除照片、旧包、恢复材料或证据。用户已取消经典/生动真机回归，未经新指令不重启该矩阵。

## 工作区与规则

- 项目Cam18：`D:/cam18pm/branches`，projectId `34469847-7c47-44ad-892f-6ed5e1988134`，local host。
- **实际源码：`D:/cam18pm/branches/lsp_fix_249_import_preview_20261031`，非Git。** 每条shell命令显式指定该workdir。运行ZIP不是源码；保留已有用户改动。
- 新源码、研究、构建、日志、照片均保存在D盘。
- 规则：用户当前AGENTS指令、`D:/cam18pm/AGENTS.md`。中文沟通，默认PowerShell，文本UTF-8。
- 技能按任务选用：android-project-handoff、android-device-evidence、reverse-engineer-anything、apk-release-verification。不要每次全量恢复或健康检查。
- 原生分析入口：`C:/Users/Ryan/.codex/REA-GHIDRA.md`。已有WSL Ubuntu-24.04 Ghidra/JDK/REA，另有Capstone/pyelftools在`D:/cam18pm/tools/native_analysis`。
- 旧交接曾记录用户指定Terra high执行拍摄UI。本轮直接串行使用ADB取证；后续涉及新的拍摄委托时，结合用户明确偏好安排，避免多代理同时操作手机。

## 当前交付物

以下相对路径均以实际源码目录为根。2026-11-09交接时亲自重算四份归档哈希。

| 角色 | 文件 | 字节数 | SHA256 |
|---|---|---:|---|
| 最新完整包，用户确认已手动安装 | `release/camera68_aurora_allinone_v2.0.70_Stock1770_Compat0195_AUTH.zip` | 290391615 | `e9b1a348576fb48352378af948953a669790923d9520388156e344a8a3e6eb21` |
| 对应源码归档 | `release/CameraCompat_2.0.70_source.zip` | 38230255 | `64ebcf96477c6ebb9669cdec797aaf15a305c98129ba53f7b4ab0a1c0993e0a2` |
| 本轮拍摄/增量构建基线 | `release/camera68_aurora_allinone_v2.0.69_Stock1770_Compat0195_AUTH.zip` | 290386688 | `dc42a51b4310e50063696537509c1de70d5e39dda690384452aac943e55f0da8` |
| 基线源码 | `release/CameraCompat_2.0.69_source.zip` | 38228298 | `ed7577242e7f12e948d920081a8dc119e8e8fd3ca9ff882914a2a2b4be9a3622` |

2.0.70的versionCode=20070。Stock1770、Compat 0.1.95-r5及其他APK沿用2.0.69字节。包版本与内部APK版本分别记录。后续升版，不覆盖现有ZIP。

2.0.70包内Natural六文件哈希（尚未在该安装后的设备现场回读）：

| 文件 | SHA256 |
|---|---|
| GS | `6575c87bedc497e0eb5b620f06cb267758670fb2ebfafd00187dbcb3e16397b6` |
| MIVI offlinepostproc | `154b1a1be34a78ef886c1d3d12d5878b0172ad72283f8a6883d76fb937635ea7` |
| wide tuned | `e2fb8251ddd9f492a4f97f6b5cf7bb08f6037ea4e52448a56e7aefa38fd558ac` |
| ultra tuned | `6acea5bc18ccdd97ebd4dcb7a0561c1cd1dcd577c83fc3ee23ca82036d8cca60` |
| tele3 tuned | `0669bc0733e8a664bf8b3d710a8e1b7ebc521ac94a12490c446f8c23fd3f469c` |
| tele5 tuned | `04c14655cc168f503c6ac8a4c1689fd78892f0b2273a0eb0973bfd22ab625fca` |

## 当前源码入口及构建方式

- `package_module289.py`：**2.0.70入口**，以2.0.69完整包与源码归档为基线。
- `native/natural_capture_profiles.py`：通用PROFILES、EXACT_BINDINGS、按镜头LENS_BINDINGS；`LENS_ADDITIONS`是本轮十二条增量；`EXACT_ROUTES`是本轮主摄Sensor4的两条MFNR路由。
- `package_module278.py`：共用`tuned_payload(blob, filename, bindings=None)`、`write_bridge_tables()`、`bridge_payload()`。本轮传入十二条增量，保留已有私有路径及canonical active-root SID。
- `native/mivi_capture.S`、`native/gs_capture.S`、`native/natural_capture_match.inc`：已有匹配器；本轮只在编译的查询表末尾增加两个精确条目，执行指令字节未变。
- `native/natural_lookup_model.py`：sections/REC/MODE、LookupModel、链接私有化和查询查找。记录SID非数组下标，用SID字典。
- `package_module288.py`：2.0.69六条普通Sensor人脸gain数据增量；`package_module287.py`：2.0.68夜景输入桥；更早278/279/280等保留历史。
- **不要直接运行旧278 main来生成后续完整包**：其固定BASE仍为2.0.58，不能作为包含夜景修复的当前运行基线。当前增量构建沿用2.0.69/70，并保留全部启动载荷。
- ZIP采用`x`模式；不要重复运行已有版本覆盖交付物。交接文档独立保存，不因写交接重新制作2.0.70源码包。

2.0.70相对2.0.69仅改八个运行条目：module.prop、natural-files.list、runtime.sha256、wide/ultra/tele5三份tuned、GS、MIVI。tele3 tuned、夜景载荷/激活链、全部APK均逐字节保留。

MIVI新表结束`0x31ee9`，GS新表结束`0x155bd9`，各RX长度增加16字节，仍在原页；原有匹配表前缀和所有执行指令保持。两个新增行对应Sensor4/UC1/Scene32/source0的36/13和51/109→76。

## 已修复范围与关键历史根因

### 2.0.59启动失败，2.0.60起的对象身份修正

重复私有注册曾给同一模块数据逐条分配新source SID，导致provider在CV/TDL加载时Scudo映射耗尽（约6.5万maps、约4GB RSS），并非简单系统内存不足。`notes.md`有完整根因和地址。

必须复用同名称/offset/size的**已实际加载canonical active-root SID**；不能把inactive作者记录作为已加载对象。模式登记可以增加，独立反序列化身份只为确有不同且必需的新数据增加。

2.0.70检查：wide active roots13201→13205，ultra4268→4272，tele3=5298，tele5=6634。新增八个身份都对应此前未加载且本轮需要的独立作者数据，没有重复加载同数据；不要为追求“对象数增加0”引用inactive SID。

### 2.0.61—67分支扩展

已按既有生产逻辑补普通HDR/MFHDR 11/30、11/48、超广RawHDR曝光帧36/61、36/48、SR/HDRSR effect36、月亮8/31、普通闪光输出44/91及夜景相关分支。详情见notes.md，不能由已列绑定反推全部模式已经穷尽。

主摄53/54的RawSR/HDRSR人脸gain Scene32、source0/36八组已于2.0.67齐备。52/53为8192×6144，54为8192×4608的Full NoRemosaic模式；52不是新的镜头。真实Full请求要求QCFA+QNo，52缺QNo，53被接受，54由16:9实际请求选中。

### 2.0.68夜景输入修复，必须保留

AIASD把session style2原样传给ARC_SND_GetEVList；该SND只支持0/1，返回状态2且checker帧数0，造成普通自动夜景不进入、专用夜景也未走真正多帧。修复只把指定SND调用的input2映射0，保留session style2、XML、阈值和拍摄选择。

源码：`native/natural_night.S`、`native/arcsoft-night-activate.sh`、`references/NaturalNight_aiasd_stock.so`；包内含`arcsoft-night-payload/com.mi.node.aiasd.so`、激活脚本及`enable-natural-arcsoft-night`。post-fs-data按natural→night→AUTH激活。

AIASD运行载荷SHA256：`30826d7aa3c620305cbf1468e01ba7d783c8e307b30a5a17e4f3cac0440c65a6`；property `vendor.debug.camera.natural_arc_sn=1`。桥限定后摄role0..3、原色style2、Photo163/op8001或9002、Night173/op800a。

2.0.68真实验证：普通原色夜景IMG_20261108_222912.jpg实际7 RAW帧，专用夜景IMG_20261108_223029.jpg实际9 RAW帧，各帧/处理返回0且保存。证据：`D:/cam18pm/analysis/natural-night-2067-20261108/`，分别N/O trace。Sensor4/5差异是夜景接受后的下游选择，不应强制Sensor来修夜景。

### 2.0.69普通Sensor人脸gain

新增wide Sensor8、tele3 Sensor1、tele5 Sensor1各自51/105、51/106、Scene32/source0→76，共六条。已取得RawSR和HDRSR实拍查询，长焦HDRSR照片为IMG_20261108_232300.jpg（75mm）、232340（120mm）。2.0.69只补数据，无须改当时已经正确的路由。

证据：`D:/cam18pm/analysis/natural-remaining-2068-20261108/`与`natural-gain32-2069-20261108/`。

### 2.0.70：四条MFNR与八条高倍率gain

新增绑定均为UC1、Scene32、source0→76：

| 镜头 | Sensor | 查询 | 条数 |
|---|---:|---|---:|
| wide | 4 | 36/13、51/109 | 2 |
| ultra | 1 | 36/13、51/109 | 2 |
| tele5 | 12、13、16、17 | 各51/105、51/106 | 8 |

MFNR的36/52与36/13是不同阶段，前者五项匹配不代表后者齐备。MFSR按stage重新写Feature2，后续13实际进入IPE。原有主摄/超广人脸最终查询与原生消费者链确认：36/13五项与作者不同，51/109只缺CC/CV/TDL。主摄仍为effect0，超广已为76。因此本轮主摄两项补路由+数据，超广只补数据。

MFNR证据：`natural-remaining-2068-20261108/A_main1_face.trace.txt`、`J_ultra_hdr_face_retake.trace.txt`、`M_ultra_mfnr_face.trace.txt`及`ultra-capture-save-logcat.txt`。原生函数Generic::PopulateConfigurationsForUltraHDR RVA430d0、MFSR::OnPopulateConfigurationSettings RVA3b9d0。旧`natural-lenses-20261106/final-batchD-uprobes.txt`有36/13、51/109真实消费者/缓存使用证据。不要把缓存命中时没有新GetModule调用解释为模块未被使用。

高倍率八张均在**2.0.69**拍摄，目录`D:/cam18pm/analysis/natural-full-isz-2070-20261109/`：

| Trace | 倍率/画幅/HDR | Sensor | main/gain | 照片IMG_20261109_*.jpg |
|---|---|---:|---|---|
| A_10_hdr_auto | 10× 3:4 HDRSR | 16 | 24/31，51/106 | 000327 |
| B_10_hdr_off | 10× 3:4 RawSR | 16 | 36/31，51/105 | 000353 |
| C_8_hdr_off | 8× 3:4 RawSR | 12 | 36/31，51/105 | 000519 |
| D_8_hdr_auto | 8× 3:4 HDRSR | 12 | 24/31，51/106 | 000544 |
| E_8_169_hdr_auto | 8× 16:9 HDRSR | 13 | 24/31，51/106 | 000738 |
| F_8_169_hdr_off | 8× 16:9 RawSR | 13 | 36/31，51/105 | 000803 |
| G_10_169_hdr_off | 10× 16:9 RawSR | 17 | 36/31，51/105 | 000901 |
| H_10_169_hdr_auto | 10× 16:9 HDRSR | 17 | 24/31，51/106 | 000928 |

全部Scene32/effect76，实际19.4mm长焦，EXIF等效192/240mm。3:4为3072×4096，16:9为2304×4096；八张本地副本均完整解码。按文件名/MIVI frame→SubmitRequest session/ChiFrame→iqmap native request→同次ccuse/cvuse/gammause/ltmuse/tdluse关联，八组主图五项均匹配作者，gain均缺CC/CV/TDL。不要补HDRSR中随后被24/31覆盖的24/62中间查询。

本轮2.0.70离线：ZIP CRC/条目/manifest/源码对应已核对；50866条绑定、254330项作者色调选择一致；原有50854条的254270项保持；新增绑定6524项非色调比较、39504项相邻模块选择保持；另13组既有镜头查询的65项五类比较一致。该结果不替代2.0.70安装后运行或视觉验收。

## 仍需推进的范围

1. **2.0.70安装后新增查询复测**：用户已手动安装，待后续任务授权/设备条件下先核实际载荷，再验证MFNR四项和高倍率新增项。不重新扫全部倍率。
2. **Sensor52**：保持不补。当前最末未知是QCOM内部AutoHDR提前配置旁路，不是界面HDR开关。
   - 普通Full snapshot：RTMCX5a7b0→5d0d4 Base→override FindBest4456f0，5c9ac要求QCFA+QNo，实证拒绝52/接受53。
   - 剩余候选5b27c/5b2f4→5b910要求physical HDR mode1、QCFA PZSL config1、PZSL hint bit45、ISZ!=2。bit45由GS GetCaptureMode的feature instance isPseudoZSLSnapshot(bit18)→capture bit13→GetFeatureHint的Hint+4 bit13产生。
   - 常规SetDeviceHDRMode3ca988在QCFA PZSL时清mode0；3ca738的提前路径可能绕过。H=HDRInfo+18，S=HDRInfo+1c，A=*(Extension+3f8)=StaticSettings+10548，U=Extension+5fcc。
   - SetAutoHDRCapability3c7f40读QCOM EnableAutoHDR+isAutoHDRSupported；ConfigureHDRInformation3a1a30还可在A开启、session HDR vector为空时自行置S。因此不能仅凭APK没字符串或UI HDR开/自动排除旁路。
   - 最少定点：override3ca738读X23的H/S及session vector+20/+28、preferred+38/+40，X20的A/U；3cac00读X19 HDRDeviceInfo+20最终mode；RTMCX5b27c/5b910核同请求门槛。
   - 已有`natural-remaining-2068-20261108/I_main15_hdr_pzsl.trace.txt`：mode0、cfg1、hint0x82021、ISZ1，最终53。
3. **NewStyle**：专用Full/ISZ SR变体已能排除，无需为它们补原色。override CheckNewStyleType4a3ee0读取session原style2；GetMiDesireSensorMode4a6774取value&1置bit46；RTMCX13处GetNewStyleMode均cmp==1；selector444240/444270要求NewStyle能力位一致。tele5 Full14/15、ISZ18/19属此类，12/13、16/17为普通原色模式。
   - 保留普通初始化Desired caps=0的例外：IsMatchingMiCapability443fdc→4446ac只检查candidate Normal位，跳过NewStyle位比较。带Normal的NewStyle候选：wide29/30/31/33/36/37/39/45/46/48，tele3 6/7，tele5与ultra 8/9。继续按尺寸/fps/排序及实际选择收敛，不能把专用Full排除扩写为所有NewStyle物理mode全局排除。
4. 其他尚未系统核对的模式继续单列，不能称全部漏接或全原色补全。当前普通拍照配置未启用融合拍照能力；专用人像/景深、UltraRaw等不能混入普通Photo这轮清单。

超广普通Photo当前SR阈值0且角色stream没有phgType2/5，因此不为它盲加105/106；实际RawHDR为36/85/86及gain107。人脸Scene32的RawHDR主图/gain五项已在近距离重拍中匹配作者。

5× Full阈值7.5–9.99999，ISZ10–120，Lux阈值65536、ShortGain16；原生比较含边界，仍受月亮scene35等状态约束。现在12/13/16/17八个人脸组合已有实际证据，不再作为待补证项。

## 设备与取证实用信息

- ADB：`D:/DevTools/AndroidSdk/platform-tools/adb.exe`；serial `6ea663f2`；aurora，KernelSU；模块`/data/adb/modules/camera68_aurora_allinone`。
- 最后成功现场：2026-11-09 00:00起，2.0.69，boot id `a8007e0a-fc56-4d19-9d64-6b9c8523996d`，app PID25977/provider29762；新任务重读PID，不复用旧值。
- 2.0.69四份tuned及GS/MIVI在本轮拍摄前均与本地2.0.69包哈希一致；夜景载荷SHA如上，property1。
- 上述高倍率目录有initial-identity.txt、21个临时探针定义、八组trace/JPEG、capture-save-logcat.txt、last-trace-stats.txt和probe-cleanup.txt。
- 21个fullisz_1109a探针及实例已撤除，最后窗口overrun/dropped=0，global tracing0，仅保留原有hsuart。上轮gain32_1108a/b也已撤除。
- 最后拍摄UI由16:9恢复3:4、HDR自动、原色、10×，final-closed-ui.png已观察到菜单关闭。此为用户安装2.0.70之前的最后UI记录。
- Python subprocess的root多命令：`[adb,'-s',serial,'shell','su','-c',shlex.quote(command)]`。文本显式UTF-8/errors=replace，输出中文用sys.stdout.reconfigure。大输出先在进程内过滤。
- 二进制取文件用adb pull或`exec-out su -c`，后者Python argv末项为未再加字面引号的`cat /absolute/path`；不要用adb shell输二进制（CRLF损坏），也不要用exec-out su -c sh传stdin（曾挂住）。
- 拍摄可在已观察正确UI后用`input keyevent 27`；idle提示需先唤醒。风格/比例面板自动收起行为不同，逐步看截图，勿盲发BACK退出相机。
- 拍摄截图物理1440×3200；长按倍率可开弧形轮盘再水平拖动。坐标仅作历史参考，按当前UI观察调整，不作为产品修复。

## 匹配输入及工具定位

- 当前1770 APK：`D:/cam18pm/analysis/natural-photo-audit-20261108/current-camera.apk`，SHA `e25b1ed5581baddd4e1d7f1ae60c1e0f1b1251645df5ba90dfbb9c42953b4a07`；不要使用旧`D:/cam18pm/decoded`冒充当前APK。
- 原厂配置/四物理sensormodule：`D:/cam18pm/analysis/natural-photo-audit-20261108/device/odm/`，xiaomi配置在etc/camera/xiaomi，模式文件在lib64/camera。
- 作者四tuned：`D:/cam18pm/analysis/natural-tuning-v140-20261013/upstream-ksu/payload/`。
- camera.xiaomi.so、override、camera.qcom.so、camera.qcom.sm8650.so：`D:/cam18pm/analysis/natural-tuning-v140-20261013/`。override SHA `43eb3c1ce976df4176c0579c753b456de7b8ea8a3334aeefbc071fdcea95fe15`；camera.xiaomi SHA `a58e66587c804fe572cfcb8da1f5ecba4f19afbeb039a6cb049cb217bff53ae3`。
- Generic：`D:/cam18pm/analysis/natural224-owner-map-20261029/device/files/odm/lib64/com.qti.feature2.generic.so`，SHA `c17d7649a36545022f48193f7b654ab40083a38520bc2f0aa07c4fa8f076ba7c`。
- MFSR：`D:/cam18pm/analysis/natural-photo-audit-20261108/device/odm/lib64/com.qti.feature2.mfsr.so`，SHA `d2f8b9d6c328e6b625d70c54ace693217635c7d6c8fe6ad7a0f4aadd75055636`。
- RTMCX：`D:/cam18pm/analysis/natural-sensor52-selector-20261108/com.qti.feature2.rtmcx.so`，SHA `3a911f350d724ebb1b5d27b62d9f42fddd3a6b000263d7e5b41d8ca328b92446`。
- 原生capability raw enum：38=CMS、39=DCG、41=QNo；NewStyle raw36→driver bit46。不要沿用早期误记的35/36作CMS/DCG。
- REA MCP本轮可用，Generic/MFSR已完成并close；新会话不能假定仍有active target。Ghidra分析地址常为ELF RVA+0x100000；procedure_pseudo_code可用file-offset:0x...，callers/xrefs须用实际分析地址。Program identity是`target-<SHA前12位>.so`，不是原始文件名。共享服务并行分析须显式document。

## 接手第一步

读取本文件、用户/项目AGENTS，必要时检查package_module289.py与当前profiles；向用户简洁确认：已手动安装2.0.70的反馈已接收，十二条修补离线完成，安装后运行待后续任务安排，剩余重点是Sensor52及普通caps0的NewStyle选择。然后等待用户指令，不自动开始以上待办。
