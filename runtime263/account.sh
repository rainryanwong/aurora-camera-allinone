#!/system/bin/sh
# Root caller; only the helper runs as com.xiaomi.account. State is not packaged.
MODDIR=${0%/*}
STATE=/data/adb/camera68_aurora_account
MODE=${1:-auto}
umask 077
[ "$(id -u)" = 0 ] || { echo "需要 root 执行"; exit 1; }
mkdir -p "$STATE" || exit 1
chmod 0700 "$STATE"
case "$MODE" in auto|apply|status|restore) ;; *) echo "auto | apply | status | restore"; exit 1;; esac
. "$MODDIR/root-compat.sh" || exit 1
root_init || exit 1
root_require_applets flock timeout || exit 1
# Android mksh closes high FDs on exec. Use flock's FILE+PROG form.
# Kernel lock releases on process exit; no stale mkdir lock.
if [ "${2:-}" != --locked ]; then
    exec "$BB" flock -n "$STATE/run.lock" /system/bin/sh "$0" "$MODE" --locked
fi
TMP=
trap '[ -z "$TMP" ] || rm -rf "$TMP"' EXIT
trap 'exit 1' HUP INT TERM
say() { printf '%s\n' "$*"; printf '%s %s\n' "$(date '+%F %T')" "$*" >> "$STATE/account.log"; }
fail() { say "失败：$*；没有确认成功，请查看 $STATE/account.log"; exit 1; }
# A deliberate restore must not be undone on the next boot.
if [ "$MODE" = auto ] && [ -f "$STATE/optout" ]; then say "已按你的选择跳过账号自动授权"; exit 0; fi
[ "$MODE" != apply ] || rm -f "$STATE/optout"
[ "$MODE" != restore ] || touch "$STATE/optout"
if [ "$MODE" = restore ] && [ ! -f "$STATE/before" ]; then say "没有本模块修改记录，无需恢复"; exit 0; fi
[ "$(am get-current-user 2>/dev/null | tr -d '\r')" = 0 ] || fail "只在系统主用户下执行"
pm path --user 0 com.android.camera >/dev/null 2>&1 || fail "未找到相机"
ACCOUNT_UID=$(pm list packages --user 0 -U com.xiaomi.account 2>/dev/null | sed -n 's/^package:com\.xiaomi\.account uid:\([0-9][0-9]*\)$/\1/p')
case "$ACCOUNT_UID" in ''|*[!0-9]*) fail "无法取得唯一的小米账号应用UID";; esac
[ "$ACCOUNT_UID" -ge 10000 ] && [ "$ACCOUNT_UID" -lt 100000 ] || fail "账号UID不是主用户应用"
# Unpredictable private staging path; traverse access allows only the JAR to be read.
TMP=$(mktemp -d /data/local/tmp/camera68-account.XXXXXXXX) || fail "无法创建探针暂存目录"
chmod 0711 "$TMP"
cp "$MODDIR/camera-account-probe.jar" "$TMP/probe.jar" || fail "缺少探针"
chmod 0644 "$TMP/probe.jar"
probe() {
    root_exec_uid "$ACCOUNT_UID" "CLASSPATH=$TMP/probe.jar /system/bin/app_process /system/bin CameraAccountVisibility $*" 2>&1
}
say "Root管理器=$ROOT_MANAGER；账号应用UID=$ACCOUNT_UID；开始读取相机账号可见性"
RESULT=$(probe status)
RC=$?
if [ "$RC" -ne 0 ] || ! printf '%s\n' "$RESULT" | grep -qx 'probe_ok=1'; then
    printf '%s\n' "$RESULT" >> "$STATE/account.log"
    fail "探针未完整执行（退出码$RC），可能未登录、未解锁、账号不唯一或su被拒绝"
fi
VIS=$(printf '%s\n' "$RESULT" | sed -n 's/^camera_visibility_before=//p')
KEY=$(printf '%s\n' "$RESULT" | sed -n 's/^account_key=//p')
case "$VIS" in 0|1|2|3|4) ;; *) fail "无有效可见性数值";; esac
[ "${#KEY}" = 64 ] || fail "账号指纹无效"
case "$KEY" in *[!0-9a-f]*) fail "账号指纹格式无效";; esac
say "当前相机账号可见性=$VIS（1=可见；不等同云端服务已验收）"
[ "$MODE" != status ] || exit 0
if [ -f "$STATE/before" ]; then
    read -r SAVED_UID SAVED_KEY SAVED_VIS < "$STATE/before"
    [ "$SAVED_UID" = "$ACCOUNT_UID" ] && [ "$SAVED_KEY" = "$KEY" ] || fail "账号与原记录不同，保留原记录并停止修改"
    case "$SAVED_VIS" in 0|1|2|3|4) ;; *) fail "恢复记录损坏";; esac
fi
TARGET=1
if [ "$MODE" = restore ]; then
    TARGET=$SAVED_VIS
    # Avoid overwriting another tool/user's later setting.
    if [ "$VIS" != 1 ] && [ "$VIS" != "$TARGET" ]; then fail "可见性已被其他操作改变，不覆盖"; fi
fi
if [ "$VIS" = "$TARGET" ]; then
    if [ "$MODE" = restore ]; then
        say "账号可见性已是原值，无需恢复"
    else
        say "相机已可访问小米账号，无需修改"
    fi
    [ "$MODE" != restore ] || rm -f "$STATE/before"
    exit 0
fi
if [ "$MODE" != restore ] && [ ! -f "$STATE/before" ]; then
    printf '%s %s %s\n' "$ACCOUNT_UID" "$KEY" "$VIS" > "$STATE/before.tmp" || fail "无法保存原值"
    mv "$STATE/before.tmp" "$STATE/before" || fail "无法提交原值记录"
fi
RESULT=$(probe set "$TARGET" "$KEY" "$VIS")
RC=$?
if [ "$RC" -ne 0 ] || ! printf '%s\n' "$RESULT" | grep -qx 'probe_ok=1' || ! printf '%s\n' "$RESULT" | grep -qx "camera_visibility_after=$TARGET"; then
    printf '%s\n' "$RESULT" >> "$STATE/account.log"
    fail "写入或回读失败，原值记录保留，可能已写入，请重试核对"
fi
if [ "$MODE" = restore ]; then
    say "已恢复账号可见性，系统回读一致"
else
    say "已授权相机访问小米账号，系统回读一致"
fi
[ "$MODE" != restore ] || rm -f "$STATE/before"
exit 0
