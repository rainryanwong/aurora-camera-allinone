#!/system/bin/sh
MODDIR=${0%/*}
exec >>"$MODDIR/formula-service.log" 2>&1
set -eu
grep -q '^com.aurora.camera.compat ' /data/system/packages.list || exit 0
[ ! -e "$MODDIR/mount_failed" ] || { echo 'AUTH mount failed; formula service not started'; exit 1; }
. "$MODDIR/../root-compat.sh"
root_init
while [ ! -d /data/user/0/com.android.camera/files ]; do
    [ ! -e "$MODDIR/../disable" ] && [ ! -e "$MODDIR/../remove" ] || exit 0
    sleep 2
done
sh "$MODDIR/init-formula.sh"
"$BB" cmp "$MODDIR/payload/libMiPhotoFilter.so" /odm/lib64/libMiPhotoFilter.so
# The normal filter factory uses the donor Candy backend through AuroraBridge.
# The old stock-library mount alone cannot establish the active AUTH pair.
"$BB" cmp "$MODDIR/../system/vendor/lib64/libAuroraCandy.so" /vendor/lib64/libAuroraCandy.so
APP=/data/user/0/com.android.camera/files/phoenix-vignette
VENDOR=/data/vendor/camera/phoenix-vignette
export PHOENIX_APP_OWNER="$(/system/bin/stat -c %u:%g "${APP%/*}")"
export PHOENIX_APP_CONTEXT="$(/system/bin/stat -c %C "${APP%/*}")"
export PHOENIX_VENDOR_OWNER="$(/system/bin/stat -c %u:%g "${VENDOR%/*}")"
export PHOENIX_VENDOR_CONTEXT="$(/system/bin/stat -c %C "${VENDOR%/*}")"
READY="$APP/aurora241-ready"
trap 'rm -f "$READY"' EXIT
trap 'exit 1' HUP INT TERM
printf 'Aurora241 %s photo-candy-mounted\n' "$(cat /proc/sys/kernel/random/boot_id)" > "$READY.pending"
chown "$PHOENIX_APP_OWNER" "$READY.pending"
chmod 0640 "$READY.pending"
chcon "$PHOENIX_APP_CONTEXT" "$READY.pending"
mv -f "$READY.pending" "$READY"
echo 'AUTH formula service ready'
"$BB" inotifyd "$MODDIR/formula-event.sh" "$APP:y"
