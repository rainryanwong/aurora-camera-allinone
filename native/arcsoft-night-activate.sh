#!/system/bin/sh
# Enable Natural night input compatibility before provider startup.
MODDIR=${0%/*}
exec >"$MODDIR/arcsoft-night-activation.log" 2>&1
[ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || exit 0
[ -f "$MODDIR/enable-natural-arcsoft-night" ] || exit 0
fail() { echo "FAILED: $*"; exit 1; }
file_sha() { /system/bin/sha256sum "$1" | /system/bin/cut -d ' ' -f 1; }
[ "$(getprop ro.product.odm.device)" = aurora ] || fail "not aurora"
. "$MODDIR/root-compat.sh" || fail "root helper missing"
root_init || fail "root tools unavailable"
root_require_applets mount umount awk || fail "mount tools missing"
case "$ROOT_MANAGER" in
    kernelsu|apatch) RESETPROP=${BB%/*}/resetprop ;;
    magisk) RESETPROP=$(command -v resetprop) ;;
esac
[ -n "$RESETPROP" ] && [ -x "$RESETPROP" ] || fail "manager resetprop unavailable"
TARGET=/odm/lib64/camera/components/com.mi.node.aiasd.so
SRC="$MODDIR/arcsoft-night-payload/com.mi.node.aiasd.so"
STOCK=618c38bb516208a3733a0a399ebcf61ad7c2b6b65c6baa91c4d2b7d5566ccc57
EXPECTED=@AIASD_SHA256@
[ -z "$(pidof vendor.qti.camera.provider-service_64)" ] || fail "provider running; no live replacement"
[ "$(file_sha "$TARGET")" = "$STOCK" ] || fail "source library changed or foreign mount"
[ "$(file_sha "$SRC")" = "$EXPECTED" ] || fail "payload hash mismatch"
[ "$(file_sha /odm/lib64/libarcsoft_super_night_detection.so)" = 9911f32830140006688d1e9ef7f50ee9ece97c1384d53b33d399bee6fb528e27 ] || fail "ArcSoft dependency changed"
STAGE=/dev/aurora_natural_night
[ ! -e "$STAGE" ] || fail "stage already exists"
mkdir "$STAGE" || fail "create stage"
chmod 0755 "$STAGE" || fail "stage permissions"
chcon u:object_r:vendor_file:s0 "$STAGE" || fail "stage context"
cp "$SRC" "$STAGE/com.mi.node.aiasd.so" || fail "stage payload"
chmod 0644 "$STAGE/com.mi.node.aiasd.so" || fail "file permissions"
chcon u:object_r:vendor_file:s0 "$STAGE/com.mi.node.aiasd.so" || fail "file context"
[ "$(file_sha "$STAGE/com.mi.node.aiasd.so")" = "$EXPECTED" ] || fail "staged hash mismatch"
mount_id() { "$BB" awk -v t="$TARGET" '$5==t {id=$1} END {print id}' /proc/self/mountinfo; }
OWNED=
COMMITTED=0
cleanup() {
    [ "$COMMITTED" = 0 ] || return 0
    if [ -n "$OWNED" ] && [ "$(mount_id)" = "$OWNED" ]; then
        "$BB" umount "$TARGET" || echo "ROLLBACK FAILED: $TARGET"
    fi
    "$RESETPROP" -n vendor.debug.camera.natural_arc_sn 0 || echo "ROLLBACK FAILED: reset property"
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM
[ -z "$(pidof vendor.qti.camera.provider-service_64)" ] || fail "provider started during staging"
BEFORE=$(mount_id)
"$BB" mount -o bind "$STAGE/com.mi.node.aiasd.so" "$TARGET" || fail "bind payload"
OWNED=$(mount_id)
[ -n "$OWNED" ] && [ "$OWNED" != "$BEFORE" ] || fail "mount identity missing"
"$MODDIR/bin/natural-remount-ro" "$TARGET" || fail "readonly remount"
[ "$(file_sha "$TARGET")" = "$EXPECTED" ] || fail "mounted hash mismatch"
"$BB" awk -v t="$TARGET" '$5==t {n=split($6,a,","); for(i=1;i<=n;i++) if(a[i]=="ro") ok=1} END {exit !ok}' /proc/self/mountinfo || fail "mount not readonly"
[ -z "$(pidof vendor.qti.camera.provider-service_64)" ] || fail "provider started before commit"
"$RESETPROP" -n vendor.debug.camera.natural_arc_sn 1 || fail "enable property"
[ "$(getprop vendor.debug.camera.natural_arc_sn)" = 1 ] || fail "enable property not effective"
COMMITTED=1
echo "natural_night=active"
