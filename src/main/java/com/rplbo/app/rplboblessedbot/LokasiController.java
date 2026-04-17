package com.blessbot.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller untuk Lokasi.fxml (halaman admin)
 * Menampilkan peta, alamat, jam operasional, patokan, dan info parkir.
 */
public class LokasiController implements Initializable {

    @FXML private Label lblAlamat;
    @FXML private Label lblJam;
    @FXML private Label lblHari;
    @FXML private Label lblPatokan;
    @FXML private Label lblParkir;
    @FXML private Label lblParkirDetail;

    private static final String GOOGLE_MAPS_URL =
            "https://maps.google.com/?q=Jl.+Anggrek+No+10,+Jogja";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data idealnya dari service / database
        lblAlamat.setText("Jl. Anggrek No.10, Jogja");
        lblJam.setText("08:00 – 22:00");
        lblHari.setText("Setiap hari");
        lblPatokan.setText("Dekat Malioboro, 500m dari Stasiun");
        lblParkir.setText("Tersedia");
        lblParkirDetail.setText("Motor & Mobil");
    }

    // ── Aksi peta ────────────────────────────────────────────────────────────

    /** Dipanggil jika ada tombol / klik pada map-placeholder. */
    @FXML
    private void onBukaGoogleMap() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(GOOGLE_MAPS_URL));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Navigasi Sidebar ────────────────────────────────────────────────────

    @FXML private void onDashboard() { navigateTo("/fxml/Dashboard-Admin.fxml"); }
    @FXML private void onEditMenu()  { navigateTo("/fxml/Manajemen-Menu.fxml"); }
    @FXML private void onLokasi()    { /* halaman ini */ }
    @FXML private void onLogout()    { navigateTo("/fxml/Logout.fxml"); }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root  = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage  stage = (Stage) lblAlamat.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
