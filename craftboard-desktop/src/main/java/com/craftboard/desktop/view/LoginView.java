package com.craftboard.desktop.view;

import com.craftboard.desktop.service.ApiService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Vue JavaFX responsable de construire une partie de l'interface.
 */
public class LoginView {

    private final ApiService apiService;
    private final Runnable onConnected;

    public LoginView(ApiService apiService, Runnable onConnected) {
        this.apiService = apiService;
        this.onConnected = onConnected;
    }

    public Parent build() {
        TextField serverUrlField = new TextField(apiService.getBaseUrl());
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Label statusLabel = new Label();

        Button testButton = new Button("Tester");
        Button loginButton = new Button("Connexion");
        loginButton.setDefaultButton(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Serveur"), serverUrlField);
        form.addRow(1, new Label("Utilisateur"), usernameField);
        form.addRow(2, new Label("Mot de passe"), passwordField);
        GridPane.setHgrow(serverUrlField, Priority.ALWAYS);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        HBox actions = new HBox(8, testButton, loginButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(18, new Label("CraftBoard"), form, actions, statusLabel);
        root.setPadding(new Insets(32));
        root.setAlignment(Pos.CENTER);
        root.setFillWidth(true);
        root.setMaxWidth(520);

        VBox wrapper = new VBox(root);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(24));

        testButton.setOnAction(event -> runHealthCheck(serverUrlField.getText(), statusLabel, null));
        loginButton.setOnAction(event -> runHealthCheck(serverUrlField.getText(), statusLabel, onConnected));

        return wrapper;
    }

    private void runHealthCheck(String baseUrl, Label statusLabel, Runnable onSuccess) {
        statusLabel.setText("Connexion...");
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return apiService.testAndSaveBaseUrl(baseUrl);
            }
        };
        task.setOnSucceeded(event -> {
            statusLabel.setText(task.getValue());
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
        task.setOnFailed(event -> statusLabel.setText("Serveur indisponible"));
        Thread thread = new Thread(task, "craftboard-health-check");
        thread.setDaemon(true);
        thread.start();
    }
}
