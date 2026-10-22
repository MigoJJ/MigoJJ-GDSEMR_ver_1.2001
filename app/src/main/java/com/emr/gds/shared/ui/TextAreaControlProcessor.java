package com.emr.gds.shared.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies shared behavior to every JavaFX TextArea in the application.
 * <p>
 * Responsibilities:
 * - Attach control-character filtering via {@link IAMTextFormatUtil}.
 * - Expand abbreviations (e.g., ":cd" or values from the abbreviation map) on space.
 * - Recursively process all TextAreas found in open JavaFX windows.
 */
public final class TextAreaControlProcessor {

    private static final String PROCESSED_KEY = "gdsfx.textarea.processed";
    private static final Pattern ABBREVIATION_PATTERN = Pattern.compile(":([a-zA-Z0-9_가-힣]+)");

    private TextAreaControlProcessor() {
        // Utility class
    }

    /**
     * Installs the processor so every current and future window has its TextAreas normalized.
     * @param abbrevMap Map of abbreviations (key -> expansion). Required.
     */
    public static void installGlobalProcessor(Map<String, String> abbrevMap) {
        Objects.requireNonNull(abbrevMap, "abbrevMap");
        Runnable task = () -> {
            processOpenWindows(abbrevMap);
            Window.getWindows().addListener((ListChangeListener<Window>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        change.getAddedSubList().forEach(window -> processWindow(window, abbrevMap));
                    }
                }
            });
        };
        runOnFxThread(task);
    }

    /**
     * Processes every TextArea in all currently open JavaFX windows.
     * @param abbrevMap Map of abbreviations (key -> expansion). Required.
     */
    public static void processOpenWindows(Map<String, String> abbrevMap) {
        Objects.requireNonNull(abbrevMap, "abbrevMap");
        Runnable task = () -> Window.getWindows().forEach(window -> processWindow(window, abbrevMap));
        runOnFxThread(task);
    }

    /**
     * Applies the standard processing (filtering + abbreviation expansion) to a single TextArea.
     * Safe to call multiple times; the behavior is attached once per control.
     */
    public static void applyStandardProcessing(TextArea textArea, Map<String, String> abbrevMap) {
        if (textArea == null) return;
        Objects.requireNonNull(abbrevMap, "abbrevMap");
        Runnable task = () -> {
            if (Boolean.TRUE.equals(textArea.getProperties().get(PROCESSED_KEY))) {
                return; // Already configured
            }

            if (textArea.getTextFormatter() == null) {
                textArea.setTextFormatter(new TextFormatter<>(IAMTextFormatUtil.filterControlChars()));
            }
            if (textArea.isEditable()) {
                attachAbbreviationHandler(textArea, abbrevMap);
            }

            textArea.getProperties().put(PROCESSED_KEY, true);
        };
        runOnFxThread(task);
    }

    /**
     * Expands all abbreviations (":key") found in the input text using the provided map.
     * @param text      Input text (may be null).
     * @param abbrevMap Abbreviation map.
     * @return Text with abbreviations expanded. Returns empty string when input is null.
     */
    public static String expandAbbreviations(String text, Map<String, String> abbrevMap) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        Objects.requireNonNull(abbrevMap, "abbrevMap");

        StringBuilder out = new StringBuilder();
        Matcher matcher = ABBREVIATION_PATTERN.matcher(text);

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = getAbbreviationReplacement(key, abbrevMap);
            matcher.appendReplacement(out, replacement != null ? Matcher.quoteReplacement(replacement) : matcher.group(0));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    // ---------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------

    private static void processWindow(Window window, Map<String, String> abbrevMap) {
        Scene scene = window.getScene();
        if (scene != null) {
            processNode(scene.getRoot(), abbrevMap);
        }
    }

    private static void processNode(Node node, Map<String, String> abbrevMap) {
        if (node == null) return;

        if (node instanceof TextArea ta) {
            applyStandardProcessing(ta, abbrevMap);
        }

        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> processNode(child, abbrevMap));
        }
    }

    private static void attachAbbreviationHandler(TextArea textArea, Map<String, String> abbrevMap) {
        ContextMenu popup = new ContextMenu();
        popup.setAutoHide(true);

        textArea.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                if (popup.isShowing()) {
                    popup.hide();
                }
                if (expandAbbreviationOnSpace(textArea, abbrevMap)) {
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                if (popup.isShowing()) {
                    popup.hide();
                    event.consume();
                }
            }
        });

        textArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!textArea.isFocused()) {
                popup.hide();
                return;
            }
            showAutocompleteSuggestions(textArea, abbrevMap, popup);
        });

        textArea.focusedProperty().addListener((obs, oldF, isFocused) -> {
            if (!isFocused && popup.isShowing()) {
                popup.hide();
            }
        });
    }

    private static void showAutocompleteSuggestions(TextArea ta, Map<String, String> abbrevMap, ContextMenu popup) {
        int caret = Math.min(ta.getCaretPosition(), ta.getText().length());
        if (caret <= 0) {
            popup.hide();
            return;
        }

        String upToCaret = ta.getText(0, caret);
        int start = Math.max(upToCaret.lastIndexOf(' '), upToCaret.lastIndexOf('\n')) + 1;
        if (start >= caret) {
            popup.hide();
            return;
        }

        String word = upToCaret.substring(start);
        if (!word.startsWith(":")) {
            popup.hide();
            return;
        }

        String prefix = word.substring(1).toLowerCase();
        List<String> matches = new ArrayList<>();
        if ("cd".startsWith(prefix)) {
            matches.add("cd");
        }
        for (String key : abbrevMap.keySet()) {
            if (key.toLowerCase().startsWith(prefix)) {
                matches.add(key);
                if (matches.size() >= 8) break;
            }
        }

        if (matches.isEmpty()) {
            popup.hide();
            return;
        }

        popup.getItems().clear();
        for (String matchKey : matches) {
            String replacement = getAbbreviationReplacement(matchKey, abbrevMap);
            String preview = (replacement != null && replacement.length() > 40)
                    ? replacement.substring(0, 40) + "..."
                    : (replacement != null ? replacement : "");
            MenuItem item = new MenuItem(":" + matchKey + "  ➔  " + preview);
            item.setOnAction(e -> {
                int curCaret = ta.getCaretPosition();
                ta.deleteText(start, curCaret);
                ta.insertText(start, replacement + " ");
                popup.hide();
            });
            popup.getItems().add(item);
        }

        if (!popup.isShowing() && ta.getScene() != null && ta.getScene().getWindow() != null) {
            popup.show(ta, Side.BOTTOM, 10, 0);
        }
    }

    private static boolean expandAbbreviationOnSpace(TextArea ta, Map<String, String> abbrevMap) {
        int caret = ta.getCaretPosition();
        String upToCaret = ta.getText(0, caret);
        int start = Math.max(upToCaret.lastIndexOf(' '), upToCaret.lastIndexOf('\n')) + 1;

        String word = upToCaret.substring(start).trim();
        if (!word.startsWith(":")) return false;

        String key = word.substring(1);
        String replacement = getAbbreviationReplacement(key, abbrevMap);
        if (replacement == null) return false;

        Platform.runLater(() -> {
            ta.deleteText(start, caret);
            ta.insertText(start, replacement + " ");
        });
        return true;
    }

    private static String getAbbreviationReplacement(String key, Map<String, String> abbrevMap) {
        if ("cd".equalsIgnoreCase(key)) {
            return LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        }
        return abbrevMap.get(key);
    }

    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
