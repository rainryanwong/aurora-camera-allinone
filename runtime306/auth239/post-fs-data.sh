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
STOCK_BODY=be77a4987e0e8fd9009d6ebd501658f2ee028b254adc98480a51726013dce790
[ "$(elf_body_sha "$TARGET" 736 20)" = "$STOCK_BODY" ] || fail "incompatible original photo library: $TARGET"
[ "$(sha256sum "$LIB" | cut -d ' ' -f 1)" = d88b9a4acb0af712f345ab8e2bdc5800bfb4010020dea3e7418dece04314ba41 ] || fail 'AUTH photo payload hash mismatch'
chcon "$(/system/bin/stat -c %C "$TARGET")" "$LIB" || fail 'label AUTH photo library'
"$BB" mount -o bind "$LIB" "$TARGET" || fail 'mount AUTH photo library'
"$BB" cmp "$LIB" "$TARGET" || fail 'verify AUTH photo mount'
echo 'AUTHVignette photo library mounted; formula service readiness is separate'
