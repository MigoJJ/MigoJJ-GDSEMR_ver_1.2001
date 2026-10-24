package com.emr.gds.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DesktopFileOpenerTest {
    @TempDir Path temp;

    @Test
    @Timeout(5)
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void returnsWithoutWaitingForTheViewerToExit() throws Exception {
        AtomicReference<String> failure = new AtomicReference<>();
        // This process stays alive until the test supplies a newline.
        Process viewer = DesktopFileOpener.launch(List.of("/bin/sh", "-c", "read value"), failure::set);
        try {
            assertTrue(viewer.isAlive(), "Opening a viewer must not wait for its lifetime");
            viewer.getOutputStream().write('\n');
            viewer.getOutputStream().flush();
            assertTrue(viewer.waitFor(3, TimeUnit.SECONDS));
            assertEquals(0, viewer.exitValue());
            assertNull(failure.get());
        } finally {
            viewer.destroy();
        }
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void reportsLauncherFailureAfterDispatch() throws Exception {
        CountDownLatch reported = new CountDownLatch(1);
        AtomicReference<String> failure = new AtomicReference<>();
        DesktopFileOpener.launch(List.of("/bin/sh", "-c", "exit 17"), message -> {
            failure.set(message);
            reported.countDown();
        });
        assertTrue(reported.await(3, TimeUnit.SECONDS));
        assertTrue(failure.get().contains("exit 17"));
    }

    @Test
    void missingFileFailsBeforeLaunchingAnything() {
        assertThrows(IOException.class, () -> DesktopFileOpener.open(temp.resolve("missing.odt"), message -> fail(message)));
    }
}
