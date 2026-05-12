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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ManajemenMenuController implements Initializable {

    private static final String IMAGE_DIR = "images/menu/";

    @FXML private TableView<MenuRow> menuTable;
    @FXML private TableColumn<MenuRow, String> colFoto;
    @FXML private TableColumn<MenuRow, String> colNama;
    @FXML private TableColumn<MenuRow, String> colKategori;
    @FXML private TableColumn<MenuRow, String> colHarga;
    @FXML private TableColumn<MenuRow, String> colAksi;

    private final ObservableList<MenuRow> menuData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new File(IMAGE_DIR).mkdirs();
        setupColumns();
        loadMenuFromDb();
    }

    // ── Load kategori dari DB ──────────────────────────────────────

    private List<KategoriItem> loadKategoriList() {
        List<KategoriItem> list = new ArrayList<>();
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT id, nama_kategori FROM kategori_menu ORDER BY urutan")) {
                while (rs.next()) {
                    list.add(new KategoriItem(rs.getInt("id"), rs.getString("nama_kategori")));
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal load kategori: " + e.getMessage());
        }
        return list;
    }

    // ── Setup kolom tabel ─────────────────────────────────────────

    private void setupColumns() {
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
                if (empty || gambar == null || gambar.isBlank()) { setGraphic(lblFallback); return; }
                File f = new File(IMAGE_DIR + gambar);
                if (f.exists()) { imgView.setImage(new Image(f.toURI().toString(), true)); setGraphic(imgView); }
                else { setGraphic(lblFallback); }
            }
        });

        colNama.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nama()));

        if (colKategori != null) {
            colKategori.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().kategori() != null ? c.getValue().kategori() : "-"));
        }

        colHarga.setCellValueFactory(c ->
                new SimpleStringProperty("Rp" + String.format("%,.0f", (double) c.getValue().harga())
                        .replace(",", ".")));

        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("✏");
            private final Button btnHapus = new Button("🗑");
            {
                btnEdit.setStyle("-fx-cursor:hand;");
                btnHapus.setStyle("-fx-cursor:hand; -fx-text-fill:red;");
                btnEdit.setOnAction(e -> showEditMenuDialog(getTableView().getItems().get(getIndex())));
                btnHapus.setOnAction(e -> onHapusMenu(getTableView().getItems().get(getIndex())));
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
                         "SELECT m.id, m.nama, m.deskripsi, m.harga, k.nama_kategori, m.gambar_url, m.kategori_id " +
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
                            rs.getString("gambar_url"),
                            rs.getInt("kategori_id")
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("ManajemenMenu: gagal load — " + e.getMessage());
        }
    }

    // ── Helper gambar ─────────────────────────────────────────────

    private File pilihGambar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Pilih Foto Menu");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("File Gambar", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        return fc.showOpenDialog((Stage) menuTable.getScene().getWindow());
    }

    private String simpanGambar(File source) throws IOException {
        String namaFile = System.currentTimeMillis() + "_" + source.getName();
        Files.copy(source.toPath(), new File(IMAGE_DIR + namaFile).toPath(), StandardCopyOption.REPLACE_EXISTING);
        return namaFile;
    }

    // ── Tambah menu ───────────────────────────────────────────────

    @FXML
    private void onTambahMenu() {
        List<KategoriItem> kategoriList = loadKategoriList();
        if (kategoriList.isEmpty()) { showAlert("Tidak ada kategori di database."); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tambah Menu");
        dialog.setHeaderText("Isi data menu baru");

        TextField tfNama  = new TextField();
        tfNama.setPromptText("Contoh: Americano");

        TextArea taDesk = new TextArea();
        taDesk.setPromptText("Deskripsi singkat menu (opsional)");
        taDesk.setPrefRowCount(3);
        taDesk.setWrapText(true);

        TextField tfHarga = new TextField("0");

        ComboBox<KategoriItem> cbKategori = new ComboBox<>(FXCollections.observableArrayList(kategoriList));
        cbKategori.getSelectionModel().selectFirst();
        cbKategori.setMaxWidth(Double.MAX_VALUE);

        Label  lblFoto = new Label("(belum ada foto)");
        Button btnFoto = new Button("📷 Pilih Foto");
        final String[] namaGambar = {null};
        btnFoto.setOnAction(e -> {
            File f = pilihGambar();
            if (f != null) {
                try { namaGambar[0] = simpanGambar(f); lblFoto.setText("✅ " + f.getName()); }
                catch (IOException ex) { showAlert("Gagal simpan foto: " + ex.getMessage()); }
            }
        });

        GridPane grid = buildGrid();
        grid.add(new Label("Kategori:"),   0, 0); grid.add(cbKategori, 1, 0);
        grid.add(new Label("Nama Menu:"),  0, 1); grid.add(tfNama,     1, 1);
        grid.add(new Label("Deskripsi:"),  0, 2); grid.add(taDesk,     1, 2);
        grid.add(new Label("Harga (Rp):"), 0, 3); grid.add(tfHarga,    1, 3);
        grid.add(new Label("Foto:"),       0, 4);
        grid.add(new HBox(8, btnFoto, lblFoto) {{ setAlignment(Pos.CENTER_LEFT); }}, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(460);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String nama = tfNama.getText().trim();
            if (nama.isEmpty()) { showAlert("Nama tidak boleh kosong."); return; }
            KategoriItem kat = cbKategori.getValue();
            if (kat == null) { showAlert("Pilih kategori."); return; }
            try {
                insertMenuToDb(nama, taDesk.getText().trim(), Integer.parseInt(tfHarga.getText().trim()), kat.id(), namaGambar[0]);
                loadMenuFromDb();
            } catch (NumberFormatException ex) { showAlert("Harga harus berupa angka."); }
        });
    }

    private void insertMenuToDb(String nama, String deskripsi, int harga, int kategoriId, String gambar) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO menu (kategori_id, nama, deskripsi, harga, gambar_url) VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, kategoriId);
                ps.setString(2, nama);
                ps.setString(3, deskripsi.isEmpty() ? null : deskripsi);
                ps.setInt(4, harga);
                ps.setString(5, gambar);
                ps.executeUpdate();
                logAktivitas("Tambah menu \"" + nama + "\"");
            }
        } catch (Exception e) { System.err.println("Gagal tambah menu: " + e.getMessage()); }
    }

    // ── Edit menu ─────────────────────────────────────────────────

    private void showEditMenuDialog(MenuRow row) {
        List<KategoriItem> kategoriList = loadKategoriList();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Menu");
        dialog.setHeaderText("Edit: " + row.nama());

        TextField tfNama  = new TextField(row.nama());
        TextArea  taDesk  = new TextArea(row.deskripsi() != null ? row.deskripsi() : "");
        taDesk.setPrefRowCount(3); taDesk.setWrapText(true);
        TextField tfHarga = new TextField(String.valueOf(row.harga()));

        ComboBox<KategoriItem> cbKategori = new ComboBox<>(FXCollections.observableArrayList(kategoriList));
        kategoriList.stream().filter(k -> k.id() == row.kategoriId()).findFirst()
                .ifPresent(k -> cbKategori.getSelectionModel().select(k));
        cbKategori.setMaxWidth(Double.MAX_VALUE);

        Label  lblFoto = new Label(row.gambar() != null ? "📷 " + row.gambar() : "(belum ada foto)");
        Button btnFoto = new Button("📷 Ganti Foto");
        final String[] namaGambar = {row.gambar()};
        btnFoto.setOnAction(e -> {
            File f = pilihGambar();
            if (f != null) {
                try { namaGambar[0] = simpanGambar(f); lblFoto.setText("✅ " + f.getName()); }
                catch (IOException ex) { showAlert("Gagal simpan foto: " + ex.getMessage()); }
            }
        });

        GridPane grid = buildGrid();
        grid.add(new Label("Kategori:"),   0, 0); grid.add(cbKategori, 1, 0);
        grid.add(new Label("Nama Menu:"),  0, 1); grid.add(tfNama,     1, 1);
        grid.add(new Label("Deskripsi:"),  0, 2); grid.add(taDesk,     1, 2);
        grid.add(new Label("Harga (Rp):"), 0, 3); grid.add(tfHarga,    1, 3);
        grid.add(new Label("Foto:"),       0, 4); grid.add(new VBox(6, btnFoto, lblFoto), 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(460);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String namaBaru = tfNama.getText().trim();
            if (namaBaru.isEmpty()) { showAlert("Nama tidak boleh kosong."); return; }
            KategoriItem kat = cbKategori.getValue();
            if (kat == null) { showAlert("Pilih kategori."); return; }
            try {
                updateMenuInDb(row.id(), namaBaru, taDesk.getText().trim(), Integer.parseInt(tfHarga.getText().trim()), kat.id(), namaGambar[0]);
                loadMenuFromDb();
            } catch (NumberFormatException ex) { showAlert("Harga harus berupa angka."); }
        });
    }

    private void updateMenuInDb(int id, String nama, String deskripsi, int harga, int kategoriId, String gambar) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE menu SET nama=?, deskripsi=?, harga=?, kategori_id=?, gambar_url=? WHERE id=?")) {
                ps.setString(1, nama);
                ps.setString(2, deskripsi.isEmpty() ? null : deskripsi);
                ps.setInt(3, harga);
                ps.setInt(4, kategoriId);
                ps.setString(5, gambar);
                ps.setInt(6, id);
                ps.executeUpdate();
                logAktivitas("Edit menu \"" + nama + "\"");
            }
        } catch (Exception e) { System.err.println("Gagal edit menu: " + e.getMessage()); }
    }

    // ── Hapus menu (soft delete) ───────────────────────────────────

    private void onHapusMenu(MenuRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus menu \"" + row.nama() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Hapus Menu");
        confirm.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) deleteMenuFromDb(row); });
    }

    private void deleteMenuFromDb(MenuRow row) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("UPDATE menu SET tersedia = 0 WHERE id = ?")) {
                ps.setInt(1, row.id());
                ps.executeUpdate();
                logAktivitas("Hapus menu \"" + row.nama() + "\"");
                loadMenuFromDb();
            }
        } catch (Exception e) { System.err.println("Gagal hapus menu: " + e.getMessage()); }
    }

    // ── Utility ──────────────────────────────────────────────────

    private GridPane buildGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        javafx.scene.layout.ColumnConstraints cc0 = new javafx.scene.layout.ColumnConstraints();
        cc0.setMinWidth(90);
        javafx.scene.layout.ColumnConstraints cc1 = new javafx.scene.layout.ColumnConstraints();
        cc1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc0, cc1);
        return grid;
    }

    // ── Log aktivitas admin (waktu lokal WIB, bukan UTC) ──────────
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

    @FXML private void onDashboard()       { Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Dashboard-Admin.fxml"); }
    @FXML private void onEditMenu()        { /* tetap di sini */ }
    @FXML private void onRekomendasiMenu() { Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Kelola-Rekomendasi-Menu.fxml"); }
    @FXML private void onLokasi()          { Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Lokasi.fxml"); }
    @FXML private void onLogout()          { Navigator.goTo(menuTable, "/com/rplbo/app/rplboblessedbot/Logout.fxml"); }

    // ── Records ───────────────────────────────────────────────────

    public record MenuRow(int id, String nama, String deskripsi, int harga,
                          String kategori, String gambar, int kategoriId) {}

    public record KategoriItem(int id, String nama) {
        @Override public String toString() { return nama; }
    }
}