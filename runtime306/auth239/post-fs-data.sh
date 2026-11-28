#!/system/bin/sh
MODDIR=${0%/*}
exec >"$MODDIR/mount.log" 2>&1
set -eu
fail() { echo "ERROR: $*"; touch "$MODDIR/mount_failed"; exit 1; }
if [ -d /data/adb/modules/phoenix_auth_vignette ] && [ ! -f /data/adb/modules/phoenix_auth_vignette/disable ] && [ ! -f /data/adb/modules/phoenix_auth_vignette/remove ]; then
    fail 'Standalone AUTHVignette is active; disable that duplicate component before using this integrated version'
fi
# Same signed Compat APK is installed by the existing All in one setup flow.
grep -q '^com.aurora.camera.compat ' /data/system/packages.list || { echo 'Compat is not installed; AUTH photo library left unchanged'; exit 0; }
. "$MODDIR/../root-compat.sh"
root_init || fail 'root tools unavailable'
rm -f "$MODDIR/mount_failed"
sh "$MODDIR/init-photo-formula.sh" || fail 'initialize photo formula'
LIB="$MODDIR/payload/libMiPhotoFilter.so"
TARGET=/odm/lib64/libMiPhotoFilter.so
[ -f "$TARGET" ] || fail 'stock MiPhotoFilter is missing'
if root_same_file "$LIB" "$TARGET"; then
    echo 'AUTH photo library already mounted'
    exit 0
fi
chcon "$(/system/bin/stat -c %C "$TARGET")" "$LIB" || fail 'label AUTH photo library'
"$BB" mount -o bind "$LIB" "$TARGET" || fail 'mount AUTH photo library'
mount_id=$(root_mount_id "$TARGET")
if ! root_same_file "$LIB" "$TARGET"; then
    root_unmount_owned "$TARGET" "$mount_id" "$TARGET" || echo 'ROLLBACK FAILED: AUTH photo mount'
    fail 'verify AUTH photo mount'
fi
echo 'AUTHVignette photo library mounted; formula service readiness is separate'
