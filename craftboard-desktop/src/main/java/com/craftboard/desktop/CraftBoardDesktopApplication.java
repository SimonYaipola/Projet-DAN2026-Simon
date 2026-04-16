package com.craftboard.desktop;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import com.craftboard.desktop.service.ApiService;

public class CraftBoardDesktopApplication extends Application {

    @Override
    public void start(Stage stage) {
        ApiService api = new ApiService();

        String result = api.getHealth();

        Label label = new Label(result);

        Scene scene = new Scene(new StackPane(label), 400, 200);

        stage.setScene(scene);
        stage.setTitle("CraftBoard");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}
