package com.craftboard.desktop;

import com.craftboard.desktop.controller.MainController;
import com.craftboard.desktop.service.ApiService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Point d'entree JavaFX qui demarre le serveur local avant d'afficher l'interface.
 */
public class CraftBoardDesktopApplication extends Application {

    private ConfigurableApplicationContext serverContext;
    private RuntimeException startupException;

    @Override
    public void init() {
        try {
            serverContext = EmbeddedServerLauncher.startIfNeeded(getParameters().getRaw().toArray(String[]::new));
        } catch (RuntimeException exception) {
            startupException = exception;
        }
    }

    @Override
    public void start(Stage stage) {
        if (startupException != null) {
            showStartupError(stage, startupException);
            return;
        }

        ApiService api = new ApiService();
        MainController controller = new MainController(stage, api);

        Scene scene = new Scene(controller.createLoginView(), 720, 480);

        stage.setScene(scene);
        stage.setTitle("CraftBoard");
        stage.setMinWidth(640);
        stage.setMinHeight(420);
        stage.show();
    }

    @Override
    public void stop() {
        if (serverContext != null) {
            serverContext.close();
        }
    }

    public static void main(String[] args) {
        launch();
    }

    private void showStartupError(Stage stage, RuntimeException exception) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("CraftBoard");
        alert.setHeaderText("Impossible de demarrer le serveur CraftBoard");
        alert.setContentText(exception.getMessage());
        alert.setOnHidden(event -> stage.close());
        alert.show();
    }

}
