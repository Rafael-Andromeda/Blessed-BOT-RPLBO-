package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller untuk Informasi.fxml
 * Menampilkan kontak kedai (WhatsApp, Email, Instagram) dan tombol edit.
 */
public class InformasiController implements Initializable {

    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblInstagram;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data idealnya dari service / preferences
        lblPhone.setText("+62 812-3456-7890");
        lblEmail.setText("kedai@blessbot.id");
        lblInstagram.setText("@kedaikopi.blessed");
    }

    // ── Aksi ────────────────────────────────────────────────────────────────

    @FXML
    private void onEditInformasi() {
        // Contoh: dialog edit nomor WhatsApp
        TextInputDialog dialog = new TextInputDialog(lblPhone.getText());
        dialog.setTitle("Edit Informasi");
        dialog.setHeaderText("Perbarui nomor WhatsApp / Telepon:");
        dialog.setContentText("Nomor:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nomor -> lblPhone.setText(nomor));
        // TODO: simpan ke database / service
    }

    // ── Navigasi Sidebar ────────────────────────────────────────────────────

    @FXML private void onDashboard() { navigateTo("/fxml/Dashboard-Admin.fxml"); }
    @FXML private void onEditMenu()  { navigateTo("/fxml/Manajemen-Menu.fxml"); }
    @FXML private void onLokasi()    { /* halaman ini */ }
    @FXML private void onLogout()    { navigateTo("/fxml/Logout.fxml"); }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root  = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage  stage = (Stage) lblPhone.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
