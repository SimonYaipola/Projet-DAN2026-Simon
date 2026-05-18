package com.craftboard.desktop.view;

import com.craftboard.core.dto.BitjitaClaimImportResponse;
import com.craftboard.core.dto.BitjitaClaimSummary;
import com.craftboard.core.dto.CityResponse;
import com.craftboard.desktop.service.ApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Vue JavaFX responsable de construire une partie de l'interface.
 */
public class BitjitaClaimSetupView {

    private final ApiService apiService;
    private final Runnable onImported;
    private final Runnable onBack;

    public BitjitaClaimSetupView(ApiService apiService, Runnable onImported, Runnable onBack) {
        this.apiService = apiService;
        this.onImported = onImported;
        this.onBack = onBack;
    }

    public Parent build() {
        TextField searchField = new TextField();
        searchField.setPromptText("Nom de ville BitJita");
        Button searchButton = new Button("Rechercher");
        Button importButton = new Button("Importer la ville");
        Button backButton = new Button("Retour");
        importButton.setDisable(true);
        Label statusLabel = new Label();

        TableView<BitjitaClaimSummary> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Recherche une ville BitJita"));
        table.getColumns().add(column("Nom", BitjitaClaimSummary::name));
        table.getColumns().add(column("Region", BitjitaClaimSummary::regionName));
        table.getColumns().add(column("Tier", claim -> claim.tier() == null ? "" : claim.tier().toString()));
        table.getColumns().add(column("Owner", claim -> blankFallback(claim.ownerPlayerUsername())));
        table.getColumns().add(column("ID", BitjitaClaimSummary::entityId));

        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                importButton.setDisable(newValue == null));

        HBox searchBar = new HBox(8, searchField, searchButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(8, backButton, importButton, statusLabel);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(10,
                new Label("Selection de la ville BitJita"),
                searchBar,
                actions
        );
        top.setPadding(new Insets(16));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(table);
        BorderPane.setMargin(table, new Insets(0, 16, 16, 16));

        searchButton.setOnAction(event -> search(searchField.getText(), table, statusLabel));
        searchField.setOnAction(event -> search(searchField.getText(), table, statusLabel));
        importButton.setOnAction(event -> importClaim(table.getSelectionModel().getSelectedItem(), statusLabel));
        backButton.setOnAction(event -> onBack.run());

        return root;
    }

    private TableColumn<BitjitaClaimSummary, String> column(String title, java.util.function.Function<BitjitaClaimSummary, String> getter) {
        TableColumn<BitjitaClaimSummary, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        return column;
    }

    private String blankFallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void search(String query, TableView<BitjitaClaimSummary> table, Label statusLabel) {
        if (query == null || query.isBlank() || query.trim().length() < 2) {
            statusLabel.setText("Entre au moins 2 caracteres.");
            return;
        }
        statusLabel.setText("Recherche...");
        Task<java.util.List<BitjitaClaimSummary>> task = new Task<>() {
            @Override
            protected java.util.List<BitjitaClaimSummary> call() throws Exception {
                return apiService.searchBitjitaClaims(query).claims();
            }
        };
        task.setOnSucceeded(event -> {
            table.getItems().setAll(task.getValue());
            statusLabel.setText(task.getValue().size() + " ville(s) trouvee(s).");
        });
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Recherche BitJita impossible", task.getException())));
        start(task, "craftboard-bitjita-search");
    }

    private void importClaim(BitjitaClaimSummary claim, Label statusLabel) {
        if (claim == null) {
            return;
        }
        statusLabel.setText("Import de " + claim.name() + "...");
        Task<BitjitaClaimImportResponse> task = new Task<>() {
            @Override
            protected BitjitaClaimImportResponse call() throws Exception {
                return apiService.importBitjitaClaim(claim.entityId());
            }
        };
        task.setOnSucceeded(event -> {
            BitjitaClaimImportResponse response = task.getValue();
            showAdminSetup(response.city(), response.memberCount(), statusLabel);
        });
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Import impossible", task.getException())));
        start(task, "craftboard-bitjita-import");
    }

    private void showAdminSetup(CityResponse city, int memberCount, Label statusLabel) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Compte administrateur");
        dialog.setHeaderText(memberCount + " membre(s) importes. Cree le premier compte admin pour " + city.name() + ".");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom utilisateur");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        TextField displayNameField = new TextField();
        displayNameField.setPromptText("Nom affiche");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Utilisateur"), usernameField);
        form.addRow(1, new Label("Mot de passe"), passwordField);
        form.addRow(2, new Label("Nom affiche"), displayNameField);
        dialog.getDialogPane().setContent(form);
        ButtonType createButton = new ButtonType("Creer admin", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        Button button = (Button) dialog.getDialogPane().lookupButton(createButton);
        button.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
                statusLabel.setText("Utilisateur et mot de passe requis pour l'admin.");
                event.consume();
                return;
            }
            try {
                apiService.createInitialAdmin(
                        city.id(),
                        usernameField.getText(),
                        passwordField.getText(),
                        displayNameField.getText());
                dialog.close();
                onImported.run();
            } catch (Exception exception) {
                statusLabel.setText(errorMessage("Creation admin impossible", exception));
                event.consume();
            }
        });
        dialog.showAndWait();
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
        return prefix + ": " + (message.length() > 160 ? message.substring(0, 160) + "..." : message);
    }
}
