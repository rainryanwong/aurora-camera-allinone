#!/system/bin/sh
MODDIR=${0%/*}
exec >"$MODDIR/mount.log" 2>&1
fail() { echo "ERROR: $*"; exit 1; }
[ ! -f "$MODDIR/disable" ] && [ ! -f "$MODDIR/remove" ] || exit 0
. "$MODDIR/root-compat.sh" || fail "root compatibility helper missing"
root_init || fail "root manager tools unavailable"
root_log_context

# Isolate M3 failures so the independent early activators still run.
mount_m3() (
root_require_applets mount cmp sha256sum cut readlink || fail "required mount tools unavailable"
echo "m3_mount_begin_uptime=$(cat /proc/uptime)"
AUX=/data/vendor/camera/offlinelog/phoenix_m3_aux
mkdir -p "$AUX" || fail "create aux staging"
chown 1047:1005 "$AUX" || fail "own aux staging"
chmod 0700 "$AUX" || fail "protect aux staging"
chcon u:object_r:vendor_camera_data_file:s0 "$AUX" || fail "label aux staging"
[ "$(getprop ro.product.odm.device)" = aurora ] || fail "not aurora"
[ ! -e /odm/lib64/camera/plugins/libM9PhotoFilter.so ] || fail "M9 native filename collision"
[ ! -e /odm/lib64/camera/plugins/com.xiaomi.plugin.m3style.so ] || fail "M3 style already mounted"
[ "$(sha256sum /odm/etc/camera/xiaomi/leicasnapshot.json | cut -d " " -f 1)" = 4dac3a756308912294a4d6b977cb227290e3b878a9dc252ad146ef9b5eaee582 ] || fail "stock graph changed: leicasnapshot.json"
[ "$(sha256sum /odm/etc/camera/xiaomi/satsnapshot.json | cut -d " " -f 1)" = d7e906e52d478fd131183b934b8828211c9dc3f2b69f7ae7d99cbc56a92bfa68 ] || fail "stock graph changed: satsnapshot.json"
[ "$(sha256sum /odm/etc/camera/xiaomi/normalsnapshot.json | cut -d " " -f 1)" = b82fd3aa7300ea2f36cbc05a7fbf508c927634a6f169b413f794a42cce57b9de ] || fail "stock graph changed: normalsnapshot.json"
case "$(sha256sum /odm/lib64/camera/plugins/com.xiaomi.plugin.filter.so | cut -d " " -f 1)" in 64b3e5ddaef859968e839b430e0dd262a89abd8f2c8db271289e8692d3ef51d5|24828f68a2bb934b10357ac0af82e28f3b86ca420822697c2b0490e3d0e3aab2) ;; *) fail "palette plugin unexpected";; esac
DATA_DEVICE=$(/system/bin/stat -c %d "$MODDIR") || fail "read module device"
MERGE=/dev/phoenix_m3
[ ! -e "$MERGE" ] || fail "M3 merge already exists in this boot"
mkdir -p "$MERGE" || fail "create merge root"

bind_entry() {
    local src="$1" dst="$2" item
    if [ -L "$src" ]; then
        cp -a "$src" "$dst" || fail "copy symlink $src"
    elif [ -d "$src" ]; then
        mkdir -p "$dst" || fail "mkdir $dst"
        if [ "$(/system/bin/stat -c %d "$src")" = "$DATA_DEVICE" ]; then
            chcon "$(/system/bin/stat -c %C "$src")" "$dst" || fail "label $dst"
            for item in "$src"/*; do
                [ -e "$item" ] || [ -L "$item" ] || continue
                bind_entry "$item" "$dst/${item##*/}"
            done
        else
            "$BB" mount -o rbind "$src" "$dst" || fail "bind directory $src"
        fi
    elif [ "$(/system/bin/stat -c %d "$src")" = "$DATA_DEVICE" ]; then
        # App mount isolation removes data-backed file binds. Materialize these
        # static assets so that the merged directory never exposes empty stubs.
        cp -p "$src" "$dst" || fail "copy static overlay $src"
        chcon "$(/system/bin/stat -c %C "$src")" "$dst" || fail "label $dst"
        "$BB" cmp "$src" "$dst" || fail "verify static overlay $src"
    else
        : >"$dst" || fail "create mount point $dst"
        "$BB" mount -o bind "$src" "$dst" || fail "bind file $src"
    fi
}

merge_tree() {
    local src="$1" lower="$2" merged="$3" entry name
    mkdir -p "$merged" || fail "create $merged"
    chcon "$(/system/bin/stat -c %C "$lower")" "$merged" || fail "label $merged"
    for entry in "$lower"/*; do
        [ -e "$entry" ] || [ -L "$entry" ] || continue
        name=${entry##*/}
        if [ -e "$src/$name" ] || [ -L "$src/$name" ]; then
            if [ -d "$entry" ] && [ ! -L "$entry" ] && [ -d "$src/$name" ]; then
                merge_tree "$src/$name" "$entry" "$merged/$name"
            else
                bind_entry "$src/$name" "$merged/$name"
            fi
        else
            bind_entry "$entry" "$merged/$name"
        fi
    done
    for entry in "$src"/*; do
        [ -e "$entry" ] || [ -L "$entry" ] || continue
        name=${entry##*/}
        if [ ! -e "$lower/$name" ] && [ ! -L "$lower/$name" ]; then
            bind_entry "$entry" "$merged/$name"
        fi
    done
}

chcon -R u:object_r:vendor_configs_file:s0 "$MODDIR/payload/odm/etc/camera" || fail "label assets"
chcon -R u:object_r:vendor_file:s0 "$MODDIR/payload/odm/lib64" || fail "label libraries"
merge_tree "$MODDIR/payload/odm/etc/camera/xiaomi" /odm/etc/camera/xiaomi "$MERGE/xiaomi"
merge_tree "$MODDIR/payload/odm/lib64/camera/plugins" /odm/lib64/camera/plugins "$MERGE/plugins"
"$BB" mount -o rbind "$MERGE/xiaomi" /odm/etc/camera/xiaomi || fail "activate camera assets"
"$BB" mount -o rbind "$MERGE/plugins" /odm/lib64/camera/plugins || fail "activate plugins"
root_check_m3_visibility || fail "M3 mount verification failed"
echo "m3_mount_end_uptime=$(cat /proc/uptime)"
echo "M3 ODM graphs, plugins and assets mounted"

)
M3_RC=0
mount_m3 || M3_RC=$?
echo "m3_activation_exit=$M3_RC"

# Independent activators run before provider startup.
NATURAL_RC=0
/system/bin/sh "$MODDIR/natural-activate.sh" || NATURAL_RC=$?
echo "natural_activation_exit=$NATURAL_RC; details: $MODDIR/natural-activation.log"

# Default-on via packaged marker; isolated original-color night EV compatibility adaptation.
ARC_NIGHT_RC=0
/system/bin/sh "$MODDIR/arcsoft-night-activate.sh" || ARC_NIGHT_RC=$?
echo "arcsoft_night_activation_exit=$ARC_NIGHT_RC; details: arcsoft-night-activation.log"

# HDRFace r1: independent early activation, preserving existing native paths.
HDR_FACE_RC=0
/system/bin/sh "$MODDIR/hdr-face-activate.sh" || HDR_FACE_RC=$?
echo "hdr_face_activation_exit=$HDR_FACE_RC; details: hdr-face-activation.log"

# AUTHVignette: explicitly authorized native exception, independent status.
AUTH239_RC=0
/system/bin/sh "$MODDIR/auth239/post-fs-data.sh" || AUTH239_RC=$?
echo "auth239_activation_exit=$AUTH239_RC; details: auth239/mount.log"
