package be.ccb_uliege.incd.uniforext_viewer.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import be.ccb_uliege.incd.uniforext_viewer.JavaBridge;
import javafx.stage.Stage;

class GraphViewTest {

    private Stage stageToClose;

    @BeforeAll
    static void startJavaFx() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @AfterEach
    void closeStage() throws Exception {
        if (stageToClose != null) {
            JavaFxTestSupport.runOnFxThreadAndWait(() -> stageToClose.close());
            stageToClose = null;
        }
    }

    @Test
    void constructorCreatesShownStageForIndividualUri() throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            GraphView view = new GraphView((JavaBridge) null, "http://example.test/events/LoginEvent");
            stageToClose = view.getStage();

            assertNotNull(stageToClose);
            assertEquals("Graph: LoginEvent", stageToClose.getTitle());
            assertEquals(1000, stageToClose.getWidth());
            assertEquals(700, stageToClose.getHeight());
            assertNotNull(stageToClose.getScene());
            assertTrue(stageToClose.isShowing());
        });
    }
}
