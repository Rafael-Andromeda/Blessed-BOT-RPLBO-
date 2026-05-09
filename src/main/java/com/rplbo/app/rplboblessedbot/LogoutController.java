package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;

/**
 * Controller untuk Logout.fxml
 * Baca email admin dan 2 aktivitas terakhir dari DB.
 * Setelah konfirmasi logout → kembali ke Welcome.fxml
 */
public class LogoutController implements Initializable {

    @FXML private Label lblAdminEmail;
    @FXML private Label lblActivity1;
    @FXML private Label lblTime1;
    @FXML private Label lblActivity2;
    @FXML private Label lblTime2;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadAdminData();
        loadAktivitasData();
    }

    private void loadAdminData() {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT email FROM admin LIMIT 1")) {
                if (rs.next()) {
                    lblAdminEmail.setText(rs.getString("email"));
                }
            }
        } catch (Exception e) {
            System.err.println("Logout: gagal load admin — " + e.getMessage());
            lblAdminEmail.setText("admin@blessbot.id");
        }
    }

    private void loadAktivitasData() {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT keterangan, strftime('%H.%M', waktu) AS jam " +
                                 "FROM aktivitas_admin " +
                                 "ORDER BY id DESC LIMIT 2")) {

                if (rs.next()) {
                    lblActivity1.setText(rs.getString("keterangan"));
                    lblTime1.setText(rs.getString("jam"));
                }
                if (rs.next()) {
                    lblActivity2.setText(rs.getString("keterangan"));
                    lblTime2.setText(rs.getString("jam"));
                }
            }
        } catch (Exception e) {
            System.err.println("Logout: gagal load aktivitas — " + e.getMessage());
            lblActivity1.setText("Edit menu \"Kopi Susu\"");
            lblTime1.setText("--:--");
            lblActivity2.setText("Tambah menu baru");
            lblTime2.setText("--:--");
        }
    }

    // ── Aksi tombol ──────────────────────────────────────────────

    @FXML
    private void onConfirmLogout() {
        DatabaseHelper.close();
        // Kembali ke halaman awal, bukan Login
        Navigator.goTo(lblAdminEmail, "/com/rplbo/app/rplboblessedbot/Welcome.fxml");
    }

    @FXML
    private void onBatal() {
        Navigator.goTo(lblAdminEmail, "/com/rplbo/app/rplboblessedbot/Dashboard-Admin.fxml");
    }

    // ── Navigasi Sidebar ──────────────────────────────────────────

    @FXML
    private void onDashboard() {
        Navigator.goTo(lblAdminEmail, "/com/rplbo/app/rplboblessedbot/Dashboard-Admin.fxml");
    }

    @FXML
    private void onEditMenu() {
        Navigator.goTo(lblAdminEmail, "/com/rplbo/app/rplboblessedbot/Manajemen-Menu.fxml");
    }

    @FXML
    private void onLokasi() {
        Navigator.goTo(lblAdminEmail, "/com/rplbo/app/rplboblessedbot/Lokasi.fxml");
    }

    @FXML private void onLogout() { /* tetap di sini */ }
}
