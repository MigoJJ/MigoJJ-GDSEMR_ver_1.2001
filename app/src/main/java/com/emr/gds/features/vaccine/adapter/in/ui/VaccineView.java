package com.emr.gds.features.vaccine.adapter.in.ui;

import com.emr.gds.features.vaccine.application.VaccineConstants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

public class VaccineView {

    private static final double FRAME_WIDTH = 500;
    private static final double FRAME_HEIGHT = 900;
    private static final Font LABEL_FONT = Font.font("Segoe UI", FontWeight.BOLD, 13);
    private static final Font BUTTON_FONT = Font.font("Segoe UI", FontWeight.NORMAL, 12);
    private static final String HEADER_STYLE = "-fx-background-color: #334155; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 12; -fx-background-radius: 4;";
    private static final String VACCINE_BUTTON_STYLE = "-fx-background-color: #ffffff; -fx-text-fill: #1e293b; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 12; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
    private static final String SIDEEFFECT_BUTTON_STYLE = "-fx-background-color: #fffbeb; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-border-color: #fde68a; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 12; -fx-cursor: hand;";
    private static final String QUIT_BUTTON_STYLE = "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-border-color: #fca5a5; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 12; -fx-cursor: hand;";

    private Stage stage;
    private final ScrollPane scrollPane;

    public VaccineView(Consumer<String> onAction) {
        VBox root = new VBox(5);
        root.setStyle("-fx-background-color: #f1f5f9;");
        root.setPadding(new Insets(10));

        for (String text : VaccineConstants.UI_ELEMENTS) {
            if (text.startsWith("###")) {
                root.getChildren().add(createHeaderLabel(text));
            } else {
                root.getChildren().add(createActionButton(text, onAction));
            }
        }

        this.scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f1f5f9; -fx-background: #f1f5f9;");
    }

    public ScrollPane getRootView() {
        return scrollPane;
    }

    public Stage getStage() {
        return stage;
    }

    public void show() {
        if (stage == null) {
            stage = new Stage(StageStyle.UTILITY);
            stage.setTitle("Vaccinations");
            stage.setWidth(FRAME_WIDTH);
            stage.setHeight(FRAME_HEIGHT);
            stage.setScene(new Scene(scrollPane));
            positionStageAtBottomRight(stage);
        }
        if (!stage.isShowing()) {
            stage.show();
        } else {
            stage.toFront();
        }
    }

    private Label createHeaderLabel(String text) {
        Label header = new Label(text.replace("###", "").trim());
        header.setFont(LABEL_FONT);
        header.setStyle(HEADER_STYLE);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(Double.MAX_VALUE);
        return header;
    }

    private Button createActionButton(String text, Consumer<String> onAction) {
        Button button = new Button(text);
        button.setFont(BUTTON_FONT);
        button.setFocusTraversable(false);
        button.setMaxWidth(Double.MAX_VALUE);

        switch (text) {
            case "Side Effect" -> button.setStyle(SIDEEFFECT_BUTTON_STYLE);
            case "Quit" -> button.setStyle(QUIT_BUTTON_STYLE);
            default -> button.setStyle(VACCINE_BUTTON_STYLE);
        }

        button.setOnAction(e -> onAction.accept(text));
        return button;
    }

    private void positionStageAtBottomRight(Stage stage) {
        var screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMaxX() - stage.getWidth());
        stage.setY(screenBounds.getMaxY() - stage.getHeight());
    }
}
