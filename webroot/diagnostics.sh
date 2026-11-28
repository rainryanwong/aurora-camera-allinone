#!/system/bin/sh
set -eu

module=$(CDPATH='' cd -- "${0%/*}/.." && pwd)
MODDIR=$module
. "$module/root-compat.sh"
camera=/data/user/0/com.android.camera/cache
editor=/data/user/0/com.miui.mediaeditor/cache

version() {
    dumpsys package "$1" | awk -F= '/^[[:space:]]+versionName=/ && !seen++ {print $2}'
}

process_start() {
    if [ -r "/proc/$1/stat" ]; then
        sed 's/^.*) //' "/proc/$1/stat" 2>/dev/null | awk '{print $20}'
    fi
}

scope() {
    journal=$1/aurora-hooks-$2.tsv
    package=$3
    process=$4
    installed=$5
    pids=$(pidof "$process" || :)
    state=stopped
    [ -z "$pids" ] || state=waiting
    if [ -z "$installed" ]; then state=missing; fi
    if [ -r "$journal" ]; then
        header=$(head -n 1 "$journal" | tr '\t' '|')
        IFS='|' read -r _ recorded_pid recorded_start recorded_boot recorded_version <<EOF
$header
EOF
        if [ "$recorded_pid" = "${pids%% *}" ] &&
            [ -n "$recorded_start" ] && [ "$recorded_start" = "$(process_start "$recorded_pid")" ] &&
            { [ -z "$recorded_boot" ] || [ "$recorded_boot" = "$boot" ]; }; then
            state=current
            if [ "$recorded_version" != "$installed" ]; then state=old; fi
        elif [ "$state" = stopped ]; then
            state=previous
        elif [ "$state" = waiting ]; then
            state=stale
        fi
    fi
    printf 'scope\t%s\t%s\t%s\t%s\t%s\t%s\n' "$2" "$state" "$process" "$pids" "$installed" "$package"
    if [ -r "$journal" ]; then
        cat "$journal"
    fi
}

check_mount() {
    group=$1
    source=$2
    target=$3
    logfile=$4
    staged=${5:-}
    if [ ! -f "$module/$source" ]; then
        state=skipped
    elif [ -n "$provider" ] && [ -r "/proc/$provider/root$target" ]; then
        mounted="/proc/$provider/root$target"
        if [ -n "$staged" ]; then
            if id=$(stat -Lc '%d:%i' "$staged") &&
                [ "$(stat -Lc '%d:%i' "$mounted")" = "$id" ] &&
                { [ "${6:-}" = bind ] ||
                    { visible_id=$(root_path_mount_id "$mounted") &&
                      awk -v id="$visible_id" -v t="$target" '$1==id && $5==t {ro=("," $6 "," ~ /,ro,/)} END {exit !ro}' "/proc/$provider/mountinfo"; }; }; then
                state=mounted
            else
                state=mount-error
            fi
        elif cmp -s "$module/$source" "$mounted"; then state=mounted
        else state=mount-error
        fi
    elif [ -n "$provider" ]; then
        state=mount-error
    else
        state=waiting
    fi
    printf 'mount\t%s\t%s\t%s\t%s\n' "$group" "$state" "$target" "$logfile"
}

natural_status() {
    stage=/dev/aurora_natural/
    night_stage=/dev/aurora_natural_night/com.mi.node.aiasd.so
    if [ "${1:-}" = deep ]; then
        stage=
        night_stage=
    fi
    check_mount 'natural-graph' 'natural-payload/com.qti.feature2.gs.sm8650.so' '/odm/lib64/com.qti.feature2.gs.sm8650.so' natural-activation.log "${stage:+${stage}com.qti.feature2.gs.sm8650.so}"
    for tuning in "$module"/natural-payload/*.bin; do
        [ -f "$tuning" ] || continue
        name=${tuning##*/}
        check_mount "$name" "natural-payload/$name" "/odm/lib64/camera/$name" natural-activation.log "${stage:+$stage$name}"
    done
    check_mount 'natural-photo' 'natural-payload/com.xiaomi.plugin.offlinepostproc.so' '/odm/lib64/camera/plugins/com.xiaomi.plugin.offlinepostproc.so' natural-activation.log "${stage:+${stage}com.xiaomi.plugin.offlinepostproc.so}"
    if [ -f "$module/enable-natural-arcsoft-night" ]; then
        check_mount 'night' 'arcsoft-night-payload/com.mi.node.aiasd.so' '/odm/lib64/camera/components/com.mi.node.aiasd.so' arcsoft-night-activation.log "$night_stage"
    else
        printf 'mount\tnight\tskipped\t\tarcsoft-night-activation.log\n'
    fi
}

provider_pid() {
    ps -A -o PID,NAME | awk '$2 ~ /camera.provider/ && !seen++ {print $1}'
}

native_status() {
    provider=$(provider_pid)
    check_mount 'palette' 'payload/com.xiaomi.plugin.filter.so' '/odm/lib64/camera/plugins/com.xiaomi.plugin.filter.so' activation.log "$module/payload/com.xiaomi.plugin.filter.so" bind
    check_mount 'auth' 'auth239/payload/libMiPhotoFilter.so' '/odm/lib64/libMiPhotoFilter.so' auth239/formula-service.log "$module/auth239/payload/libMiPhotoFilter.so" bind
    natural_status
    for sensor in "$module"/open-gate-sensors/*.bin; do
        [ -f "$sensor" ] || continue
        name=${sensor##*/}
        check_mount "$name" "open-gate-sensors/$name" "/odm/lib64/camera/$name" mount.log "$sensor"
    done
    if [ -n "$provider" ]; then
        if grep -q ' /odm/lib64/camera/plugins ' "/proc/$provider/mountinfo" &&
            grep -q ' /odm/etc/camera/xiaomi ' "/proc/$provider/mountinfo" &&
           root_check_m3_visibility "/proc/$provider/root" >/dev/null; then
            state=mounted
        else
            state=mount-error
        fi
    else
        state=waiting
    fi
    printf 'mount\tm3\t%s\t/odm/lib64/camera/plugins\tmount.log\n' "$state"
}

status() {
    boot=$(cat /proc/sys/kernel/random/boot_id)
    compat=$(version com.aurora.camera.compat)
    motion=$(version com.aurora.camera.livemotion)
    printf 'module\t%s\t%s\n' "$(sed -n 's/^version=//p' "$module/module.prop")" "$(date '+%Y-%m-%d %H:%M:%S')"
    scope "$camera" compat com.aurora.camera.compat com.android.camera "$compat"
    scope "$camera" live-motion com.aurora.camera.livemotion com.android.camera "$motion"
    editor_version=$(version com.miui.mediaeditor)
    if [ -z "$editor_version" ]; then compat=; fi
    scope "$editor" editor com.miui.mediaeditor com.miui.mediaeditor "$compat"
    scope "$editor" photo-editor com.miui.mediaeditor com.miui.mediaeditor:photo_editor "$compat"
    native_status
    preview=$(sh "$module/webroot/preview-diagnostics.sh" status)
    printf 'preview\t%s\n' "$preview"
}

camera_logs() {
    since=$(date -d "@$(($(date +%s) - 300))" '+%m-%d %H:%M:%S.000')
    pids=$({ pidof com.android.camera || :; ps -A -o PID,NAME |
        awk '$2 ~ /camera.provider|^cameraserver$/ {print $1}'; } | tr '\n' ' ')
    logcat -b main,system,crash -v threadtime -t "$since" -f "$scratch/raw.log"
    awk -v pids="$pids" '
        BEGIN {split(pids, ids); for (n in ids) if (ids[n] != "") wanted[ids[n]]=1}
        ($3 in wanted) || /Aurora|MiCam|MiVi|CamX|CameraService|Camera3|Camera2|CHI(USECASE|FEATURE)|Natural|com[.]android[.]camera|cameraserver|camera[.]provider/ {print}
    ' "$scratch/raw.log"
}

diagnostic_logs() {
    printf '[模块与 Hook 状态]\n'
    status
    printf '\n[Hook 与 LSPosed 日志]\n'
    logcat -b main,system,crash -v threadtime -d -s \
        AuroraHookDiagnostics:V AuroraCameraCompat:V AuroraEditor245:V AuroraLiveMotion:V \
        AuroraAUTH239:V AuroraAUTH248:V AuroraNearRangeLens:V LSPosed-Bridge:V AndroidRuntime:E
    if [ -r /dev/aurora-preview-diagnostics/collector.log ]; then
        printf '\n[预览逐帧诊断]\n'
        tail -n 100 /dev/aurora-preview-diagnostics/collector.log
    fi
    for name in boot.log mount.log activation.log natural-activation.log arcsoft-night-activation.log \
        aux-relay.log auth239/mount.log auth239/formula-service.log; do
        if [ -r "$module/$name" ]; then
            printf '\n[%s]\n' "$name"
            tail -n 200 "$module/$name"
        fi
    done
    printf '\n[LSPosed 模块日志]\n'
    ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | sed -n '1,2p' |
        while IFS= read -r file; do
            tail -n 600 "$file" | grep -Ei 'Aurora|com.android.camera|com.miui.mediaeditor|exception|error|^[[:space:]]+at ' || :
        done
}

export_log() {
    download=/storage/emulated/0/Download
    mkdir -p "$download"
    umask 077
    scratch=$(mktemp -d /data/local/tmp/aurora-camera.XXXXXX)
    trap 'rm -f "$scratch/raw.log" "$scratch/result.log"; rmdir "$scratch"' EXIT
    trap 'exit 1' HUP INT TERM
    timestamp=$(date '+%Y%m%d-%H%M%S')
    output="$download/Aurora-$1-$timestamp-${scratch##*.}.log"
    {
        printf 'Aurora %s\n' "$1"
        date '+%Y-%m-%d %H:%M:%S %z'
        printf 'OS %s / %s\n\n' "$(getprop ro.build.version.incremental)" "$(getprop ro.product.device)"
        "$2"
    } > "$scratch/result.log"
    (umask 022; cp "$scratch/result.log" "$output")
    printf '%s\n' "$output"
}

case "${1:-}" in
    status) status ;;
    verify-natural) provider=$(provider_pid); natural_status deep ;;
    save-camera) export_log Camera camera_logs ;;
    export-diagnostics) export_log Diagnostics diagnostic_logs ;;
    *) printf '%s\n' 'Unknown diagnostics operation' >&2; exit 2 ;;
esac
