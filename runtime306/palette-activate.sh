#!/system/bin/sh
# Preserve stock files, add two vendor libraries, then bind one photo plugin.
MODDIR=${0%/*}
exec >>"$MODDIR/activation.log" 2>&1
echo "activate $(date)"
fail() { echo "FAILED: $*"; exit 1; }
# mksh has a tracked-alias builtin named hash; never use that function name.
file_sha() { /system/bin/sha256sum "$1" 2>/dev/null | /system/bin/cut -d ' ' -f 1; }
TARGET=/odm/lib64/camera/plugins/com.xiaomi.plugin.filter.so
STOCK_BODY=ace431468fdfcc9d7a15d75e9f8ec8d195d28a013fd1e1e4d1a5d520bccd233f
EXPECTED=24828f68a2bb934b10357ac0af82e28f3b86ca420822697c2b0490e3d0e3aab2
[ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || exit 0
[ "$(file_sha "$MODDIR/payload/com.xiaomi.plugin.filter.so")" = "$EXPECTED" ] || fail "candidate photo plugin checksum mismatch"
. "$MODDIR/root-compat.sh" || fail "root compatibility helper missing"
root_init || fail "root manager tools unavailable"
root_require_applets mount umount mountpoint awk readlink || fail "required palette tools unavailable"
root_log_context
"$MODDIR/bin/odm-compat" filter-factory /odm/lib64/libMiPhotoFilter.so ||
  fail "stock filter factory unavailable"
CURRENT=$(file_sha "$TARGET")
if [ "$CURRENT" != "$EXPECTED" ]; then
  [ "$(elf_body_sha "$TARGET" 776 16)" = "$STOCK_BODY" ] || fail "incompatible original plugin: $TARGET"
fi
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
    chmod 0644 "$STAGE/$name"
    [ "$(file_sha "$STAGE/$name")" = "$(file_sha "$MODDIR/system/vendor/lib64/$name")" ] || fail "staging hash mismatch"
  done
  COUNT=$(find "$STAGE" -mindepth 1 -maxdepth 1 | wc -l)
  [ "$COUNT" -eq 2 ] || fail "unexpected staging membership"
  chcon -R u:object_r:vendor_file:s0 /dev/aurora_palette_stage || fail "label vendor stage"
  BASE=/dev/aurora_palette_vendor_base
  mkdir -p "$BASE" || fail "make lower view"
  if ! "$BB" mountpoint -q "$BASE"; then
    "$BB" mount -o bind /vendor/lib64 "$BASE" || fail "preserve vendor lower view"
  fi
  # Do not let the destination overlay propagate back onto its lower view.
  "$BB" mount --make-private "$BASE" || fail "isolate vendor lower view"
  "$BB" mount -t overlay -o "ro,lowerdir=$STAGE:$BASE" "$ROOT_MOUNT_SOURCE" /vendor/lib64 || {
    "$BB" umount "$BASE"
    fail "readonly vendor file merge unavailable"
  }
  echo "Read-only vendor merge added the two palette libraries"
fi
for name in libAuroraBridge.so libAuroraCandy.so; do
  localfile=$MODDIR/system/vendor/lib64/$name
  [ -s "$localfile" ] && [ -f "/vendor/lib64/$name" ] || fail "vendor module mount missing: $name"
  [ "$(file_sha "$localfile")" = "$(file_sha "/vendor/lib64/$name")" ] || fail "vendor library mismatch: $name"
done
[ "$CURRENT" != "$EXPECTED" ] || { echo "Already activated"; exit 0; }
chcon u:object_r:vendor_file:s0 "$MODDIR/payload/com.xiaomi.plugin.filter.so" || fail "label plugin"
"$BB" mount -o bind "$MODDIR/payload/com.xiaomi.plugin.filter.so" "$TARGET" || fail "bind photo plugin"
if [ "$(file_sha "$TARGET")" != "$EXPECTED" ]; then
  "$BB" umount "$TARGET"
  fail "bind hash mismatch; unmounted"
fi
echo "Photo plugin file activated. Existing provider mappings may still be old."
echo "No process was stopped. Inspect provider maps and AuroraPalette logs before acceptance."
