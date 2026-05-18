package com.craftboard.desktop.view;

import com.craftboard.core.dto.CityResponse;
import com.craftboard.desktop.service.ApiService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Vue JavaFX responsable de construire une partie de l'interface.
 */
public class CityLoginView {

    private final ApiService apiService;
    private final Runnable onLoggedIn;
    private final Runnable onBack;

    public CityLoginView(ApiService apiService, Runnable onLoggedIn, Runnable onBack) {
        this.apiService = apiService;
        this.onLoggedIn = onLoggedIn;
        this.onBack = onBack;
    }

    public Parent build() {
        ComboBox<CityResponse> cityBox = new ComboBox<>();
        cityBox.setMaxWidth(Double.MAX_VALUE);
        cityBox.setCellFactory(list -> cityCell());
        cityBox.setButtonCell(cityCell());
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Label statusLabel = new Label();
        Button backButton = new Button("Retour");
        Button loginButton = new Button("Connexion");
        Button activateButton = new Button("Premier acces");
        Button resetButton = new Button("Reset ville");
        loginButton.setDefaultButton(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Ville"), cityBox);
        form.addRow(1, new Label("Utilisateur"), usernameField);
        form.addRow(2, new Label("Mot de passe"), passwordField);
        GridPane.setHgrow(cityBox, Priority.ALWAYS);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        HBox actions = new HBox(8, backButton, resetButton, activateButton, loginButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox root = new VBox(18, new Label("Selectionner une ville"), form, actions, statusLabel);
        root.setPadding(new Insets(32));
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(560);
        VBox wrapper = new VBox(root);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(24));

        backButton.setOnAction(event -> onBack.run());
        loginButton.setOnAction(event -> login(cityBox.getValue(), usernameField.getText(), passwordField.getText(), statusLabel));
        activateButton.setOnAction(event -> activate(cityBox.getValue(), usernameField.getText(), passwordField.getText(), statusLabel));
        resetButton.setOnAction(event -> resetCity(cityBox.getValue(), cityBox, statusLabel));
        loadCities(cityBox, statusLabel);
        return wrapper;
    }

    private ListCell<CityResponse> cityCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(CityResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        };
    }

    private void loadCities(ComboBox<CityResponse> cityBox, Label statusLabel) {
        statusLabel.setText("Chargement des villes...");
        Task<java.util.List<CityResponse>> task = new Task<>() {
            @Override
            protected java.util.List<CityResponse> call() throws Exception {
                return apiService.getCities();
            }
        };
        task.setOnSucceeded(event -> {
            cityBox.getItems().setAll(task.getValue());
            if (!task.getValue().isEmpty()) {
                cityBox.getSelectionModel().selectFirst();
            }
            statusLabel.setText(task.getValue().isEmpty() ? "Aucune ville ajoutee." : "");
        });
        task.setOnFailed(event -> statusLabel.setText("Impossible de charger les villes."));
        start(task, "craftboard-load-cities");
    }

    private void login(CityResponse city, String username, String password, Label statusLabel) {
        if (city == null || username == null || username.isBlank() || password == null || password.isBlank()) {
            statusLabel.setText("Ville, utilisateur et mot de passe requis.");
            return;
        }
        statusLabel.setText("Connexion...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                apiService.login(city.id(), username, password);
                return null;
            }
        };
        task.setOnSucceeded(event -> onLoggedIn.run());
        task.setOnFailed(event -> statusLabel.setText("Connexion refusee."));
        start(task, "craftboard-city-login");
    }

    private void activate(CityResponse city, String username, String password, Label statusLabel) {
        if (city == null || username == null || username.isBlank() || password == null || password.isBlank()) {
            statusLabel.setText("Ville, utilisateur et mot de passe requis.");
            return;
        }
        TextInputDialog displayNameDialog = new TextInputDialog(username);
        displayNameDialog.setTitle("Premier acces");
        displayNameDialog.setHeaderText("Creation du mot de passe pour " + username);
        displayNameDialog.setContentText("Nom affiche");
        displayNameDialog.showAndWait().ifPresent(displayName -> {
            statusLabel.setText("Activation...");
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    apiService.activateAccount(city.id(), username, password, displayName);
                    return null;
                }
            };
            task.setOnSucceeded(event -> onLoggedIn.run());
            task.setOnFailed(event -> statusLabel.setText(errorMessage("Activation refusee", task.getException())));
            start(task, "craftboard-activate-account");
        });
    }

    private void resetCity(CityResponse city, ComboBox<CityResponse> cityBox, Label statusLabel) {
        if (city == null) {
            statusLabel.setText("Selectionne une ville a reset.");
            return;
        }
        PasswordInputDialog dialog = new PasswordInputDialog("Mot de passe admin app");
        dialog.setTitle("Reset ville");
        dialog.setHeaderText("Reset complet de " + city.name());
        dialog.setContentText("Mot de passe admin app");
        dialog.showAndWait().ifPresent(password -> {
            statusLabel.setText("Reset en cours...");
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    apiService.resetCity(city.id(), password);
                    return null;
                }
            };
            task.setOnSucceeded(event -> loadCities(cityBox, statusLabel));
            task.setOnFailed(event -> statusLabel.setText(errorMessage("Reset refuse ou impossible", task.getException())));
            start(task, "craftboard-reset-city");
        });
    }

    private static final class PasswordInputDialog extends Dialog<String> {

        private final PasswordField passwordField = new PasswordField();

        private PasswordInputDialog(String promptText) {
            passwordField.setPromptText(promptText);
            getDialogPane().setContent(passwordField);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            setResultConverter(buttonType -> buttonType == ButtonType.OK ? passwordField.getText() : null);
        }
    }

    private void start(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    private String errorMessage(String prefix, Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return prefix + ".";
        }
        String message = throwable.getMessage();
        String detail = extractProblemDetail(message);
        return prefix + ": " + (detail.length() > 420 ? detail.substring(0, 420) + "..." : detail);
    }

    private String extractProblemDetail(String message) {
        int detailIndex = message.indexOf("\"detail\":\"");
        if (detailIndex < 0) {
            return message;
        }
        int start = detailIndex + "\"detail\":\"".length();
        int end = message.indexOf('"', start);
        return end > start ? message.substring(start, end) : message.substring(start);
    }
}
