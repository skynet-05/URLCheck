package com.trianguloy.urlchecker.update;

/** A GitHub release asset ready to install. */
public final class UpdateRelease {

    public final String tagName;
    public final int versionCode;
    public final String versionName;
    public final String apkUrl;
    public final long apkSizeBytes;

    public UpdateRelease(String tagName, int versionCode, String versionName, String apkUrl, long apkSizeBytes) {
        this.tagName = tagName;
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.apkUrl = apkUrl;
        this.apkSizeBytes = apkSizeBytes;
    }
}
