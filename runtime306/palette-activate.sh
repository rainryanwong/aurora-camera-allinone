#!/system/bin/sh
# Preserve stock files, add two vendor libraries, then bind one photo plugin.
MODDIR=${0%/*}
exec >>"$MODDIR/activation.log" 2>&1
echo "activate $(date)"
fail() { echo "FAILED: $*"; exit 1; }
TARGET=/odm/lib64/camera/plugins/com.xiaomi.plugin.filter.so
SOURCE="$MODDIR/payload/com.xiaomi.plugin.filter.so"
[ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || exit 0
. "$MODDIR/root-compat.sh" || fail "root compatibility helper missing"
root_init || fail "root manager tools unavailable"
root_require_applets mount umount mountpoint awk cmp || fail "required palette tools unavailable"
root_log_context
"$MODDIR/bin/odm-compat" filter-factory /odm/lib64/libMiPhotoFilter.so ||
  fail "stock filter factory unavailable"
BASE=/dev/aurora_palette_vendor_base
base_id=
overlay_id=
plugin_id=
rollback_palette() {
  root_unmount_owned "$TARGET" "$plugin_id" "$TARGET" || echo 'ROLLBACK FAILED: Palette plugin'
  root_unmount_owned /vendor/lib64 "$overlay_id" /vendor/lib64 || echo 'ROLLBACK FAILED: Palette vendor overlay'
  root_unmount_owned "$BASE" "$base_id" "$BASE" || echo 'ROLLBACK FAILED: Palette lower view'
}
trap rollback_palette EXIT
trap 'exit 1' HUP INT TERM
# Prefer the manager-mounted system/vendor files on all three managers.
# When neither is present, retain the accepted read-only lowerdir merge.
# Never hide existing vendor child mounts or a partial/foreign library set.
if [ -e /vendor/lib64/libAuroraBridge.so ] && [ -e /vendor/lib64/libAuroraCandy.so ]; then
  echo "vendor_library_strategy=existing_manager_mount_or_known_overlay"
elif [ -e /vendor/lib64/libAuroraBridge.so ] || [ -e /vendor/lib64/libAuroraCandy.so ]; then
  fail "partial vendor library set; refusing to cover an existing mount"
else
  echo "vendor_library_strategy=readonly_overlay_fallback; source=$ROOT_MOUNT_SOURCE"
  "$BB" awk '$5 == "/vendor/lib64" || index($5,"/vendor/lib64/")==1 { found=1 } END { exit found ? 0 : 1 }' /proc/self/mountinfo && fail "existing vendor lib mount; no automatic stacking"
  [ -s "$MODDIR/system/vendor/lib64/libAuroraBridge.so" ] || fail "bridge payload missing"
  [ -s "$MODDIR/system/vendor/lib64/libAuroraCandy.so" ] || fail "donor payload missing"
  COUNT=$(find "$MODDIR/system/vendor/lib64" -mindepth 1 -maxdepth 1 -type f | wc -l)
  [ "$COUNT" -eq 2 ] || fail "unexpected vendor payload membership"
  # This device rejects /data's filesystem as an OverlayFS lower layer.
  # Stage only these two small files on /dev (tmpfs), keeping original names
  # untouched. The staging files disappear on reboot and are recreated here.
  STAGE=/dev/aurora_palette_stage/lib64
  mkdir -p "$STAGE" || fail "make temporary library stage"
  chmod 0755 /dev/aurora_palette_stage "$STAGE"
  for name in libAuroraBridge.so libAuroraCandy.so; do
    cp "$MODDIR/system/vendor/lib64/$name" "$STAGE/$name" || fail "stage $name"
    chmod 0644 "$STAGE/$name" || fail "stage permissions: $name"
  done
  COUNT=$(find "$STAGE" -mindepth 1 -maxdepth 1 | wc -l)
  [ "$COUNT" -eq 2 ] || fail "unexpected staging membership"
  chcon -R u:object_r:vendor_file:s0 /dev/aurora_palette_stage || fail "label vendor stage"
  mkdir -p "$BASE" || fail "make lower view"
  if ! "$BB" mountpoint -q "$BASE"; then
    "$BB" mount -o bind /vendor/lib64 "$BASE" || fail "preserve vendor lower view"
    base_id=$(root_mount_id "$BASE")
  fi
  # Do not let the destination overlay propagate back onto its lower view.
  "$BB" mount --make-private "$BASE" || fail "isolate vendor lower view"
  "$BB" mount -t overlay -o "ro,lowerdir=$STAGE:$BASE" "$ROOT_MOUNT_SOURCE" /vendor/lib64 ||
    fail "readonly vendor file merge unavailable"
  overlay_id=$(root_mount_id /vendor/lib64)
  echo "Read-only vendor merge added the two palette libraries"
fi
for name in libAuroraBridge.so libAuroraCandy.so; do
  localfile=$MODDIR/system/vendor/lib64/$name
  [ -s "$localfile" ] && [ -f "/vendor/lib64/$name" ] || fail "vendor module mount missing: $name"
  if [ -z "$overlay_id" ] && ! root_same_file "$localfile" "/vendor/lib64/$name"; then
    "$BB" cmp -s "$localfile" "/vendor/lib64/$name" || fail "vendor library mismatch: $name"
  fi
done
if ! root_same_file "$SOURCE" "$TARGET"; then
  # The M3 directory merge already bind-mounts its lower photo plugin.
  if [ -n "$(root_mount_id "$TARGET")" ] &&
      ! root_same_file "/dev/phoenix_m3/plugins/${TARGET##*/}" "$TARGET"; then
    fail 'photo plugin is already mounted by another source'
  fi
  chcon u:object_r:vendor_file:s0 "$SOURCE" || fail "label plugin"
  "$BB" mount -o bind "$SOURCE" "$TARGET" || fail "bind photo plugin"
  plugin_id=$(root_mount_id "$TARGET")
  root_same_file "$SOURCE" "$TARGET" || fail "bind identity mismatch"
fi
trap - EXIT HUP INT TERM
echo "Photo plugin file activated. Existing provider mappings may still be old."
echo "No process was stopped. Inspect provider maps and AuroraPalette logs before acceptance."
