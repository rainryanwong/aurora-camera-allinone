#!/system/bin/sh
SKIPUNZIP=0
ui_print "========================================"
MODULE_VERSION=$(sed -n 's/^version=//p' "$MODPATH/module.prop")
ui_print " Xiaomi 14 Ultra（6.8相机移植） $MODULE_VERSION"
ui_print "========================================"
[ "$(getprop ro.product.odm.device)" = aurora ] || abort "仅支持小米14Ultra aurora"
file_sha() { sha256sum "$1" 2>/dev/null | cut -d ' ' -f 1; }
ui_print "- [1/8] 核对整合包完整性"
(cd "$MODPATH" && sha256sum -c runtime.sha256) > "$MODPATH/install-audit.log" 2>&1 || abort "整合包文件校验失败，请重新下载"
# Installation does not depend on a ROM hash allowlist or manager branding.
# Native activators keep their own compatibility/ownership guards at boot.
ui_print "- 检查 LSPosed 配置"
/system/bin/sh "$MODPATH/lsp-setup.sh" check >> "$MODPATH/install-audit.log" 2>&1 || abort "LSPosed 配置检查失败；尚未安装 APK，详见 install-audit.log"
EDITOR_INSTALLED=$(pm path --user 0 com.miui.mediaeditor | sed -n 's/^package://p' | head -n 1)
[ -n "$EDITOR_INSTALLED" ] || abort "未找到系统主用户的相册编辑器（com.miui.mediaeditor），请先安装后重试"
ui_print "- 使用前请禁用独立 NaturalTuning 模块及其 LSP，并重启"
ui_print "- 请禁用独立 Aurora Phoenix Watermark/Filters，避免重复加载"
set_perm_recursive "$MODPATH" 0 0 0755 0644
for f in lsp-setup.sh lutshare-access.sh natural-activate.sh root-compat.sh customize.sh service.sh palette-activate.sh post-fs-data.sh m3-relay.sh action.sh account.sh restore.sh uninstall.sh; do
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
  [ "${#expected}" = 64 ] || abort "无法读取${label}APK摘要；前面已完成的 APK 安装会保留，本次刷入未完成"
  stage_dir=$(mktemp -d /data/local/tmp/camera68-install.XXXXXXXX 2>&1) || abort "无法创建${label}暂存目录：$stage_dir；前面已完成的 APK 安装会保留，本次刷入未完成"
  if ! chmod 0755 "$stage_dir" || ! cp "$MODPATH/$source" "$stage_dir/$staged_name" || ! chmod 0644 "$stage_dir/$staged_name"; then
    rm -f "$stage_dir/$staged_name"
    rmdir "$stage_dir"
    abort "暂存${label}失败；前面已完成的 APK 安装会保留，详见install-audit.log"
  fi
  output=$(pm install -r --user 0 "$stage_dir/$staged_name" 2>&1)
  rc=$?
  rm -f "$stage_dir/$staged_name"
  rmdir "$stage_dir"
  printf '%s\n' "$output" >> "$MODPATH/install-audit.log"
  if [ "$rc" -ne 0 ] || ! printf '%s\n' "$output" | grep -qx Success; then
    ui_print "$output"
    abort "${label}覆盖安装失败；前面已完成的 APK 安装会保留，请按上方系统错误处理后重试，不要卸载相机"
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
ui_print "- [4/8] 安装/更新原包适配模块"
install_apk camera-compat.apk com.aurora.camera.compat camera-compat.apk 原包适配
COMPAT_INSTALLED=$INSTALLED_APK
ui_print "- [5/8] 安装原版 6.8.001770.0 相机，保留应用数据"
ui_print "- 若当前相机签名不同，需 CorePatch 允许覆盖安装；请勿卸载相机"
install_apk camera.apk com.android.camera camera.apk 原版相机
ui_print "- [6/8] 配置 M3 保存桥、实况运镜和原包适配的 LSPosed 作用域"
/system/bin/sh "$MODPATH/lsp-setup.sh" apply "$LSP_INSTALLED" "$LIVE_INSTALLED" "$COMPAT_INSTALLED" >> "$MODPATH/install-audit.log" 2>&1 || abort "LSPosed 配置失败；四个 APK 已安装，模块刷入未完成，详见 install-audit.log"
ui_print "- 已保存三个LSP的启用与作用域；重启后加载，无需手动勾选"
/system/bin/sh "$MODPATH/lutshare-access.sh" apply >> "$MODPATH/install-audit.log" 2>&1 || abort "滤镜共享后台权限配置失败；四个 APK 已安装，模块刷入未完成，详见 install-audit.log"
ui_print "- [7/8] 原生组件已就绪，重启时按各自兼容条件加载"
ui_print "- 请禁用独立 AUTHVignette 和 Q0Fix 等重复功能模块"
ui_print "- [8/8] 设置相机的小米账号访问权限"
ACCOUNT_OUTPUT=$(/system/bin/sh "$MODPATH/account.sh" auto 2>&1)
ACCOUNT_RC=$?
printf '%s\n' "$ACCOUNT_OUTPUT" >> "$MODPATH/install-audit.log"
if [ "$ACCOUNT_RC" = 0 ]; then
  ui_print "- $(printf '%s\n' "$ACCOUNT_OUTPUT" | tail -n 1)"
else
  ui_print "- 账号权限暂未设置；重启解锁后会重试，也可登录后点击模块操作按钮"
fi
ui_print "- 安装完成，请重启并解锁手机"
