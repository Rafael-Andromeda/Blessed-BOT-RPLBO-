package com.rplbo.app.rplboblessedbot;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * WelcomeController
 * -----------------
 * Halaman awal aplikasi.
 * User  → langsung ke User-Chat.fxml (tanpa login)
 * Admin → ke Login.fxml (perlu login dulu)
 */
public class WelcomeController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    @FXML
    private void onMasukUser(ActionEvent event) {
        // Langsung lempar ke chatbot tanpa login
        Navigator.goTo((Node) event.getSource(), "/com/rplbo/app/rplboblessedbot/User-Chat.fxml");
    }

    @FXML
    private void onMasukAdmin(ActionEvent event) {
        // Perlu login dulu
        Navigator.goTo((Node) event.getSource(), "/com/rplbo/app/rplboblessedbot/Login.fxml");
    }
}
