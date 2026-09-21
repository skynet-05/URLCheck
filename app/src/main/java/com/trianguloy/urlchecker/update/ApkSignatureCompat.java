package com.trianguloy.urlchecker.update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.io.File;
import java.security.MessageDigest;
import java.util.Arrays;

/** Compares installed app signing cert with a downloaded APK. */
final class ApkSignatureCompat {

    private ApkSignatureCompat() {
    }

    /**
     * @return true if not installed, or APK signing cert matches the installed app.
     */
    static boolean signaturesMatch(Context context, File apkFile) {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            int flags = signingFlags();

            PackageInfo installed = pm.getPackageInfo(packageName, flags);
            PackageInfo archive = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), flags);
            if (archive == null) return true;
            if (archive.applicationInfo != null) {
                archive.applicationInfo.sourceDir = apkFile.getAbsolutePath();
                archive.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();
            }

            byte[] installedHash = certificateSha256(installed);
            byte[] archiveHash = certificateSha256(archive);
            if (installedHash == null || archiveHash == null) return true;
            return Arrays.equals(installedHash, archiveHash);
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private static int signingFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return PackageManager.GET_SIGNING_CERTIFICATES;
        }
        return PackageManager.GET_SIGNATURES;
    }

    private static byte[] certificateSha256(PackageInfo info) throws Exception {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null) {
            signatures = info.signingInfo.getApkContentsSigners();
        } else {
            signatures = info.signatures;
        }
        if (signatures == null || signatures.length == 0) return null;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(signatures[0].toByteArray());
    }
}
