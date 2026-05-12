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
 * Baca email admin dan 4 aktivitas terakhir dari DB.
 * Setelah konfirmasi logout → kembali ke Welcome.fxml
 */
public class LogoutController implements Initializable {

    @FXML private Label lblAdminEmail;

    @FXML private Label lblActivity1;
    @FXML private Label lblTime1;

    @FXML private Label lblActivity2;
    @FXML private Label lblTime2;

    @FXML private Label lblActivity3;
    @FXML private Label lblTime3;

    @FXML private Label lblActivity4;
    @FXML private Label lblTime4;

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
        Label[] activityLabels = {
                lblActivity1, lblActivity2, lblActivity3, lblActivity4
        };

        Label[] timeLabels = {
                lblTime1, lblTime2, lblTime3, lblTime4
        };

        // Default dulu kalau data kurang dari 4
        for (int i = 0; i < activityLabels.length; i++) {
            if (activityLabels[i] != null) activityLabels[i].setText("-");
            if (timeLabels[i] != null) timeLabels[i].setText("--.--");
        }

        try {
            Connection conn = DatabaseHelper.getConnection();

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT keterangan, strftime('%H.%M', waktu) AS jam " +
                                 "FROM aktivitas_admin " +
                                 "ORDER BY id DESC LIMIT 4")) {

                int index = 0;

                while (rs.next() && index < activityLabels.length) {
                    activityLabels[index].setText(rs.getString("keterangan"));
                    timeLabels[index].setText(rs.getString("jam"));
                    index++;
                }
            }

        } catch (Exception e) {
            System.err.println("Logout: gagal load aktivitas — " + e.getMessage());

            lblActivity1.setText("Edit menu \"Kopi Susu\"");
            lblTime1.setText("--.--");

            lblActivity2.setText("Tambah menu baru");
            lblTime2.setText("--.--");

            lblActivity3.setText("-");
            lblTime3.setText("--.--");

            lblActivity4.setText("-");
            lblTime4.setText("--.--");
        }
    }

    // Aksi tombol

    @FXML
    private void onConfirmLogout() {
        DatabaseHelper.close();
        Navigator.goTo(lblAdminEmail, "/com/rplbo/app/rplboblessedbot/Welcome.fxml");
    }

    @FXML
    private void onBatal() {
        Navigator.goTo(lblAdminEmail, "/com/rplbo/app/rplboblessedbot/Dashboard-Admin.fxml");
    }

    // Navigasi Sidebar

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

    @FXML
    private void onLogout() {
        // Tetap di halaman Logout
    }
}