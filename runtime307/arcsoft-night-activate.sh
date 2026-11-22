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
STOCK_BODY=f79400f8fba1334676cd4c13683954c72a562f5b048953f40b0841209476a7cb
EXPECTED=30826d7aa3c620305cbf1468e01ba7d783c8e307b30a5a17e4f3cac0440c65a6
[ -z "$(pidof vendor.qti.camera.provider-service_64)" ] || fail "provider running; no live replacement"
[ "$(elf_body_sha "$TARGET" 776 16)" = "$STOCK_BODY" ] || fail "incompatible original aiASD library"
[ "$(file_sha "$SRC")" = "$EXPECTED" ] || fail "payload hash mismatch"
"$MODDIR/bin/odm-compat" arcsoft-night /odm/lib64/libarcsoft_super_night_detection.so ||
    fail "ArcSoft exposure-sequence interface unavailable"
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
