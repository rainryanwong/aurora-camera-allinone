#!/system/bin/sh
MODDIR=${0%/*}
exec >"$MODDIR/natural-activation.log" 2>&1
fail() { echo "FAILED: $*"; exit 1; }
target_for() {
  case "$1" in
    com.qti.feature2.gs.sm8650.so) printf '/odm/lib64/%s' "$1" ;;
    com.qti.tuned.aurora_semco_LYT900_wide.bin|com.qti.tuned.aurora_semco_imx858_ultra.bin|com.qti.tuned.aurora_ofilm_imx858_tele3x.bin|com.qti.tuned.aurora_semco_imx858_tele5x.bin) printf '/odm/lib64/camera/%s' "$1" ;;
    com.xiaomi.plugin.offlinepostproc.so) printf '/odm/lib64/camera/plugins/%s' "$1" ;;
    *) return 1 ;;
  esac
}
[ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || exit 0
[ "$(getprop ro.product.odm.device)" = aurora ] || fail 'not aurora'
. "$MODDIR/root-compat.sh" || fail 'root helper missing'
root_init || fail 'root tools unavailable'
root_require_applets mount umount awk || fail 'mount tools missing'
root_log_context
[ -z "$(pidof vendor.qti.camera.provider-service_64)" ] || fail 'provider already started; no live replacement'
COUNT=0
while read -r name stock expected; do
  target_for "$name" >/dev/null || fail 'unknown file list entry'
  COUNT=$((COUNT+1))
done < "$MODDIR/natural-files.list"
[ "$COUNT" = 6 ] || fail 'expected exactly 6 Natural files'
STAGE=/dev/aurora_natural
[ ! -e "$STAGE" ] || fail 'stage exists; refusing stacked activation'
mkdir "$STAGE" || fail 'create stage'
mount_id() { "$BB" awk -v t="$1" '$5==t {id=$1} END {print id}' /proc/self/mountinfo; }
OWNED=
COMMITTED=0
cleanup() {
  [ "$COMMITTED" = 0 ] || return 0
  rollback_ok=1
  for pair in $OWNED; do
    target=${pair%:*}; id=${pair##*:}
    if [ "$(mount_id "$target")" = "$id" ]; then
      "$BB" umount "$target" || { rollback_ok=0; echo "ROLLBACK FAILED: $target"; }
    fi
  done
  [ "$rollback_ok" = 1 ] || return
  while read -r name stock expected; do
    target_for "$name" >/dev/null || continue
    rm -f "$STAGE/$name"
  done < "$MODDIR/natural-files.list"
  rmdir "$STAGE" || echo 'Could not remove Natural stage'
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM
chmod 0755 "$STAGE" || fail 'stage permissions'
chcon u:object_r:vendor_file:s0 "$STAGE" || fail 'stage context'
while read -r name stock expected; do
  cp "$MODDIR/natural-payload/$name" "$STAGE/$name" || fail "stage $name"
  chmod 0644 "$STAGE/$name" || fail 'file permissions'
  chcon u:object_r:vendor_file:s0 "$STAGE/$name" || fail 'file context'
done < "$MODDIR/natural-files.list"
[ -z "$(pidof vendor.qti.camera.provider-service_64)" ] || fail 'provider started while staging; no mounts applied'
# Mount the tuning before the preview and post-processing libraries.
while read -r name stock expected; do
  [ -z "$(pidof vendor.qti.camera.provider-service_64)" ] || fail 'provider started before activation finished'
  target=$(target_for "$name") || fail 'invalid activation target'
  before=$(mount_id "$target")
  "$BB" mount -o bind "$STAGE/$name" "$target" || fail "bind $name"
  id=$(mount_id "$target")
  [ -n "$id" ] && [ "$id" != "$before" ] || fail 'missing new mount'
  OWNED="$target:$id $OWNED"
  "$MODDIR/bin/natural-remount-ro" "$target" || fail 'readonly remount'
  staged_id=$(stat -Lc '%d:%i' "$STAGE/$name") || fail "staged identity: $name"
  [ "$(stat -Lc '%d:%i' "$target")" = "$staged_id" ] || fail "mounted identity: $name"
  "$BB" awk -v t="$target" '$5==t {n=split($6,a,","); for(i=1;i<=n;i++) if(a[i]=="ro") ok=1} END {exit !ok}' /proc/self/mountinfo || fail 'mount not readonly'
done < "$MODDIR/natural-files.list"
[ -z "$(pidof vendor.qti.camera.provider-service_64)" ] || fail 'provider started before commit'
COMMITTED=1
echo 'natural_tuning=active'
