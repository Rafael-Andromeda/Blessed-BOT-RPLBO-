package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * DashboardAdminController
 * ------------------------
 * Controller untuk Dashboard-Admin.fxml. Menampilkan:
 *  - Jumlah menu dari DB (update otomatis)
 *  - Jam operasional & alamat dari DB
 *  - Aktivitas login terakhir dari tabel login_log
 *  - Preview rekomendasi menu per hari
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

    // ── Preview Rekomendasi Menu ──────────────────────────────────
    @FXML private VBox rekomendasiPreviewBox;

    private static final DateTimeFormatter IN_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMenuCount();
        loadInformasiKedai();
        loadLoginLog();
        loadRekomendasiPreview();
    }

    // ── Load jumlah menu ──────────────────────────────────────────

    private void loadMenuCount() {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(*) AS total FROM menu WHERE tersedia = 1")) {
                if (rs.next() && lblMenuCount != null) {
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
        Label[] labels = { lblLogin1, lblLogin2, lblLogin3, lblLogin4 };

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

            String[] namaHari  = { "", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu" };
            String[] namaBulan = { "", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
                                       "Jul", "Agu", "Sep", "Okt", "Nov", "Des" };

            String hari = namaHari[waktu.getDayOfWeek().getValue()];
            String tgl  = waktu.getDayOfMonth() + " " + namaBulan[waktu.getMonthValue()];
            return hari + ", " + tgl + " - " + jam;

        } catch (Exception e) {
            return waktuStr;
        }
    }

    // ── Load preview rekomendasi menu ─────────────────────────────

    private void loadRekomendasiPreview() {
        if (rekomendasiPreviewBox == null) return;
        rekomendasiPreviewBox.getChildren().clear();

        try {
            Connection conn = DatabaseHelper.getConnection();
            String sql = "SELECT r.hari, m.nama AS nama_menu, m.harga, r.catatan " +
                         "FROM rekomendasi_menu r " +
                         "JOIN menu m ON r.menu_id = m.id " +
                         "ORDER BY CASE r.hari " +
                         "  WHEN 'Senin'  THEN 1 WHEN 'Selasa' THEN 2 WHEN 'Rabu'   THEN 3 " +
                         "  WHEN 'Kamis'  THEN 4 WHEN 'Jumat'  THEN 5 WHEN 'Sabtu'  THEN 6 " +
                         "  WHEN 'Minggu' THEN 7 ELSE 8 END";

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                boolean ada = false;

                while (rs.next()) {
                    ada = true;
                    String hari     = rs.getString("hari");
                    String namaMenu = rs.getString("nama_menu");
                    int    harga    = rs.getInt("harga");
                    String catatan  = rs.getString("catatan");

                    String hargaFmt    = "Rp" + String.format("%,.0f", (double) harga).replace(",", ".");
                    String catatanTeks = (catatan != null && !catatan.isBlank()) ? " · " + catatan : "";

                    HBox row = new HBox(10);
                    row.setStyle("-fx-alignment:CENTER_LEFT; -fx-padding:4 0 4 0;");

                    Label lblHari = new Label(hari);
                    lblHari.setStyle("-fx-font-size:12px; -fx-font-weight:bold; " +
                                     "-fx-text-fill:#FFFFFF; -fx-background-color:#7B5441; " +
                                     "-fx-background-radius:6; -fx-padding:2 8 2 8; -fx-min-width:64px;");

                    Label lblMenu = new Label(namaMenu + "  " + hargaFmt + catatanTeks);
                    lblMenu.setStyle("-fx-font-size:13px; -fx-text-fill:#3B2414;");

                    row.getChildren().addAll(lblHari, lblMenu);
                    rekomendasiPreviewBox.getChildren().add(row);

                    Region div = new Region();
                    div.setStyle("-fx-background-color:#E2D0B5; -fx-pref-height:1; " +
                                 "-fx-min-height:1; -fx-max-height:1;");
                    rekomendasiPreviewBox.getChildren().add(div);
                }

                if (!ada) {
                    Label kosong = new Label("Belum ada rekomendasi menu yang diatur.");
                    kosong.setStyle("-fx-font-size:13px; -fx-text-fill:#8B6E5A;");
                    rekomendasiPreviewBox.getChildren().add(kosong);
                }
            }
        } catch (Exception e) {
            System.err.println("Dashboard: gagal load rekomendasi preview — " + e.getMessage());
            Label err = new Label("⚠ Gagal memuat rekomendasi.");
            err.setStyle("-fx-font-size:13px; -fx-text-fill:#E05555;");
            rekomendasiPreviewBox.getChildren().add(err);
        }
    }

    // ── Navigasi Sidebar ──────────────────────────────────────────

    @FXML private void onDashboard() { /* tetap di sini */ }

    @FXML
    private void onEditMenu() {
        Navigator.goTo(lblMenuCount, "/com/rplbo/app/rplboblessedbot/Manajemen-Menu.fxml");
    }

    @FXML
    private void onRekomendasiMenu() {
        Navigator.goTo(lblMenuCount, "/com/rplbo/app/rplboblessedbot/Kelola-Rekomendasi-Menu.fxml");
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
