#!/system/bin/sh
# Sourced by installer and boot helpers. Never choose a foreign manager's tools
# merely because its directory survived an earlier root migration.
root_init() {
    ROOT_MANAGER=
    ROOT_DETECTION=
    BB=
    if [ "${APATCH:-}" = true ]; then
        ROOT_MANAGER=apatch; ROOT_DETECTION=manager_environment
    elif [ "${KSU:-}" = true ]; then
        ROOT_MANAGER=kernelsu; ROOT_DETECTION=manager_environment
    elif [ -n "${MAGISK_VER_CODE:-}" ]; then
        ROOT_MANAGER=magisk; ROOT_DETECTION=manager_environment
    else
        ROOT_CONTEXT=$(/system/bin/id -Z 2>/dev/null)
        case "$ROOT_CONTEXT" in
            u:r:ksu:*) ROOT_MANAGER=kernelsu; ROOT_DETECTION=selinux_context ;;
            u:r:magisk:*) ROOT_MANAGER=magisk; ROOT_DETECTION=selinux_context ;;
            u:r:apatch:*) ROOT_MANAGER=apatch; ROOT_DETECTION=selinux_context ;;
        esac
    fi
    if [ -z "$ROOT_MANAGER" ]; then
        # Only a bounded version query; no root grants, namespace changes or
        # foreign daemon invocation. This also handles manually invoked actions.
        local b probe_bb= count=0 only_manager= su_path version=
        for b in /data/adb/ksu/bin/busybox /data/adb/magisk/busybox /data/adb/ap/bin/busybox; do
            [ -x "$b" ] || continue
            [ -n "$probe_bb" ] || probe_bb=$b
            count=$((count+1))
            case "$b" in
                /data/adb/ksu/*) only_manager=kernelsu ;;
                /data/adb/magisk/*) only_manager=magisk ;;
                /data/adb/ap/*) only_manager=apatch ;;
            esac
        done
        su_path=$(command -v su)
        if [ -n "$probe_bb" ] && [ -n "$su_path" ] && [ -x "$su_path" ]; then
            version=$("$probe_bb" timeout 2 "$su_path" -v 2>/dev/null)
            case "$version" in
                *APatch*|*APATCH*) ROOT_MANAGER=apatch ;;
                *KernelSU*|*KERNELSU*) ROOT_MANAGER=kernelsu ;;
                *MAGISK*|*Magisk*) ROOT_MANAGER=magisk ;;
            esac
            [ -z "$ROOT_MANAGER" ] || ROOT_DETECTION=su_version
        fi
        if [ -z "$ROOT_MANAGER" ] && [ "$count" = 1 ]; then
            ROOT_MANAGER=$only_manager; ROOT_DETECTION=single_tool_directory
        fi
    fi
    case "$ROOT_MANAGER" in
        kernelsu) BB=/data/adb/ksu/bin/busybox; ROOT_MOUNT_SOURCE=KSU ;;
        magisk) BB=/data/adb/magisk/busybox; ROOT_MOUNT_SOURCE=aurora_palette ;;
        apatch) BB=/data/adb/ap/bin/busybox; ROOT_MOUNT_SOURCE=APatch ;;
        *) echo 'ERROR: root manager unknown or ambiguous; no mounts changed'; return 1 ;;
    esac
    [ -x "$BB" ] || { echo "ERROR: $ROOT_MANAGER BusyBox missing: $BB"; return 1; }
    ROOT_SU=$(command -v su)
    export ROOT_MANAGER ROOT_DETECTION BB ROOT_MOUNT_SOURCE ROOT_SU
}

root_require_applets() {
    local app available
    available=$("$BB" --list) || return 1
    for app in "$@"; do
        printf '%s\n' "$available" | /system/bin/grep -qx "$app" || {
            echo "ERROR: $ROOT_MANAGER BusyBox lacks $app"; return 1;
        }
    done
}

root_log_context() {
    echo "root_manager=$ROOT_MANAGER; detection=$ROOT_DETECTION; busybox=$BB"
    echo "mount_namespace_self=$("$BB" readlink /proc/self/ns/mnt 2>/dev/null)"
    echo "mount_namespace_init=$("$BB" readlink /proc/1/ns/mnt 2>/dev/null)"
    echo 'namespace_policy=manager_boot_context; no nsenter or app-profile changes'
}

root_apply_policy() {
    local rule=$1 tool= rc=0
    [ -s "$rule" ] || { echo 'ERROR: sepolicy.rule missing'; return 1; }
    case "$ROOT_MANAGER" in
        kernelsu)
            for tool in /data/adb/ksud /data/adb/ksu/bin/ksud; do
                [ -x "$tool" ] || continue
                "$tool" sepolicy apply "$rule"
                rc=$?
                echo "sepolicy_refresh=ksud; exit=$rc"
                return "$rc"
            done
            ;;
        apatch)
            tool=/data/adb/ap/bin/magiskpolicy
            if [ -x "$tool" ]; then
                "$tool" --live --apply "$rule"
                rc=$?
                echo "sepolicy_refresh=apatch_magiskpolicy; exit=$rc"
                return "$rc"
            fi
            ;;
        magisk)
            tool=$(command -v magiskpolicy)
            if [ -n "$tool" ] && [ -x "$tool" ]; then
                "$tool" --live --apply "$rule"
                rc=$?
                echo "sepolicy_refresh=magiskpolicy; exit=$rc"
                return "$rc"
            fi
            ;;
    esac
    # All three officially support sepolicy.rule. An absent optional live tool
    # does not imply that their boot-time loader failed or that it succeeded.
    echo 'sepolicy_refresh=unavailable; relying_on_manager_sepolicy_rule_loader; live_rules_not_verified'
    return 0
}

root_exec_uid() {
    local uid=$1 command_text=$2
    case "$uid" in ''|*[!0-9]*) echo 'ERROR: invalid target UID'; return 1 ;; esac
    [ -n "$ROOT_SU" ] && [ -x "$ROOT_SU" ] || { echo 'ERROR: su unavailable'; return 1; }
    # UID BEFORE -c works with Magisk's stop-at-user parser and KSU/APatch's
    # command-tail parser. Putting UID after -c can append it to the command.
    # Verify the effective UID before allowing the existing account helper run.
    "$BB" timeout 25 "$ROOT_SU" "$uid" -c \
        "[ \"\$(/system/bin/id -u)\" = '$uid' ] || { echo 'ERROR: su target UID mismatch'; exit 126; }; $command_text"
}

elf_body_sha() (
    # The reference ELF's note header and every byte outside the ID stay in the digest.
    set -o pipefail
    {
        /system/bin/head -c "$2" "$1" &&
        /system/bin/tail -c "+$(($2 + $3 + 1))" "$1"
    } | /system/bin/sha256sum | /system/bin/cut -d ' ' -f 1
)

root_check_m3_tree() {
    local src dst expected actual failed=0
    [ -d "$1" ] || { echo "ERROR: M3 payload directory missing: $1"; return 1; }
    # Recursion stays in this shell so a nested mismatch is not lost in a
    # pipeline subshell. Quoted glob entries also preserve filenames verbatim.
    for src in "$1"/*; do
        dst="$2/${src##*/}"
        if [ -d "$src" ]; then
            root_check_m3_tree "$src" "$dst" || failed=1
        elif [ -f "$src" ]; then
            expected=$("$BB" sha256sum "$src" | "$BB" cut -d ' ' -f 1)
            actual=$("$BB" sha256sum "$dst" 2>/dev/null | "$BB" cut -d ' ' -f 1)
            if [ -z "$expected" ] || [ "$actual" != "$expected" ]; then
                echo "ERROR: M3 visibility mismatch: $dst; expected=$expected; actual=${actual:-unreadable}"
                failed=1
            fi
        fi
    done
    [ "$failed" = 0 ]
}

root_check_m3_visibility() {
    # A successful mount syscall alone cannot prove camera-visible resources.
    root_check_m3_tree "$MODDIR/payload/odm/etc/camera/xiaomi" /odm/etc/camera/xiaomi || return 1
    local graph
    for graph in leicasnapshot.json normalsnapshot.json satsnapshot.json; do
        "$BB" cmp -s "/dev/phoenix_m3/graphs/$graph" "/odm/etc/camera/xiaomi/$graph" || {
            echo "ERROR: M3 generated graph not visible: $graph"
            return 1
        }
    done
    root_check_m3_tree "$MODDIR/payload/odm/lib64/camera/plugins" /odm/lib64/camera/plugins || return 1
    echo 'M3 payload visible and hash-matched in this script context; camera/provider validation still required'
}
