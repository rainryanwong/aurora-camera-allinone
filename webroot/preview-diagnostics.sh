#!/system/bin/sh
set -eu
umask 077

web=${0%/*}
module=${web%/*}
state=/dev/aurora-preview-diagnostics
trace=/sys/kernel/tracing
group=aurora_preview
instance=$trace/instances/$group
property=debug.aurora.preview_session
package=com.aurora.camera.compat
endpoint=content://$package.preview
frame_file=/data/user/0/com.android.camera/cache/aurora-preview.frames
control_file=/data/user/0/com.android.camera/cache/aurora-preview.control
marker_file=/data/user/0/$package/cache/aurora-preview.marker
mkdir -p "$state"

phase() {
    printf '%s\n' "$1" > "$state/status.tmp"
    mv "$state/status.tmp" "$state/status"
}

set_session() {
    setprop "$property" "$1" && touch "$control_file"
}

uptime_seconds() {
    read -r up _ < /proc/uptime
    printf '%s\n' "${up%%.*}"
}

notify() {
    if [ "$1" = START ]; then
        printf '' | content call --user 0 --uri "$endpoint" --method START --arg "$token" 2>&1
    else
        printf '' | content call --user 0 --uri "$endpoint" --method STOP 2>&1
    fi
}

unregister() {
    if [ -d "$instance" ]; then
        printf '0\n' > "$instance/tracing_on"
        if [ -f "$instance/events/$group/enable" ]; then
            printf '0\n' > "$instance/events/$group/enable"
        fi
        rmdir "$instance"
    fi
    while read -r event _; do
        event=${event#p:}
        if [ -d "$trace/events/$event" ]; then
            printf -- '-:%s\n' "$event" >> "$trace/uprobe_events"
        fi
    done < "$web/preview-probes.txt"
}

setup_native() {
    provider=$(ps -A -o PID,NAME | awk '$2 ~ /camera.provider/ && !seen++ {print $1}')
    [ -n "$provider" ] || return 1
    while read -r expected path; do
        actual=$(sha256sum "/proc/$provider/root$path") || return 1
        if [ "${actual%% *}" != "$expected" ]; then
            printf '底层库与探针不匹配 %s\n' "$path" >&2
            return 1
        fi
        printf '%s\n' "$actual" >> "$state/device.txt"
    done < "$web/preview-libraries.txt"
    mkdir "$instance" || return 1
    printf '0\n' > "$instance/tracing_on" || return 1
    printf 'boot\n' > "$instance/trace_clock" || return 1
    printf '3072\n' > "$instance/buffer_size_kb" || return 1
    while read -r event path args; do
        printf '%s /proc/%s/root%s %s\n' "$event" "$provider" "$path" "$args" \
            >> "$trace/uprobe_events" || return 1
    done < "$web/preview-probes.txt"
    for name in gs_source gs_final ipe_input; do
        printf 'q2 == 0\n' > "$instance/events/$group/$name/filter" || return 1
    done
    printf '1\n' > "$instance/events/$group/enable" || return 1
    printf '1\n' > "$instance/tracing_on"
}

collect() {
    session_active=0
    cleanup() {
        if [ "$session_active" = 1 ]; then
            set_session '' || printf '无法通知相机停止逐帧采集\n' >&2
        fi
        case "$(cat "$state/status")" in
            starting|recording|marked) phase capture-error ;;
        esac
        unregister
        notify STOP >/dev/null
    }
    trap cleanup EXIT
    trap 'exit 1' HUP INT TERM
    phase starting
    token=$(cat /proc/sys/kernel/random/uuid)
    boot_id=$(cat /proc/sys/kernel/random/boot_id)
    {
        printf 'session=%s\nboot_id=%s\n' "$token" "$boot_id"
        printf 'clock=boot (native seconds; app and marker elapsed realtime nanoseconds)\n'
        printf 'device=%s\nrom=%s\n' "$(getprop ro.product.device)" "$(getprop ro.build.version.incremental)"
        cat "$module/module.prop"
    } > "$state/device.txt"
    if ! setup_native; then
        phase native-error
        exit 1
    fi
    notification=$(notify START)
    # ActivityManager can time out acquiring a provider during its first launch.
    case "$notification" in
        *"Could not find provider"*)
            printf '%s\n' "$notification"
            sleep 1
            notification=$(notify START)
            ;;
    esac
    printf '%s\n' "$notification"
    case "$notification" in
        *posted=false*) phase notification-error; exit 1 ;;
        *fileCapture=true*posted=true*|*posted=true*fileCapture=true*) ;;
        *posted=true*) phase compat-error; exit 1 ;;
        *) phase capture-error; exit 1 ;;
    esac
    deadline=$(( $(uptime_seconds) + 600 ))
    session_active=1
    set_session "file:$token:$deadline"
    phase recording
    interrupted=0
    trap 'interrupted=1' HUP INT TERM
    finish=timeout
    mark_ns=
    while [ "$interrupted" = 0 ] && [ "$(uptime_seconds)" -lt "$deadline" ]; do
        if [ -f "$state/stop" ]; then
            finish=manual
            break
        fi
        if [ -s "$marker_file" ]; then
            mark_ns=$(awk -v token="$token" '$1 == token && NF == 2 && $2 ~ /^[0-9]+$/ {print $2; exit}' "$marker_file")
            if [ -n "$mark_ns" ]; then
                phase marked
                finish=marked
                sleep 5
                break
            fi
        fi
        sleep 1
    done
    if [ "$interrupted" = 1 ]; then finish=interrupted; fi
    set_session ''
    session_active=0
    printf '0\n' > "$instance/tracing_on"
    printf '0\n' > "$instance/events/$group/enable"
    if [ -n "$mark_ns" ]; then
        center=$(awk -v ns="$mark_ns" 'BEGIN {printf "%.6f",ns/1000000000}')
        printf '%s %s\n' "$token" "$mark_ns" > "$state/marker"
    else
        read -r center _ < /proc/uptime
        : > "$state/marker"
    fi
    awk -v center="$center" '
        /^#/ {print;next}
        match($0, /[0-9]+\.[0-9]+: /) {
            t=substr($0,RSTART,RLENGTH-2)+0
            if (t >= center-60 && t <= center+5) print
        }
    ' "$instance/trace" > "$state/native.trace"
    if [ -f "$frame_file" ]; then
        awk -v token="$token" -v center="$center" '$1 == token && $2/1000000000 >= center-60 && $2/1000000000 <= center+5 && /[}]$/ {
            sub(/^[^ ]+ /,"");print
        }' "$frame_file" > "$state/frames.log"
    else
        : > "$state/frames.log"
    fi
    for stats in "$instance"/per_cpu/cpu*/stats; do
        printf '\n%s\n' "${stats#"$instance"/}"
        cat "$stats"
    done >> "$state/device.txt"
    app_count=$(awk 'END {print NR}' "$state/frames.log")
    read -r native_count ccuse_count <<EOF
$(awk '!/^#/ && NF {n++} /: ccuse:/ {c++} END {print n+0, c+0}' "$state/native.trace")
EOF
    printf '\nwindow_center_boot_seconds=%s\nmarked=%s\nfinish=%s\napp_frames=%s\nnative_events=%s\nccuse_events=%s\n' \
        "$center" "${mark_ns:-no}" "$finish" "$app_count" "$native_count" "$ccuse_count" >> "$state/device.txt"
    printf '%s\t%s\t%s\n' "$finish" "$app_count" "$native_count" > "$state/result"
    if [ "$finish" = interrupted ]; then
        phase interrupted
    elif [ "$app_count" = 0 ] && [ "$native_count" = 0 ]; then
        phase empty
    elif [ "$app_count" = 0 ] || [ "$ccuse_count" = 0 ]; then
        phase incomplete
    elif [ "$finish" = manual ]; then
        phase stopped
    else
        phase saved
    fi
}

show_status() {
    current=off
    [ ! -r "$state/status" ] || current=$(cat "$state/status")
    app=waiting
    native=waiting
    case "$current" in
        recording|marked) if [ -s "$frame_file" ]; then app=ran; fi ;;
        *) if [ -s "$state/frames.log" ]; then app=ran; fi ;;
    esac
    if [ -f "$instance/events/$group/enable" ]; then
        native=registered
        if awk '/^entries:/ && $2>0 {ok=1} END {exit !ok}' "$instance"/per_cpu/cpu*/stats 2>/dev/null; then
            native=ran
        fi
    elif [ -r "$state/native.trace" ] && grep -q ': ccuse:' "$state/native.trace"; then
        native=ran
    fi
    printf '%s\t%s\t%s' "$current" "$app" "$native"
    case "$current" in
        saved|stopped|incomplete|empty|interrupted) printf '\t'; cat "$state/result" ;;
        *) printf '\n' ;;
    esac
}

start() {
    exec 8> "$state/collector.lock"
    if ! flock -nx 8 8>&8; then
        show_status
        return
    fi
    unregister
    rm -f "$frame_file" "$marker_file" "$state"/stop "$state"/marker "$state"/result \
        "$state"/frames.log "$state"/native.trace "$state"/device.txt "$state"/collector.log
    phase starting
    setsid /system/bin/sh "$0" collect 8>&8 9>&- </dev/null > "$state/collector.log" 2>&1 &
    show_status
}

export_capture() {
    if [ ! -f "$state/native.trace" ] || [ ! -f "$state/frames.log" ]; then
        printf '%s\n' '请先标记异常，或停止当前采集。' >&2
        exit 1
    fi
    case "$(cat "$state/status")" in saved|stopped|incomplete|interrupted) ;; *) exit 1 ;; esac
    download=/storage/emulated/0/Download
    mkdir -p "$download"
    output="$download/Aurora-Preview-$(date '+%Y%m%d-%H%M%S-%N').tar.gz"
    temporary="$state/export.tar.gz"
    tar -czf "$temporary" -C "$state" device.txt frames.log native.trace marker collector.log
    (umask 022; cp "$temporary" "$output")
    rm -f "$temporary"
    printf '%s\n' "$output"
}

case "${1:-}" in
    collect) collect ;;
    status) show_status ;;
    notification-settings) am start --user 0 -a android.settings.APP_NOTIFICATION_SETTINGS \
        --es android.provider.extra.APP_PACKAGE "$package" ;;
    start|stop|export)
        exec 9> "$state/control.lock"
        flock -x 9 9>&9
        case "$1" in
            start) start ;;
            stop) touch "$state/stop"; show_status ;;
            export) export_capture ;;
        esac
        ;;
    *) exit 2 ;;
esac
