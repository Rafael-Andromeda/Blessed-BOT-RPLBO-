package com.rplbo.app.rplboblessedbot;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
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

    private String googleMapsUrl = "https://maps.app.goo.gl/3YT6BkGtAh994yiv7";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
            }
        });
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
            }
        });
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