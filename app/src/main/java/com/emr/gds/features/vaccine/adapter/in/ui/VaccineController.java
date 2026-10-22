package com.emr.gds.features.vaccine.adapter.in.ui;

import javafx.stage.Stage;
import com.emr.gds.features.vaccine.application.VaccineService;

public class VaccineController {

    private final VaccineService service;
    private final VaccineView view;

    public VaccineController(VaccineService service) {
        this.service = service;
        this.view = new VaccineView(this::handleAction);
    }

    private void handleAction(String text) {
        switch (text) {
            case "Quit" -> com.emr.gds.IttiaApp.closeWorkspaceTabOrStage(view.getRootView());
            case "Side Effect" -> VaccineSideEffect.open();
            default -> {
                boolean ok = service.logVaccine(text);
                if (!ok) {
                    view.getStage().requestFocus();
                }
            }
        }
    }

    public javafx.scene.control.ScrollPane getView() {
        return view.getRootView();
    }

    public void show() {
        Stage stage = view.getStage();
        if (!stage.isShowing()) {
            stage.show();
        } else {
            stage.toFront();
        }
    }
}
