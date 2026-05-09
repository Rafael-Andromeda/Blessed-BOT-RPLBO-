package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

public class WelcomeController implements Initializable {

    @FXML private Button btnUser;
    @FXML private Button btnAdmin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    private void onMasukUser() {
        Navigator.goTo(btnUser, "/com/rplbo/app/rplboblessedbot/User-Chat.fxml");
    }

    @FXML
    private void onMasukAdmin() {
        Navigator.goTo(btnAdmin, "/com/rplbo/app/rplboblessedbot/Login.fxml");
    }
}