package com.trianguloy.urlchecker.update;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary OTA source: GitHub releases Atom feed (no REST API rate limit).
 * Only {@code linkguard-v*} tags with {@link LinkGuardReleaseParser#OTA_APK_NAME}.
 */
final class GitHubReleasesAtomClient {

    private static final String ATOM_URL =
            "https://github.com/" + LinkGuardUpdateConfig.GITHUB_OWNER + "/"
                    + LinkGuardUpdateConfig.GITHUB_REPO + "/releases.atom";

    private GitHubReleasesAtomClient() {
    }

    static UpdateRelease findLatestUpdate(int installedVersionCode, String installedVersionName) throws Exception {
        String xml = fetchAtomXml();
        List<UpdateRelease> candidates = parseEntries(xml);
        return LinkGuardReleaseParser.pickNewestEligible(candidates, installedVersionCode, installedVersionName);
    }

    private static String fetchAtomXml() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new java.net.URL(ATOM_URL).openConnection();
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
        return xml;
    }

    private static List<UpdateRelease> parseEntries(String xml) throws Exception {
        List<UpdateRelease> candidates = new ArrayList<>();
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(new StringReader(xml));

        String link = null;
        StringBuilder content = new StringBuilder();
        boolean inEntry = false;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (event == XmlPullParser.START_TAG) {
                if ("entry".equals(name)) {
                    inEntry = true;
                    link = null;
                    content.setLength(0);
                } else if (inEntry && "link".equals(name) && link == null) {
                    link = parser.getAttributeValue(null, "href");
                } else if (inEntry && ("content".equals(name) || "summary".equals(name))) {
                    content.append(readText(parser));
                }
            } else if (event == XmlPullParser.END_TAG && "entry".equals(name) && inEntry) {
                inEntry = false;
                UpdateRelease parsed = parseEntry(link, content.toString());
                if (parsed != null) candidates.add(parsed);
            }
            event = parser.next();
        }
        return candidates;
    }

    private static String readText(XmlPullParser parser) throws Exception {
        if (parser.next() != XmlPullParser.TEXT) return "";
        return parser.getText();
    }

    private static UpdateRelease parseEntry(String link, String body) {
        if (link == null) return null;
        String tag = tagFromLink(link);
        return LinkGuardReleaseParser.releaseFromTagAndBody(tag, body);
    }

    private static String tagFromLink(String link) {
        int idx = link.lastIndexOf("/tag/");
        if (idx < 0) return null;
        return link.substring(idx + 5);
    }
}
