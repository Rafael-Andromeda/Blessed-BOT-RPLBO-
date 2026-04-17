package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller untuk Logout.fxml
 * Menampilkan profil admin, aktivitas terakhir, dan konfirmasi logout.
 */
public class LogoutController implements Initializable {

    @FXML private Label lblAdminEmail;
    @FXML private Label lblActivity1;
    @FXML private Label lblTime1;
    @FXML private Label lblActivity2;
    @FXML private Label lblTime2;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH.mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblAdminEmail.setText("admin@blessbot.id");

        // Contoh aktivitas statis; idealnya diambil dari audit log
        lblActivity1.setText("Edit menu \"Kopi Susu\"");
        lblTime1.setText(LocalDateTime.now().format(TIME_FMT));

        lblActivity2.setText("Tambah menu baru");
        lblTime2.setText(LocalDateTime.now().minusMinutes(24).format(TIME_FMT));
    }

    // ── Aksi konfirmasi ─────────────────────────────────────────────────────

    @FXML
    private void onConfirmLogout() {
        // Hapus sesi / token, lalu arahkan ke halaman Login
        // SessionManager.getInstance().clear();
        navigateTo("/fxml/Login.fxml");
    }

    @FXML
    private void onBatal() {
        navigateTo("/fxml/Dashboard-Admin.fxml");
    }

    // ── Navigasi Sidebar ────────────────────────────────────────────────────

    @FXML private void onDashboard() { navigateTo("/fxml/Dashboard-Admin.fxml"); }
    @FXML private void onEditMenu()  { navigateTo("/fxml/Manajemen-Menu.fxml"); }
    @FXML private void onLokasi()    { navigateTo("/fxml/Lokasi.fxml"); }
    @FXML private void onLogout()    { /* halaman ini */ }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root  = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage  stage = (Stage) lblAdminEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
