package be.ccb_uliege.incd.uniforext_viewer.views;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;

final class JavaFxTestSupport {

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private JavaFxTestSupport() {
    }

    static void startToolkit() throws Exception {
        if (STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out while starting JavaFX toolkit");
            }
        }
    }

    static void runOnFxThreadAndWait(ThrowingRunnable action) throws Exception {
        startToolkit();
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out while waiting for JavaFX task");
        }
        Throwable throwable = failure.get();
        if (throwable instanceof Exception exception) {
            throw exception;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable != null) {
            throw new AssertionError(throwable);
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
