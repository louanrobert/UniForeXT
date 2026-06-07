package be.ccb_uliege.incd.uniforext_viewer.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

import be.ccb_uliege.incd.uniforext_viewer.JavaBridge;

class WebViewConstructionTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void baseWebViewSubclassesCreateDarkRootWithWebViewCenter() throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            StubWebView view = new StubWebView();

            BorderPane root = view.getRoot();
            assertNotNull(root);
            assertEquals("-fx-background-color: #020617;", root.getStyle());

            Node center = root.getCenter();
            assertTrue(center instanceof WebView);
            WebView webView = (WebView) center;
            assertEquals("-fx-background-color: #020617;", webView.getStyle());
            assertFalse(webView.isContextMenuEnabled());
            assertTrue(webView.getEngine().isJavaScriptEnabled());
            assertSame(webView, view.exposedWebView());
            assertSame(webView.getEngine(), view.exposedWebEngine());
            assertEquals(null, view.exposedInitFunctionName());
        });
    }

    @Test
    void concreteBaseWebViewSubclassesExposeRoots() throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            assertRootContainsWebView(new EventExplorerView((JavaBridge) null));
            assertRootContainsWebView(new QueryView((JavaBridge) null));
            assertRootContainsWebView(new UndatedEventsView((JavaBridge) null));
        });
    }

    @Test
    void fastTimelineViewCreatesStackPaneWithConfiguredWebView() throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            FastTimelineView view = new FastTimelineView((JavaBridge) null);

            StackPane root = view.getRoot();
            assertNotNull(root);
            assertEquals("-fx-background-color: #020617;", root.getStyle());
            assertEquals(1, root.getChildren().size());
            assertTrue(root.getChildren().get(0) instanceof WebView);

            WebView webView = (WebView) root.getChildren().get(0);
            assertEquals("-fx-background-color: #020617;", webView.getStyle());
            assertFalse(webView.isContextMenuEnabled());
            assertTrue(webView.getEngine().isJavaScriptEnabled());
        });
    }

    private static void assertRootContainsWebView(BaseWebView view) {
        BorderPane root = view.getRoot();
        assertNotNull(root);
        assertEquals("-fx-background-color: #020617;", root.getStyle());
        assertTrue(root.getCenter() instanceof WebView);
        WebView webView = (WebView) root.getCenter();
        assertFalse(webView.isContextMenuEnabled());
        assertTrue(webView.getEngine().isJavaScriptEnabled());
    }

    private static final class StubWebView extends BaseWebView {
        StubWebView() {
            super(new Object(), "testBridge", "missing-stub-view.html");
        }

        WebView exposedWebView() {
            return webView;
        }

        javafx.scene.web.WebEngine exposedWebEngine() {
            return webEngine;
        }

        String exposedInitFunctionName() {
            return getInitFunctionName();
        }
    }
}
