package com.rplbo.app.rplboblessedbot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * BlessedApplication
 * ------------------
 * Entry point utama aplikasi BlessedBot (JavaFX).
 * Membuka tampilan User-Chat.fxml saat aplikasi dijalankan.
 */
public class BlessedApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Memuat file FXML User-Chat.fxml dari resources
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/rplbo/app/rplboblessedbot/User-Chat.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root);

        primaryStage.setTitle("BlessedBot - User Chat");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}