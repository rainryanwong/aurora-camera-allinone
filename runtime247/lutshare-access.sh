#!/system/bin/sh
# One-shot installation configuration using MIUI's persistent user-policy API.
# AppOps alone is transient and was reset after a package update on the target ROM.
MODDIR=${0%/*}
case "$1" in apply|restore|check) :;; *) echo 'Expected apply, restore or check' >&2; exit 2;; esac
[ -f "$MODDIR/camera-compat.apk" ] || { echo 'Packaged Compat APK is missing' >&2; exit 1; }
umask 077
CLASSPATH="$MODDIR/camera-compat.apk" /system/bin/app_process /system/bin com.aurora.camera.compat.install.MiuiLutAccess247 "$1"
