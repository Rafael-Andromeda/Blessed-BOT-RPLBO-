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
 * Membuka Welcome.fxml sebagai halaman awal.
 *
 * User memilih:
 *   - Masuk sebagai User  → langsung ke User-Chat.fxml
 *   - Masuk sebagai Admin → ke Login.fxml → Dashboard-Admin.fxml
 */
public class BlessedApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/rplbo/app/rplboblessedbot/Welcome.fxml")
        );

        Parent root = loader.load();
        Scene scene = new Scene(root);

        primaryStage.setTitle("BlessedBot - Kedai Kopi Blessed");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
