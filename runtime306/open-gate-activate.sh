#!/system/bin/sh
set -eu
MODDIR=${0%/*}
. "$MODDIR/root-compat.sh"
# post-fs-data.sh has already selected and exported BB.
owned=
rollback() {
    for target in $owned; do
        if root_same_file "$MODDIR/open-gate-sensors/${target##*/}" "$target"; then
            "$BB" umount "$target" || echo "Open Gate rollback failed: $target"
        fi
    done
}
trap rollback EXIT
trap 'exit 1' HUP INT TERM
for source in "$MODDIR"/open-gate-sensors/*.bin; do
    target=/odm/lib64/camera/${source##*/}
    "$BB" mount -o bind "$source" "$target"
    owned="$target $owned"
    "$MODDIR/bin/natural-remount-ro" "$target"
done
trap - EXIT HUP INT TERM
