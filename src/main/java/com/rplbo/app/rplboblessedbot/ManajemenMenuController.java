package com.rplbo.app.rplboblessedbot;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller untuk Manajemen-Menu.fxml
 * Baca, tambah, edit, dan hapus menu dari DB.
 * Gambar disimpan ke folder lokal images/menu/ — DB hanya menyimpan nama file.
 */
public class ManajemenMenuController implements Initializable {

    // Folder lokal tempat gambar menu disimpan (relatif ke working directory)
    private static final String IMAGE_DIR = "images/menu/";

    @FXML private TableView<MenuRow> menuTable;
    @FXML private TableColumn<MenuRow, String> colFoto;
    @FXML private TableColumn<MenuRow, String> colNama;
    @FXML private TableColumn<MenuRow, String> colHarga;
    @FXML private TableColumn<MenuRow, String> colAksi;

    private final ObservableList<MenuRow> menuData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Pastikan folder gambar sudah ada
        new File(IMAGE_DIR).mkdirs();

        setupColumns();
        loadMenuFromDb();
    }

    // ── Setup kolom tabel ─────────────────────────────────────────

    private void setupColumns() {
        // Kolom foto: tampilkan ImageView dari file lokal
        colFoto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().gambar()));
        colFoto.setCellFactory(col -> new TableCell<>() {
            private final ImageView imgView = new ImageView();
            private final Label lblFallback = new Label("☕");

            {
                imgView.setFitWidth(56);
                imgView.setFitHeight(56);
                imgView.setPreserveRatio(true);
                imgView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 4, 0, 0, 1);");
            }

            @Override
            protected void updateItem(String gambar, boolean empty) {
                super.updateItem(gambar, empty);
                if (empty || gambar == null || gambar.isBlank()) {
                    setGraphic(lblFallback);
                    return;
                }
                File f = new File(IMAGE_DIR + gambar);
                if (f.exists()) {
                    imgView.setImage(new Image(f.toURI().toString(), true));
                    setGraphic(imgView);
                } else {
                    setGraphic(lblFallback);
                }
            }
        });

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
                setGraphic(empty ? null : new HBox(4, btnEdit, btnHapus));
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
                         "SELECT m.id, m.nama, m.deskripsi, m.harga, k.nama_kategori, m.gambar_url " +
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
                            rs.getString("nama_kategori"),
                            rs.getString("gambar_url")   // nama file, bukan URL
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("ManajemenMenu: gagal load — " + e.getMessage());
        }
    }

    // ── Helper: FileChooser untuk pilih gambar ────────────────────

    private File pilihGambar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Pilih Foto Menu");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("File Gambar", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        Stage stage = (Stage) menuTable.getScene().getWindow();
        return fc.showOpenDialog(stage);
    }

    /**
     * Salin gambar yang dipilih ke IMAGE_DIR dengan nama unik.
     * @return nama file yang disimpan (inilah yang masuk ke DB)
     */
    private String simpanGambar(File source) throws IOException {
        String namaFile = System.currentTimeMillis() + "_" + source.getName();
        File dest = new File(IMAGE_DIR + namaFile);
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return namaFile;
    }

    // ── Tambah menu ───────────────────────────────────────────────

    @FXML
    private void onTambahMenu() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tambah Menu");
        dialog.setHeaderText("Isi data menu baru");

        // Form fields
        TextField tfNama  = new TextField();
        TextField tfHarga = new TextField("0");
        Label     lblFoto = new Label("(belum ada foto)");
        Button    btnFoto = new Button("📷 Pilih Foto");

        // State: nama file gambar yang dipilih
        final String[] namaGambar = {null};

        btnFoto.setOnAction(e -> {
            File f = pilihGambar();
            if (f != null) {
                try {
                    namaGambar[0] = simpanGambar(f);
                    lblFoto.setText("✅ " + f.getName());
                } catch (IOException ex) {
                    showAlert("Gagal simpan foto: " + ex.getMessage());
                }
            }
        });

        GridPane grid = buildFormGrid(
                new String[]{"Nama Menu:", "Harga (Rp):", "Foto:"},
                new javafx.scene.Node[]{tfNama, tfHarga,
                        new HBox(8, btnFoto, lblFoto) {{ setAlignment(Pos.CENTER_LEFT); }}}
        );

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String nama = tfNama.getText().trim();
                if (nama.isEmpty()) { showAlert("Nama tidak boleh kosong."); return; }
                try {
                    int harga = Integer.parseInt(tfHarga.getText().trim());
                    insertMenuToDb(nama, harga, namaGambar[0]);
                    loadMenuFromDb();
                } catch (NumberFormatException ex) {
                    showAlert("Harga harus berupa angka.");
                }
            }
        });
    }

    private void insertMenuToDb(String nama, int harga, String gambar) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO menu (kategori_id, nama, harga, gambar_url) VALUES (1, ?, ?, ?)")) {
                ps.setString(1, nama);
                ps.setInt(2, harga);
                ps.setString(3, gambar);    // null kalau belum pilih foto — tidak apa-apa
                ps.executeUpdate();

                logAktivitas("Tambah menu \"" + nama + "\"");
                System.out.println("✅ Menu ditambah: " + nama);
            }
        } catch (Exception e) {
            System.err.println("Gagal tambah menu: " + e.getMessage());
        }
    }

    // ── Edit menu ─────────────────────────────────────────────────

    private void showEditMenuDialog(MenuRow row) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Menu");
        dialog.setHeaderText("Edit data menu: " + row.nama());

        TextField tfNama  = new TextField(row.nama());
        TextField tfHarga = new TextField(String.valueOf(row.harga()));
        Label     lblFoto = new Label(row.gambar() != null ? "📷 " + row.gambar() : "(belum ada foto)");
        Button    btnFoto = new Button("📷 Ganti Foto");

        final String[] namaGambar = {row.gambar()};

        btnFoto.setOnAction(e -> {
            File f = pilihGambar();
            if (f != null) {
                try {
                    namaGambar[0] = simpanGambar(f);
                    lblFoto.setText("✅ " + f.getName());
                } catch (IOException ex) {
                    showAlert("Gagal simpan foto: " + ex.getMessage());
                }
            }
        });

        GridPane grid = buildFormGrid(
                new String[]{"Nama Menu:", "Harga (Rp):", "Foto:"},
                new javafx.scene.Node[]{tfNama, tfHarga,
                        new VBox(6, btnFoto, lblFoto)}
        );

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String namaBaru = tfNama.getText().trim();
                if (namaBaru.isEmpty()) { showAlert("Nama tidak boleh kosong."); return; }
                try {
                    int hargaBaru = Integer.parseInt(tfHarga.getText().trim());
                    updateMenuInDb(row.id(), namaBaru, hargaBaru, namaGambar[0]);
                    loadMenuFromDb();
                } catch (NumberFormatException ex) {
                    showAlert("Harga harus berupa angka.");
                }
            }
        });
    }

    private void updateMenuInDb(int id, String nama, int harga, String gambar) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE menu SET nama = ?, harga = ?, gambar_url = ? WHERE id = ?")) {
                ps.setString(1, nama);
                ps.setInt(2, harga);
                ps.setString(3, gambar);
                ps.setInt(4, id);
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

    // ── Utility: build GridPane form ──────────────────────────────

    private GridPane buildFormGrid(String[] labels, javafx.scene.Node[] fields) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        for (int i = 0; i < labels.length; i++) {
            grid.add(new Label(labels[i]), 0, i);
            grid.add(fields[i], 1, i);
        }
        return grid;
    }

    // ── Log aktivitas admin ───────────────────────────────────────

    private void logAktivitas(String keterangan) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            String waktu = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO aktivitas_admin (admin_id, keterangan, waktu) VALUES (1, ?, ?)")) {
                ps.setString(1, keterangan);
                ps.setString(2, waktu);
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
    private void onRekomendasiMenu() {
        Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Kelola-Rekomendasi-Menu.fxml");
    }

    @FXML
    private void onLokasi() {
        Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Lokasi.fxml");
    }

    @FXML
    private void onLogout() {
        Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Logout.fxml");
    }

    // ── Record ────────────────────────────────────────────────────

    /**
     * @param gambar nama file gambar di folder images/menu/ (bukan URL penuh)
     */
    public record MenuRow(int id, String nama, String deskripsi, int harga, String kategori, String gambar) {}
}