package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * LoginController
 * ---------------
 * Validasi login admin dari DB.
 * Jika berhasil → catat ke login_log → buka Dashboard-Admin.fxml
 * Jika gagal    → tampilkan pesan error di lblError
 */
public class LoginController implements Initializable {

    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (lblError != null) lblError.setText("");
    }

    @FXML
    private void onLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isBlank() || password.isBlank()) {
            showError("Username dan password tidak boleh kosong.");
            return;
        }

        try {
            Connection conn = DatabaseHelper.getConnection();

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM admin WHERE username=? AND password=?")) {
                ps.setString(1, username);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int adminId = rs.getInt("id");

                        // Catat waktu login ke login_log
                        String waktu = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        try (PreparedStatement psLog = conn.prepareStatement(
                                "INSERT INTO login_log (admin_id, waktu) VALUES (?, ?)")) {
                            psLog.setInt(1, adminId);
                            psLog.setString(2, waktu);
                            psLog.executeUpdate();
                        }

                        System.out.println("✅ Login berhasil: " + username);
                        Navigator.goTo(txtUsername,
                                "/com/rplbo/app/rplboblessedbot/Dashboard-Admin.fxml");

                    } else {
                        showError("Username atau password salah.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            showError("Terjadi kesalahan. Coba lagi.");
        }
    }

    @FXML
    private void onKembali() {
        Navigator.goTo(txtUsername, "/com/rplbo/app/rplboblessedbot/Welcome.fxml");
    }

    private void showError(String msg) {
        if (lblError != null) lblError.setText(msg);
    }
}
