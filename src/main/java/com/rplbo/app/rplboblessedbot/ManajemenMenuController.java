package com.rplbo.app.rplboblessedbot;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;

/**
 * Controller untuk Manajemen-Menu.fxml
 * Baca, tambah, edit, dan hapus menu dari DB.
 */
public class ManajemenMenuController implements Initializable {

    @FXML private TableView<MenuRow> menuTable;
    @FXML private TableColumn<MenuRow, String> colFoto;
    @FXML private TableColumn<MenuRow, String> colNama;
    @FXML private TableColumn<MenuRow, String> colHarga;
    @FXML private TableColumn<MenuRow, String> colAksi;

    private final ObservableList<MenuRow> menuData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadMenuFromDb();
    }

    // ── Setup kolom tabel ─────────────────────────────────────────

    private void setupColumns() {
        colFoto.setCellValueFactory(c -> new SimpleStringProperty("☕"));

        colNama.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().nama()));

        colHarga.setCellValueFactory(c ->
                new SimpleStringProperty("Rp" + String.format("%,.0f", (double) c.getValue().harga())
                        .replace(",", ".")));

        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("✏");
            private final Button btnHapus = new Button("🗑");

            {
                btnEdit.setStyle("-fx-cursor:hand;");
                btnHapus.setStyle("-fx-cursor:hand; -fx-text-fill:red;");

                btnEdit.setOnAction(e -> {
                    MenuRow row = getTableView().getItems().get(getIndex());
                    showEditMenuDialog(row);
                });

                btnHapus.setOnAction(e -> {
                    MenuRow row = getTableView().getItems().get(getIndex());
                    onHapusMenu(row);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new javafx.scene.layout.HBox(4, btnEdit, btnHapus));
            }
        });

        menuTable.setItems(menuData);
    }

    // ── Load data dari DB ─────────────────────────────────────────

    private void loadMenuFromDb() {
        menuData.clear();
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT m.id, m.nama, m.deskripsi, m.harga, k.nama_kategori " +
                                 "FROM menu m " +
                                 "JOIN kategori_menu k ON m.kategori_id = k.id " +
                                 "WHERE m.tersedia = 1 " +
                                 "ORDER BY k.urutan, m.id")) {

                while (rs.next()) {
                    menuData.add(new MenuRow(
                            rs.getInt("id"),
                            rs.getString("nama"),
                            rs.getString("deskripsi"),
                            rs.getInt("harga"),
                            rs.getString("nama_kategori")
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("ManajemenMenu: gagal load — " + e.getMessage());
        }
    }

    // ── Tambah menu ───────────────────────────────────────────────

    @FXML
    private void onTambahMenu() {
        // Dialog sederhana — bisa diganti form FXML tersendiri
        TextInputDialog namaDialog = new TextInputDialog();
        namaDialog.setTitle("Tambah Menu");
        namaDialog.setHeaderText("Nama menu baru:");
        namaDialog.setContentText("Nama:");

        namaDialog.showAndWait().ifPresent(nama -> {
            TextInputDialog hargaDialog = new TextInputDialog("0");
            hargaDialog.setTitle("Tambah Menu");
            hargaDialog.setHeaderText("Harga untuk: " + nama);
            hargaDialog.setContentText("Harga (angka):");

            hargaDialog.showAndWait().ifPresent(hargaStr -> {
                try {
                    int harga = Integer.parseInt(hargaStr.trim());
                    insertMenuToDb(nama, harga);
                    loadMenuFromDb(); // refresh tabel
                } catch (NumberFormatException ex) {
                    showAlert("Harga harus berupa angka.");
                }
            });
        });
    }

    private void insertMenuToDb(String nama, int harga) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            // Default masuk kategori 1 (Kopi Hitam), bisa dikembangkan
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO menu (kategori_id, nama, harga) VALUES (1, ?, ?)")) {
                ps.setString(1, nama);
                ps.setInt(2, harga);
                ps.executeUpdate();

                // Catat aktivitas
                logAktivitas("Tambah menu \"" + nama + "\"");
                System.out.println("✅ Menu ditambah: " + nama);
            }
        } catch (Exception e) {
            System.err.println("Gagal tambah menu: " + e.getMessage());
        }
    }

    // ── Edit menu ─────────────────────────────────────────────────

    private void showEditMenuDialog(MenuRow row) {
        TextInputDialog dialog = new TextInputDialog(row.nama());
        dialog.setTitle("Edit Menu");
        dialog.setHeaderText("Edit nama menu:");
        dialog.setContentText("Nama:");

        dialog.showAndWait().ifPresent(namaBaru -> {
            TextInputDialog hargaDialog = new TextInputDialog(String.valueOf(row.harga()));
            hargaDialog.setTitle("Edit Menu");
            hargaDialog.setHeaderText("Edit harga untuk: " + namaBaru);
            hargaDialog.setContentText("Harga:");

            hargaDialog.showAndWait().ifPresent(hargaStr -> {
                try {
                    int hargaBaru = Integer.parseInt(hargaStr.trim());
                    updateMenuInDb(row.id(), namaBaru, hargaBaru);
                    loadMenuFromDb();
                } catch (NumberFormatException ex) {
                    showAlert("Harga harus berupa angka.");
                }
            });
        });
    }

    private void updateMenuInDb(int id, String nama, int harga) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE menu SET nama = ?, harga = ? WHERE id = ?")) {
                ps.setString(1, nama);
                ps.setInt(2, harga);
                ps.setInt(3, id);
                ps.executeUpdate();

                logAktivitas("Edit menu \"" + nama + "\"");
                System.out.println("✅ Menu diupdate: " + nama);
            }
        } catch (Exception e) {
            System.err.println("Gagal edit menu: " + e.getMessage());
        }
    }

    // ── Hapus menu (soft delete) ───────────────────────────────────

    private void onHapusMenu(MenuRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus menu \"" + row.nama() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Hapus Menu");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                deleteMenuFromDb(row);
            }
        });
    }

    private void deleteMenuFromDb(MenuRow row) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            // Soft delete: set tersedia = 0
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE menu SET tersedia = 0 WHERE id = ?")) {
                ps.setInt(1, row.id());
                ps.executeUpdate();

                logAktivitas("Hapus menu \"" + row.nama() + "\"");
                loadMenuFromDb();
                System.out.println("✅ Menu dihapus: " + row.nama());
            }
        } catch (Exception e) {
            System.err.println("Gagal hapus menu: " + e.getMessage());
        }
    }

    // ── Log aktivitas admin ───────────────────────────────────────

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

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    // ── Navigasi Sidebar ──────────────────────────────────────────

    @FXML
    private void onDashboard() {
        Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Dashboard-Admin.fxml");
    }

    @FXML private void onEditMenu() { /* tetap di sini */ }

    @FXML
    private void onLokasi() {
        Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Lokasi.fxml");
    }

    @FXML
    private void onLogout() {
        Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Logout.fxml");
    }

    // ── Record ───────────────────────────────────────────────────

    public record MenuRow(int id, String nama, String deskripsi, int harga, String kategori) {}
}
