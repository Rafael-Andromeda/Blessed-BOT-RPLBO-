package com.rplbo.app.rplboblessedbot;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller untuk Manajemen-Menu.fxml
 * Menampilkan daftar menu di TableView dan menyediakan aksi Tambah / Edit / Hapus.
 */
public class ManajemenMenuController implements Initializable {

    @FXML private TableView<MenuRow>    menuTable;
    @FXML private TableColumn<MenuRow, String> colFoto;
    @FXML private TableColumn<MenuRow, String> colNama;
    @FXML private TableColumn<MenuRow, String> colHarga;
    @FXML private TableColumn<MenuRow, String> colAksi;

    private final ObservableList<MenuRow> menuData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadDummyData();
    }

    // ── Setup kolom ─────────────────────────────────────────────────────────

    private void setupColumns() {
        colFoto.setCellValueFactory(c -> new SimpleStringProperty("☕"));

        colNama.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().nama()));

        colHarga.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().harga()));

        // Kolom aksi: tombol Edit dan Hapus
        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("✏");
            private final Button btnHapus = new Button("🗑");

            {
                btnEdit.setStyle("-fx-cursor:hand;");
                btnHapus.setStyle("-fx-cursor:hand; -fx-text-fill:red;");

                btnEdit.setOnAction(e -> {
                    MenuRow row = getTableView().getItems().get(getIndex());
                    onEditMenu(row);
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

    private void loadDummyData() {
        menuData.addAll(
                new MenuRow("Americano",         "Rp18.000"),
                new MenuRow("Long Black",        "Rp20.000"),
                new MenuRow("Latte",             "Rp22.000"),
                new MenuRow("Cappuccino",        "Rp22.000"),
                new MenuRow("Kopi Gula Aren",    "Rp25.000")
        );
    }

    // ── Aksi ────────────────────────────────────────────────────────────────

    @FXML
    private void onTambahMenu() {
        // TODO: Buka dialog / form tambah menu
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Form tambah menu akan dibuka di sini.", ButtonType.OK);
        alert.setTitle("Tambah Menu");
        alert.showAndWait();
    }

    private void onEditMenu(MenuRow row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Edit menu: " + row.nama(), ButtonType.OK);
        alert.setTitle("Edit Menu");
        alert.showAndWait();
    }

    private void onHapusMenu(MenuRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus menu \"" + row.nama() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Hapus Menu");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) menuData.remove(row);
        });
    }

    // ── Navigasi Sidebar ────────────────────────────────────────────────────

    @FXML private void onDashboard() { navigateTo("/fxml/Dashboard-Admin.fxml"); }
    @FXML private void onEditMenu()   { /* halaman ini */ }
    @FXML private void onLokasi()     { navigateTo("/fxml/Lokasi.fxml"); }
    @FXML private void onLogout()     { navigateTo("/fxml/Logout.fxml"); }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root  = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage  stage = (Stage) menuTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Inner record ────────────────────────────────────────────────────────

    public record MenuRow(String nama, String harga) {}
}
