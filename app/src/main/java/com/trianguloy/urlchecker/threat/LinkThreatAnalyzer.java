package com.trianguloy.urlchecker.threat;

import android.net.Uri;

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Local, offline URL heuristics — no network. */
public final class LinkThreatAnalyzer {

    private static final Pattern IPV4_HOST = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
    private static final Pattern SUSPICIOUS_TLD = Pattern.compile(
            "\\.(zip|mov|top|xyz|click|country|gq|tk|ml|cf|ga|work|cam|rest|bond|sbs)$",
            Pattern.CASE_INSENSITIVE);

    private LinkThreatAnalyzer() {
    }

    public static List<String> analyze(String url) {
        List<String> warnings = new ArrayList<>();
        if (url == null || url.isBlank()) return warnings;

        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception e) {
            warnings.add("invalid_url");
            return warnings;
        }

        String host = uri.getHost();
        if (host == null) {
            warnings.add("no_host");
            return warnings;
        }

        if (IPV4_HOST.matcher(host).matches()) {
            warnings.add("ip_literal_host");
        }

        if (host.contains("@") || (uri.getUserInfo() != null && !uri.getUserInfo().isEmpty())) {
            warnings.add("userinfo_trick");
        }

        if (SUSPICIOUS_TLD.matcher(host).find()) {
            warnings.add("suspicious_tld");
        }

        if (looksLikePunycodeHomograph(host)) {
            warnings.add("punycode_homograph");
        }

        String query = uri.getEncodedQuery();
        if (query != null && query.length() > 500) {
            warnings.add("long_query");
        }

        String path = uri.getPath();
        if (path != null) {
            String lower = path.toLowerCase(Locale.ROOT);
            if (lower.contains("login") && lower.contains("verify")
                    || lower.contains("password") && lower.contains("reset")
                    || lower.contains("wallet") && lower.contains("seed")) {
                warnings.add("sensitive_path_pattern");
            }
        }

        if (url.length() > 2000) {
            warnings.add("very_long_url");
        }

        return warnings;
    }

    private static boolean looksLikePunycodeHomograph(String host) {
        if (host.startsWith("xn--")) return true;
        try {
            String unicode = IDN.toUnicode(host);
            if (!unicode.equals(host) && containsMixedScripts(unicode)) return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean containsMixedScripts(String s) {
        boolean hasLatin = false;
        boolean hasCyrillic = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                Character.UnicodeScript script = Character.UnicodeScript.of(c);
                if (script == Character.UnicodeScript.LATIN) hasLatin = true;
                if (script == Character.UnicodeScript.CYRILLIC) hasCyrillic = true;
            }
        }
        return hasLatin && hasCyrillic;
    }
}
