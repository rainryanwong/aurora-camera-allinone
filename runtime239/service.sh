#!/system/bin/sh
MODDIR=${0%/*}
LOG="$MODDIR/boot.log"
umask 077
: > "$LOG"
[ ! -f "$MODDIR/disable" ] && [ ! -f "$MODDIR/remove" ] || exit 0
. "$MODDIR/root-compat.sh" || exit 1
root_init >> "$LOG" 2>&1 || exit 1
root_log_context >> "$LOG" 2>&1

# Activate at the same early service stage as the accepted module. Do not wait
# for account unlock before mounting libraries used by the camera provider.
PALETTE_RC=0
/system/bin/sh "$MODDIR/palette-activate.sh" || PALETTE_RC=$?
echo "palette_activation_exit=$PALETTE_RC; details: $MODDIR/activation.log" >> "$LOG"
M3_RC=0
root_check_m3_visibility >> "$LOG" 2>&1 || M3_RC=$?
echo "m3_visibility_exit=$M3_RC; details: $MODDIR/mount.log" >> "$LOG"
if [ "$M3_RC" = 0 ] && [ -x "$MODDIR/bin/m3_aux_relay" ]; then
    /system/bin/sh "$MODDIR/m3-relay.sh" &
    echo 'M3 auxiliary relay launched; details: aux-relay.log' >> "$LOG"
else
    echo 'M3 auxiliary relay not launched: mount visibility or executable missing' >> "$LOG"
    M3_RC=1
fi

# Start the AUTH formula worker independently of account authorization.
/system/bin/sh "$MODDIR/auth239/service.sh" &
echo "AUTH239 formula worker launched; details: auth239/formula-service.log" >> "$LOG"

# Native failures remain separate from the account task. Preserve its opt-out,
# snapshot, bounded retry and rollback behavior.
n=0
while [ "$(getprop sys.boot_completed)" != 1 ] && [ "$n" -lt 120 ]; do
    sleep 5
    n=$((n+1))
done
[ "$(getprop sys.boot_completed)" = 1 ] || { echo '系统启动未完成，停止账号任务' >> "$LOG"; exit 1; }
POLICY_RC=0
root_apply_policy "$MODDIR/sepolicy.rule" >> "$LOG" 2>&1 || POLICY_RC=$?
echo "policy_refresh_exit=$POLICY_RC" >> "$LOG"
n=0
while [ "$n" -lt 12 ]; do
    [ ! -f "$MODDIR/disable" ] && [ ! -f "$MODDIR/remove" ] || exit 0
    if /system/bin/sh "$MODDIR/account.sh" auto >> "$LOG" 2>&1; then
        echo "boot_result: palette=$PALETTE_RC m3=$M3_RC policy=$POLICY_RC account=0" >> "$LOG"
        [ "$PALETTE_RC" = 0 ] && [ "$M3_RC" = 0 ] && [ "$POLICY_RC" = 0 ]
        exit $?
    fi
    n=$((n+1))
    [ "$n" -ge 12 ] || sleep 10
done
echo '账号授权未完成；解锁并登录后点击模块操作按钮。详见/data/adb/camera68_aurora_account/account.log' >> "$LOG"
exit 1
