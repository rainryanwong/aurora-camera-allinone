#!/system/bin/sh
SKIPUNZIP=0
ui_print "========================================"
ui_print " Xiaomi 14 Ultra（6.8相机移植） 2.0.31"
ui_print " Camera + Palette + M3 + AI + LiveMotion; no W1/F13/G123 extensions"
ui_print "========================================"
[ "$(getprop ro.product.odm.device)" = aurora ] || abort "仅支持小米14Ultra aurora"
file_sha() { sha256sum "$1" 2>/dev/null | cut -d ' ' -f 1; }
ui_print "- [1/8] 核对整合包完整性"
(cd "$MODPATH" && sha256sum -c runtime.sha256) > "$MODPATH/install-audit.log" 2>&1 || abort "整合包文件校验失败，请重新下载"
# Installation does not depend on a ROM hash allowlist or manager branding.
# Native activators keep their own compatibility/ownership guards at boot.
ui_print "- APK安装不要求ROM SHA、当前前台用户或Root管理器品牌匹配"
ui_print "- 原生M3/色盘/原色仍在启动时独立检查兼容性；不匹配会跳过并记录日志"
ui_print "- 核对LSPosed自动配置能力（只读预检）"
/system/bin/sh "$MODPATH/lsp-setup.sh" check >> "$MODPATH/install-audit.log" 2>&1 || abort "当前LSPosed配置结构不支持自动设置；APK尚未覆盖，详见install-audit.log"
EDITOR_INSTALLED=$(pm path --user 0 com.miui.mediaeditor | sed -n 's/^package://p' | head -n 1)
[ -n "$EDITOR_INSTALLED" ] || abort "未找到系统主用户的相册编辑器com.miui.mediaeditor，无法设置M3编辑作用域"
ui_print "- 原色范围：普通拍照、AI帮拍、夜景、专业JPEG与普通街拍；按镜头和处理参数选择，不限制固定倍率；RAW/UltraRAW、50MP原色与动图另行验证"
ui_print "- 请先禁用上游NaturalTuning KSU/LSP并重启，不可同时挂载四镜头替换调校"
ui_print "- M3 Core: disable independent Aurora Phoenix Watermark/Filters in LSPosed before use"
ui_print "- M3 Bridge, LiveMotion and CameraCompat will be enabled for their required user-0 scopes"
set_perm_recursive "$MODPATH" 0 0 0755 0644
for f in lsp-setup.sh natural-activate.sh root-compat.sh customize.sh service.sh palette-activate.sh post-fs-data.sh m3-relay.sh action.sh account.sh restore.sh uninstall.sh; do
  set_perm "$MODPATH/$f" 0 0 0755
done
set_perm_recursive "$MODPATH/auth239" 0 0 0755 0644
for f in post-fs-data.sh service.sh formula-event.sh init-formula.sh init-photo-formula.sh sync-formula.sh; do
  set_perm "$MODPATH/auth239/$f" 0 0 0755
done
set_perm_recursive "$MODPATH/system/vendor/lib64" 0 0 0755 0644 u:object_r:vendor_file:s0
set_perm_recursive "$MODPATH/payload" 0 0 0755 0644 u:object_r:vendor_file:s0
set_perm "$MODPATH/bin/m3_aux_relay" 0 0 0755
set_perm "$MODPATH/bin/natural-remount-ro" 0 0 0755
# All four APKs share the same staging, update and readback contract.
install_apk() {
  local source=$1 package=$2 staged_name=$3 label=$4 expected stage_dir output rc installed
  expected=$(file_sha "$MODPATH/$source")
  [ "${#expected}" = 64 ] || abort "无法读取${label}APK摘要；此前APK可能已更新，本次刷入未完成"
  stage_dir=$(mktemp -d /data/local/tmp/camera68-install.XXXXXXXX 2>&1) || abort "无法创建${label}暂存目录：$stage_dir；此前APK可能已更新，本次刷入未完成"
  if ! chmod 0755 "$stage_dir" || ! cp "$MODPATH/$source" "$stage_dir/$staged_name" || ! chmod 0644 "$stage_dir/$staged_name"; then
    rm -f "$stage_dir/$staged_name"
    rmdir "$stage_dir"
    abort "暂存${label}失败；此前APK可能已更新，详见install-audit.log"
  fi
  output=$(pm install -r --user 0 "$stage_dir/$staged_name" 2>&1)
  rc=$?
  rm -f "$stage_dir/$staged_name"
  rmdir "$stage_dir"
  printf '%s\n' "$output" >> "$MODPATH/install-audit.log"
  if [ "$rc" -ne 0 ] || ! printf '%s\n' "$output" | grep -qx Success; then
    ui_print "$output"
    abort "${label}覆盖安装失败；此前APK可能已更新，请检查CorePatch与安装限制，不要卸载相机"
  fi
  installed=$(pm path --user 0 "$package" | sed -n 's/^package://p' | head -n 1)
  [ -n "$installed" ] && [ "$(file_sha "$installed")" = "$expected" ] || abort "${label}安装后APK缺失或哈希不一致；本次刷入未完成"
  INSTALLED_APK=$installed
}
ui_print "- [2/8] 安装/更新M3保存桥"
install_apk m3-save-editor.apk com.prometheus.camera.m3v16save m3.apk M3保存桥
LSP_INSTALLED=$INSTALLED_APK
ui_print "- [3/8] 安装/更新实况运镜0.5.0"
install_apk live-motion.apk com.aurora.camera.livemotion live-motion.apk 实况运镜
LIVE_INSTALLED=$INSTALLED_APK
ui_print "- [4/8] 安装全功能原包适配LSP"
install_apk camera-compat.apk com.aurora.camera.compat camera-compat.apk 原包适配
COMPAT_INSTALLED=$INSTALLED_APK
ui_print "- [5/8] 恢复原版6.8.001770.0相机，保留应用数据"
ui_print "- 原版与改包签名不同；仍需现有CorePatch允许覆盖更新，失败时中止，不卸载相机"
install_apk camera.apk com.android.camera camera.apk 原版相机
ui_print "- 四个APK已安装并回读；随后自动配置启用和作用域"
ui_print "- 实况运镜原同包名独立模块会覆盖更新，不重复嵌入hooks"
ui_print "- 不叠加Q0Fix等同类运镜hooks；不要禁用必要的M3保存桥"
ui_print "- [6/8] 自动启用M3 Bridge、实况运镜和原包适配，并勾选对应作用域"
/system/bin/sh "$MODPATH/lsp-setup.sh" apply "$LSP_INSTALLED" "$LIVE_INSTALLED" "$COMPAT_INSTALLED" >> "$MODPATH/install-audit.log" 2>&1 || abort "LSP自动设置失败；APK可能已更新，详见install-audit.log，本次模块刷入未完成"
ui_print "- 已保存三个LSP的启用与作用域；重启后加载，无需手动勾选"
ui_print "- [7/8] 原生载荷已打包；重启后兼容检查通过才会加载，详见mount/activation日志"
ui_print "- AUTHVignette 已整合；请禁用独立 AUTHVignette，重启后核对 auth239 日志"
ui_print "- [8/8] 自动获取小米账号UID，读取并授权相机可见性"
if /system/bin/sh "$MODPATH/account.sh" auto >> "$MODPATH/install-audit.log" 2>&1; then
  ui_print "- 账号任务完成，原值已为1时无需修改"
else
  ui_print "- 账号任务暂未完成；重启解锁后自动重试，登录后也可点模块操作按钮"
fi
ui_print "- 四个APK安装与回读校验完成，请自行重启并解锁"
ui_print "- 完整模块离线核验后交测试窗口验证；本脚本不会自动重启"
ui_print "- 仅设置本包三个LSP的启用及作用域；不改其他模块、用户或Root/DenyList设置"
ui_print "- 已登录单个小米账号时无需MT终端步骤"
ui_print "- M3作用域已自动设置为相机和相册编辑；M9/M10R卡片保留显示且点击不切换"

ui_print "- Natural ArcSoft N3 retained: rear photo163 auto-night + night173 enabled after reboot"
