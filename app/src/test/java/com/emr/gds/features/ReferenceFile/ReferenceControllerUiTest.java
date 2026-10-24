package com.emr.gds.features.ReferenceFile;

import com.emr.gds.features.ReferenceFile.adapter.in.ui.ReferenceController;
import com.emr.gds.features.ReferenceFile.application.ReferenceItem;
import com.emr.gds.features.ReferenceFile.application.ReferenceService;
import com.emr.gds.features.ReferenceFile.persistence.SqliteReferenceRepository;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.testfx.util.WaitForAsyncUtils;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class ReferenceControllerUiTest {
    @TempDir Path temp;
    private ReferenceController controller;
    private TableView<ReferenceItem> table;
    private TextField search;
    private ComboBox<String> category;
    private SqliteReferenceRepository repository;
    private final CountDownLatch opening = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private volatile boolean openedOnFxThread;

    @Start
    @SuppressWarnings("unchecked")
    void start(Stage stage) throws Exception {
        repository = new SqliteReferenceRepository(temp.resolve("references.db"));
        repository.save(new ReferenceItem("Medical", "Influenza", "guidelines/flu"));
        repository.save(new ReferenceItem("Lab", "Blood count", "labs"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reference_frame.fxml"));
        loader.setControllerFactory(type -> new ReferenceController() {
            @Override
            protected void launchReferencePath(Path path, Consumer<String> onFailure)
                    throws IOException, InterruptedException {
                openedOnFxThread = Platform.isFxApplicationThread();
                opening.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Test opener timed out");
            }
        });
        stage.setScene(new Scene(loader.load()));
        controller = loader.getController();
        controller.setBasePath(Files.createDirectories(temp.resolve("base")).toFile());
        controller.setReferenceService(new ReferenceService(repository));
        controller.initData();
        stage.show();
        table = (TableView<ReferenceItem>) loader.getNamespace().get("referenceTable");
        search = (TextField) loader.getNamespace().get("searchField");
        category = (ComboBox<String>) loader.getNamespace().get("categoryFilter");
    }

    @Test
    void folderOpeningDoesNotBlockReferenceSearch(FxRobot robot) throws Exception {
        Button open = (Button) table.getScene().lookup("#openBaseButton");
        Label status = (Label) table.getScene().lookup("#statusLabel");
        robot.interact(open::fire);
        assertTrue(opening.await(3, TimeUnit.SECONDS));
        assertFalse(openedOnFxThread);
        try {
            robot.interact(() -> { search.setText("flu"); search.fireEvent(new ActionEvent()); });
            assertEquals(1, table.getItems().size());
        } finally {
            release.countDown();
        }
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> status.getText().startsWith("Open request sent"));
    }

    @Test
    void realFxmlSearchTrimsAndFiltersAllThreeFields(FxRobot robot) {
        assertEquals(2, table.getItems().size());
        robot.interact(() -> { search.setText(" FLU "); search.fireEvent(new ActionEvent()); });
        assertEquals(1, table.getItems().size());
        assertEquals("Influenza", table.getItems().getFirst().getContents());
        robot.interact(() -> { search.setText("labs"); search.fireEvent(new ActionEvent()); });
        assertEquals(1, table.getItems().size());
        assertEquals("Blood count", table.getItems().getFirst().getContents());
    }

    @Test
    void reloadingPreservesCategoryAndEmptyDatabaseHasNoFakeRows(FxRobot robot) throws Exception {
        var reload = ReferenceController.class.getDeclaredMethod("reloadDataFromDb");
        reload.setAccessible(true);
        robot.interact(() -> {
            category.setValue("Medical");
            try { reload.invoke(controller); } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals("Medical", category.getValue());
        assertEquals(1, table.getItems().size());
        robot.interact(() -> {
            repository.findAll().forEach(repository::delete);
            controller.initData();
        });
        assertTrue(table.getItems().isEmpty());
    }

    @Test
    void findUsesSelectedFilesReferenceFolder(FxRobot robot) throws Exception {
        Path selected = temp.resolve("base/guidelines/flu/document.docx");
        Files.createDirectories(selected.getParent());
        Files.writeString(selected, "test selection");
        var find = ReferenceController.class.getDeclaredMethod("findReferencesForFile", java.io.File.class);
        find.setAccessible(true);
        robot.interact(() -> {
            category.setValue("Lab");
            try { find.invoke(controller, selected.toFile()); } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals("All", category.getValue());
        assertEquals(1, table.getItems().size());
        assertEquals("Influenza", table.getItems().getFirst().getContents());
    }

    @Test
    void editDraftNeverMutatesOriginalBeforePersistence(FxRobot robot) {
        ReferenceItem original = new ReferenceItem(5, "Medical", "Original", "guidelines");
        robot.interact(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reference_item_edit.fxml"));
                loader.load();
                ReferenceItemEditController editor = loader.getController();
                editor.setBasePath(temp.resolve("base").toFile());
                editor.setReferenceItem(original);
                ((TextField) loader.getNamespace().get("contentsField")).setText("Changed");
                ReferenceItem draft = editor.getReferenceItem();
                assertNotSame(original, draft);
                assertEquals(5, draft.getId());
                assertEquals("Changed", draft.getContents());
                assertEquals("Original", original.getContents());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
