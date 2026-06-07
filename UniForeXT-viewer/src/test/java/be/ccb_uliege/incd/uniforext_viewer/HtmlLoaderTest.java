package be.ccb_uliege.incd.uniforext_viewer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HtmlLoaderTest {

    @Test
    void loadReturnsPackagedHtmlAndInjectsFallbackStyle() {
        String html = HtmlLoader.load("timeline-fast.html");

        assertTrue(html.contains("<!DOCTYPE html>") || html.contains("<html"));
        assertTrue(html.contains("body{background:#020617;color:#e2e8f0}"));
    }

    @Test
    void loadReturnsEscapedErrorPageWhenResourceIsMissing() {
        String html = HtmlLoader.load("missing-<file>.html");

        assertTrue(html.contains("Resource Not Found"));
        assertTrue(html.contains("missing-&lt;file&gt;.html"));
    }

    @Test
    void resourceUrlReflectsClasspathResourcePresence() {
        assertNotNull(HtmlLoader.resourceUrl("timeline-fast.html"));
        assertNull(HtmlLoader.resourceUrl("missing.html"));
    }
}
