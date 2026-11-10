# 可复用的故障根因与修复

## 自定义滤镜去重和删除

- 自建配方可以引用导入 LUT 的动态槽位。配方与该 LUT 的内容相同，也仍然存在资源依赖。自动选择重复项时优先保留 LUT，并保留仍被未删除配方引用的槽位；批量删除先移除配方，再检查剩余引用并删除 LUT。
- 独立 LUT／原生滤镜的颗粒走 CV noise，CamStyle 配方的颗粒走 StyleAdjust。去重签名必须保留各渲染阶段及其参数，不能把二者都换成同一个颗粒数值后比较。名称、排序和 PNG 编码元数据不属于配方内容。
- 前后摄工作区各自可以保留选中标记。原生 `capture.d.O()` 使用当前相机的配置，不能通过它切换非当前镜头的原图。删除另一镜头的选中项时，分别更新它的标记和对应镜头的 `r2.Z0` 配置。
- ZIP 的解析和导入任务由 Hook 实例持有，页面只接收进度与结果。将任务绑定旧 Fragment 会在配置重建时丢失结果，并让新页面读取已经清理的暂存目录。待选择列表独立暂存，选中索引随 Fragment 参数保存，实际导入只消费所选条目。

## Windows 上的反编译输出

`u2.y` 是菜单组件，`u2.Y` 是全局设置。Windows 文件名大小写不敏感，平铺输出会互相覆盖；导出时为这类原始类名使用不同的完整文件名，保留二者的对应关系。

## 相机与相册滤镜目录同步

- 相册读取模块 Provider 的已提交目录。不能按相机 APK 更新时间只请求一次同步，也不能仅凭相机进程内的“已发布”记录跳过提交；重新读取源与 Provider 的实际目录后比较，最后一项删除时提交空目录。
- 动态 LUT 的枚举名是 `AURORA_USER_LUT229_<slot>`，资源 token 是小写前缀，两者用途不同。读取动态扩展使用被 Hook 的 `values()`；`Class.getEnumConstants()` 可能仍持有扩展前的缓存。动态基底直接引用配置对应的 PNG，不能绕过动态 Hook 调用原生 LUT 工厂。
- 导入 LUT 的颗粒属于 CV 阶段，导入柔光在 LUT 前且强度为 1.0；配方颗粒与柔光属于 StyleAdjust 及其后阶段。相册导出和去重必须保留阶段；配方显式覆盖为 0 时，也要清除继承的 LUT 颗粒与柔光。

## 扩展 LUT 数量与原图特殊编号

- Stock1770 的原生枚举共有 274 项；`i3.b.P=66048` 的低位 512 表示无滤镜。扩展为 512 个自定义槽位后，枚举长度达到 786，原图查询会误进入 `values()[512]`，恰好落到自定义槽位 239。`si.f.d/e` 收到完整原图 ID 时，在调用期间使用原生枚举视图，并在返回或抛错后恢复先前作用域。
- 槽位判断只排除完整的 `P`，不能排除所有低位为 512 的编号。真实槽位 239 的 category2 ID 是 131584。工厂 after 回调对动态枚举也必须使用该槽位的真实 ID，避免重新拼成 category1 的原图 ID 而丢失颗粒与柔光参数。

## 录像导入 LUT 的 MiSys 写入

- Phoenix 1.2.14 的 `si.i.d` 用 Common 的 `instanceof Fuxi` 判断是否调用 HIDL；K60E 的 rembrandt 对象使该判断为假，但设备实际注册了 MiSys V2 HIDL。AIDL 未注册时，导入缩略图和列表仍可出现，`Video LUT publication failed` 后原生编号只剩照片 ID 的低八位，真实录像 LUT 没有发布。
- 正确接点是实际服务与原始协议：AIDL 直接调用 `IMiSysImpl.MiSysWriteBuffer`；无 AIDL 时非等待取得 MiSys V2 HIDL，使用事务 2 写目录、文件名、int8 向量及 int64 长度。K60E 已通过 donor `cn.a.b` 写入 253.png 并读回相同 SHA256。
- Stock1770 `on.a.e` 与 donor `cn.b.e` 的异常重试会删除 persist/camera 下 PNG/WebP，不能拿来发布自定义录像 LUT。录像槽位使用独立完整选择 ID，再显式映射到 HAL 的 253/254，避免照片编号截断与不同槽位低位相同造成混淆。

## 滤镜跨包可见性与普通拍照名称

- Camera/Editor 首次查询模块 Provider 前必须先获得模块包可见性；由 Provider 收到请求后才握手形成循环。API34+ 显式广播共享真实发送身份，双向核对包、UID、签名，收到身份共享 ACK 后再查询 Provider。读取不到 Provider 时，单独恢复 LUT 数据不会解决三个页面同时缺项。
- 普通拍照 `j0.Ns()` 使用实时适配器 `s9.i`。动态行的名称契约是 `data.d.l == -1` 时读取 `data.d.o`。`fragment.I.m` 原来主动清空这种名称；修复这个文字消费者，转用其 `h(String)`。无障碍 `d.x` 和选择更新 `P9.i.Zs` 也必须遵循同一字符串契约。Toast 读对名字不代表卡片文字已绑定。
- 普通 Photo 的 `r2.a.getItems()` 消费者直接读组件目录，未按 CamStyle 工作区隐藏与排序。仅在 Photo UI/缩略图读取作用域投影可见列表，工作区维护仍使用完整组件目录；原生把投影列表写回组件时恢复完整来源，避免隐藏项从主目录丢失。

## 整体设置备份的槽位与目录别名

- LUT 存储已扩为 512 槽，备份校验不可保留旧的 99 常量；统一引用 `LutStore229.SLOTS`。130 项内置目录暴露了这个旧上限。
- 相机进程中 `/data/user/0/com.android.camera/files` 的 canonical 路径可成为 `/data/data/com.android.camera/files`。`inside` 返回 canonical 文件后，若 `relative` 仍用未 canonical 的基准，会生成 `../../../../data/...` 并触发正确的越界校验。基准和文件统一 `getCanonicalFile().toPath()` 后计算相对路径；不放宽 ZIP 路径与目录范围校验。Root app_process 的挂载/路径观察可与相机进程不同，应以失败现场的实际路径为准。

## 录像之后冷启动滤镜管理

- MiCamStyleRear/Front 是拍照工作区，管理页冷加载不能使用持久化的上次拍摄模式。上次停在录像162时，`capture.d.G` 经 `capture.e.i` 调用 `v2.U.d(162)`，目录类别成为7；照片动态条目只填完整照片ID，录像mapper读取其未赋值的 `i3.b.n`，42项都映射成0，最终触发LUT缺项异常。定向现场为mode162/category7/current55/catalog56/mapped42/bound空。
- 仅把G的入参改163不够：`U9.a.G` 和 `capture.d.G` 内部还会各自读取 `u2.Y.D(intentType)`。在 `FilterWorkspace215.workspace` 的拍照冷加载调用期间，以共享静态ThreadLocal限定当前线程的模式读取为163，并用try/finally退出该上下文；不修改用户持久化模式。其它FilterWorkspace215实例在同一原生加载链中也读取此上下文。
- 保留映射完整性异常，不给照片条目虚构录像编号，不跳过失败或要求用户先打开相机。正常录像使用已有独立ImportedVideoLut256协议。


## CamStyle 工作区并发与完整目录

- 原生后台 G/f/Q 与模块 append 遍历同一个 mItemList，主线程 dispatch 对该列表调用 ArrayList.sort；即使成员没有增删，sort 也会使正在使用的迭代器失效。原先的 ThreadLocal 只能防同线程重入。工作区与共享 Photo component 的读写事务现由 CamStyleAccess260 的同一可重入锁保护，外层 Hook 覆盖原生实现、R2 归一化和 LUT 补齐；读取不能顺便进入 full 作用域，否则可见位置语义会变化。
- 原生管理网格保留 master 引用，Ks 直接追加，onPause 会按旧 p 清空重建 master。这些路径同样要持锁，退出前以当前 master 成员核对 p，保留新加载条目。界面适配器长期保留的 visible 列表只由主线程更新；后台维护仅发 changed，后台 getList 返回独立投影，主线程不再排序 master。
- slots() 表示全局已启用项，configuredSlots() 表示资源仍存在。把前者当存在性，会让停用的 LUT 从管理目录消失，甚至被 prepare 当成删除。Photo 工作区与其定向完整目录查询改用 configuredSlots，界面显示使用全局 enabled 与当前前/后摄 hidden 的组合。管理页开启全局关闭项时开启全局位并解除当前 hidden；关闭只修改当前 hidden。普通目录、录像和相册发布继续使用原启用规则，native-gallery 不发布到相册更多。备份须编码有效开关状态，排序本身不能开启全局关闭项。

## 传奇一瞬 M3 的预览入口

- 运行包 m3-save-editor.apk 的 xposed_init 只注册 M3SaveOnly，它只安装最终 JPEG 保存桥；已有 M3Runtime 的 OES 灰度与 CvStyle 预览从未安装。不能据保存桥加载日志认定预览也已启用，也不能直接注册旧 Runtime 再装一遍保存桥。
- Compat 的 M3Preview260 只为当前 mode256/M3 的真实主预览打开灰度，在 Iu.s.e 之后使用双缓冲执行同源 CvStyleEffect。Stock1770 接点为 Ru.a、Iu.s.f0/D/B、F8.j.Z0 和 Tu.w.f；旧版本 Au.a、ru.h、Cu.w 不能照搬。预览参数 assets/legend260/leica_filter_param_m3.bin 与原 M3 APK及 ODM 成片参数相同，由 lux/zoom 查询八个参数。该预览沿用原 M3 的灰度与 CvStyle 设计，成片还有独立 Monopan 颗粒阶段。

- 停用仍保留的当前 LUT 时，切回原图必须在原生 f 完成 M 绑定后执行。capture.e.b0() 会缓存 `p && M!=null && M.r==Original`；在 f 的 before 中枚举判断原图，会把尚未绑定的真正原图永久缓存为 false，随后报告缺少原图。先完成原生绑定，再在 append 后置阶段处理停用选中项。


## M3 保存完成与相册、缩略图的衔接

- Stock1770 的快速缩略图 URI 先于最终成片落盘。F26 只检查新 URI、`needWaitSaveFinish()` 与 `isCannotGotoGallery()`，实机曾在 M3 JPEG 写入前约 1.8 秒进入 Gallery；Gallery 可在这段时间读取到快速预览，不能把早期 URI 当成完整 M3 文件。
- `l1.a.e(OutputStream)` 返回后，原生 Storage 仍需 flush/close、复制文件与更新 MediaStore。以 `l7.J.invoke()` / `l7.H.invoke()` 的非空 URI 返回确认文件提交，并在所属 `t7.d/e.a(Sh.s)` 完成原生通知后衔接相册；按任务对象核对 writer，使用同一照片 URI 匹配已有点击状态，不加固定等待时间。
- `t7.e` 保存更新分支保留快速缩略图，只改 URI/大小；独立 M3 保存桥修改输出流，不会修改快速缩略图的位图。从已提交 JPEG 直接解码，再经 `E1.a5.d` 和 `l7.C.e` 发布缩略图，避免 MediaStore 缩略图缓存及彩色旧位图。原生 callback 可为空，后台保存不要求存在活动相机界面。
- 当前 Editor 2.4.0.5.2 的 M3 能力入口为 `lb.a$i`，已有 M3EntryPoint 会命中；实机二次增强的 MonopanProcess 返回 0。Gallery 读取过早与 Editor ABI 适配是不同链路，应先定位失败发生在哪个阶段。


## 相册编辑的特殊机型水印

- 相机的 Ti 是原生版本标识节点，不是主文字中的普通后缀。编辑器 Qn.a$a.c 的 Xiaomi 分支不拆 Ti；在返回的 In.a 中拆为 a=基名、b=空格、c=空串、d=Ti，Cloud rect_params 与 XML FormatModelElementView 才会绘制原生方框。自定义机型不能把主节点和 model_versionName 都覆盖为完整名称。
- by Leica / Leitz 的品牌图在相机和编辑器 APK 中各有同样的 30 个素材。Editor c.e 的固定特殊名分支直接解码 assets，不经过 on.a.a；限定 logo 渲染窗口临时走普通 loader，再在原生尺寸计算前提供完整品牌图。保留 in 位置的 shadow 名称及 loader 的 boundsOnly 语义。
- hn.a.F 会把机型名转小写存入 q；按配置对象保留原输入，并在 c.b 的线程调用窗口传给合成器，避免任意机型名丢失大小写。XML LogoElementView 用 m/n 的比例测量，替图时保留原高度、更新宽高比；更新前恢复原 logo 与文字可见性，避免切回普通样式仍隐藏文字。


## 2026-11-02 后续复核：编辑器滤镜可见性与 Ti 模板

- “更多”缺失的异常用户：Compat已安装、连接回复已到达，但编辑器PackageManager查询Compat抛NameNotFoundException，HMA相关模块/广播线索同时存在；用户调整HMA设置后确认恢复。另一份Ryan日志是MIUI阻止连接请求唤醒，43条旧缓存仍使入口显示。这是两个不同失败点；后续优化握手/目录同步时一并复核，保留必要身份校验。
- 2.0.51 Ti静态疑点：手动机型覆盖分支统一把Ti从主model移至model_versionName，而部分徕卡JSON只有包含versionName占位符的主节点。用户随后逐模板测试未发现Ti消失，记录为潜在兼容问题；后续先核实际UI模板与运行消费者，再决定是否按模板结构调整。Ti不带方框本身是部分模板的正常设计。
- 用户要求先记录、后续相关修复时再加入；本次不改代码或重打包。证据路径、具体回归项见项目入口 HANDOFF_REVIEW_OPTIMIZATION_20261102.md 文末“后续待处理”。


## 2026-11-03 用户补充的待修复问题

1. 相机设置 `17 Ultra by Leica` 后，直出照片水印只有 `xiaomi`，缺少 `17 Ultra by Leica`。待定位相机文字抑制与品牌图选择/合成、最终成片发布链，具体模板与实际版本待核。
2. 有用户使用传奇一瞬黑白胶片模式拍照后，不显示“徕卡一瞬”二次增强按钮。用户说明相册编辑和相册版本均与 Ryan 相同；原因未知，待结合原始照片、组件加载与入口判断日志分析。

这两项目前是用户反馈，先记录、后续再修复；详细已知条件与回归目标见项目入口 HANDOFF_REVIEW_OPTIMIZATION_20261102.md 的“新增待修复问题（2026-11-03）”。


## 2026-11-04 新研究入口（等待用户开始）

HDR新增反馈：普通拍照徕卡经典/生动/原色，大光比高光压制与官方14U相机实拍不同。M3新增本人复现：11月3日卸载更新切官方再恢复模块、未重启后，160631/160930/161018不能二次增强；日志在手机Download/DLManager/161050.zip、Download/EditiorMoreLogs/13666.tgz。与先前问题合并于 C:/Users/Ryan/Documents/ChatGPT/小米14Ultra/HANDOFF_HDR_M3_WATERMARK_20261104.md。新Astra Medium会话等待用户明确“开始”；实机操作由新开的6.1 Terra High子代理承担。


## 2026-11-04 HDR 测光变量与 M3 目录重建

- HDR 对照原图已确认6.8 MeteringMode=3点测光、官方430=2中央重点，含11月3日户外及11月4日各组。不能把混入测光方式的曝光差异归因为HDR算法；用户要求本项收尾。
- 相机切换后目录重建，m3_aux_relay PID4470仍持有deleted目标fd4 inode894017，当前路径inode956945。源码仅启动时打开目标并缓存owner/context；修复应重新绑定当前目标目录及其权限上下文。原生40秒完整图像回传超时发生在aux生成前，是本次另一故障，不能用重绑目录解释。详细现场及进度见项目HANDOFF_HDR_M3_WATERMARK_20261104.md末尾。


## 2026-11-04 水印与 M3 中转修复实现

- 水印缺项根因：Stock1770宿主assets没有prometheus/watermark-suffix，而router从宿主枚举；现从Compat APK供给37份后缀到模板目录，再转换/发布MiVi资源。共享文本配置提前清空byLeica文字会使发布链丢失机型；现保留完整文字，实际model/model_versionName绘制确认合成可用后才去重。新增预设左保存预设白底、右修正并保存蓝底，normalize绑定右侧positive。
- M3中转修复维护在native/m3_aux_relay.c：每次投递获取当前目标目录、属主与SELinux context，发布后确认目标仍对应当前路径再清理源。目录暂缺时仅对实际积压阻塞等待恢复。旧历史audit源码不修改。
- 本次M3拍摄额外故障最早可见为frame15782的Prefilter_UBWC_OUT_MFSR_0目标buffer等待超时→MMF process timeout→MfnrInstance0返回1→相机40秒未收到完整图；仍未确定buffer上游未返回原因，未据此猜改算法。


## 2026-11-04 M3 原生服务重建后完整链路恢复

仅重启init服务vendor.camera-provider（2796→29157），btime1793693549不变，relay21586不变。单张IMG_20261104_171445.jpg成功，MFNR八帧533.756ms，SensorMode7/UC1/feature1:0/feature2:13/scene46，M3Style/M3Grain执行，aux publish→relay delivered→aux matched→M3 JPEG committed。主JPEG3344×4852和附加JPEG4096×3072完整解码，包含Legend.MONOPAN；SHA2563289630cdc89bb64f8b8c7a4d514d3fac9488a3890575ce50c868a6f20614cbd。相册徕卡一瞬开始处理后MonopanInit成功、MonopanProcess ret0，原图哈希不变。证据D:/cam18pm/analysis/hdr-m3-watermark-20261104/evidence/1712-m3-provider-restart；父代理亲自查看原图和关键日志。

两个问题分别处理：相机目录重建后的relay旧句柄已有源码修复并通过真实投递；MFNR持续超时本次通过重建provider运行状态恢复，具体是哪项MMF内部状态触发仍未确定，没有加入自动重启或算法降级。后续若再跨版本切换，重点捕获失败前的session/buffer生命周期，不能把一次成功写成永久消除超时。当前手机普通拍照1x HDRAuto，水印开启；采集停止。


## 原生接口目标修正与首轮短时跟踪

用户批准一次最长10分钟uprobes，设备代理独立实例跟踪libcamerapostproc.so四个入口；17:36前已停止并删除全部6事件、实例及限时任务，无残留。430阶段快门输入未取得CAPTURE_START/成片，不能记作有效430拍摄。6.8 IMG_20261104_173604.jpg八帧MFNR/M3Style/M3Grain/aux matched/最终M3 JPEG成功，该次切换未复现。探针为0条，属于未覆盖到目标调用而非证明未调用release。

关键修正：natural-payload/com.xiaomi.plugin.offlinepostproc.so字符串指向libofflinefeatureintf.so。设备已取/odm/lib64/libofflinefeatureintf.so，230840字节，inode85874919，SHA256baa89b9cddefb171e4a578e1ebbe4c98be7efa9fe77c60b480e79a917042db3c，证据evidence/20261104-1731-native-session-trace。该库真正实现ChiOfflineFeatureManager；早前libcamerapostproc.so对应ChiOfflineJpegEncode，不能混用释放契约。

REA反编译实际导出file offsets：Create0x1dc60，PreProcess0x1dc70，Flush0x1dd60，Process0x1de60，Destroy0x1df50，ReleaseResources0x1df60。实际ReleaseResources(handle,output,type)只在type==1时释放输出Qcom metadata；Flush(handle,frame)对0xffffffff遍历offlinefeature调用Flush，单frame另有待flush集合；Destroy调用ClearFreeFeature(0x10)，销metadata与map。ClearFreeFeature会跳过busy feature（+0x3a0非0），需动态关联flush/process/destroy顺序，不能仅凭此写补丁。

下一步跟踪须改用实际MFNR接口，并先以6.8单张确认Create/Process事件非零，再请用户430实际拍摄、确认CAPTURE_START和保存后恢复6.8。不得再凭发送坐标就报告拍摄成功。旧10分钟窗口结束，不默认延长或留下探针。当前生产源码/包未新增任何猜测修复。


## 用户暂停：2026-11-04 17:49 后

用户要求“暂时不用这么深入推进，先这样”，停止进一步根因实验。计划的HDR→M3对照已取消，等待设备交还的代理确认未执行任何ADB/手机操作/拍摄。不要自动恢复该实验。

最终真实接口跟踪已完成：证据C:/Users/Ryan/Documents/ChatGPT/小米14Ultra/evidence/20261104-1742-m3-true-interface-trace/full-trace-final.txt。旧6.8四manager及430三manager均在Process返回后Flush/Destroy；切回6.8的M3创建四新manager，Process均ret0，IMG_20261104_174900.jpg保存成功，3344×4852，SHA2569fad3bf6d81cec0a989f3607f37894eadec208ce3b46a4f578b859d7fa0ccf6f。本轮未复现异常，不能据此称永久根治；没有新增推测修复。17:49:42本组8探针、独立instance、watchdog均清零，仅保留原有hsuart。provider未重启。

保留已安装Compat0.1.77与relay v3，水印及目录重建修复保持；当前M3已多次成功。下次仅在用户明确恢复时继续排查原生异常触发条件。

## 相册水印的显示名称与保存元数据

相册 2.4.0.5.2 的名称 getter Hook 未改变实际绘制结果：原图在文字模板仍显示 Original，图形徽标缺失。CloudWatermarkMaker.a 的第 35 个参数是绘制时使用的辅助对象；在此入口复制全部 12 个字段，仅将副本中的滤镜、虚拟镜头名转换成模板所需显示名，原图显示 Standard。保留共享原对象中的 Original 及其他元数据，避免编辑保存把显示名称写回拍摄语义。不能把 getter 未生效直接归因为 ART 内联，现有证据没有确认这一点。

## 水印样式页封面的绘制尺寸

Stock1770 的 WmGalleryPreference 已在后台生成封面，但 yu.a.i 按模板原生测量尺寸创建成品 Bitmap，列表再缩放显示。生成后再 createScaledBitmap 虽减少纹理上传，却增加后台绘制和缩图成本。保留 TYPE_PREFERENCE 与原生 measure/layout、background/content 调用，在照片样式页 loader/时钟更新的线程作用域内，仅对 yu.n 封面根节点改用实际卡片高度的小 Bitmap，并先缩放 Canvas 坐标后原生绘制；避免大成品 Bitmap 与二次缩图。不要直接切 TYPE_THUMBNAIL：它会把部分色卡取色替换为灰色占位。不要仅改 yu.a.i 的宽高参数：根节点采用自身测量尺寸，会覆盖这些值。

## CamStyle 照片滤镜的原生强度刻度

Stock 1770 的 AI 帮拍由 P9.i.mt 创建刻度配置并绑定 CombineSlideView 和 L9.d。反编译结果里出现的 new J8.j() 在实际 DEX 中没有可反射的无参构造。CamStyle 复用 mt，并把刻度放在卡片列表下方的相机内容层；将刻度追加为列表容器子项会抬高卡片、使标题重叠。

相机冷启动会清除 live slide key。CamStyle 将用户修改保存在当前滤镜目录的 p_ 标记中，而 U9.S.O 重新载入时只将注册组件的参数填入 e，已编辑强度的原值键仍在 f。首选该滤镜时要从它自己的强度标记恢复 e，再绑定原生刻度。否则 UI 会显示 100，下一次调整会新增第二个标记，而非让原生 R 重命名旧标记。

## 2026-11-05 自定义顶部／下拉菜单的双提供器

Stock1770 的 b5.i$a.c 提供顶部 b5.j，b5.i$a.d 独立提供下拉 b5.a。只设置前者时，菜单 token 已进入 p5.W.zs 的结果，但 p5.c.onBindViewHolder 因 b5.i.h 为 null 直接返回，界面留下未绘制项。通过 b5.i$b.c 返回 b5.a$a 构建的图标、名称和选中状态修正；不需要修改 zs 布局分支。顶部 listener 用 builder.e，下拉由原生 dispatcher 调用 r6.W.fk，不再向 builder.f 重复加入切换。

## 2026-11-05 亮度提升与系统渐亮竞争

启动相机立即提升时，root 将面板节点写为 3600，系统自动亮度的启动渐变又写回 427、446 等中间值；仅在失去焦点时暂停 root 写入，不能处理启动和环境光引起的竞争。提升期间使用相机窗口的 screenBrightness override 暂时接管亮度，保留原窗口值；root 先读取当前系统 SDR 亮度供窗口固定，再等系统动画、ScreenReady 和背光提交完成后开始写入。暂停或结束时恢复节点并释放窗口，让系统重新接管自动亮度；切模式继续使用同一个 root 计时。

这台 Android 16 的窗口 override 保存在 OverrideBrightnessStrategy.mWindowManagerBrightnessOverride；mPowerRequest.screenBrightnessOverride 仍可为 NaN，不能据此判定接管失败。原生 display Binder 16777213 的接口令牌是 android.view.android.hardware.display.IDisplayManager，设置 mIsResetRate；系统软件报告跳过渐变后，仍需等实际背光更新结束。读取 dumpsys 的 awk 应消费完整输出后退出，否则上游会报告 Broken pipe。将窗口先固定到 1.0 会增加一次无用的升至原生上限过渡，改为固定接管时的当前亮度。

## 2026-11-05 街拍切换清除窗口亮度覆盖

Stock1770 的 N4.a.fs 在街拍界面初始化时按原生策略写入 screenBrightness=-1。真机调用栈经过 PhoneWindow.setAttributes、PhoneWindow.dispatchWindowAttributesChanged，最终进入 Activity.onWindowAttributesChanged；挂在 Window 基类的 setAttributes 和 dispatchWindowAttributesChanged 上的 Hook 没有拦住这条调用。窗口覆盖因此消失，自动亮度与仍在运行的 root 提升会话竞争，出现 3600 与数百之间的反复跳变。

将亮度参数保护移到 Activity.onWindowAttributesChanged 提交窗口布局之前，沿用原有 pinned 窗口身份判断及 previous 值保存。这样街拍请求的 -1 被留作结束后的恢复值，提升期间提交的仍是固定覆盖值；无需重启 root 会话或刷新计时。按用户后续要求，删除特殊街拍退出回调和特殊街拍切模式时的强制 stop，仅保留录像限制、失焦暂停、退后台结束及原有计时。

## 2026-11-06 MIVI 成片调校查询的场景分支

MIVI 的 MiMetadata::Entry.type 位于 Entry+8；读取错位会使模块判定在进入查询筛选前退出。修正类型读取后，实拍进一步暴露两层问题：主摄查询 (0,9,1,36,85/86,32,0) 被旧场景白名单拒绝，且私有 effect76 未覆盖实际下游 36/86、51/107 分支。只改 MIVI 白名单或观察到 effect76 都不足以确认数据来源。保持同一场景与设置重启后，自动 scene 可从32变为0，scene0 同样需要对应作者分支。

package_natural_snapshot.py 以 natural1 为底，仅为主摄 sensor9 的上述三个 profile、scene0/32 补齐 effect76 的作者五类色调注册；数据区与引用符号沿用作者载荷，Gamma/LTM 的作者 keep 项保留。native/mivi_snapshot.S 在原 style/owner/module/layout 检查之后增加 scene32 的精确85/86匹配，其余输入返回原筛选。成片关联使用 MIVI 文件名/帧号 -> 实际 SubmitRequest Session/ChiFrame -> Session ring 的原生 request -> 同次 IPE GetModule 返回对象，再接最终保存日志；不能用相同 SID 数字推断作者数据相同。
符号表原有记录并非按 SID 顺序排列；关联实际对象时按记录显式 SID 建索引，并解析 alias/source SID，不能用 SID-1 作为旧记录的数组下标。


## 2026-11-06 GS 重建查询与数码变焦分支

MIVI 已写入 effect76 后，GS 的 ChxUtils::FillTuningModeData 仍会从 Android/vendor effect 元数据重建七值查询。现有 GS 桥同时限制 sensor 范围并使用 profile 表；只扩大 sensor 范围会继续漏掉 HDRSR 的 UC1/F1=24/F2=31。natural5 在原 style/owner/module/layout 检查之后，仅匹配实测主摄 (0,53,1,24,31,0,0) 和 tele3x (0,1,1,24,31,0,0)，写76后进入原 store_effect 路径；保留其他输入原流程。跳转目标须为0x1556b4，不能绕过比较后直接进入依赖NZCV的0x1556a8/csel。

数码变焦分段可先查设备 /odm/etc/camera/xiaomi/mivistreamformat.json：Role23 的 FullSize 配置为7.5–9.99999×，ISZ QuadRaw为10–120×，并有增益/亮度条件。本次7.7×实际FullRemosaic对应调校sensor12，10×及以上实测ISZ QuadRaw对应sensor16；这些编号属于调校查询，不是物理镜头ID。主图与增益图需分别追踪，后者51/105分支不能由主图36/31或22/31的匹配情况代替。

相邻照片复用IPE模块时可能不再调用GetModule；应在真正消费缓存对象的入口读取SID，并按本次ISP/request/query归属。Session环形表有pipeline列（每列8字节、每行0x90、64行），关联ChiFrame时必须结合pipeid，不能只读pipe0列。

### 同倍率 SR / HDRSR 调校分支

2× 的 ISZ 输入可在 RawSR 与 HDRSR 之间切换；前者主图36/31、增益图51/105，后者主图24/31、增益图51/106。只覆盖倍率或 sensor 编号会漏掉另一组 feature 查询。修复需同时处理 MIVI/GS 的精确 effect 桥和对应私有调校注册，不能由其中一张图推断另一张图。sensormodule 的 resolutionData 模式编号可与 CHI GetSensorModeIndex → FillDefaultTuningMetadata 的 sensor 字段交叉核对；不同算法不会在该填表函数里重新编号。


## 2026-11-08 原色 2.0.59 覆盖结论与后续范围

2.0.59 已制作：四份调校及 MIVI/GS 路由新增夜景 AI Scene、夜景/夜间运动 Feature2、夜景 gain108/110、普通 Raw HDR 36/86 与 gain107，并为已列查询的 effect24/36/37 建立私有 effect78/79/80。原算法、Sensor、Scene 和 QuickShot 选择逻辑保留。离线已核对已接入绑定的作者五类色调及非色调模块选择；这些结论不替代设备运行结果。

继续对照生成代码后，剩余可具体指出的查询缺口：HDR/MFHDR 的 11/30、11/48；超广角 Raw HDR 部分曝光帧的 36/61、36/48；RawSR/HDRSR 与 effect36 的交叉组合，包括 24/31、51/105、51/106，以及主摄 Sensor53 的 36/31。AINR/闪光、高像素、多摄融合、人像/景深扩展、QuickShot 转单帧 B2Y 的最终查询仍需系统核对。2.0.58 推迟的分支和 2.0.59 新增分支均保留后续真机复测。

用户随后反馈刷入本轮新包后相机无法打开，当前任务转为定位启动失败原因；先保留失败现场及安装身份，暂停继续扩展上述查询。设备故障原因与修复结果另按实际证据补充。


## 2026-11-08 原色 2.0.59 启动失败：重复调校对象耗尽映射

用户刷入后相机无法打开。真机六个实际加载文件哈希与2.0.59一致；provider 在启动调校加载阶段反复 SIGABRT，Scudo internal map failure (Out of memory)，CV/TDL triggerType::Load → LoadModule → ParameterSetManager::ReadSections。故障现场provider约65842条映射、VmRSS约4011428kB；系统MemAvailable约8137156kB，vm.max_map_count=65530。证据：D:/cam18pm/analysis/natural-2059-startup-20261108/。

根因是package_module278.py将每个新模块注册的末字段source SID都设成新SID。四镜头active roots从42113增至276740，实际不同的数据只增加80份，却新增234627个独立加载身份。原生 ReadSections RVA0x6fb2b0逐条Create/LoadModule；CV Create RVA0x985700通过source SID查找已创建对象，LoadModule对已加载对象仅增加引用。此前离线查询选择正确，但未覆盖原生对象加载数量。

修复位于打包器：保留每个mode注册；同名称、offset和size的新增记录复用已实际加载的canonical active-root SID。优先使用基线self-source active root；缺少时使用该组第一个实际新root作为canonical，后续记录引用它。不能把inactive作者记录作为已加载canonical。此修改不改变查询、色调数据和非色调模块选择，不修改native加载器或提高系统资源上限。修正版升2.0.60，保留2.0.59故障包和源码作为证据；本轮未自动安装修正版。


## 2026-11-08 原色 SR 场景 effect 与 HDR 曝光帧扩展

按用户要求继续补齐上一轮列出的三组：SR/HDRSR effect36 的 24/31、51/105、51/106，并将36/31的scene32接入；主摄Sensor53仅精确加入scene0下36/31、51/105、24/31、51/106的effect36。原0..19/35通用Sensor范围保持原样。普通HDR增加11/30、11/48，Raw HDR增加36/61、36/48，按已有生成代码保留曝光补偿及场景选择，并接入对应source effect0/24/36。新入口package_module280.py，版本2.0.61。

调校对象沿用2.0.60的canonical active-root source SID复用；原选择逻辑和非色调模块选择不变。本轮继续仅修改、离线核对和打包，不操作手机，不扩展AINR/融合/人像等后续模式。2.0.60的provider启动恢复尚无本会话安装复测结论。

## 2026-11-08 RawSR/HDRSR 人脸与普通闪光查询

Generic 的 SR/HDRSR helper 读取 ANDROID_STATISTICS_FACE_RECTANGLES，矩形数据 count>3 时写 Scene32；SR 处理还会把 effect 归0。仅登记 effect36 不会覆盖这条 source effect0 查询，GS 重建查询后也不会自动补上。2.0.62 在原 Sensor0..19/35 范围内精确增加36/31、24/31的Scene32/effect0；Sensor53另登记这两组Scene32的effect0/36，保持其余精确匹配范围。

普通拍照的 AIURFlash 在 OfflineBayer2Yuv 输出节点生成44/91，继承上游Scene/effect；UC1/F2=91不满足FlatDetected=1的effect37条件。新增44/91、Scene0/40/46、source effect0/36的作者色调路由与私有登记，避免把Flash与专用UltraRaw的39/40分支混淆。手机实际chiusecaseselector库的ZSLSnapshotYUVHAL及Bayer2YUVWithSWRemosaic均有BPS输出port1到IPE输入port0、IPE输出port8到Sink的链路；44/90的ZSLSnapshotFormatConvertor只有BPS输出port2到Sink，不消费这五类IPE色调，故不为44/90增加私有色调登记。

## 2026-11-08 普通拍照月亮 SR 查询

HAL 的 CameraMode::updateStatus 从结果 xiaomi.ai.asd.sceneApplied 写入场景状态；Generic 通过 ExtensionModule+0x1af0 读取的也是该 tag。其值35会关闭 Full/ISZ Remosaic，但保留普通SR资格。普通Photo模块163、SR开启、HDRSR关闭时，SR helper 对当前或app metadata中35生成UC1/8/31，source effect归0；GS的FlatDetected=2可重建为36。Night在SR前处理；后置UltraHDR仅重写linearFrame，非linear主图仍保留8/31，不能由增益图51/105的覆盖代替。

2.0.63在原Sensor0..19/35范围内补8/31、Scene0/32/40/46、source effect0/36的匹配及作者色调登记。Scene32来自SR的人脸矩形条件；此分支不扩大Sensor范围、不改变场景35的原选择逻辑。月亮关闭Full/ISZ，故本次不增加Sensor52/53/54的月亮登记。

## 2026-11-08 主摄16:9 Full RawSR的Sensor54漏接

在已安装2.0.61上按原色、HDR Auto拍摄三张：1×/4:3的current_mode与最终查询Sensor为9，主图36/86、gain51/107；1.5×/4:3为53，RawSR主图36/31、gain51/105；仅将比例改为16:9后，两图Sensor均变54，MIVI与GS最终effect仍为0。该请求通过SubmitRequest的frame17445关联到IMG_20261108_192438.jpg，证据保存在D:/cam18pm/analysis/natural-sensor52-54-live-20261108。最终Sensor优先读取org.codeaurora.qcamera3.sensor_meta_data.current_mode，不能仅由离线pipeline尺寸或默认mode推断。

根因是原色通用Sensor范围0..19/35及Sensor53精确表未包含54；Full的ratio0会沿用照片比例，16:9确实可生成54。2.0.64仅增加54下RawSR主图36/31的Scene0/32及gain51/105的Scene0，分别接source effect0/36；保留通用Sensor范围和原来的尺寸、算法、场景选择。人脸32及GS effect36沿已读生产逻辑登记，不将本轮自然选中的RawSR扩大为所有HDRSR组合，也不由此次结果直接登记Sensor52。

## 2026-11-08 Sensor54的HDRSR与最后一次调校写入

现场已安装2.0.64。原色1.5×、16:9、原生HDR开启且HDR检测成立、运动及温度门控均为0时，自然进入HDRSR；SrHdrPhotographer::postUpdateReqMeta命中5次，最终Sensor54/UC1的主图24/31及gain51/106仍为effect0，对应IMG_20261108_195706.jpg、frame3582。4:3对照IMG_20261108_200058.jpg同为HDRSR但Sensor53，主图及gain已有effect76。证据：D:/cam18pm/analysis/natural-hdrsr52-live-20261108。

2.0.65精确补Sensor54下24/31的Scene0/32及51/106的Scene0，分别接source effect0/36；其余选择逻辑与Sensor范围保持不变。采集中还见同一metadata/qptr先出现24/62、约5微秒后立即改为24/31，与SR helper后接HDRSR helper的顺序一致，提交前已被覆盖；不能把每次FillTuningModeData返回都当作该节点最终消费的查询而增加中间62的色调登记。

## 2026-11-08 HDRSR人脸增益图的独立调校登记

Sensor53的HDRSR主图Scene32已接入，但现场的gain查询也保留Scene32。仅确认gain最终effect76仍会遗漏数据登记：查询(0,53,1,51,106,32,76)在2.0.64/65选到的CC、CV、TDL仍为stock，Gamma和LTM已与作者一致。2.0.66精确增加该查询source effect0到76的绑定，复用作者五类IPE模块及已有canonical source SID，不扩大Sensor或算法选择范围。

追Sensor52时，真实Full snapshot调用链是RTMCX::OnSelectSensorMode(0x5a7b0)在0x5d0d4调用Base，随后FindBestSensorMode；预览默认选择和GS初始化候选图均不能代替这条请求链。现场CaptureIntent=2、SR=1、remosaic.enabled=1、IszQuadRawEnabled=0，目标物理pipeline匹配后，RTMCX在0x5c9ac设置QCFA+QNoRemosaic、remtype2、pixels4；选择器实际比较52/53，52缺少QNo能力而拒绝，53接受。证据目录D:/cam18pm/analysis/natural-sensor52-selector-20261108。此路径不应通过强制52或改capability来补原色。

## 2026-11-08 Full输入的RawSR/HDRSR人脸增益图

人脸主图的Scene32覆盖需要同步检查gain51/105、51/106。2.0.66只登记53下51/106的source effect0；53的51/105及54的两类gain仍缺Scene32，53/54的source effect36交叉组合也缺独立登记。2.0.67将53/54、gain105/106、Scene32、source effect0/36组成的八条精确绑定补齐，其中七条是新增。新增项的CC/CV/TDL此前落回stock，Gamma/LTM已经与作者一致；仍按完整五类IPE色调登记，并复用已有canonical source SID。Sensor范围、Scene32生成条件、算法选择和非色调模块选择均沿用原路径。

## 2026-11-08 原色夜景回归：SND不支持风格输入2

2.0.67暗光实拍：生动Photo163和Night173分别选择RawSupernight并构造7/9帧；原色两个模式的夜景能力及请求参数有效，但HAL结果xiaomi.supernight.checker的sum为0，canCheckerValDoSuperNight拒绝，改走其他拍摄器。AIASD调用ARC_SND_GetEVList时将会话stylizationType2原样作为inputMode；当前SND库只接受0/1，输入2返回状态2，预先清零的帧列表被继续发布。现场连续38次证实2→返回2→sum0；生动对照inputMode0返回0。Sensor4/5差异是夜景接受后RTMCX加bit33选模式的下游结果，不应强制切Sensor修复。

历史夜景桥被package_natural_entry_only.py连同激活脚本和默认标记移除；package_natural_tuning.py仅恢复natural-payload六文件，后续调校扩展没有恢复夜景桥。旧N3桥还只覆盖Photo163/op8001和Night173/op800a，并映射2→1；本轮实测当前普通Photo为op9002，Night为800a，生动SND合法输入为0。

2.0.68复用历史单次调用桥的ABI保存方式，在后摄role0..3、有效App module163/173及对应operation组合下，把此SND调用的输入2转换为0。普通Photo保留8001并增加本轮9002，Night仍限800a；会话风格2、XML配置、night trigger、相机模式选择及后续作者调校均保留。arcsoft-night-activate.sh和默认标记恢复到启动激活链。证据目录D:/cam18pm/analysis/natural-night-2067-20261108。

## 2026-11-08 普通后摄RawSR/HDRSR人脸增益图的镜头范围

2.0.68实际请求确认主摄2×的Sensor8、3.2×和5×长焦的Sensor1均能生成RawSR/HDRSR增益图51/105、51/106的Scene32/effect76。长焦HDRSR补测照片为IMG_20261108_232300.jpg和IMG_20261108_232340.jpg，EXIF等效焦距分别75mm、120mm；实际主图24/31的五类对象匹配作者，增益图51/106的CC/CV/TDL仍为原厂数据。仅检查effect76会漏掉这些数据登记。

2.0.69按镜头文件精确增加上述三组Sensor各自的105/106、Scene32、source effect0到76绑定，共六条。现有路由已实际提交effect76，因此只补三份调校的数据登记，MIVI、GS和超广角调校保持原载荷。新增记录继续复用canonical active-root source SID。package_module288.py以2.0.68为运行基线，保留已恢复的夜景载荷和激活链。证据目录D:/cam18pm/analysis/natural-gain32-2069-20261108。

## 2026-11-09 MFNR阶段与5×高倍率人脸增益图

MFNR的36/52只覆盖自身处理阶段；MFSR后续按stage将Feature2重写为13，最终36/13和增益图51/109均进入IPE色调消费者。已有主摄Sensor4与超广角Sensor1人脸Scene32查询中，36/13五项与作者不同、51/109的CC/CV/TDL不同。主摄两个查询仍带effect0，超广角已带76。2.0.70按镜头补四条数据登记，MIVI/GS仅增加主摄Sensor4两个source effect0精确查询到76的匹配，原Sensor、Scene、算法和各阶段Feature值均保留。

在已安装2.0.69上补拍8×/10×、3:4/16:9、HDR关闭/自动触发共八张：8×分别选Full Sensor12/13，10×分别选ISZ Sensor16/17；关闭HDR走36/31与gain51/105，HDR成立走24/31与gain51/106，均保留Scene32/effect76。实际下游主图五类均匹配作者，八组gain的CC/CV/TDL仍为原厂。2.0.70精确补这八条gain数据登记，已有source36到79登记保持。24/62随后被24/31覆盖，仍不为该中间查询新增色调登记。证据目录D:/cam18pm/analysis/natural-full-isz-2070-20261109。

本轮合并共十二条按镜头数据登记；增量构建以2.0.69为基线，仅重新生成这些登记及两个MFNR路由，保留夜景载荷与激活链，新增模块注册继续复用canonical active-root source SID。

## 2026-11-09 摄影风格选择后的弹窗归属

普通拍照顶部摄影风格三选项实际由`p5.u`的独立弹窗承载，选项组为`top_choice_popup_group`（1770资源ID `0x7f0b0b62`），并非`CvTypeSecondMenu`。Compat 0.1.96按后者查找父控件，用户选择后无法进入关闭分支。修复为在原生选项触摸/无障碍点击处理完成后，识别实际选项组并调用`p5.u$a.a()`原生关闭入口；不要另加延时、模拟空白点击或保留错误菜单作为fallback。同一风格再次点击也通过实际选项控件完成关闭。

原生`r2.m.p(String)`及1770中文资源明确对应`0=生动、1=经典、2=原色`，会话风格值与界面顺序不能混用。顺序切换应为`2→0→1→2`。此前`2→1→0→2`会交换生动、经典次序；按原生文案映射修正即可，不调整原生风格事务。

## 2026-11-10 摄影风格切换的模式范围

摄影风格优化曾同时把点击事务和选项关闭限制为 mode163。原生 r2.m 按活动模式维护可用 items、存储 key、关闭状态及 RAW/UltraRAW 互斥理由；X9.i0.k 为实际活动模式，R6.D.qd 交给当前模块处理。跨模式时使用 r2.m.m(currentMode) 计算下一项，清除互斥和重新打开也传当前模式，不能继续写死163或假定所有模式共用存储 key。顺序切换沿原生 getDisableReasonString 保留格式限制，选择后关闭仍只识别实际 top_choice_popup_group 内的 enabled 选项。

## 2026-11-09 相册编辑 2.11 的动态契约与原生云水印

2.11.10.0.3 将原来保留名称的 ViewModel、设置 Fragment、云水印数据类及部分 AndroidX/miuix 声明一起混淆，并重排滤镜分类/滤镜项构造参数、给 FilterData 增加 styleParams。沿用旧类名锚点会在安装 Hook 前解析失败。保留旧 DEX 契约，增加新版参考契约与逻辑符号别名，继续用结构指纹、调用边及构造字段角色在实际 APK 中解析；AndroidX/miuix 包内的短混淆名也参与名称归一化。新版真实构造参数、LiveData 声明 owner 和设置 Preference 的内联属性按消费者适配。

生成契约时，只有 PhotoShopApp.attachBaseContext 的实际 typed sput 所发布的上下文持有类需要裁去无关工具方法。不能把所有含静态 PhotoShopApp 字段的类都判为上下文持有类，否则会错误裁去本地滤镜目录方法。

新版 g60.i1.a 从字符串输入进入 CloudWatermarkFilterBridge；libeditor_config_native.so 在 JNI_OnLoad 检测 XposedBridge/liblspd，检测状态分别阻断 nativeQuery 和云水印 filter/unwrap。LSP 在这些 JNI 调用前通过独立桥库，按加载 ELF 的 ADRP/LDRB/TBZ/zero-return 指令关系定位两个环境状态字节并恢复；原生解码、过滤、时间/机型和渲染实现继续执行，不替换过滤返回值。运行定位不使用固定原生地址。新版产品解析只保留 series/versionName，Ti 使用其两个真实节点；Logo 文件加载新增 ColorSpace 参数，替换品牌图须保留该颜色空间。

新版本地水印目录已无旧 jb.a 三个无参机型 gate 对应的 car 创建分支；旧 car 缺项恢复只用于旧契约，2.11 沿其原有云目录处理。静态和本机离线接口核对不替代真机预览、保存与用户验收。


## 2026-11-09 徕卡云目录的机型限制

旧版 catalog 内强制 LCC capability 与渲染前 eligibility 放行只覆盖部分路径；CloudWatermarkData.supportDeviceList 中的具体机型（如水印2限定 xuanyuan）仍会在目录阶段剔除。2.11 的目录还先经过 CloudWatermarkFilterBridge.nativeFilter，等解析返回对象后再处理会错过被筛掉的条目。应在目录输入处仅处理 leica 分组的 groupSupportProduct、supportDeviceList、unSupportDeviceList、restrictPhotoList；旧版复制实际配置对象，新版处理原生接口接收的加密封装，再继续原生筛选、资源加载及渲染，保留必要拍摄参数、时间、版本与地区规则。2.11 的实际入参来自 EncryptedItem.encryptedContent，不能直接按 JSONObject 解析；其格式为 Base64 的 Content protobuf（字段1密文、字段2 seed），解码变换只对每个 UTF-8 码点的末字节 XOR(seed & 63)。外层 JSON.content 才是水印配置。改动后使用原 seed 重新封装，并保留其他 protobuf 字段；原生 cc_text_load 和宿主 fc.a/TextLoader 可交叉核对该协议。

旧2.4与新2.11的 CloudFilterItem 均保留 lcc_support_devices 机型检查；同一云滤镜配置的徕卡 category4 包含12个这样的白名单条目。仅将这些条目的该白名单 token 替换为通配符；不要全局改设备判断，否则本地徕卡目录会切换到另一套分支。


## 2026-11-09 相册编辑新功能名单与调色盘消费者

2.11 的滤镜调色盘使用 bt.a.d 数组；qz.g 条目入口与 uz.d.V 状态切换共同检查 cq.c.b(该数组)。AI构图使用 at.d 的两个集合，图生视频使用 at.a.a 集合，AI助手使用 at.b.a 集合。只扩展功能自己的设备集合即可保留各自地区、系统和服务条件，运行符号继续由目标 DEX 契约解析。

“更多”相机配方在 EditorMore246Hooks 的 renderBitmap 接管中提前返回，原先没有消费 FilterData.entryStyle。调色盘开放后，需要读取实际 FilterStyleParams.a() 输出，执行同一 StyleAdjustEffect，按官方 Bitmap 路径处理原始 RGBA，保留宿主色彩空间与 gainmap。不能只显示面板或保存条目状态。

AI助手真正使用小爱的 com.aios.osbot.external.CliCommandService / ICliCommandService；打包的 ExternalAgentClient 不是当前自然语言修图调用者。其入口另检查 ro.mi.os.version.code>=4。AI构图资源包11提供三个入口库，实际构图时还动态加载 libQnnHtp.so；缺少它会在 nativeCompose 返回ERROR，不能由 nativeInit 成功推断构图成功。


## 2026-11-09 相机配方滤镜的调色盘响应

更多组接管额外添加的逐帧 Canvas P3→sRGB→P3 往返，耗时远高于实际 Candy 处理。2.11 的 CandySDK.processBitmap JNI 只接收 RGBA_8888 的原始像素地址、宽高与 ROI，不读取 ColorSpace 或 gainmap；其 AHB 上传、输入 shader、下载也不转换此 Bitmap 路径的色域。应沿官方原始 RGBA 路径，保留宿主 Bitmap 的颜色空间与 gainmap；相机空间 shader 的中间 Bitmap 也须沿用源颜色空间，不能默认创建 sRGB 后复制 P3 字节。

官方 CandyAlgorithm 在选定滤镜时配置一次 SDK/effect chain/LUT，拖盘只 updatePipeline 后 processBitmap。configPipeline 在已有 chain 上不更新参数，updatePipeline 也不能新增或删除 effect，因此按配方身份、不可变 LUT 路径和 effect 名称顺序决定重建；参数更新复用现有实例。无自定义空间 shader 且强度100的配方，可把 LUT、后置效果和调色盘放进同一条管线。中间强度仍先混合配方再调盘；强度0独立调盘。新版本在初始链中保留零参数 StyleAdjustEffect，避免首次拖盘时动态增加 effect。

连续拖动在原生 PhotoFilterFunctionFragment 中已有 buffer(1, DROP_OLDEST)，并等待渲染结果再处理后续更新，无需另加固定延时或重复队列。
