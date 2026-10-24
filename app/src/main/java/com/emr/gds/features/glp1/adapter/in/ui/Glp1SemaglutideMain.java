package com.emr.gds.features.glp1.adapter.in.ui;

import com.emr.gds.core.db.DbPaths;
import com.emr.gds.input.IAIFxTextAreaManager;
import com.emr.gds.input.IAIMain;
import com.emr.gds.input.IAITextAreaManager;
import com.emr.gds.util.StageSizing;
import com.emr.gds.util.DesktopFileOpener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class Glp1SemaglutideMain extends Application {

    private static final String INSURANCE_REFERENCE =
            "references/guidelines/Ozempic® (semaglutide) 한국 건강보험 급여 기준.odt";

    @Override
    public void start(Stage stage) {
        Parent root = createView(stage);
        Scene scene = new Scene(root);
        stage.setTitle("GLP-1RA (Semaglutide) - EMR Module Demo");
        stage.setScene(scene);
        StageSizing.fitToScreen(stage, 0.5, 0.9);
        stage.show();
    }

    public Parent createView() {
        return createView(null);
    }

    public Parent createView(Stage stage) {
        Glp1SemaglutidePane medPane = new Glp1SemaglutidePane();

        TextArea txtOutput = new TextArea();
        txtOutput.setPromptText("Problem-list output will appear here...");
        txtOutput.setPrefRowCount(8);
        txtOutput.setWrapText(true);

        Button btnExport = new Button("Generate to EMR");
        Button btnSave   = new Button("Save to EMR");
        Button btnClear  = new Button("Clear All");
        Button btnQuit   = new Button("Quit");
        Button btnInsurance = new Button("오젬픽 건강보험 급여 기준");
        btnInsurance.setId("ozempicInsuranceReference");
        btnInsurance.setOnAction(e -> openInsuranceReference(stage, btnInsurance));

        btnExport.setOnAction(e -> txtOutput.setText(medPane.toProblemListString()));
        btnClear.setOnAction(e -> {
            medPane.clearAll();
            txtOutput.clear();
        });
        btnSave.setOnAction(e -> {
            String content = txtOutput.getText().trim();
            if (content.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Nothing to save. Generate text first.").showAndWait();
                return;
            }

            IAIMain.getManagerSafely().ifPresentOrElse(
                    manager -> {
                        String textToAppend = content.endsWith("\n") ? content : content + "\n";
                        manager.appendTextToSection(IAITextAreaManager.AREA_PI, textToAppend);

                        String assessmentLine = medPane.toAssessmentSummary();
                        if (!assessmentLine.isBlank()) {
                            manager.appendTextToSection(IAIFxTextAreaManager.AREA_A, assessmentLine + "\n");
                        }
                    },
                    () -> new Alert(
                            Alert.AlertType.ERROR,
                            "EMR mainframe is not connected.\nOpen this tool from the EMR to enable saving."
                    ).showAndWait()
            );
        });
        btnQuit.setOnAction(e -> com.emr.gds.IttiaApp.closeWorkspaceTabOrStage(btnQuit));

        HBox buttonBox = new HBox(10, btnExport, btnSave, btnClear, btnQuit);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(6));

        BorderPane bottomPane = new BorderPane();
        bottomPane.setTop(new Separator());
        bottomPane.setCenter(txtOutput);
        bottomPane.setBottom(buttonBox);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("glp1-window");
        try {
            root.getStylesheets().add(getClass().getResource("/css/glp1.css").toExternalForm());
        } catch (Exception ignored) {}
        HBox referenceBox = new HBox(btnInsurance);
        referenceBox.setPadding(new Insets(10, 12, 0, 12));
        root.setTop(referenceBox);
        root.setCenter(medPane);
        root.setBottom(bottomPane);

        return root;
    }

    private void openInsuranceReference(Stage owner, Button button) {
        Task<Void> openTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                openReferenceDocument(DbPaths.resolveDbPath(INSURANCE_REFERENCE),
                        message -> Platform.runLater(() -> showReferenceError(owner, message)));
                return null;
            }
        };
        button.setDisable(true);
        openTask.setOnSucceeded(e -> button.setDisable(false));
        openTask.setOnFailed(e -> {
            button.setDisable(false);
            showReferenceError(owner, openTask.getException().getMessage());
        });
        Thread worker = new Thread(openTask, "ozempic-reference-opener");
        worker.setDaemon(true);
        worker.start();
    }

    private void showReferenceError(Stage owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "급여 기준 문서를 열지 못했습니다.\n" + message);
        alert.setTitle("오젬픽 급여 기준 문서 열기 오류");
        alert.initOwner(owner);
        alert.show();
    }

    protected void openReferenceDocument(Path document, Consumer<String> onFailure)
            throws IOException, InterruptedException {
        DesktopFileOpener.open(document, onFailure);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
