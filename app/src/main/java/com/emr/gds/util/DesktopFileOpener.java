package com.emr.gds.util;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Dispatches a desktop open request without waiting for the external viewer to close. */
public final class DesktopFileOpener {
    private DesktopFileOpener() {}

    /** Call off the FX thread. Late launcher errors are reported on a background thread. */
    public static void open(Path target, Consumer<String> onFailure) throws IOException {
        Path absolute = target.toAbsolutePath().normalize();
        if (!Files.exists(absolute)) {
            throw new IOException("File or folder not found: " + absolute);
        }
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("linux")) {
            // GNOME's native opener avoids xdg-open's desktop-detection shell wrapper.
            List<String> command = Files.isExecutable(Path.of("/usr/bin/gio"))
                    ? List.of("/usr/bin/gio", "open", absolute.toString())
                    : List.of("xdg-open", absolute.toString());
            launch(command, onFailure);
        } else if (os.contains("mac")) {
            launch(List.of("open", absolute.toString()), onFailure);
        } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(absolute.toFile());
        } else {
            throw new IOException("Opening files is not supported on this desktop.");
        }
    }

    static Process launch(List<String> command, Consumer<String> onFailure) throws IOException {
        Process process = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        process.onExit().thenAccept(finished -> {
            if (finished.exitValue() != 0) {
                onFailure.accept(command.getFirst() + " failed (exit " + finished.exitValue()
                        + "). Check the default application for this file type.");
            }
        });
        return process;
    }
}
