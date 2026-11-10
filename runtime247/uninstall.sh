#!/system/bin/sh
MODDIR=${0%/*}
RESTORE_LOG="$MODDIR/restore.log"
[ ! -d /data/adb/camera68_aurora_lsp ] || RESTORE_LOG=/data/adb/camera68_aurora_lsp/restore.log
# Restore only this module's recorded change. Retain private state on failure.
ACCOUNT_RC=0
/system/bin/sh "$MODDIR/account.sh" restore || ACCOUNT_RC=$?
LUT_RC=0
/system/bin/sh "$MODDIR/lutshare-access.sh" restore >> "$RESTORE_LOG" 2>&1 || LUT_RC=$?
LSP_RC=0
if [ -f /data/adb/camera68_aurora_lsp/settings.before.json ]; then
  /system/bin/sh "$MODDIR/lsp-setup.sh" restore >> "$RESTORE_LOG" 2>&1 || LSP_RC=$?
fi
[ "$ACCOUNT_RC" = 0 ] && [ "$LSP_RC" = 0 ] && [ "$LUT_RC" = 0 ]
