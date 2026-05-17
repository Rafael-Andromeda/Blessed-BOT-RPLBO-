package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.ResourceBundle;

public class InformasiController implements Initializable {

    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblInstagram;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadInformasiData();
    }

    private void loadInformasiData() {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT phone, email, instagram FROM informasi_kedai LIMIT 1")) {
                if (rs.next()) {
                    lblPhone.setText(rs.getString("phone"));
                    lblEmail.setText(rs.getString("email"));
                    lblInstagram.setText(rs.getString("instagram"));
                }
            }
        } catch (Exception e) {
            System.err.println("Informasi: gagal load data — " + e.getMessage());
            lblPhone.setText("+62 812-3456-7890");
            lblEmail.setText("kedai@blessbot.id");
            lblInstagram.setText("@kedaikopi.blessed");
        }
    }

    @FXML
    private void onEditPhone() {
        TextInputDialog dialog = new TextInputDialog(lblPhone.getText());
        dialog.setTitle("Edit Informasi");
        dialog.setHeaderText("Perbarui nomor WhatsApp / Telepon:");
        dialog.setContentText("Nomor:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nomor -> {
            lblPhone.setText(nomor);
            updateField("phone", nomor, "Edit nomor telepon menjadi: " + nomor);
        });
    }

    @FXML
    private void onEditEmail() {
        TextInputDialog dialog = new TextInputDialog(lblEmail.getText());
        dialog.setTitle("Edit Informasi");
        dialog.setHeaderText("Perbarui Email:");
        dialog.setContentText("Email:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(email -> {
            lblEmail.setText(email);
            updateField("email", email, "Edit email menjadi: " + email);
        });
    }

    @FXML
    private void onEditInstagram() {
        TextInputDialog dialog = new TextInputDialog(lblInstagram.getText());
        dialog.setTitle("Edit Informasi");
        dialog.setHeaderText("Perbarui Instagram:");
        dialog.setContentText("Instagram:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(ig -> {
            lblInstagram.setText(ig);
            updateField("instagram", ig, "Edit instagram menjadi: " + ig);
        });
    }

    // Kalau di FXML hanya ada satu tombol edit, pakai method ini
    @FXML
    private void onEditInformasi() {
        onEditPhone();
    }

    private void updateField(String column, String value, String keterangan) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE informasi_kedai SET " + column + " = ? WHERE id = 1")) {
                ps.setString(1, value);
                ps.executeUpdate();
                logAktivitas(keterangan);
                System.out.println("✅ " + column + " disimpan: " + value);
            }
        } catch (Exception e) {
            System.err.println("Gagal simpan " + column + ": " + e.getMessage());
        }
    }

    private void logAktivitas(String keterangan) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO aktivitas_admin (admin_id, keterangan) VALUES (1, ?)")) {
                ps.setString(1, keterangan);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Gagal log aktivitas: " + e.getMessage());
        }
    }

    // ── Navigasi Sidebar ──────────────────────────────────────────

    @FXML
    private void onDashboard() {
        Navigator.goTo(lblPhone, "/com/rplbo/app/rplboblessedbot/Dashboard-Admin.fxml");
    }

    @FXML
    private void onEditMenu() {
        Navigator.goTo(lblPhone, "/com/rplbo/app/rplboblessedbot/Manajemen-Menu.fxml");
    }

    @FXML
    private void onRekomendasiMenu() {
        Navigator.goTo(lblPhone, "/com/rplbo/app/rplboblessedbot/Kelola-Rekomendasi-Menu.fxml");
    }

    @FXML
    private void onLokasi() {
        Navigator.goTo(lblPhone, "/com/rplbo/app/rplboblessedbot/Lokasi.fxml");
    }

    @FXML
    private void onLogout() {
        Navigator.goTo(lblPhone, "/com/rplbo/app/rplboblessedbot/Logout.fxml");
    }
}
