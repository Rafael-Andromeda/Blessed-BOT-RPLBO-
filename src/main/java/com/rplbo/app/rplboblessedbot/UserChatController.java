package com.rplbo.app.rplboblessedbot;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

public class UserChatController {

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox       chatContainer;
    @FXML private TextField  inputField;
    @FXML private Label      lblScreenTag;

    private Map<String, List<String[]>> menuByKategori = new LinkedHashMap<>();
    private List<String[]> fasilitasList = new ArrayList<>();
    private String jamBuka = "08:00", jamTutup = "22:00", hariOps = "Setiap Hari";
    private String namaKedai = "Kedai Kopi Blessed", alamat = "", mapsUrl = "";

    @FXML
    public void initialize() {
        loadAllDataFromDb();
        javafx.application.Platform.runLater(this::showRekomendasiMenuHariIni);
    }

    private void loadAllDataFromDb() {
        try {
            Connection conn = DatabaseHelper.getConnection();

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT k.nama_kategori, m.nama, m.deskripsi, m.harga, m.gambar_url " +
                                 "FROM menu m JOIN kategori_menu k ON m.kategori_id = k.id " +
                                 "WHERE m.tersedia = 1 ORDER BY k.urutan, m.id")) {
                while (rs.next()) {
                    String kat = rs.getString("nama_kategori");
                    menuByKategori.computeIfAbsent(kat, x -> new ArrayList<>()).add(new String[]{
                            rs.getString("nama"),
                            rs.getString("deskripsi"),
                            "Rp" + String.format("%,.0f", (double) rs.getInt("harga")).replace(",", "."),
                            rs.getString("gambar_url")
                    });
                }
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT icon, nama FROM fasilitas WHERE tersedia = 1")) {
                while (rs.next()) {
                    fasilitasList.add(new String[]{rs.getString("icon"), rs.getString("nama")});
                }
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT nama_kedai, jam_buka, jam_tutup, hari_operasi, alamat, maps_url " +
                                 "FROM informasi_kedai LIMIT 1")) {
                if (rs.next()) {
                    namaKedai = rs.getString("nama_kedai");
                    jamBuka   = rs.getString("jam_buka");
                    jamTutup  = rs.getString("jam_tutup");
                    hariOps   = rs.getString("hari_operasi");
                    alamat    = rs.getString("alamat");
                    mapsUrl   = rs.getString("maps_url");
                }
            }

            System.out.println("✅ UserChat: data berhasil dimuat dari DB");
        } catch (Exception e) {
            System.err.println("UserChat: gagal load DB, pakai data statis — " + e.getMessage());
            loadFallbackData();
        }
    }

    private void loadFallbackData() {
        menuByKategori.clear();
        menuByKategori.put("Kopi Hitam", List.of(
                new String[]{"Americano", "Espresso + air panas", "Rp18.000", null},
                new String[]{"Long Black", "Espresso kuat & bold", "Rp20.000", null},
                new String[]{"V60", "Pour-over filter coffee", "Rp22.000", null}
        ));
        menuByKategori.put("Kopi Susu", List.of(
                new String[]{"Latte", "Espresso + susu lembut", "Rp25.000", null},
                new String[]{"Cappuccino", "Espresso + foam tebal", "Rp23.000", null},
                new String[]{"Flat White", "Double shot + microfoam", "Rp26.000", null}
        ));
        menuByKategori.put("Kopi Gula", List.of(
                new String[]{"Kopi Gula Aren", "Espresso + gula aren", "Rp24.000", null},
                new String[]{"Es Kopi Susu", "Kopi susu dingin", "Rp22.000", null},
                new String[]{"Dalgona Coffee", "Kopi kocok creamy", "Rp27.000", null}
        ));
        fasilitasList = List.of(
                new String[]{"📶", "Free Wi-Fi"},
                new String[]{"🚪", "Private Room"},
                new String[]{"🚗", "Parkir Area"},
                new String[]{"☂", "Outdoor"}
        );
        mapsUrl = "https://maps.google.com/?q=Kedai+Kopi+Blessed+Jogja";
        alamat = "Jl. Anggrek No.10, Jogja";
    }

    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.clear();

        appendUserBubble(text);
        String topic = detectTopic(text.toLowerCase());

        PauseTransition delay = new PauseTransition(Duration.millis(300));
        delay.setOnFinished(e -> routeToTopic(topic));
        delay.play();

        logChatToDb(text, topic, "Bot membalas topik: " + topic);
    }

    private void logChatToDb(String pesan, String topik, String balasan) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            conn.prepareStatement(
                            "INSERT INTO chat_log (pesan_user, topik, balasan_bot) VALUES ('" +
                                    pesan.replace("'", "''") + "','" + topik + "','" +
                                    balasan.replace("'", "''") + "')")
                    .executeUpdate();
        } catch (Exception e) {
            System.err.println("Gagal log chat: " + e.getMessage());
        }
    }

    @FXML public void onMenuDanHarga()   { appendUserBubble("Menu & Harga");    showMenuDanHarga(); }
    @FXML public void onFasilitas()      { appendUserBubble("Fasilitas");       showFasilitas();    }
    @FXML public void onJamOperasional() { appendUserBubble("Jam Operasional"); showJamOperasional(); }
    @FXML public void onLokasiKedai()    { appendUserBubble("Lokasi Kedai");    showLokasi();       }

    @FXML
    private void onLogoutUser() {
        Navigator.goTo(inputField, "/com/rplbo/app/rplboblessedbot/Welcome.fxml");
    }

    private String detectTopic(String text) {
        if (text.matches(".*\\b(rekomendasi|rekomen|saran|suggest|pilihan hari|menu hari|hari ini).*"))
            return "rekomendasi";
        if (text.matches(".*\\b(menu|harga|kopi|minum|makan|pesan|beli|ada apa|tersedia|lainnya|selain itu).*"))
            return "menu";
        if (text.matches(".*\\b(fasilitas|wifi|wi-fi|parkir|ruang|outdoor|private).*"))
            return "fasilitas";
        if (text.matches(".*\\b(jam|buka|tutup|operasional|waktu|kapan).*"))
            return "jam";
        if (text.matches(".*\\b(lokasi|alamat|dimana|mana|tempat|maps|jalan).*"))
            return "lokasi";
        return "unknown";
    }

    private void routeToTopic(String topic) {
        switch (topic) {
            case "rekomendasi" -> showRekomendasiMenuHariIni();
            case "menu"        -> showMenuDanHarga();
            case "fasilitas"   -> showFasilitas();
            case "jam"         -> showJamOperasional();
            case "lokasi"      -> showLokasi();
            default            -> showUnknown();
        }
    }

    private void showMenuDanHarga() {
        updateScreenTag("User-Chat-Menu-dan-Harga");
        appendBotMessage("Berikut informasi menu yang tersedia:");

        VBox card = makeCard();
        card.setMaxWidth(520);

        HBox tabs = new HBox(4);
        tabs.setMaxWidth(430);
        VBox menuList = new VBox(8);

        List<String> kategoriList = new ArrayList<>(menuByKategori.keySet());
        List<Button> tabButtons = new ArrayList<>();

        for (int i = 0; i < kategoriList.size(); i++) {
            String kat = kategoriList.get(i);
            Button btn = makeTabBtn(kat, i == 0);
            tabButtons.add(btn);
            tabs.getChildren().add(btn);
        }

        if (!kategoriList.isEmpty()) {
            populateMenuList(menuList, menuByKategori.get(kategoriList.get(0)));
        }

        for (int i = 0; i < tabButtons.size(); i++) {
            final int idx = i;
            tabButtons.get(i).setOnAction(e -> {
                setActiveTab(tabs, tabButtons.get(idx));
                populateMenuList(menuList, menuByKategori.get(kategoriList.get(idx)));
            });
        }

        card.getChildren().addAll(tabs, menuList);
        appendBotNode(card);
    }

    private void populateMenuList(VBox container, List<String[]> items) {
        container.getChildren().clear();
        if (items == null) return;
        for (String[] item : items) {
            HBox row = new HBox(12);
            row.getStyleClass().add("menu-item-row");
            row.setAlignment(Pos.CENTER_LEFT);

            // Gambar dari URL, fallback ke emoji
            StackPane icon = new StackPane();
            icon.getStyleClass().add("image-placeholder");
            icon.setPrefSize(56, 56);
            icon.setMinSize(56, 56);
            icon.setMaxSize(56, 56);

            String imgUrl = (item.length > 3 && item[3] != null) ? item[3] : null;
            if (imgUrl != null && !imgUrl.isBlank()) {
                try {
                    File imgFile = new File("images/menu/" + imgUrl);
                    String imageUri = imgFile.exists() ? imgFile.toURI().toString() : imgUrl;
                    ImageView iv = new ImageView(new Image(imageUri, 56, 56, true, true, true));
                    iv.setFitWidth(56);
                    iv.setFitHeight(56);
                    icon.getChildren().add(iv);
                } catch (Exception e) {
                    Label emoji = new Label("☕");
                    emoji.setStyle("-fx-font-size:20px;");
                    icon.getChildren().add(emoji);
                }
            } else {
                Label emoji = new Label("☕");
                emoji.setStyle("-fx-font-size:20px;");
                icon.getChildren().add(emoji);
            }

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label name = new Label(item[0]);
            name.getStyleClass().add("menu-item-name");
            Label desc = new Label(item[1]);
            desc.getStyleClass().add("menu-item-desc");
            desc.setWrapText(true);
            desc.setMaxWidth(200);
            info.getChildren().addAll(name, desc);

            Label price = new Label(item[2]);
            price.getStyleClass().add("menu-item-price");

            row.getChildren().addAll(icon, info, price);
            container.getChildren().add(row);
        }
    }

    private void showRekomendasiMenuHariIni() {
        DayOfWeek dow = LocalDate.now().getDayOfWeek();
        String hariIni = switch (dow) {
            case MONDAY    -> "Senin";
            case TUESDAY   -> "Selasa";
            case WEDNESDAY -> "Rabu";
            case THURSDAY  -> "Kamis";
            case FRIDAY    -> "Jumat";
            case SATURDAY  -> "Sabtu";
            case SUNDAY    -> "Minggu";
        };

        updateScreenTag("User-Chat-Rekomendasi-Menu");

        String namaMenu = null, deskripsi = null, catatan = null, hargaStr = null;
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT m.nama, m.deskripsi, m.harga, r.catatan " +
                            "FROM rekomendasi_menu r " +
                            "JOIN menu m ON r.menu_id = m.id " +
                            "WHERE r.hari = ?")) {
                ps.setString(1, hariIni);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    namaMenu  = rs.getString("nama");
                    deskripsi = rs.getString("deskripsi");
                    catatan   = rs.getString("catatan");
                    hargaStr  = "Rp" + String.format("%,.0f", (double) rs.getInt("harga"))
                            .replace(",", ".");
                }
            }
        } catch (Exception e) {
            System.err.println("showRekomendasiMenuHariIni: gagal query — " + e.getMessage());
        }

        appendBotMessage("☀ Rekomendasi Menu Hari Ini (" + hariIni + ")");

        if (namaMenu == null) {
            appendBotMessage("Belum ada rekomendasi menu untuk hari " + hariIni + " ini. "
                    + "Silakan tanyakan menu lainnya!");
            return;
        }

        VBox card = makeCard();
        card.setMaxWidth(370);
        card.setStyle("-fx-background-color:#FFF3E0; -fx-background-radius:14;"
                + "-fx-border-color:#C8956C; -fx-border-radius:14; -fx-border-width:1.5;"
                + "-fx-padding:14 16 14 16;");

        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label("⭐  Pilihan Hari " + hariIni);
        badge.setStyle("-fx-background-color:#C8956C; -fx-text-fill:white;"
                + "-fx-background-radius:20; -fx-padding:3 10 3 10;"
                + "-fx-font-size:11px; -fx-font-weight:bold;");
        badgeRow.getChildren().add(badge);

        HBox namaHargaRow = new HBox();
        namaHargaRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(namaHargaRow, Priority.ALWAYS);

        Label lblNama = new Label("☕  " + namaMenu);
        lblNama.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");
        HBox.setHgrow(lblNama, Priority.ALWAYS);

        Label lblHarga = new Label(hargaStr);
        lblHarga.getStyleClass().add("menu-item-price");
        lblHarga.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#C8956C;");

        namaHargaRow.getChildren().addAll(lblNama, lblHarga);

        Label lblDesc = new Label(deskripsi != null ? deskripsi : "");
        lblDesc.getStyleClass().add("menu-item-desc");
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size:13px; -fx-text-fill:#7A5C3A;");

        card.getChildren().addAll(badgeRow, namaHargaRow, lblDesc);

        if (catatan != null && !catatan.isBlank()) {
            Region divider = new Region();
            divider.setStyle("-fx-background-color:#E2C4A0; -fx-pref-height:1;");

            HBox catatanRow = new HBox(6);
            catatanRow.setAlignment(Pos.CENTER_LEFT);
            Label iconCatatan = new Label("💬");
            iconCatatan.setStyle("-fx-font-size:13px;");
            Label lblCatatan = new Label(catatan);
            lblCatatan.setStyle("-fx-font-size:12px; -fx-text-fill:#8B6347; -fx-font-style:italic;");
            lblCatatan.setWrapText(true);
            catatanRow.getChildren().addAll(iconCatatan, lblCatatan);

            card.getChildren().addAll(divider, catatanRow);
        }

        appendBotNode(card);
    }

    @FXML
    public void onRekomendasiMenu() {
        appendUserBubble("Rekomendasi Menu");
        showRekomendasiMenuHariIni();
    }

    private void showFasilitas() {
        updateScreenTag("User-Chat-Fasilitas");
        appendBotMessage("Kami menyediakan berbagai fasilitas seperti wifi dan tempat nyaman untuk bersantai:");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setMaxWidth(300);

        int col = 0, row = 0;
        for (String[] f : fasilitasList) {
            VBox card = new VBox(8);
            card.getStyleClass().add("facility-card");
            card.setAlignment(Pos.CENTER);

            Label icon = new Label(f[0]);
            icon.setStyle("-fx-font-size:28px;");
            Label lbl  = new Label(f[1]);
            lbl.getStyleClass().add("facility-card-text");

            card.getChildren().addAll(icon, lbl);
            GridPane.setColumnIndex(card, col);
            GridPane.setRowIndex(card, row);
            grid.getChildren().add(card);

            col++;
            if (col > 1) { col = 0; row++; }
        }

        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(50);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(cc, cc2);

        appendBotNode(grid);
    }

    private void showJamOperasional() {
        updateScreenTag("User-Chat-Jam-Operasional");
        appendBotMessage("Jam Operasional " + namaKedai + " adalah sebagai berikut:");

        VBox card = makeCard();
        card.setMaxWidth(320);

        Label header = new Label("Jam Operasional");
        header.getStyleClass().add("label-subtitle");
        header.setStyle("-fx-font-weight:bold;");

        Region divider = new Region();
        divider.getStyleClass().add("divider");

        HBox jamRow = new HBox(14);
        jamRow.setAlignment(Pos.CENTER_LEFT);
        Label clock = new Label("🕐");
        clock.setStyle("-fx-font-size:24px;");

        HBox timeBox = new HBox(6);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        Label jamLabel = new Label(jamBuka + " – " + jamTutup);
        jamLabel.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");
        Label hariLabel = new Label(hariOps);
        hariLabel.getStyleClass().add("label-muted");
        hariLabel.setStyle("-fx-font-size:13px; -fx-padding:6 0 0 0;");
        timeBox.getChildren().addAll(jamLabel, hariLabel);

        jamRow.getChildren().addAll(clock, timeBox);
        card.getChildren().addAll(header, divider, jamRow);

        appendBotNode(card);
    }

    private void showLokasi() {
        updateScreenTag("User-Chat-Lokasi");
        appendBotMessage("Berikut lokasi " + namaKedai + ":");

        VBox card = makeCard();
        card.setMaxWidth(360);
        VBox.setMargin(card, new Insets(0));

        HBox alamatRow = new HBox(8);
        alamatRow.setAlignment(Pos.CENTER_LEFT);
        Label pin = new Label("📍");
        pin.setStyle("-fx-font-size:20px; -fx-text-fill:#C8956C;");
        VBox alamatInfo = new VBox(2);
        Label namaKedaiLbl = new Label("\"" + namaKedai + "\"");
        namaKedaiLbl.getStyleClass().add("label-subtitle");
        namaKedaiLbl.setStyle("-fx-font-size:15px;");
        Label alamatLbl = new Label(alamat);
        alamatLbl.getStyleClass().add("label-accent");
        alamatInfo.getChildren().addAll(namaKedaiLbl, alamatLbl);
        alamatRow.getChildren().addAll(pin, alamatInfo);

        StackPane mapBox = new StackPane();
        mapBox.getStyleClass().add("map-placeholder");
        mapBox.setPrefHeight(140);
        VBox mapContent = new VBox(6);
        mapContent.setAlignment(Pos.CENTER);
        Label mapEmoji = new Label("🗺");
        mapEmoji.setStyle("-fx-font-size:40px;");
        Label mapHint = new Label("Lihat di Google Maps");
        mapHint.getStyleClass().add("label-muted");
        mapContent.getChildren().addAll(mapEmoji, mapHint);
        mapBox.getChildren().add(mapContent);

        Button btnMaps = new Button("Buka di Google Maps");
        btnMaps.getStyleClass().add("btn-secondary");
        btnMaps.setMaxWidth(Double.MAX_VALUE);
        final String url = mapsUrl;
        btnMaps.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(alamatRow, mapBox, btnMaps);
        appendBotNode(card);
    }

    private void showUnknown() {
        appendBotMessage("Maaf, saya belum memahami pertanyaan itu. "
                + "Coba tanyakan tentang Menu & Harga, Fasilitas, Jam Operasional, atau Lokasi kami ya!");
    }

    private void appendUserBubble(String text) {
        HBox wrap = new HBox();
        wrap.setAlignment(Pos.CENTER_RIGHT);
        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-bubble-user");
        bubble.setStyle("-fx-font-size:14px; -fx-text-fill:#3B2414;"
                + "-fx-background-color:#E0D5C8;"
                + "-fx-background-radius:16 2 16 16;"
                + "-fx-padding:10 14 10 14;"
                + "-fx-max-width:280px;");
        bubble.setWrapText(true);
        wrap.getChildren().add(bubble);
        chatContainer.getChildren().add(wrap);
        scrollToBottom();
    }

    private void appendBotMessage(String text) {
        HBox wrap = new HBox(10);
        wrap.setAlignment(Pos.CENTER_LEFT);
        Label botIcon = new Label("🤖");
        botIcon.setStyle("-fx-font-size:22px;");
        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-bubble-bot");
        bubble.setWrapText(true);
        bubble.setMaxWidth(340);
        wrap.getChildren().addAll(botIcon, bubble);
        chatContainer.getChildren().add(wrap);
        scrollToBottom();
    }

    private void appendBotNode(Node node) {
        HBox wrap = new HBox(10);
        wrap.setAlignment(Pos.CENTER_LEFT);
        Label botIcon = new Label("🤖");
        botIcon.setStyle("-fx-font-size:22px; -fx-padding:0 0 60 0;");
        wrap.getChildren().addAll(botIcon, node);
        chatContainer.getChildren().add(wrap);
        scrollToBottom();
    }

    private void updateScreenTag(String screenName) {
        if (lblScreenTag != null) lblScreenTag.setText(screenName);
    }

    private VBox makeCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        return card;
    }

    private Button makeTabBtn(String text, boolean active) {
        Button btn = new Button(text);
        btn.getStyleClass().add(active ? "tab-btn-active" : "tab-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        return btn;
    }

    private void setActiveTab(HBox tabs, Button active) {
        for (Node n : tabs.getChildren()) {
            if (n instanceof Button b) {
                b.getStyleClass().removeAll("tab-btn-active", "tab-btn");
                b.getStyleClass().add(b == active ? "tab-btn-active" : "tab-btn");
            }
        }
    }

    private void scrollToBottom() {
        javafx.application.Platform.runLater(() ->
                chatScrollPane.setVvalue(1.0));
    }
}