package com.craftboard.desktop.view;

import com.craftboard.core.dto.AuthLoginResponse;
import com.craftboard.core.dto.ActivityLogResponse;
import com.craftboard.core.dto.CityResponse;
import com.craftboard.core.dto.CraftOrderRequest;
import com.craftboard.core.dto.CraftOrderResponse;
import com.craftboard.core.dto.EquippedItemResponse;
import com.craftboard.core.dto.OrderItemRequest;
import com.craftboard.core.dto.OrderItemResponse;
import com.craftboard.core.dto.PlayerEquipmentResponse;
import com.craftboard.core.dto.PoleResponse;
import com.craftboard.core.dto.QuestDeliveryResponse;
import com.craftboard.core.dto.QuestRequest;
import com.craftboard.core.dto.QuestResponse;
import com.craftboard.core.dto.UserResponse;
import com.craftboard.core.enums.QuestRecurrence;
import com.craftboard.core.enums.QuestStatus;
import com.craftboard.core.enums.UserRole;
import com.craftboard.core.enums.OrderStatus;
import com.craftboard.desktop.service.ApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.Comparator;
import java.util.List;
import java.time.format.DateTimeFormatter;

/**
 * Vue JavaFX responsable de construire une partie de l'interface.
 */
public class MainView {

    private final ApiService apiService;
    private final Runnable onChangeCity;
    private final Runnable onLogout;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MainView(ApiService apiService, Runnable onChangeCity, Runnable onLogout) {
        this.apiService = apiService;
        this.onChangeCity = onChangeCity;
        this.onLogout = onLogout;
    }

    public Parent build() {
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildTabs());
        Label apiLabel = new Label("Serveur: " + apiService.getBaseUrl());
        apiLabel.setStyle("-fx-text-fill: #57606a;");
        root.setBottom(apiLabel);
        BorderPane.setMargin(root.getBottom(), new Insets(8));
        return root;
    }

    private Parent buildHeader() {
        CityResponse city = apiService.getCurrentCity();
        AuthLoginResponse session = apiService.getCurrentSession();
        String cityName = city == null ? "Aucune ville" : city.name();
        String userName = session == null ? "" : session.user().username() + " - " + roleLabel(session.user().role().name());

        Label cityLabel = new Label("Ville: " + cityName);
        Label userLabel = new Label("Connecte: " + userName);
        cityLabel.setStyle("-fx-font-weight: bold;");
        Button backButton = new Button("Changer de ville");
        Button serverButton = new Button("Serveur");
        Button logoutButton = new Button("Deconnexion");
        backButton.setOnAction(event -> onChangeCity.run());
        serverButton.setOnAction(event -> changeServer());
        logoutButton.setOnAction(event -> onLogout.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(16, cityLabel, userLabel, spacer, backButton, serverButton, logoutButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 12, 10, 12));
        header.setStyle("-fx-background-color: #f6f8fa; -fx-border-color: transparent transparent #d0d7de transparent;");
        return header;
    }

    private void changeServer() {
        TextInputDialog dialog = new TextInputDialog(apiService.getBaseUrl());
        dialog.setTitle("Serveur");
        dialog.setHeaderText("Modifier le serveur CraftBoard");
        dialog.setContentText("URL du serveur");
        dialog.showAndWait().ifPresent(url -> {
            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return apiService.testAndSaveBaseUrl(url);
                }
            };
            task.setOnSucceeded(event -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Serveur");
                alert.setHeaderText("Connexion confirmee");
                alert.setContentText(apiService.getBaseUrl() + "\n" + task.getValue());
                alert.showAndWait();
            });
            task.setOnFailed(event -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Serveur");
                alert.setHeaderText("Connexion refusee");
                alert.setContentText("Serveur conserve: " + apiService.getBaseUrl());
                alert.showAndWait();
            });
            Thread thread = new Thread(task, "craftboard-change-server");
            thread.setDaemon(true);
            thread.start();
        });
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                dashboardTab(),
                profileTab(),
                ordersTab(),
                questsTab(),
                equipmentsTab(),
                membersTab()
        );
        if (canManageOrders()) {
            tabs.getTabs().add(artisanLogTab());
        }
        if (isCurrentUserAdmin()) {
            tabs.getTabs().add(activityLogTab());
            tabs.getTabs().add(adminTab());
        }
        return tabs;
    }

    private Tab dashboardTab() {
        Label statusLabel = new Label();
        Label membersValue = dashboardValue();
        Label activeOrdersValue = dashboardValue();
        Label myOrdersValue = dashboardValue();
        Label activeQuestsValue = dashboardValue();

        ListView<String> ordersList = new ListView<>();
        ordersList.setPlaceholder(new Label("Aucune commande active."));
        ListView<String> questsList = new ListView<>();
        questsList.setPlaceholder(new Label("Aucune quete active."));
        ListView<String> activityList = new ListView<>();
        activityList.setPlaceholder(new Label("Aucune activite recente."));

        HBox counters = new HBox(12,
                dashboardCard("Membres", membersValue),
                dashboardCard("Commandes actives", activeOrdersValue),
                dashboardCard("Mes commandes", myOrdersValue),
                dashboardCard("Quetes actives", activeQuestsValue)
        );
        counters.setFillHeight(true);

        Button refreshButton = new Button("Actualiser");
        refreshButton.setOnAction(event -> loadDashboard(
                membersValue,
                activeOrdersValue,
                myOrdersValue,
                activeQuestsValue,
                ordersList,
                questsList,
                activityList,
                statusLabel
        ));

        HBox lists = new HBox(12,
                dashboardSection("Commandes a suivre", ordersList),
                dashboardSection("Quetes actives", questsList),
                dashboardSection("Activite recente", activityList)
        );
        VBox.setVgrow(lists, Priority.ALWAYS);
        HBox.setHgrow(lists.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lists.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(lists.getChildren().get(2), Priority.ALWAYS);

        VBox content = new VBox(14, new ToolBar(refreshButton, statusLabel), counters, lists);
        content.setPadding(new Insets(12));
        loadDashboard(membersValue, activeOrdersValue, myOrdersValue, activeQuestsValue, ordersList, questsList, activityList, statusLabel);

        Tab tab = new Tab("Dashboard", content);
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                loadDashboard(membersValue, activeOrdersValue, myOrdersValue, activeQuestsValue, ordersList, questsList, activityList, statusLabel);
            }
        });
        return tab;
    }

    private VBox dashboardCard(String title, Label value) {
        Label titleLabel = new Label(title);
        VBox card = new VBox(6, titleLabel, value);
        card.setPadding(new Insets(12));
        card.setMinWidth(160);
        card.setStyle("-fx-border-color: #d0d7de; -fx-border-radius: 6; -fx-background-radius: 6; -fx-background-color: #f6f8fa;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Label dashboardValue() {
        Label label = new Label("-");
        label.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        return label;
    }

    private VBox dashboardSection(String title, ListView<String> listView) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");
        VBox section = new VBox(8, titleLabel, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return section;
    }

    private Tab profileTab() {
        AuthLoginResponse session = apiService.getCurrentSession();
        UserResponse user = session == null ? null : session.user();
        CityResponse city = session == null ? null : session.city();

        Label usernameValue = new Label(user == null ? "-" : user.username());
        Label roleValue = new Label(user == null ? "-" : roleLabel(user.role().name()));
        Label cityValue = new Label(city == null ? "-" : city.name());
        Label passwordValue = new Label(passwordLabel(user == null ? null : user.passwordConfigured()));
        TextField displayNameField = new TextField(user == null ? "" : emptyFallback(user.displayName()));
        PasswordField passwordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        Label statusLabel = new Label();
        Button saveButton = new Button("Enregistrer");
        Button clearPasswordButton = new Button("Ne pas changer le mot de passe");

        passwordField.setPromptText("Nouveau mot de passe");
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        displayNameField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        confirmPasswordField.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Utilisateur"), usernameValue);
        form.addRow(1, new Label("Ville"), cityValue);
        form.addRow(2, new Label("Role"), roleValue);
        form.addRow(3, new Label("Mot de passe"), passwordValue);
        form.addRow(4, new Label("Nom affiche"), displayNameField);
        form.addRow(5, new Label("Nouveau mot de passe"), passwordField);
        form.addRow(6, new Label("Confirmation"), confirmPasswordField);
        GridPane.setHgrow(displayNameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);
        GridPane.setHgrow(confirmPasswordField, Priority.ALWAYS);

        clearPasswordButton.setOnAction(event -> {
            passwordField.clear();
            confirmPasswordField.clear();
            statusLabel.setText("Le mot de passe ne sera pas modifie.");
        });
        saveButton.setOnAction(event -> saveProfile(
                displayNameField,
                passwordField,
                confirmPasswordField,
                usernameValue,
                roleValue,
                cityValue,
                passwordValue,
                statusLabel
        ));

        HBox actions = new HBox(8, saveButton, clearPasswordButton);
        VBox content = new VBox(16, new Label("Profil utilisateur"), form, actions, statusLabel);
        content.setPadding(new Insets(18));
        return new Tab("Profil", content);
    }

    private Tab membersTab() {
        TableView<UserResponse> table = new TableView<>();
        ObservableList<UserResponse> source = FXCollections.observableArrayList();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Chargement des membres..."));
        table.getColumns().add(column("Utilisateur", UserResponse::username));
        table.getColumns().add(column("Nom affiche", user -> blankFallback(user.displayName())));
        table.getColumns().add(column("Role", user -> roleLabel(user.role().name())));
        table.getColumns().add(column("Mot de passe", user -> passwordLabel(user.passwordConfigured())));

        Label statusLabel = new Label();
        TextField searchField = searchField("Rechercher un membre");
        Button refreshButton = new Button("Actualiser");
        searchField.textProperty().addListener((observable, oldValue, value) -> applyMemberFilter(source, table, value, statusLabel));
        refreshButton.setOnAction(event -> loadMembers(table, statusLabel, source, searchField));

        ToolBar toolbar = new ToolBar(searchField, refreshButton, statusLabel);
        VBox content = new VBox(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        loadMembers(table, statusLabel, source, searchField);
        return new Tab("Membres", content);
    }

    private Tab adminTab() {
        TableView<UserResponse> table = new TableView<>();
        ObservableList<UserResponse> source = FXCollections.observableArrayList();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Aucun membre."));
        table.getColumns().add(column("Utilisateur", UserResponse::username));
        table.getColumns().add(column("Nom affiche", user -> blankFallback(user.displayName())));
        table.getColumns().add(column("Role", user -> roleLabel(user.role().name())));
        table.getColumns().add(column("Mot de passe", user -> passwordLabel(user.passwordConfigured())));

        Label statusLabel = new Label();
        TextField searchField = searchField("Rechercher un membre");
        Button refreshButton = new Button("Actualiser");
        Button roleButton = new Button("Changer role");
        Button passwordButton = new Button("Definir mot de passe");
        Button displayNameButton = new Button("Renommer");
        searchField.textProperty().addListener((observable, oldValue, value) -> applyMemberFilter(source, table, value, statusLabel));
        refreshButton.setOnAction(event -> loadMembers(table, statusLabel, source, searchField));
        roleButton.setOnAction(event -> changeRole(table.getSelectionModel().getSelectedItem(), table, statusLabel));
        passwordButton.setOnAction(event -> resetPassword(table.getSelectionModel().getSelectedItem(), table, statusLabel));
        displayNameButton.setOnAction(event -> renameUser(table.getSelectionModel().getSelectedItem(), table, statusLabel));

        ToolBar toolbar = new ToolBar(searchField, refreshButton, roleButton, passwordButton, displayNameButton, statusLabel);
        VBox content = new VBox(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        loadMembers(table, statusLabel, source, searchField);
        return new Tab("Administration", content);
    }

    private Tab activityLogTab() {
        TableView<ActivityLogResponse> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Aucune activite."));
        table.getColumns().add(activityLogColumn("Date", log -> log.createdAt() == null ? "-" : DATE_TIME_FORMAT.format(log.createdAt())));
        table.getColumns().add(activityLogColumn("Utilisateur", log -> blankFallback(log.username())));
        table.getColumns().add(activityLogColumn("Action", log -> activityLabel(log.actionType())));
        table.getColumns().add(activityLogColumn("Description", log -> blankFallback(log.description())));

        Label statusLabel = new Label();
        Button refreshButton = new Button("Actualiser");
        refreshButton.setOnAction(event -> loadActivityLogs(table, statusLabel));

        VBox content = new VBox(new ToolBar(refreshButton, statusLabel), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        loadActivityLogs(table, statusLabel);

        Tab tab = new Tab("Historique", content);
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                loadActivityLogs(table, statusLabel);
            }
        });
        return tab;
    }

    private Tab questsTab() {
        TableView<QuestResponse> questTable = new TableView<>();
        ObservableList<QuestResponse> source = FXCollections.observableArrayList();
        questTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        questTable.setPlaceholder(new Label("Chargement des quetes..."));
        questTable.getColumns().add(questColumn("Titre", QuestResponse::title));
        questTable.getColumns().add(questColumn("Frequence", quest -> recurrenceLabel(quest.recurrence())));
        questTable.getColumns().add(questColumn("Ressource", QuestResponse::resourceName));
        questTable.getColumns().add(questColumn("Objectif", quest -> numberText(quest.targetQuantity())));
        questTable.getColumns().add(questColumn("Livre", quest -> numberText(quest.deliveredQuantity())));
        questTable.getColumns().add(questColumn("Restant", quest -> numberText(quest.remainingQuantity())));
        questTable.getColumns().add(questColumn("Statut", quest -> questStatusLabel(quest.status())));

        TableView<QuestDeliveryResponse> deliveryTable = new TableView<>();
        deliveryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        deliveryTable.setPlaceholder(new Label("Selectionne une quete."));
        deliveryTable.getColumns().add(deliveryColumn("Joueur", QuestDeliveryResponse::username));
        deliveryTable.getColumns().add(deliveryColumn("Quantite livree", delivery -> numberText(delivery.deliveredQuantity())));

        questTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, quest) -> {
            deliveryTable.getItems().clear();
            if (quest != null && quest.deliveries() != null) {
                deliveryTable.getItems().setAll(quest.deliveries());
            }
        });

        Label statusLabel = new Label();
        TextField searchField = searchField("Rechercher une quete");
        Button refreshButton = new Button("Actualiser");
        Button createButton = new Button("Nouvelle quete");
        Button deliveryButton = new Button("Livrer");
        createButton.setVisible(isCurrentUserAdmin());
        createButton.setManaged(isCurrentUserAdmin());
        searchField.textProperty().addListener((observable, oldValue, value) -> applyQuestFilter(source, questTable, value, statusLabel));
        refreshButton.setOnAction(event -> loadQuests(questTable, statusLabel, source, searchField));
        createButton.setOnAction(event -> createQuest(questTable, statusLabel));
        deliveryButton.setOnAction(event -> deliverQuest(questTable.getSelectionModel().getSelectedItem(), questTable, statusLabel));

        SplitPane splitPane = new SplitPane(questTable, deliveryTable);
        splitPane.setDividerPositions(0.68);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox content = new VBox(new ToolBar(searchField, refreshButton, createButton, deliveryButton, statusLabel), splitPane);
        loadQuests(questTable, statusLabel, source, searchField);
        return new Tab("Quetes", content);
    }

    private Tab ordersTab() {
        TableView<CraftOrderResponse> orderTable = new TableView<>();
        ObservableList<CraftOrderResponse> source = FXCollections.observableArrayList();
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        orderTable.setPlaceholder(new Label("Aucune commande."));
        orderTable.getColumns().add(orderColumn("Pole", CraftOrderResponse::poleName));
        orderTable.getColumns().add(orderColumn("Demandeur", CraftOrderResponse::createdByUsername));
        orderTable.getColumns().add(orderColumn("Artisan", order -> blankFallback(order.assignedToUsername())));
        orderTable.getColumns().add(orderColumn("Statut", order -> orderStatusLabel(order.status())));
        orderTable.getColumns().add(orderColumn("Commande", this::orderSummary));
        orderTable.getColumns().add(orderColumn("Notes", order -> blankFallback(order.notes())));

        Label statusLabel = new Label();
        TextField searchField = searchField("Rechercher une commande");
        Button refreshButton = new Button("Actualiser");
        Button createButton = new Button("Nouvelle commande");
        Button assignButton = new Button("M'affecter");
        Button completeButton = new Button("Completer");
        Button cancelButton = new Button("Annuler");
        Button notesButton = new Button("Notes");
        boolean canManageOrders = canManageOrders();
        assignButton.setVisible(canManageOrders);
        assignButton.setManaged(canManageOrders);
        completeButton.setVisible(canManageOrders);
        completeButton.setManaged(canManageOrders);
        cancelButton.setVisible(canManageOrders);
        cancelButton.setManaged(canManageOrders);
        notesButton.setVisible(canManageOrders);
        notesButton.setManaged(canManageOrders);
        searchField.textProperty().addListener((observable, oldValue, value) -> applyOrderFilter(source, orderTable, value, statusLabel));
        refreshButton.setOnAction(event -> loadOrders(orderTable, statusLabel, false, source, searchField));
        createButton.setOnAction(event -> createOrder(orderTable, statusLabel));
        assignButton.setOnAction(event -> orderAction(orderTable.getSelectionModel().getSelectedItem(), orderTable, statusLabel, "assign", false));
        completeButton.setOnAction(event -> orderAction(orderTable.getSelectionModel().getSelectedItem(), orderTable, statusLabel, "complete", false));
        cancelButton.setOnAction(event -> orderAction(orderTable.getSelectionModel().getSelectedItem(), orderTable, statusLabel, "cancel", false));
        notesButton.setOnAction(event -> orderActionWithNotes(orderTable.getSelectionModel().getSelectedItem(), orderTable, statusLabel, "notes"));

        VBox content = new VBox(new ToolBar(searchField, refreshButton, createButton, assignButton, completeButton, cancelButton, notesButton, statusLabel), orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);
        loadOrders(orderTable, statusLabel, false, source, searchField);
        Tab tab = new Tab("Commandes", content);
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                loadOrders(orderTable, statusLabel, false, source, searchField);
            }
        });
        return tab;
    }

    private Tab artisanLogTab() {
        TableView<CraftOrderResponse> orderTable = new TableView<>();
        ObservableList<CraftOrderResponse> source = FXCollections.observableArrayList();
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        orderTable.setPlaceholder(new Label("Aucune commande archivee."));
        orderTable.getColumns().add(orderColumn("Pole", CraftOrderResponse::poleName));
        orderTable.getColumns().add(orderColumn("Demandeur", CraftOrderResponse::createdByUsername));
        orderTable.getColumns().add(orderColumn("Artisan", order -> blankFallback(order.assignedToUsername())));
        orderTable.getColumns().add(orderColumn("Statut", order -> orderStatusLabel(order.status())));
        orderTable.getColumns().add(orderColumn("Commande", this::orderSummary));
        orderTable.getColumns().add(orderColumn("Notes", order -> blankFallback(order.notes())));

        Label statusLabel = new Label();
        TextField searchField = searchField("Rechercher dans le log");
        Button refreshButton = new Button("Actualiser");
        Button reopenButton = new Button("Remettre en cours");
        searchField.textProperty().addListener((observable, oldValue, value) -> applyOrderFilter(source, orderTable, value, statusLabel));
        refreshButton.setOnAction(event -> loadOrders(orderTable, statusLabel, true, source, searchField));
        reopenButton.setOnAction(event -> orderAction(orderTable.getSelectionModel().getSelectedItem(), orderTable, statusLabel, "reopen", true));

        VBox content = new VBox(new ToolBar(searchField, refreshButton, reopenButton, statusLabel), orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);
        loadOrders(orderTable, statusLabel, true, source, searchField);
        Tab tab = new Tab("Log Artisans", content);
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                loadOrders(orderTable, statusLabel, true, source, searchField);
            }
        });
        return tab;
    }

    private Tab equipmentsTab() {
        TableView<UserResponse> membersTable = new TableView<>();
        ObservableList<UserResponse> memberSource = FXCollections.observableArrayList();
        membersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        membersTable.setPlaceholder(new Label("Chargement des membres..."));
        membersTable.getColumns().add(column("Joueur", UserResponse::username));
        membersTable.getColumns().add(column("Role", user -> roleLabel(user.role().name())));

        TableView<EquippedItemResponse> equipmentTable = new TableView<>();
        ObservableList<EquippedItemResponse> equipmentSource = FXCollections.observableArrayList();
        equipmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        equipmentTable.setPlaceholder(new Label("Selectionne un joueur."));
        equipmentTable.getColumns().add(equipmentColumn("Source", EquippedItemResponse::source));
        equipmentTable.getColumns().add(equipmentColumn("Slot", EquippedItemResponse::slot));
        equipmentTable.getColumns().add(equipmentColumn("Nom", EquippedItemResponse::name));
        equipmentTable.getColumns().add(equipmentColumn("Type", item -> categoryLabel(item.category() == null ? null : item.category().name())));
        equipmentTable.getColumns().add(equipmentColumn("Tier", item -> item.tier() == null ? "-" : "T" + item.tier()));
        equipmentTable.getColumns().add(equipmentColumn("Rarete", item -> blankFallback(item.rarity())));

        Label statusLabel = new Label();
        TextField memberSearchField = searchField("Rechercher joueur");
        TextField equipmentSearchField = searchField("Rechercher outil");
        Button refreshMembersButton = new Button("Actualiser joueurs");
        Button refreshEquipmentButton = new Button("Actualiser equipement");
        memberSearchField.textProperty().addListener((observable, oldValue, value) -> applyMemberFilter(memberSource, membersTable, value, statusLabel));
        equipmentSearchField.textProperty().addListener((observable, oldValue, value) -> applyEquipmentFilter(equipmentSource, equipmentTable, value, statusLabel));
        refreshMembersButton.setOnAction(event -> loadMembers(membersTable, statusLabel, memberSource, memberSearchField));
        refreshEquipmentButton.setOnAction(event -> {
            UserResponse selectedUser = membersTable.getSelectionModel().getSelectedItem();
            if (selectedUser == null) {
                statusLabel.setText("Selectionne un joueur.");
                return;
            }
            loadPlayerEquipment(selectedUser, equipmentTable, statusLabel, equipmentSource, equipmentSearchField);
        });

        membersTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, user) -> {
            if (user != null) {
                loadPlayerEquipment(user, equipmentTable, statusLabel, equipmentSource, equipmentSearchField);
            }
        });

        SplitPane splitPane = new SplitPane(membersTable, equipmentTable);
        splitPane.setDividerPositions(0.28);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox content = new VBox(new ToolBar(memberSearchField, equipmentSearchField, refreshMembersButton, refreshEquipmentButton, statusLabel), splitPane);
        loadMembers(membersTable, statusLabel, memberSource, memberSearchField);
        return new Tab("Equipements", content);
    }

    private boolean isCurrentUserAdmin() {
        AuthLoginResponse session = apiService.getCurrentSession();
        return session != null && "ADMIN".equals(session.user().role().name());
    }

    private boolean canManageOrders() {
        AuthLoginResponse session = apiService.getCurrentSession();
        return session != null
                && ("ADMIN".equals(session.user().role().name()) || "ARTISAN".equals(session.user().role().name()));
    }

    private TableColumn<UserResponse, String> column(String title, java.util.function.Function<UserResponse, String> getter) {
        TableColumn<UserResponse, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        return column;
    }

    private TableColumn<QuestResponse, String> questColumn(
            String title,
            java.util.function.Function<QuestResponse, String> getter) {
        TableColumn<QuestResponse, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        return column;
    }

    private TableColumn<CraftOrderResponse, String> orderColumn(
            String title,
            java.util.function.Function<CraftOrderResponse, String> getter) {
        TableColumn<CraftOrderResponse, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        return column;
    }

    private TableColumn<QuestDeliveryResponse, String> deliveryColumn(
            String title,
            java.util.function.Function<QuestDeliveryResponse, String> getter) {
        TableColumn<QuestDeliveryResponse, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        return column;
    }

    private TableColumn<EquippedItemResponse, String> equipmentColumn(
            String title,
            java.util.function.Function<EquippedItemResponse, String> getter) {
        TableColumn<EquippedItemResponse, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        return column;
    }

    private TableColumn<ActivityLogResponse, String> activityLogColumn(
            String title,
            java.util.function.Function<ActivityLogResponse, String> getter) {
        TableColumn<ActivityLogResponse, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        return column;
    }

    private TextField searchField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setPrefWidth(190);
        return field;
    }

    private void loadDashboard(
            Label membersValue,
            Label activeOrdersValue,
            Label myOrdersValue,
            Label activeQuestsValue,
            ListView<String> ordersList,
            ListView<String> questsList,
            ListView<String> activityList,
            Label statusLabel) {
        CityResponse city = apiService.getCurrentCity();
        AuthLoginResponse session = apiService.getCurrentSession();
        if (city == null || session == null) {
            statusLabel.setText("Aucune session active.");
            return;
        }
        statusLabel.setText("Chargement dashboard...");
        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                List<ActivityLogResponse> logs;
                try {
                    logs = apiService.getActivityLogsByCity(city.id());
                } catch (Exception exception) {
                    logs = List.of();
                }
                return new DashboardData(
                        apiService.getUsersByCity(city.id()),
                        apiService.getOrdersByCity(city.id()),
                        apiService.getQuestsByCity(city.id()),
                        logs
                );
            }
        };
        task.setOnSucceeded(event -> {
            DashboardData data = task.getValue();
            List<CraftOrderResponse> activeOrders = data.orders().stream()
                    .filter(order -> !isArchivedOrder(order))
                    .toList();
            List<CraftOrderResponse> myOrders = activeOrders.stream()
                    .filter(order -> order.assignedTo() != null && order.assignedTo().equals(session.user().id()))
                    .toList();
            List<QuestResponse> activeQuests = data.quests().stream()
                    .filter(quest -> quest.status() != QuestStatus.COMPLETED)
                    .toList();

            membersValue.setText(String.valueOf(data.users().size()));
            activeOrdersValue.setText(String.valueOf(activeOrders.size()));
            myOrdersValue.setText(String.valueOf(myOrders.size()));
            activeQuestsValue.setText(String.valueOf(activeQuests.size()));

            ordersList.getItems().setAll(activeOrders.stream()
                    .sorted(Comparator.comparing(CraftOrderResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(8)
                    .map(this::dashboardOrderLine)
                    .toList());
            questsList.getItems().setAll(activeQuests.stream()
                    .sorted(Comparator.comparing(QuestResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(8)
                    .map(this::dashboardQuestLine)
                    .toList());
            activityList.getItems().setAll(data.logs().stream()
                    .limit(10)
                    .map(this::dashboardActivityLine)
                    .toList());
            statusLabel.setText("Dashboard a jour.");
        });
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Dashboard impossible", task.getException())));
        Thread thread = new Thread(task, "craftboard-dashboard");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadActivityLogs(TableView<ActivityLogResponse> table, Label statusLabel) {
        CityResponse city = apiService.getCurrentCity();
        if (city == null) {
            statusLabel.setText("Aucune ville active.");
            return;
        }
        statusLabel.setText("Chargement historique...");
        Task<List<ActivityLogResponse>> task = new Task<>() {
            @Override
            protected List<ActivityLogResponse> call() throws Exception {
                return apiService.getActivityLogsByCity(city.id());
            }
        };
        task.setOnSucceeded(event -> {
            table.getItems().setAll(task.getValue());
            statusLabel.setText(task.getValue().size() + " activite(s).");
        });
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Historique impossible", task.getException())));
        Thread thread = new Thread(task, "craftboard-load-activity-logs");
        thread.setDaemon(true);
        thread.start();
    }

    private void saveProfile(
            TextField displayNameField,
            PasswordField passwordField,
            PasswordField confirmPasswordField,
            Label usernameValue,
            Label roleValue,
            Label cityValue,
            Label passwordValue,
            Label statusLabel) {
        String displayName = displayNameField.getText() == null ? "" : displayNameField.getText().trim();
        String password = passwordField.getText();
        String confirmation = confirmPasswordField.getText();
        if (displayName.isBlank()) {
            statusLabel.setText("Le nom affiche est requis.");
            return;
        }
        if (password != null && !password.isBlank() && !password.equals(confirmation)) {
            statusLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }
        String passwordToUpdate = password == null || password.isBlank() ? null : password;
        statusLabel.setText("Mise a jour du profil...");
        Task<UserResponse> task = new Task<>() {
            @Override
            protected UserResponse call() throws Exception {
                return apiService.updateCurrentUserProfile(displayName, passwordToUpdate);
            }
        };
        task.setOnSucceeded(event -> {
            UserResponse updated = task.getValue();
            CityResponse city = apiService.getCurrentCity();
            usernameValue.setText(updated.username());
            roleValue.setText(roleLabel(updated.role().name()));
            cityValue.setText(city == null ? "-" : city.name());
            passwordValue.setText(passwordLabel(updated.passwordConfigured()));
            displayNameField.setText(emptyFallback(updated.displayName()));
            passwordField.clear();
            confirmPasswordField.clear();
            statusLabel.setText("Profil mis a jour.");
        });
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Profil impossible", task.getException())));
        Thread thread = new Thread(task, "craftboard-save-profile");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadMembers(TableView<UserResponse> table, Label statusLabel) {
        loadMembers(table, statusLabel, null, null);
    }

    private void loadMembers(
            TableView<UserResponse> table,
            Label statusLabel,
            ObservableList<UserResponse> source,
            TextField searchField) {
        CityResponse city = apiService.getCurrentCity();
        if (city == null) {
            statusLabel.setText("Aucune ville active.");
            return;
        }
        statusLabel.setText("Chargement...");
        Task<java.util.List<UserResponse>> task = new Task<>() {
            @Override
            protected java.util.List<UserResponse> call() throws Exception {
                return apiService.getUsersByCity(city.id());
            }
        };
        task.setOnSucceeded(event -> {
            if (source == null) {
                table.getItems().setAll(task.getValue());
                statusLabel.setText(task.getValue().size() + " membre(s).");
            } else {
                source.setAll(task.getValue());
                applyMemberFilter(source, table, searchField == null ? "" : searchField.getText(), statusLabel);
            }
        });
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Chargement impossible", task.getException())));
        Thread thread = new Thread(task, "craftboard-load-members");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadPlayerEquipment(
            UserResponse user,
            TableView<EquippedItemResponse> table,
            Label statusLabel) {
        loadPlayerEquipment(user, table, statusLabel, null, null);
    }

    private void loadPlayerEquipment(
            UserResponse user,
            TableView<EquippedItemResponse> table,
            Label statusLabel,
            ObservableList<EquippedItemResponse> source,
            TextField searchField) {
        table.getItems().clear();
        if (source != null) {
            source.clear();
        }
        statusLabel.setText("Chargement equipements de " + user.username() + "...");
        Task<PlayerEquipmentResponse> task = new Task<>() {
            @Override
            protected PlayerEquipmentResponse call() throws Exception {
                return apiService.getBitjitaEquippedItems(user.id());
            }
        };
        task.setOnSucceeded(event -> {
            if (source == null) {
                table.getItems().setAll(task.getValue().items());
                statusLabel.setText(task.getValue().items().size() + " outil(s)/armure(s) equipes.");
            } else {
                source.setAll(task.getValue().items());
                applyEquipmentFilter(source, table, searchField == null ? "" : searchField.getText(), statusLabel);
            }
        });
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Equipements impossibles", task.getException())));
        Thread thread = new Thread(task, "craftboard-load-equipment");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadQuests(TableView<QuestResponse> table, Label statusLabel) {
        loadQuests(table, statusLabel, null, null);
    }

    private void loadQuests(
            TableView<QuestResponse> table,
            Label statusLabel,
            ObservableList<QuestResponse> source,
            TextField searchField) {
        CityResponse city = apiService.getCurrentCity();
        if (city == null) {
            statusLabel.setText("Aucune ville active.");
            return;
        }
        statusLabel.setText("Chargement quetes...");
        Task<java.util.List<QuestResponse>> task = new Task<>() {
            @Override
            protected java.util.List<QuestResponse> call() throws Exception {
                return apiService.getQuestsByCity(city.id());
            }
        };
        task.setOnSucceeded(event -> {
            if (source == null) {
                table.getItems().setAll(task.getValue());
                statusLabel.setText(task.getValue().size() + " quete(s).");
            } else {
                source.setAll(task.getValue());
                applyQuestFilter(source, table, searchField == null ? "" : searchField.getText(), statusLabel);
            }
        });
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Quetes impossibles", task.getException())));
        Thread thread = new Thread(task, "craftboard-load-quests");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadOrders(TableView<CraftOrderResponse> table, Label statusLabel, boolean archivedOnly) {
        loadOrders(table, statusLabel, archivedOnly, null, null);
    }

    private void loadOrders(
            TableView<CraftOrderResponse> table,
            Label statusLabel,
            boolean archivedOnly,
            ObservableList<CraftOrderResponse> source,
            TextField searchField) {
        CityResponse city = apiService.getCurrentCity();
        if (city == null) {
            statusLabel.setText("Aucune ville active.");
            return;
        }
        statusLabel.setText("Chargement commandes...");
        Task<java.util.List<CraftOrderResponse>> task = new Task<>() {
            @Override
            protected java.util.List<CraftOrderResponse> call() throws Exception {
                return apiService.getOrdersByCity(city.id());
            }
        };
        task.setOnSucceeded(event -> {
            java.util.List<CraftOrderResponse> filtered = task.getValue().stream()
                    .filter(order -> archivedOnly == isArchivedOrder(order))
                    .toList();
            if (source == null) {
                table.getItems().setAll(filtered);
                statusLabel.setText(filtered.size() + " commande(s).");
            } else {
                source.setAll(filtered);
                applyOrderFilter(source, table, searchField == null ? "" : searchField.getText(), statusLabel);
            }
        });
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Commandes impossibles", task.getException())));
        Thread thread = new Thread(task, "craftboard-load-orders");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyMemberFilter(
            ObservableList<UserResponse> source,
            TableView<UserResponse> table,
            String query,
            Label statusLabel) {
        String normalized = normalizeSearch(query);
        List<UserResponse> filtered = source.stream()
                .filter(user -> normalized.isBlank() || memberSearchText(user).contains(normalized))
                .toList();
        table.getItems().setAll(filtered);
        statusLabel.setText(filtered.size() + " membre(s).");
    }

    private void applyOrderFilter(
            ObservableList<CraftOrderResponse> source,
            TableView<CraftOrderResponse> table,
            String query,
            Label statusLabel) {
        String normalized = normalizeSearch(query);
        List<CraftOrderResponse> filtered = source.stream()
                .filter(order -> normalized.isBlank() || orderSearchText(order).contains(normalized))
                .toList();
        table.getItems().setAll(filtered);
        statusLabel.setText(filtered.size() + " commande(s).");
    }

    private void applyQuestFilter(
            ObservableList<QuestResponse> source,
            TableView<QuestResponse> table,
            String query,
            Label statusLabel) {
        String normalized = normalizeSearch(query);
        List<QuestResponse> filtered = source.stream()
                .filter(quest -> normalized.isBlank() || questSearchText(quest).contains(normalized))
                .toList();
        table.getItems().setAll(filtered);
        statusLabel.setText(filtered.size() + " quete(s).");
    }

    private void applyEquipmentFilter(
            ObservableList<EquippedItemResponse> source,
            TableView<EquippedItemResponse> table,
            String query,
            Label statusLabel) {
        String normalized = normalizeSearch(query);
        List<EquippedItemResponse> filtered = source.stream()
                .filter(item -> normalized.isBlank() || equipmentSearchText(item).contains(normalized))
                .toList();
        table.getItems().setAll(filtered);
        statusLabel.setText(filtered.size() + " equipement(s).");
    }

    private void createOrder(TableView<CraftOrderResponse> table, Label statusLabel) {
        CityResponse city = apiService.getCurrentCity();
        AuthLoginResponse session = apiService.getCurrentSession();
        if (city == null || session == null) {
            statusLabel.setText("Aucune session active.");
            return;
        }
        statusLabel.setText("Chargement poles...");
        Task<java.util.List<PoleResponse>> polesTask = new Task<>() {
            @Override
            protected java.util.List<PoleResponse> call() throws Exception {
                return apiService.getPolesByCity(city.id());
            }
        };
        polesTask.setOnSucceeded(event -> {
            OrderDialog dialog = new OrderDialog(polesTask.getValue());
            dialog.showAndWait().ifPresent(form -> submitOrder(form, city, session, table, statusLabel));
        });
        polesTask.setOnFailed(event -> statusLabel.setText(errorMessage("Poles impossibles", polesTask.getException())));
        Thread thread = new Thread(polesTask, "craftboard-load-poles-for-order");
        thread.setDaemon(true);
        thread.start();
    }

    private void submitOrder(
            OrderForm form,
            CityResponse city,
            AuthLoginResponse session,
            TableView<CraftOrderResponse> table,
            Label statusLabel) {
        statusLabel.setText("Creation commande...");
        CraftOrderRequest request = new CraftOrderRequest(
                city.id(),
                session.user().id(),
                form.pole().id(),
                com.craftboard.core.enums.OrderStatus.PENDING,
                java.util.List.of(new OrderItemRequest(
                        form.category(),
                        form.tier(),
                        form.rarity(),
                        form.quantity(),
                        form.toolType(),
                        form.equipmentType(),
                        form.armorMaterial()
                ))
        );
        Task<CraftOrderResponse> task = new Task<>() {
            @Override
            protected CraftOrderResponse call() throws Exception {
                return apiService.createOrder(request);
            }
        };
        task.setOnSucceeded(event -> loadOrders(table, statusLabel, false));
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Creation commande impossible", task.getException())));
        Thread thread = new Thread(task, "craftboard-create-order");
        thread.setDaemon(true);
        thread.start();
    }

    private void orderAction(
            CraftOrderResponse order,
            TableView<CraftOrderResponse> table,
            Label statusLabel,
            String action) {
        orderAction(order, table, statusLabel, action, false);
    }

    private void orderAction(
            CraftOrderResponse order,
            TableView<CraftOrderResponse> table,
            Label statusLabel,
            String action,
            boolean archivedOnly) {
        AuthLoginResponse session = apiService.getCurrentSession();
        if (order == null) {
            statusLabel.setText("Selectionne une commande.");
            return;
        }
        if (session == null) {
            statusLabel.setText("Aucune session active.");
            return;
        }
        statusLabel.setText("Mise a jour commande...");
        Task<CraftOrderResponse> task = new Task<>() {
            @Override
            protected CraftOrderResponse call() throws Exception {
                if ("assign".equals(action)) {
                    return apiService.assignOrder(order.id(), session.user().id());
                }
                if ("complete".equals(action)) {
                    return apiService.completeOrder(order.id(), session.user().id(), null);
                }
                if ("cancel".equals(action)) {
                    return apiService.cancelOrder(order.id(), session.user().id(), null);
                }
                if ("reopen".equals(action)) {
                    return apiService.reopenOrder(order.id(), session.user().id(), null);
                }
                throw new IllegalArgumentException("Action inconnue: " + action);
            }
        };
        task.setOnSucceeded(event -> loadOrders(table, statusLabel, archivedOnly));
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Action impossible", task.getException())));
        Thread thread = new Thread(task, "craftboard-order-action");
        thread.setDaemon(true);
        thread.start();
    }

    private void orderActionWithNotes(
            CraftOrderResponse order,
            TableView<CraftOrderResponse> table,
            Label statusLabel,
            String action) {
        AuthLoginResponse session = apiService.getCurrentSession();
        if (order == null) {
            statusLabel.setText("Selectionne une commande.");
            return;
        }
        if (session == null) {
            statusLabel.setText("Aucune session active.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(blankFallback(order.notes()).equals("-") ? "" : order.notes());
        dialog.setTitle("Notes commande");
        dialog.setHeaderText(orderSummary(order));
        dialog.setContentText("Notes");
        dialog.showAndWait().ifPresent(notes -> {
            statusLabel.setText("Mise a jour commande...");
            Task<CraftOrderResponse> task = new Task<>() {
                @Override
                protected CraftOrderResponse call() throws Exception {
                    return switch (action) {
                        case "complete" -> apiService.completeOrder(order.id(), session.user().id(), notes);
                        case "cancel" -> apiService.cancelOrder(order.id(), session.user().id(), notes);
                        case "notes" -> apiService.updateOrderNotes(order.id(), session.user().id(), notes);
                        default -> throw new IllegalArgumentException("Action inconnue: " + action);
                    };
                }
            };
            task.setOnSucceeded(event -> loadOrders(table, statusLabel, false));
            task.setOnFailed(event -> statusLabel.setText(errorMessage("Action impossible", task.getException())));
            Thread thread = new Thread(task, "craftboard-order-action-notes");
            thread.setDaemon(true);
            thread.start();
        });
    }

    private void createQuest(TableView<QuestResponse> table, Label statusLabel) {
        CityResponse city = apiService.getCurrentCity();
        AuthLoginResponse session = apiService.getCurrentSession();
        if (city == null || session == null) {
            statusLabel.setText("Aucune session active.");
            return;
        }
        QuestDialog dialog = new QuestDialog();
        dialog.showAndWait().ifPresent(form -> {
            statusLabel.setText("Creation quete...");
            QuestRequest request = new QuestRequest(
                    city.id(),
                    form.title(),
                    form.description(),
                    QuestStatus.PENDING,
                    form.recurrence(),
                    form.resourceName(),
                    form.targetQuantity(),
                    session.user().id()
            );
            Task<QuestResponse> task = new Task<>() {
                @Override
                protected QuestResponse call() throws Exception {
                    return apiService.createQuest(request);
                }
            };
            task.setOnSucceeded(event -> loadQuests(table, statusLabel));
            task.setOnFailed(event -> statusLabel.setText(errorMessage("Creation impossible", task.getException())));
            Thread thread = new Thread(task, "craftboard-create-quest");
            thread.setDaemon(true);
            thread.start();
        });
    }

    private void deliverQuest(QuestResponse quest, TableView<QuestResponse> table, Label statusLabel) {
        AuthLoginResponse session = apiService.getCurrentSession();
        if (quest == null) {
            statusLabel.setText("Selectionne une quete.");
            return;
        }
        if (session == null) {
            statusLabel.setText("Aucune session active.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Livrer");
        dialog.setHeaderText(quest.title());
        dialog.setContentText("Quantite livree");
        dialog.showAndWait().ifPresent(value -> {
            int quantity;
            try {
                quantity = Integer.parseInt(value.trim());
            } catch (NumberFormatException exception) {
                statusLabel.setText("Quantite invalide.");
                return;
            }
            statusLabel.setText("Livraison...");
            Task<QuestResponse> task = new Task<>() {
                @Override
                protected QuestResponse call() throws Exception {
                    return apiService.deliverQuest(quest.id(), session.user().id(), quantity);
                }
            };
            task.setOnSucceeded(event -> loadQuests(table, statusLabel));
            task.setOnFailed(event -> statusLabel.setText(errorMessage("Livraison impossible", task.getException())));
            Thread thread = new Thread(task, "craftboard-deliver-quest");
            thread.setDaemon(true);
            thread.start();
        });
    }

    private void changeRole(UserResponse user, TableView<UserResponse> table, Label statusLabel) {
        if (user == null) {
            statusLabel.setText("Selectionne un membre.");
            return;
        }
        ChoiceDialog<UserRole> dialog = new ChoiceDialog<>(user.role(), UserRole.CITIZEN, UserRole.ARTISAN, UserRole.ADMIN);
        dialog.setTitle("Changer role");
        dialog.setHeaderText(user.username());
        dialog.setContentText("Role");
        dialog.showAndWait().ifPresent(role -> adminUpdate(user, role, null, null, table, statusLabel));
    }

    private void resetPassword(UserResponse user, TableView<UserResponse> table, Label statusLabel) {
        if (user == null) {
            statusLabel.setText("Selectionne un membre.");
            return;
        }
        PasswordInputDialog dialog = new PasswordInputDialog();
        dialog.setTitle("Definir mot de passe");
        dialog.setHeaderText(user.username());
        dialog.setContentText("Nouveau mot de passe");
        dialog.showAndWait().ifPresent(password -> adminUpdate(user, null, password, null, table, statusLabel));
    }

    private void renameUser(UserResponse user, TableView<UserResponse> table, Label statusLabel) {
        if (user == null) {
            statusLabel.setText("Selectionne un membre.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(blankFallback(user.displayName()));
        dialog.setTitle("Renommer");
        dialog.setHeaderText(user.username());
        dialog.setContentText("Nom affiche");
        dialog.showAndWait().ifPresent(displayName -> adminUpdate(user, null, null, displayName, table, statusLabel));
    }

    private void adminUpdate(
            UserResponse user,
            UserRole role,
            String password,
            String displayName,
            TableView<UserResponse> table,
            Label statusLabel) {
        statusLabel.setText("Mise a jour...");
        Task<UserResponse> task = new Task<>() {
            @Override
            protected UserResponse call() throws Exception {
                return apiService.adminUpdateUser(user.id(), role, password, displayName);
            }
        };
        task.setOnSucceeded(event -> loadMembers(table, statusLabel));
        task.setOnFailed(event -> statusLabel.setText(errorMessage("Mise a jour impossible", task.getException())));
        Thread thread = new Thread(task, "craftboard-admin-update-user");
        thread.setDaemon(true);
        thread.start();
    }

    private String dashboardOrderLine(CraftOrderResponse order) {
        return orderStatusLabel(order.status())
                + " | " + blankFallback(order.poleName())
                + " | " + orderSummary(order)
                + " | demandeur: " + blankFallback(order.createdByUsername())
                + " | artisan: " + blankFallback(order.assignedToUsername());
    }

    private String dashboardQuestLine(QuestResponse quest) {
        return recurrenceLabel(quest.recurrence())
                + " | " + quest.title()
                + " | " + numberText(quest.deliveredQuantity())
                + "/" + numberText(quest.targetQuantity())
                + " " + blankFallback(quest.resourceName());
    }

    private String dashboardActivityLine(ActivityLogResponse log) {
        return blankFallback(log.username()) + " | " + activityLabel(log.actionType()) + " | " + blankFallback(log.description());
    }

    private String normalizeSearch(String query) {
        return query == null ? "" : query.trim().toLowerCase();
    }

    private String searchable(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String memberSearchText(UserResponse user) {
        return searchable(user.username())
                + " " + searchable(user.displayName())
                + " " + searchable(roleLabel(user.role().name()))
                + " " + searchable(passwordLabel(user.passwordConfigured()));
    }

    private String orderSearchText(CraftOrderResponse order) {
        return searchable(order.poleName())
                + " " + searchable(order.createdByUsername())
                + " " + searchable(order.assignedToUsername())
                + " " + searchable(orderStatusLabel(order.status()))
                + " " + searchable(orderSummary(order))
                + " " + searchable(order.notes());
    }

    private String questSearchText(QuestResponse quest) {
        return searchable(quest.title())
                + " " + searchable(recurrenceLabel(quest.recurrence()))
                + " " + searchable(quest.resourceName())
                + " " + searchable(questStatusLabel(quest.status()))
                + " " + numberText(quest.targetQuantity())
                + " " + numberText(quest.deliveredQuantity());
    }

    private String equipmentSearchText(EquippedItemResponse item) {
        return searchable(item.source())
                + " " + searchable(item.slot())
                + " " + searchable(item.name())
                + " " + searchable(item.category() == null ? null : categoryLabel(item.category().name()))
                + " " + searchable(item.rarity())
                + " " + searchable(item.tier() == null ? null : "T" + item.tier());
    }

    private String blankFallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String emptyFallback(String value) {
        return value == null ? "" : value;
    }

    private String activityLabel(String actionType) {
        if (actionType == null) {
            return "-";
        }
        return switch (actionType) {
            case "ADMIN_CREATED" -> "Admin cree";
            case "ACCOUNT_ACTIVATED" -> "Compte active";
            case "ORDER_CREATED" -> "Commande creee";
            case "ORDER_ASSIGNED" -> "Commande assignee";
            case "ORDER_COMPLETED" -> "Commande completee";
            case "ORDER_CANCELLED" -> "Commande annulee";
            case "ORDER_REOPENED" -> "Commande remise en cours";
            case "ORDER_NOTES_UPDATED" -> "Notes commande";
            case "QUEST_CREATED" -> "Quete creee";
            case "QUEST_DELIVERED" -> "Livraison quete";
            case "QUEST_COMPLETED" -> "Quete completee";
            case "QUEST_RESET" -> "Quete reinitialisee";
            case "PROFILE_UPDATED" -> "Profil modifie";
            case "USER_ADMIN_UPDATED" -> "Utilisateur modifie";
            default -> actionType;
        };
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "CITIZEN" -> "Citoyen";
            case "ARTISAN" -> "Artisan";
            case "ADMIN" -> "Admin";
            default -> role;
        };
    }

    private String passwordLabel(Boolean configured) {
        return Boolean.TRUE.equals(configured) ? "Configure" : "A definir";
    }

    private String orderStatusLabel(OrderStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case PENDING -> "En attente";
            case IN_PROGRESS -> "En cours";
            case COMPLETED -> "Terminee";
            case CANCELLED -> "Annulee";
        };
    }

    private String questStatusLabel(QuestStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case PENDING -> "En attente";
            case IN_PROGRESS -> "En cours";
            case COMPLETED -> "Terminee";
        };
    }

    private String categoryLabel(String category) {
        if (category == null) {
            return "-";
        }
        return switch (category) {
            case "TOOL" -> "Outil";
            case "ARMOR" -> "Equipement";
            default -> category;
        };
    }

    private String orderSummary(CraftOrderResponse order) {
        if (order.items() == null || order.items().isEmpty()) {
            return "-";
        }
        return order.items().stream()
                .map(this::itemSummary)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private String itemSummary(OrderItemResponse item) {
        String type = "TOOL".equals(item.category())
                ? blankFallback(item.toolType())
                : blankFallback(item.equipmentType());
        String material = item.armorMaterial() == null || item.armorMaterial().isBlank()
                ? ""
                : " " + item.armorMaterial();
        return item.quantity() + "x " + type + material + " " + item.tier() + " " + item.rarity();
    }

    private boolean isArchivedOrder(CraftOrderResponse order) {
        return order.status() == OrderStatus.COMPLETED || order.status() == OrderStatus.CANCELLED;
    }

    private String recurrenceLabel(QuestRecurrence recurrence) {
        if (recurrence == null) {
            return "-";
        }
        return switch (recurrence) {
            case DAILY -> "Journaliere";
            case WEEKLY -> "Hebdomadaire";
            case ONCE -> "Une fois";
        };
    }

    private String numberText(Integer value) {
        return value == null ? "0" : value.toString();
    }

    private String errorMessage(String prefix, Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return prefix + ".";
        }
        String message = throwable.getMessage();
        return prefix + ": " + (message.length() > 180 ? message.substring(0, 180) + "..." : message);
    }

    private static final class PasswordInputDialog extends Dialog<String> {

        private final PasswordField passwordField = new PasswordField();

        private PasswordInputDialog() {
            getDialogPane().setContent(passwordField);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            setResultConverter(buttonType -> buttonType == ButtonType.OK ? passwordField.getText() : null);
        }
    }

    private static final class QuestDialog extends Dialog<QuestForm> {

        private final TextField titleField = new TextField();
        private final TextField resourceField = new TextField();
        private final TextField targetField = new TextField("1");
        private final TextArea descriptionArea = new TextArea();
        private final ChoiceBox<QuestRecurrence> recurrenceChoice = new ChoiceBox<>();

        private QuestDialog() {
            setTitle("Nouvelle quete");
            titleField.setPromptText("Titre");
            resourceField.setPromptText("Ressource");
            descriptionArea.setPromptText("Description");
            descriptionArea.setPrefRowCount(3);
            recurrenceChoice.getItems().setAll(QuestRecurrence.DAILY, QuestRecurrence.WEEKLY, QuestRecurrence.ONCE);
            recurrenceChoice.setValue(QuestRecurrence.ONCE);

            VBox content = new VBox(8,
                    new Label("Titre"), titleField,
                    new Label("Frequence"), recurrenceChoice,
                    new Label("Ressource demandee"), resourceField,
                    new Label("Quantite cible"), targetField,
                    new Label("Description"), descriptionArea
            );
            content.setPadding(new Insets(8));
            getDialogPane().setContent(content);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            setResultConverter(buttonType -> {
                if (buttonType != ButtonType.OK) {
                    return null;
                }
                int target;
                try {
                    target = Integer.parseInt(targetField.getText().trim());
                } catch (NumberFormatException exception) {
                    target = 1;
                }
                return new QuestForm(
                        titleField.getText(),
                        descriptionArea.getText(),
                        recurrenceChoice.getValue(),
                        resourceField.getText(),
                        target
                );
            });
        }
    }

    private record QuestForm(
            String title,
            String description,
            QuestRecurrence recurrence,
            String resourceName,
            Integer targetQuantity
    ) {
    }

    private record DashboardData(
            List<UserResponse> users,
            List<CraftOrderResponse> orders,
            List<QuestResponse> quests,
            List<ActivityLogResponse> logs
    ) {
    }

    private static final class OrderDialog extends Dialog<OrderForm> {

        private final ChoiceBox<PoleResponse> poleChoice = new ChoiceBox<>();
        private final ChoiceBox<String> categoryChoice = new ChoiceBox<>();
        private final ChoiceBox<String> typeChoice = new ChoiceBox<>();
        private final ChoiceBox<String> tierChoice = new ChoiceBox<>();
        private final ChoiceBox<String> rarityChoice = new ChoiceBox<>();
        private final TextField quantityField = new TextField("1");

        private OrderDialog(java.util.List<PoleResponse> poles) {
            setTitle("Nouvelle commande");
            poleChoice.getItems().setAll(poles);
            poleChoice.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(PoleResponse pole) {
                    return pole == null ? "" : pole.name();
                }

                @Override
                public PoleResponse fromString(String string) {
                    return null;
                }
            });
            if (!poles.isEmpty()) {
                poleChoice.setValue(poles.get(0));
            }
            categoryChoice.getItems().setAll("TOOL", "ARMOR");
            categoryChoice.setValue("TOOL");
            tierChoice.getItems().setAll("T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9");
            tierChoice.setValue("T1");
            refreshRarities();

            poleChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshTypes());
            categoryChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshTypes());
            tierChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshRarities());
            refreshTypes();

            VBox content = new VBox(8,
                    new Label("Pole"), poleChoice,
                    new Label("Categorie"), categoryChoice,
                    new Label("Type"), typeChoice,
                    new Label("Tier"), tierChoice,
                    new Label("Rarete"), rarityChoice,
                    new Label("Quantite"), quantityField
            );
            content.setPadding(new Insets(8));
            getDialogPane().setContent(content);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            setResultConverter(buttonType -> {
                if (buttonType != ButtonType.OK) {
                    return null;
                }
                int quantity;
                try {
                    quantity = Integer.parseInt(quantityField.getText().trim());
                } catch (NumberFormatException exception) {
                    quantity = 1;
                }
                String category = categoryChoice.getValue();
                String rarity = rarityChoice.isDisabled() ? "Common" : rarityChoice.getValue();
                return new OrderForm(
                        poleChoice.getValue(),
                        category,
                        tierChoice.getValue(),
                        rarity,
                        quantity,
                        "TOOL".equals(category) ? typeChoice.getValue() : null,
                        "ARMOR".equals(category) ? typeChoice.getValue() : null,
                        armorMaterial(poleChoice.getValue())
                );
            });
        }

        private void refreshTypes() {
            PoleResponse pole = poleChoice.getValue();
            String poleName = pole == null ? "" : pole.name();
            if ("LeatherWorking".equalsIgnoreCase(poleName) || "Couture".equalsIgnoreCase(poleName) || "Maconnerie".equalsIgnoreCase(poleName)) {
                categoryChoice.setValue("ARMOR");
                categoryChoice.setDisable(true);
            } else {
                categoryChoice.setDisable(false);
            }
            String category = categoryChoice.getValue();
            typeChoice.getItems().clear();
            if ("Maconnerie".equalsIgnoreCase(poleName)) {
                typeChoice.getItems().setAll("Anneau");
                rarityChoice.setValue("Common");
                rarityChoice.setDisable(true);
            } else if ("ARMOR".equals(category)) {
                typeChoice.getItems().setAll("Casque", "Plastron", "Gants", "Ceinture", "Jambes", "Bottes");
                rarityChoice.setDisable(false);
                refreshRarities();
            } else if ("Maconnerie".equalsIgnoreCase(poleName)) {
                typeChoice.getItems().setAll("Anneau");
                rarityChoice.setValue("Common");
                rarityChoice.setDisable(true);
            } else {
                typeChoice.getItems().setAll("Hache", "Pioche", "Scie", "Ciseau", "Houe", "Machette", "Canne a peche", "Plume");
                rarityChoice.setDisable(false);
                refreshRarities();
            }
            if (!typeChoice.getItems().isEmpty()) {
                typeChoice.setValue(typeChoice.getItems().get(0));
            }
        }

        private String armorMaterial(PoleResponse pole) {
            if (pole == null) {
                return null;
            }
            return switch (pole.name()) {
                case "LeatherWorking" -> "Cuir";
                case "Couture" -> "Tissu";
                case "Forge" -> "Metal";
                default -> null;
            };
        }

        private void refreshRarities() {
            java.util.List<String> allowed = allowedRarities(tierChoice.getValue());
            String current = rarityChoice.getValue();
            rarityChoice.getItems().setAll(allowed);
            if (current != null && allowed.contains(current)) {
                rarityChoice.setValue(current);
            } else {
                rarityChoice.setValue(allowed.get(allowed.size() - 1));
            }
        }

        private java.util.List<String> allowedRarities(String tier) {
            int tierNumber = parseTier(tier);
            java.util.List<String> rarities = java.util.List.of("Common", "Uncommon", "Rare", "Epic", "Legendary");
            int maxIndex = Math.min(4, Math.max(0, tierNumber - 1));
            return rarities.subList(0, maxIndex + 1);
        }

        private int parseTier(String tier) {
            if (tier == null || tier.isBlank()) {
                return 1;
            }
            try {
                return Integer.parseInt(tier.replace("T", ""));
            } catch (NumberFormatException exception) {
                return 1;
            }
        }
    }

    private record OrderForm(
            PoleResponse pole,
            String category,
            String tier,
            String rarity,
            Integer quantity,
            String toolType,
            String equipmentType,
            String armorMaterial
    ) {
    }
}
