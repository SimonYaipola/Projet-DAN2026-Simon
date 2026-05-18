package com.craftboard.desktop.controller;

import com.craftboard.desktop.service.ApiService;
import com.craftboard.desktop.view.MainView;
import com.craftboard.desktop.view.BitjitaClaimSetupView;
import com.craftboard.desktop.view.SetupHomeView;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 * Controle la navigation entre les ecrans JavaFX principaux.
 */
public class MainController {

    private final Stage stage;
    private final ApiService apiService;

    public MainController(Stage stage, ApiService apiService) {
        this.stage = stage;
        this.apiService = apiService;
    }

    public Parent createLoginView() {
        if (apiService.getCurrentSession() != null) {
            return new MainView(apiService, this::changeCity, this::logoutToSetup).build();
        }
        return new SetupHomeView(apiService, this::openClaimSetupView, this::openCityLoginView).build();
    }

    private void openSetupHomeView() {
        stage.getScene().setRoot(new SetupHomeView(apiService, this::openClaimSetupView, this::openCityLoginView).build());
    }

    private void openClaimSetupView() {
        stage.getScene().setRoot(new BitjitaClaimSetupView(apiService, this::openMainView, this::openSetupHomeView).build());
    }

    private void openCityLoginView() {
        stage.getScene().setRoot(new com.craftboard.desktop.view.CityLoginView(apiService, this::openMainView, this::openSetupHomeView).build());
    }

    private void openMainView() {
        stage.getScene().setRoot(new MainView(apiService, this::changeCity, this::logoutToSetup).build());
        stage.setMinWidth(980);
        stage.setMinHeight(640);
    }

    private void changeCity() {
        apiService.clearSession();
        openSetupHomeView();
    }

    private void logoutToSetup() {
        apiService.forgetRememberedSession();
        openSetupHomeView();
    }

    public Task<String> createHealthCheckTask(String baseUrl) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                apiService.setBaseUrl(baseUrl);
                return apiService.getHealth();
            }
        };
    }
}
