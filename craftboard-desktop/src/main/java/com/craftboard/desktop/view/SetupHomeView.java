package com.craftboard.desktop.view;

import com.craftboard.desktop.service.ApiService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

/**
 * Vue JavaFX responsable de construire une partie de l'interface.
 */
public class SetupHomeView {

    private final ApiService apiService;
    private final Runnable onAddCity;
    private final Runnable onSelectCity;

    public SetupHomeView(ApiService apiService, Runnable onAddCity, Runnable onSelectCity) {
        this.apiService = apiService;
        this.onAddCity = onAddCity;
        this.onSelectCity = onSelectCity;
    }

    public Parent build() {
        Label title = new Label("CraftBoard");
        Label server = new Label("Serveur: " + apiService.getBaseUrl());
        Label statusLabel = new Label();
        Button selectCity = new Button("Selectionner une ville");
        Button addCity = new Button("Ajouter une ville");
        Button serverButton = new Button("Modifier serveur");
        selectCity.setMaxWidth(Double.MAX_VALUE);
        addCity.setMaxWidth(Double.MAX_VALUE);
        serverButton.setMaxWidth(Double.MAX_VALUE);
        selectCity.setOnAction(event -> onSelectCity.run());
        addCity.setOnAction(event -> onAddCity.run());
        serverButton.setOnAction(event -> changeServer(server, statusLabel));

        VBox root = new VBox(18, title, server, selectCity, addCity, serverButton, statusLabel);
        root.setPadding(new Insets(32));
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(420);

        VBox wrapper = new VBox(root);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(24));
        return wrapper;
    }

    private void changeServer(Label serverLabel, Label statusLabel) {
        TextInputDialog dialog = new TextInputDialog(apiService.getBaseUrl());
        dialog.setTitle("Serveur");
        dialog.setHeaderText("Modifier le serveur CraftBoard");
        dialog.setContentText("URL du serveur");
        dialog.showAndWait().ifPresent(url -> {
            statusLabel.setText("Verification de la connexion...");
            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return apiService.testAndSaveBaseUrl(url);
                }
            };
            task.setOnSucceeded(event -> {
                serverLabel.setText("Serveur: " + apiService.getBaseUrl());
                statusLabel.setText("Connexion confirmee: " + task.getValue());
            });
            task.setOnFailed(event -> statusLabel.setText("Connexion refusee. Serveur conserve: " + apiService.getBaseUrl()));
            Thread thread = new Thread(task, "craftboard-change-server");
            thread.setDaemon(true);
            thread.start();
        });
    }
}
