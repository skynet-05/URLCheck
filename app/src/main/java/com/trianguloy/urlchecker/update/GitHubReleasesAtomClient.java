package com.trianguloy.urlchecker.update;

import com.trianguloy.urlchecker.modules.companions.VersionManager;

import java.net.HttpURLConnection;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fallback when REST API is blocked: GitHub releases Atom feed (no auth).
 * Assumes OTA asset is {@code LinkGuard.apk} on matching tags.
 */
final class GitHubReleasesAtomClient {

    private static final String ATOM_URL =
            "https://github.com/" + LinkGuardUpdateConfig.GITHUB_OWNER + "/"
                    + LinkGuardUpdateConfig.GITHUB_REPO + "/releases.atom";
    private static final Pattern VERSION_CODE = Pattern.compile("versionCode\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_NAME = Pattern.compile("versionName\\s*:\\s*([^\\s\\n<]+)", Pattern.CASE_INSENSITIVE);

    private GitHubReleasesAtomClient() {
    }

    static UpdateRelease findLatestUpdate(int installedVersionCode, String installedVersionName) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new java.net.URL(ATOM_URL).openConnection(); // URL class
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(30_000);
        conn.setInstanceFollowRedirects(true);
        GitHubHttp.applyFeedHeaders(conn);
        int code = conn.getResponseCode();
        String xml = GitHubHttp.readBody(conn);
        conn.disconnect();
        if (code != 200) {
            throw GitHubHttp.httpFailure(code, xml, "GitHub releases feed");
        }

        UpdateRelease best = null;
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(new StringReader(xml));

        String title = null;
        String link = null;
        StringBuilder content = new StringBuilder();
        boolean inEntry = false;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (event == XmlPullParser.START_TAG) {
                if ("entry".equals(name)) {
                    inEntry = true;
                    title = null;
                    link = null;
                    content.setLength(0);
                } else if (inEntry && "title".equals(name)) {
                    title = readText(parser);
                } else if (inEntry && "link".equals(name) && link == null) {
                    link = parser.getAttributeValue(null, "href");
                } else if (inEntry && ("content".equals(name) || "summary".equals(name))) {
                    content.append(readText(parser));
                }
            } else if (event == XmlPullParser.END_TAG && "entry".equals(name) && inEntry) {
                inEntry = false;
                UpdateRelease parsed = parseEntry(title, link, content.toString());
                if (parsed != null && isNewer(parsed, installedVersionCode, installedVersionName)) {
                    if (best == null || parsed.versionCode > best.versionCode
                            || (parsed.versionCode == best.versionCode
                            && VersionManager.isVersionNewer(parsed.versionName))) {
                        best = parsed;
                    }
                }
            }
            event = parser.next();
        }
        return best;
    }

    private static String readText(XmlPullParser parser) throws Exception {
        if (parser.next() != XmlPullParser.TEXT) return "";
        return parser.getText();
    }

    private static UpdateRelease parseEntry(String title, String link, String body) {
        if (link == null) return null;
        String tag = tagFromLink(link);
        if (tag == null || !qualifies(tag)) return null;

        int versionCode = parseVersionCode(body, tag);
        String versionName = parseVersionName(body, tag);
        String apkUrl = "https://github.com/" + LinkGuardUpdateConfig.GITHUB_OWNER + "/"
                + LinkGuardUpdateConfig.GITHUB_REPO + "/releases/download/" + tag + "/LinkGuard.apk";
        return new UpdateRelease(tag, versionCode, versionName, apkUrl, 0);
    }

    private static String tagFromLink(String link) {
        int idx = link.lastIndexOf("/tag/");
        if (idx < 0) return null;
        return link.substring(idx + 5);
    }

    private static boolean qualifies(String tag) {
        String lower = tag.toLowerCase(Locale.ROOT);
        return lower.startsWith(LinkGuardUpdateConfig.TAG_PREFIX)
                || lower.startsWith("whitelabel-")
                || lower.startsWith("linkguard-");
    }

    private static int parseVersionCode(String body, String tag) {
        Matcher m = VERSION_CODE.matcher(body);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private static String parseVersionName(String body, String tag) {
        Matcher m = VERSION_NAME.matcher(body);
        if (m.find()) return m.group(1).trim();
        if (tag.toLowerCase(Locale.ROOT).startsWith(LinkGuardUpdateConfig.TAG_PREFIX)) {
            return tag.substring(LinkGuardUpdateConfig.TAG_PREFIX.length());
        }
        return tag;
    }

    private static boolean isNewer(UpdateRelease release, int localCode, String localName) {
        if (release.versionCode > 0) {
            if (release.versionCode > localCode) return true;
            if (release.versionCode < localCode) return false;
        }
        if (release.versionName == null || release.versionName.isEmpty()) return false;
        if (release.versionName.equals(localName)) return false;
        return VersionManager.isVersionNewer(release.versionName);
    }
}
