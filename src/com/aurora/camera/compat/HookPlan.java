package com.aurora.camera.compat;

/** A resolved feature group. install() only registers the already-resolved hooks. */
public interface HookPlan {
    String id();
    void install();
}
