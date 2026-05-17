package com.rplbo.app.rplboblessedbot;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;

/**
 * KelolaRekomendasiMenuController
 * ─────────────────────────────────
 * Controller untuk Kelola-Rekomendasi-Menu.fxml.
 *
 * Fitur:
 *  - Load daftar menu dari tabel `menu` → ComboBox
 *  - Load rekomendasi per hari (Senin–Minggu) dari tabel `rekomendasi_menu`
 *  - Simpan / Update rekomendasi (upsert berdasarkan hari)
 *  - Hapus rekomendasi untuk hari tertentu
 *
 * Tabel SQLite yang digunakan:
 *  - menu                (id, nama, harga, ...)
 *  - rekomendasi_menu    (id, hari, menu_id, catatan)
 */
public class KelolaRekomendasiMenuController implements Initializable {

    // ── FXML Bindings ─────────────────────────────────────────────

    @FXML private ComboBox<String>              cbHari;
    @FXML private ComboBox<MenuItem>            cbMenu;
    @FXML private TextField                     tfCatatan;
    @FXML private Button                        btnSimpan;
    @FXML private Button                        btnBatal;
    @FXML private Label                         lblStatus;

    @FXML private TableView<RekomendasiRow>               rekomendasiTable;
    @FXML private TableColumn<RekomendasiRow, String>     colHari;
    @FXML private TableColumn<RekomendasiRow, String>     colNamaMenu;
    @FXML private TableColumn<RekomendasiRow, String>     colHarga;
    @FXML private TableColumn<RekomendasiRow, String>     colCatatan;
    @FXML private TableColumn<RekomendasiRow, String>     colAksi;

    // ── State ─────────────────────────────────────────────────────

    /** ID rekomendasi yang sedang diedit. -1 = mode tambah baru. */
    private int editingId = -1;

    private static final String[] HARI_LIST = {
        "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    };

    private final ObservableList<RekomendasiRow> rekomendasiData =
            FXCollections.observableArrayList();

    // ── Lifecycle ─────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHariComboBox();
        loadMenuComboBox();
        setupTableColumns();
        loadRekomendasiFromDb();
    }

    // ── Setup ComboBox Hari ───────────────────────────────────────

    private void setupHariComboBox() {
        cbHari.setItems(FXCollections.observableArrayList(HARI_LIST));
    }

    // ── Load daftar Menu ke ComboBox ──────────────────────────────

    private void loadMenuComboBox() {
        ObservableList<MenuItem> menuList = FXCollections.observableArrayList();
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT id, nama, harga FROM menu WHERE tersedia = 1 ORDER BY nama")) {
                while (rs.next()) {
                    menuList.add(new MenuItem(
                            rs.getInt("id"),
                            rs.getString("nama"),
                            rs.getInt("harga")
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("KelolaRekomendasi: gagal load menu — " + e.getMessage());
        }
        cbMenu.setItems(menuList);
    }

    // ── Setup Kolom Tabel ─────────────────────────────────────────

    private void setupTableColumns() {
        colHari.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().hari()));

        colNamaMenu.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().namaMenu()));

        colHarga.setCellValueFactory(c ->
                new SimpleStringProperty(
                        "Rp" + String.format("%,.0f", (double) c.getValue().harga())
                                .replace(",", ".")));

        colCatatan.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().catatan() == null ? "-" : c.getValue().catatan()));

        // Kolom Aksi: tombol Edit + Hapus
        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("✏");
            private final Button btnHapus = new Button("🗑");

            {
                btnEdit.setStyle("-fx-cursor:hand;");
                btnHapus.setStyle("-fx-cursor:hand; -fx-text-fill:#E05555;");

                btnEdit.setOnAction(e -> {
                    RekomendasiRow row = getTableView().getItems().get(getIndex());
                    populateFormForEdit(row);
                });

                btnHapus.setOnAction(e -> {
                    RekomendasiRow row = getTableView().getItems().get(getIndex());
                    onHapusRekomendasi(row);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(6, btnEdit, btnHapus));
            }
        });

        rekomendasiTable.setItems(rekomendasiData);
    }

    // ── Load Data dari DB ─────────────────────────────────────────

    private void loadRekomendasiFromDb() {
        rekomendasiData.clear();
        try {
            Connection conn = DatabaseHelper.getConnection();
            String sql = "SELECT r.id, r.hari, m.nama AS nama_menu, m.harga, r.catatan, r.menu_id " +
                         "FROM rekomendasi_menu r " +
                         "JOIN menu m ON r.menu_id = m.id " +
                         "ORDER BY CASE r.hari " +
                         "  WHEN 'Senin'   THEN 1 " +
                         "  WHEN 'Selasa'  THEN 2 " +
                         "  WHEN 'Rabu'    THEN 3 " +
                         "  WHEN 'Kamis'   THEN 4 " +
                         "  WHEN 'Jumat'   THEN 5 " +
                         "  WHEN 'Sabtu'   THEN 6 " +
                         "  WHEN 'Minggu'  THEN 7 " +
                         "  ELSE 8 END";

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    rekomendasiData.add(new RekomendasiRow(
                            rs.getInt("id"),
                            rs.getString("hari"),
                            rs.getInt("menu_id"),
                            rs.getString("nama_menu"),
                            rs.getInt("harga"),
                            rs.getString("catatan")
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("KelolaRekomendasi: gagal load data — " + e.getMessage());
            setStatus("⚠ Gagal memuat data: " + e.getMessage(), true);
        }
    }

    // ── Isi Form untuk Edit ───────────────────────────────────────

    private void populateFormForEdit(RekomendasiRow row) {
        editingId = row.id();

        cbHari.setValue(row.hari());

        cbMenu.getItems().stream()
                .filter(m -> m.id() == row.menuId())
                .findFirst()
                .ifPresent(cbMenu::setValue);

        tfCatatan.setText(row.catatan() != null ? row.catatan() : "");
        btnSimpan.setText("💾  Update Rekomendasi");
        setStatus("Mode edit: " + row.hari() + " → " + row.namaMenu(), false);
    }

    // ── Simpan / Update ───────────────────────────────────────────

    @FXML
    private void onSimpan() {
        String   hari    = cbHari.getValue();
        MenuItem menu    = cbMenu.getValue();
        String   catatan = tfCatatan.getText().trim();

        if (hari == null || hari.isBlank()) {
            setStatus("⚠ Pilih hari terlebih dahulu.", true);
            return;
        }
        if (menu == null) {
            setStatus("⚠ Pilih menu terlebih dahulu.", true);
            return;
        }

        try {
            Connection conn = DatabaseHelper.getConnection();

            if (editingId == -1) {
                // INSERT (atau REPLACE jika hari sudah ada)
                String sql = "INSERT INTO rekomendasi_menu (hari, menu_id, catatan) " +
                             "VALUES (?, ?, ?) " +
                             "ON CONFLICT(hari) DO UPDATE SET menu_id = excluded.menu_id, " +
                             "                                catatan = excluded.catatan";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, hari);
                    ps.setInt(2, menu.id());
                    ps.setString(3, catatan.isEmpty() ? null : catatan);
                    ps.executeUpdate();
                }
                setStatus("✅ Rekomendasi untuk " + hari + " berhasil disimpan.", false);
                System.out.println("✅ Rekomendasi disimpan: " + hari + " → " + menu.nama());

            } else {
                // UPDATE berdasarkan ID
                String sql = "UPDATE rekomendasi_menu SET hari = ?, menu_id = ?, catatan = ? " +
                             "WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, hari);
                    ps.setInt(2, menu.id());
                    ps.setString(3, catatan.isEmpty() ? null : catatan);
                    ps.setInt(4, editingId);
                    ps.executeUpdate();
                }
                setStatus("✅ Rekomendasi untuk " + hari + " berhasil diperbarui.", false);
                System.out.println("✅ Rekomendasi diperbarui: id=" + editingId);
            }

            resetForm();
            loadRekomendasiFromDb();

        } catch (Exception e) {
            System.err.println("KelolaRekomendasi: gagal simpan — " + e.getMessage());
            setStatus("⚠ Gagal menyimpan: " + e.getMessage(), true);
        }
    }

    // ── Hapus Rekomendasi ─────────────────────────────────────────

    private void onHapusRekomendasi(RekomendasiRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus rekomendasi menu untuk hari \"" + row.hari() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Hapus Rekomendasi");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    Connection conn = DatabaseHelper.getConnection();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM rekomendasi_menu WHERE id = ?")) {
                        ps.setInt(1, row.id());
                        ps.executeUpdate();
                    }
                    setStatus("🗑 Rekomendasi hari " + row.hari() + " dihapus.", false);
                    System.out.println("🗑 Rekomendasi dihapus: id=" + row.id());
                    loadRekomendasiFromDb();

                    // Jika yang dihapus sedang diedit, reset form
                    if (editingId == row.id()) resetForm();

                } catch (Exception e) {
                    System.err.println("KelolaRekomendasi: gagal hapus — " + e.getMessage());
                    setStatus("⚠ Gagal menghapus: " + e.getMessage(), true);
                }
            }
        });
    }

    // ── Batal / Reset Form ────────────────────────────────────────

    @FXML
    private void onBatal() {
        resetForm();
        setStatus("", false);
    }

    private void resetForm() {
        editingId = -1;
        cbHari.setValue(null);
        cbMenu.setValue(null);
        tfCatatan.clear();
        btnSimpan.setText("💾  Simpan Rekomendasi");
    }

    // ── Refresh ───────────────────────────────────────────────────

    @FXML
    private void onRefresh() {
        loadMenuComboBox();
        loadRekomendasiFromDb();
        setStatus("Data diperbarui.", false);
    }

    // ── Helper: tampilkan status ──────────────────────────────────

    private void setStatus(String msg, boolean isError) {
        lblStatus.setText(msg);
        lblStatus.setStyle(isError
                ? "-fx-text-fill:#E05555; -fx-font-size:13px;"
                : "-fx-text-fill:#5C3D2E; -fx-font-size:13px;");
    }

    // ── Navigasi Sidebar ──────────────────────────────────────────

    @FXML private void onDashboard() {
        Navigator.goTo(rekomendasiTable, "/com/rplbo/app/rplboblessedbot/Dashboard-Admin.fxml");
    }

    @FXML private void onEditMenu() {
        Navigator.goTo(rekomendasiTable, "/com/rplbo/app/rplboblessedbot/Manajemen-Menu.fxml");
    }

    @FXML private void onRekomendasiMenu() { /* tetap di sini */ }

    @FXML private void onLokasi() {
        Navigator.goTo(rekomendasiTable, "/com/rplbo/app/rplboblessedbot/Lokasi.fxml");
    }

    @FXML private void onLogout() {
        Navigator.goTo(rekomendasiTable, "/com/rplbo/app/rplboblessedbot/Logout.fxml");
    }

    // ── Records (model data) ──────────────────────────────────────

    /** Baris data untuk TableView rekomendasi. */
    public record RekomendasiRow(
            int    id,
            String hari,
            int    menuId,
            String namaMenu,
            int    harga,
            String catatan
    ) {}

    /**
     * Item di ComboBox menu.
     * toString() dipakai sebagai label tampilan ComboBox.
     */
    public record MenuItem(int id, String nama, int harga) {
        @Override
        public String toString() {
            return nama + " — Rp" + String.format("%,.0f", (double) harga).replace(",", ".");
        }
    }
}
