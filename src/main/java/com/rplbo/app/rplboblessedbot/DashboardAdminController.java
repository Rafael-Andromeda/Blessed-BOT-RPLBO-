package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller untuk Dashboard-Admin.fxml
 * Menampilkan:
 *  - Jumlah menu dari DB (update otomatis)
 *  - Jam operasional & alamat dari DB
 *  - Aktivitas login terakhir dari tabel login_log
 */
public class DashboardAdminController implements Initializable {

    // ── Manajemen Menu ────────────────────────────────────────────
    @FXML private Label lblMenuCount;

    // ── Informasi Kedai ───────────────────────────────────────────
    @FXML private Label lblJamOps;
    @FXML private Label lblAlamat;

    // ── Aktivitas Login (sampai 4 entri) ─────────────────────────
    @FXML private Label lblLogin1;
    @FXML private Label lblLogin2;
    @FXML private Label lblLogin3;
    @FXML private Label lblLogin4;

    private static final DateTimeFormatter IN_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMenuCount();
        loadInformasiKedai();
        loadLoginLog();
    }

    // ── Load jumlah menu ──────────────────────────────────────────

    private void loadMenuCount() {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(*) AS total FROM menu WHERE tersedia = 1")) {
                if (rs.next()) {
                    if (lblMenuCount != null)
                        lblMenuCount.setText(String.valueOf(rs.getInt("total")));
                }
            }
        } catch (Exception e) {
            System.err.println("Dashboard: gagal load menu count — " + e.getMessage());
            if (lblMenuCount != null) lblMenuCount.setText("-");
        }
    }

    // ── Load informasi kedai ──────────────────────────────────────

    private void loadInformasiKedai() {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT jam_buka, jam_tutup, alamat FROM informasi_kedai LIMIT 1")) {
                if (rs.next()) {
                    if (lblJamOps != null)
                        lblJamOps.setText(rs.getString("jam_buka") + " - " + rs.getString("jam_tutup"));
                    if (lblAlamat != null)
                        lblAlamat.setText(rs.getString("alamat"));
                }
            }
        } catch (Exception e) {
            System.err.println("Dashboard: gagal load info kedai — " + e.getMessage());
            if (lblJamOps != null) lblJamOps.setText("08:00 - 22:00");
            if (lblAlamat != null) lblAlamat.setText("Jl. Anggrek No.10 Jogja");
        }
    }

    // ── Load aktivitas login terakhir ─────────────────────────────

    private void loadLoginLog() {
        Label[] labels = {lblLogin1, lblLogin2, lblLogin3, lblLogin4};

        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT waktu FROM login_log ORDER BY id DESC LIMIT 4")) {

                int idx = 0;
                while (rs.next() && idx < labels.length) {
                    if (labels[idx] != null) {
                        labels[idx].setText("Admin berhasil login\n" + formatLoginTime(rs.getString("waktu")));
                    }
                    idx++;
                }
            }
        } catch (Exception e) {
            System.err.println("Dashboard: gagal load login log — " + e.getMessage());
        }
    }

    /**
     * Format waktu login menjadi teks yang mudah dibaca.
     * Contoh: "10:30 hari ini" atau "Jumat, 9 Mei - 12:02"
     */
    private String formatLoginTime(String waktuStr) {
        try {
            LocalDateTime waktu = LocalDateTime.parse(waktuStr, IN_FMT);
            LocalDateTime now   = LocalDateTime.now();
            String jam = waktu.format(DateTimeFormatter.ofPattern("HH:mm"));

            if (waktu.toLocalDate().equals(now.toLocalDate())) {
                return jam + " hari ini";
            }

            String[] namaHari  = {"", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
            String[] namaBulan = {"", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
                    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"};

            String hari = namaHari[waktu.getDayOfWeek().getValue()];
            String tgl  = waktu.getDayOfMonth() + " " + namaBulan[waktu.getMonthValue()];
            return hari + ", " + tgl + " - " + jam;

        } catch (Exception e) {
            return waktuStr;
        }
    }

    // ── Navigasi Sidebar ──────────────────────────────────────────

    @FXML private void onDashboard() { /* tetap di sini */ }

    @FXML
    private void onEditMenu() {
        Navigator.goTo(lblMenuCount, "/com/rplbo/app/rplboblessedbot/Manajemen-Menu.fxml");
    }

    @FXML
    private void onLokasi() {
        Navigator.goTo(lblMenuCount, "/com/rplbo/app/rplboblessedbot/Lokasi.fxml");
    }

    @FXML
    private void onLogout() {
        Navigator.goTo(lblMenuCount, "/com/rplbo/app/rplboblessedbot/Logout.fxml");
    }

    @FXML
    private void onTambahMenu() {
        Navigator.goTo(lblMenuCount, "/com/rplbo/app/rplboblessedbot/Manajemen-Menu.fxml");
    }

    @FXML
    private void onEditInfo() {
        Navigator.goTo(lblMenuCount, "/com/rplbo/app/rplboblessedbot/Informasi.fxml");
    }
}
