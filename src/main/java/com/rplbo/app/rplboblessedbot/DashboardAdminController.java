package com.blessbot.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller untuk Dashboard-Admin.fxml
 * Menampilkan ringkasan: jumlah menu, jam operasional, dan alamat kedai.
 */
public class DashboardAdminController implements Initializable {

    @FXML private Label lblMenuCount;
    @FXML private Label lblJamOps;
    @FXML private Label lblAlamat;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Idealnya data diambil dari service / repository
        lblMenuCount.setText("Menu Terdaftar: 10");
        lblJamOps.setText("08:00 - 22:00");
        lblAlamat.setText("Jl. Anggrek No.10 Jogja");
    }

    // ── Navigasi Sidebar ────────────────────────────────────────────────────

    @FXML
    private void onDashboard() {
        // Halaman ini – tidak perlu reload
    }

    @FXML
    private void onEditMenu() {
        navigateTo("/fxml/Manajemen-Menu.fxml");
    }

    @FXML
    private void onLokasi() {
        navigateTo("/fxml/Lokasi.fxml");
    }

    @FXML
    private void onLogout() {
        navigateTo("/fxml/Logout.fxml");
    }

    // ── Aksi di dalam Dashboard ─────────────────────────────────────────────

    @FXML
    private void onTambahMenu() {
        navigateTo("/fxml/Manajemen-Menu.fxml");
    }

    @FXML
    private void onEditInfo() {
        navigateTo("/fxml/Informasi.fxml");
    }

    // ── Helper navigasi ─────────────────────────────────────────────────────

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage  stage = getCurrentStage();
            if (stage != null) {
                stage.setScene(new Scene(root));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Stage getCurrentStage() {
        if (lblMenuCount.getScene() != null) {
            return (Stage) lblMenuCount.getScene().getWindow();
        }
        return null;
    }
}
