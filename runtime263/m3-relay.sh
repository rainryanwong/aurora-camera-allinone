#!/system/bin/sh
MODDIR=${0%/*}
exec >"$MODDIR/aux-relay.log" 2>&1
set -e
APP=/data/user/0/com.android.camera/files
while :; do
    [ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || exit 0
    [ ! -d "$APP" ] || break
    sleep 1
done
DEST=$APP/phoenix_m3_aux
mkdir -p "$DEST"
chown "$(/system/bin/stat -c %u:%g "$APP")" "$DEST"
chmod 0700 "$DEST"
chcon "$(/system/bin/stat -c %C "$APP")" "$DEST"
for DIR in /data/vendor/camera/offlinelog/phoenix_m3_aux "$DEST"; do
    find "$DIR" -maxdepth 1 -type f \( -name '*.aux' -o -name '*.tmp' \) -mmin +15 -delete
done
exec "$MODDIR/bin/m3_aux_relay"
