package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;

public class LokasiController implements Initializable {

    @FXML private Label lblMapAlamat;
    @FXML private Label lblAlamat;
    @FXML private Label lblJam;
    @FXML private Label lblHari;
    @FXML private Label lblPatokan;
    @FXML private Label lblParkir;
    @FXML private Label lblParkirDetail;

    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblInstagram;

    private String googleMapsUrl = "https://maps.app.goo.gl/KWUP1wLZooBtDFXP7";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadInformasiDariDb();
    }

    private void loadInformasiDariDb() {
        try {
            Connection conn = DatabaseHelper.getConnection();

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT nama_kedai, alamat, jam_buka, jam_tutup, hari_operasi, maps_url, phone, email, instagram " +
                                 "FROM informasi_kedai LIMIT 1")) {

                if (rs.next()) {
                    String alamat = rs.getString("alamat");
                    String jamBuka = rs.getString("jam_buka");
                    String jamTutup = rs.getString("jam_tutup");
                    String hari = rs.getString("hari_operasi");
                    String mapsUrl = rs.getString("maps_url");

                    lblMapAlamat.setText(alamat);
                    lblAlamat.setText(alamat);
                    lblJam.setText(jamBuka + " – " + jamTutup);
                    lblHari.setText(hari);
                    googleMapsUrl = mapsUrl;

                    lblPhone.setText(rs.getString("phone"));
                    lblEmail.setText(rs.getString("email"));
                    lblInstagram.setText(rs.getString("instagram"));

                    lblPatokan.setText("Dekat Malioboro, 500m dari Stasiun");
                    lblParkir.setText("Tersedia");
                    lblParkirDetail.setText("Motor & Mobil");
                }
            }

        } catch (Exception e) {
            System.err.println("Lokasi: gagal load data dari DB — " + e.getMessage());

            lblMapAlamat.setText("Lokasi Kedai Kopi Blessed");
            lblAlamat.setText("Lokasi Kedai Kopi Blessed");
            lblJam.setText("08:00 – 22:00");
            lblHari.setText("Setiap hari");
            lblPatokan.setText("Dekat Malioboro, 500m dari Stasiun");
            lblParkir.setText("Tersedia");
            lblParkirDetail.setText("Motor & Mobil");

            lblPhone.setText("+62 812-3456-7890");
            lblEmail.setText("kedai@blessbot.id");
            lblInstagram.setText("@kedaikopi.blessed");
        }
    }

    @FXML
    private void onBukaGoogleMap() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(googleMapsUrl));
            } else {
                System.out.println("Desktop tidak didukung");
            }
        } catch (Exception e) {
            System.err.println("Gagal membuka Google Maps: " + e.getMessage());
        }
    }

    @FXML
    private void onEditPetaLokasi() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Peta Lokasi");
        dialog.setHeaderText("Perbarui informasi peta lokasi:");

        TextField txtMapAlamat = new TextField(lblMapAlamat.getText());
        TextField txtAlamat = new TextField(lblAlamat.getText());
        TextField txtJam = new TextField(lblJam.getText());
        TextField txtHari = new TextField(lblHari.getText());
        TextField txtPatokan = new TextField(lblPatokan.getText());
        TextField txtParkir = new TextField(lblParkir.getText());
        TextField txtParkirDetail = new TextField(lblParkirDetail.getText());
        TextField txtGoogleMaps = new TextField(googleMapsUrl);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(new Label("Alamat di Peta:"), 0, 0);
        grid.add(txtMapAlamat, 1, 0);

        grid.add(new Label("Alamat Card:"), 0, 1);
        grid.add(txtAlamat, 1, 1);

        grid.add(new Label("Jam Operasional:"), 0, 2);
        grid.add(txtJam, 1, 2);

        grid.add(new Label("Hari:"), 0, 3);
        grid.add(txtHari, 1, 3);

        grid.add(new Label("Patokan:"), 0, 4);
        grid.add(txtPatokan, 1, 4);

        grid.add(new Label("Parkir:"), 0, 5);
        grid.add(txtParkir, 1, 5);

        grid.add(new Label("Detail Parkir:"), 0, 6);
        grid.add(txtParkirDetail, 1, 6);

        grid.add(new Label("Link Google Maps:"), 0, 7);
        grid.add(txtGoogleMaps, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                lblMapAlamat.setText(txtMapAlamat.getText());
                lblAlamat.setText(txtAlamat.getText());
                lblJam.setText(txtJam.getText());
                lblHari.setText(txtHari.getText());
                lblPatokan.setText(txtPatokan.getText());
                lblParkir.setText(txtParkir.getText());
                lblParkirDetail.setText(txtParkirDetail.getText());
                googleMapsUrl = txtGoogleMaps.getText();

                updatePetaLokasiKeDb(
                        txtAlamat.getText(),
                        txtJam.getText(),
                        txtHari.getText(),
                        txtGoogleMaps.getText()
                );
            }
        });
    }

    private void updatePetaLokasiKeDb(String alamat, String jam, String hari, String mapsUrl) {
        try {
            String jamBuka = jam;
            String jamTutup = "";

            if (jam.contains("–")) {
                String[] parts = jam.split("–");
                jamBuka = parts[0].trim();
                jamTutup = parts[1].trim();
            } else if (jam.contains("-")) {
                String[] parts = jam.split("-");
                jamBuka = parts[0].trim();
                jamTutup = parts[1].trim();
            }

            Connection conn = DatabaseHelper.getConnection();

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE informasi_kedai " +
                            "SET alamat = ?, jam_buka = ?, jam_tutup = ?, hari_operasi = ?, maps_url = ? " +
                            "WHERE id = 1")) {

                ps.setString(1, alamat);
                ps.setString(2, jamBuka);
                ps.setString(3, jamTutup);
                ps.setString(4, hari);
                ps.setString(5, mapsUrl);

                ps.executeUpdate();
                System.out.println("✅ Peta lokasi berhasil disimpan ke database.");
            }

        } catch (Exception e) {
            System.err.println("Gagal simpan peta lokasi ke DB: " + e.getMessage());
        }
    }

    @FXML
    private void onEditInformasi() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Informasi");
        dialog.setHeaderText("Perbarui informasi kontak:");

        TextField txtPhone = new TextField(lblPhone.getText());
        TextField txtEmail = new TextField(lblEmail.getText());
        TextField txtInstagram = new TextField(lblInstagram.getText());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(new Label("WhatsApp/Telepon:"), 0, 0);
        grid.add(txtPhone, 1, 0);

        grid.add(new Label("Email:"), 0, 1);
        grid.add(txtEmail, 1, 1);

        grid.add(new Label("Instagram:"), 0, 2);
        grid.add(txtInstagram, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                lblPhone.setText(txtPhone.getText());
                lblEmail.setText(txtEmail.getText());
                lblInstagram.setText(txtInstagram.getText());

                updateKontakKeDb(
                        txtPhone.getText(),
                        txtEmail.getText(),
                        txtInstagram.getText()
                );
            }
        });
    }

    private void updateKontakKeDb(String phone, String email, String instagram) {
        try {
            Connection conn = DatabaseHelper.getConnection();

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE informasi_kedai " +
                            "SET phone = ?, email = ?, instagram = ? " +
                            "WHERE id = 1")) {

                ps.setString(1, phone);
                ps.setString(2, email);
                ps.setString(3, instagram);

                ps.executeUpdate();
                System.out.println("✅ Kontak berhasil disimpan ke database.");
            }

        } catch (Exception e) {
            System.err.println("Gagal simpan kontak ke DB: " + e.getMessage());
        }
    }

    // Navigasi Sidebar

    @FXML
    private void onDashboard() {
        Navigator.goTo(lblAlamat, "/com/rplbo/app/rplboblessedbot/Dashboard-Admin.fxml");
    }

    @FXML
    private void onEditMenu() {
        Navigator.goTo(lblAlamat, "/com/rplbo/app/rplboblessedbot/Manajemen-Menu.fxml");
    }

    @FXML
    private void onLokasi() {
        // Tetap di halaman Lokasi
    }

    @FXML
    private void onLogout() {
        Navigator.goTo(lblAlamat, "/com/rplbo/app/rplboblessedbot/Logout.fxml");
    }
}