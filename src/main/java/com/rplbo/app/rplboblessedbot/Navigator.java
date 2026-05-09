package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Navigator {

    public static void goTo(Node node, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(
                    Navigator.class.getResource(fxmlPath)
            );

            Stage stage = (Stage) node.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            System.out.println("Gagal pindah ke halaman: " + fxmlPath);
            e.printStackTrace();
        }
    }
}