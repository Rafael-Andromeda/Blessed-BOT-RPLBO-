package com.blessbot.controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * UserChatController
 * ------------------
 * Controller tunggal untuk User-Chat.fxml.
 *
 * Cara kerja:
 *  • Semua tampilan chat (Menu & Harga, Fasilitas, Jam Operasional, Lokasi)
 *    dirender secara programatik ke dalam chatContainer.
 *  • Label fx:id="lblScreenTag" di topbar diupdate sesuai topik aktif,
 *    sehingga nama layar terlihat: "User-Chat-Menu-dan-Harga", dst.
 *  • Deteksi topik berdasarkan kata kunci dari input pengguna.
 */
public class UserChatController {

    // ── FXML Bindings ──────────────────────────────────────────────
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox       chatContainer;
    @FXML private TextField  inputField;
    @FXML private Label      lblScreenTag;

    // ── Data menu ──────────────────────────────────────────────────
    private static final String[][] KOPI_HITAM = {
        {"Americano",  "Espresso + air panas",     "Rp18.000"},
        {"Long Black", "Espresso kuat & bold",      "Rp20.000"},
        {"V60",        "Pour-over filter coffee",   "Rp22.000"},
    };
    private static final String[][] KOPI_SUSU = {
        {"Latte",      "Espresso + susu lembut",    "Rp25.000"},
        {"Cappuccino", "Espresso + foam tebal",     "Rp23.000"},
        {"Flat White", "Double shot + microfoam",   "Rp26.000"},
    };
    private static final String[][] KOPI_GULA = {
        {"Kopi Gula Aren", "Espresso + gula aren",  "Rp24.000"},
        {"Es Kopi Susu",   "Kopi susu dingin",       "Rp22.000"},
        {"Dalgona Coffee", "Kopi kocok creamy",      "Rp27.000"},
    };

    // ──────────────────────────────────────────────────────────────
    //  KIRIM PESAN
    // ──────────────────────────────────────────────────────────────

    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.clear();

        appendUserBubble(text);
        String topic = detectTopic(text.toLowerCase());

        // Tunda sedikit agar terasa natural
        PauseTransition delay = new PauseTransition(Duration.millis(300));
        delay.setOnFinished(e -> routeToTopic(topic));
        delay.play();
    }

    // ──────────────────────────────────────────────────────────────
    //  QUICK-REPLY BUTTONS  (dipanggil dari FXML dan dari kode)
    // ──────────────────────────────────────────────────────────────

    @FXML public void onMenuDanHarga()    { appendUserBubble("Menu & Harga");    showMenuDanHarga(); }
    @FXML public void onFasilitas()       { appendUserBubble("Fasilitas");       showFasilitas();    }
    @FXML public void onJamOperasional()  { appendUserBubble("Jam Operasional"); showJamOperasional(); }
    @FXML public void onLokasiKedai()     { appendUserBubble("Lokasi Kedai");    showLokasi();       }

    // ──────────────────────────────────────────────────────────────
    //  DETEKSI TOPIK
    // ──────────────────────────────────────────────────────────────

    private String detectTopic(String text) {
        if (text.matches(".*\\b(menu|harga|kopi|minum|makan|pesan|beli|ada apa|tersedia).*"))
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
            case "menu"      -> showMenuDanHarga();
            case "fasilitas" -> showFasilitas();
            case "jam"       -> showJamOperasional();
            case "lokasi"    -> showLokasi();
            default          -> showUnknown();
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  TOPIK: MENU & HARGA  →  layar "User-Chat-Menu-dan-Harga"
    // ──────────────────────────────────────────────────────────────

    private void showMenuDanHarga() {
        updateScreenTag("User-Chat-Menu-dan-Harga");
        appendBotMessage("Berikut informasi menu yang tersedia:");

        // Card utama
        VBox card = makeCard();
        card.setMaxWidth(430);

        // Tab buttons
        HBox tabs = new HBox(8);
        Button btnHitam = makeTabBtn("Kopi Hitam", true);
        Button btnSusu  = makeTabBtn("Kopi Susu",  false);
        Button btnGula  = makeTabBtn("Kopi Gula",  false);
        tabs.getChildren().addAll(btnHitam, btnSusu, btnGula);

        // Container untuk list menu (ditukar saat tab diklik)
        VBox menuList = new VBox(8);
        populateMenuList(menuList, KOPI_HITAM);

        // Aksi tab
        btnHitam.setOnAction(e -> {
            setActiveTab(tabs, btnHitam);
            populateMenuList(menuList, KOPI_HITAM);
        });
        btnSusu.setOnAction(e -> {
            setActiveTab(tabs, btnSusu);
            populateMenuList(menuList, KOPI_SUSU);
        });
        btnGula.setOnAction(e -> {
            setActiveTab(tabs, btnGula);
            populateMenuList(menuList, KOPI_GULA);
        });

        card.getChildren().addAll(tabs, menuList);
        appendBotNode(card);

        // Follow-up
        appendFollowUp("Ada yang bisa dibantu lagi?",
            new String[]{"Lihat Fasilitas", "fasilitas"},
            new String[]{"Lokasi Kedai",    "lokasi"});
    }

    private void populateMenuList(VBox container, String[][] items) {
        container.getChildren().clear();
        for (String[] item : items) {
            HBox row = new HBox(12);
            row.getStyleClass().add("menu-item-row");
            row.setAlignment(Pos.CENTER_LEFT);

            StackPane icon = new StackPane();
            icon.getStyleClass().add("image-placeholder");
            Label emoji = new Label("☕");
            emoji.setStyle("-fx-font-size:20px;");
            icon.getChildren().add(emoji);

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label name = new Label(item[0]);
            name.getStyleClass().add("menu-item-name");
            Label desc = new Label(item[1]);
            desc.getStyleClass().add("menu-item-desc");
            info.getChildren().addAll(name, desc);

            Label price = new Label(item[2]);
            price.getStyleClass().add("menu-item-price");

            row.getChildren().addAll(icon, info, price);
            container.getChildren().add(row);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  TOPIK: FASILITAS  →  layar "User-Chat-Fasilitas"
    // ──────────────────────────────────────────────────────────────

    private void showFasilitas() {
        updateScreenTag("User-Chat-Fasilitas");
        appendBotMessage("Kami menyediakan berbagai fasilitas seperti wifi dan tempat nyaman untuk bersantai:");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setMaxWidth(300);

        String[][] fac = {
            {"📶", "Free Wi-Fi"},
            {"🚪", "Private Room"},
            {"🚗", "Parkir Area"},
            {"☂",  "Outdoor"}
        };
        int col = 0, row = 0;
        for (String[] f : fac) {
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

        // Tambahkan column constraints
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(50);
        grid.getColumnConstraints().addAll(cc, new ColumnConstraints() {{ setPercentWidth(50); }});

        appendBotNode(grid);

        appendFollowUp("Ada yang bisa dibantu lagi?",
            new String[]{"Lokasi Kedai",    "lokasi"},
            new String[]{"Jam Operasional", "jam"});
    }

    // ──────────────────────────────────────────────────────────────
    //  TOPIK: JAM OPERASIONAL  →  layar "User-Chat-Jam-Operasional"
    // ──────────────────────────────────────────────────────────────

    private void showJamOperasional() {
        updateScreenTag("User-Chat-Jam-Operasional");
        appendBotMessage("Jam Operasional Kedai Kopi \"Blessed\" adalah sebagai berikut:");

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
        Label jamLabel = new Label("08:00 – 22:00");
        jamLabel.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");
        Label hariLabel = new Label("Setiap Hari");
        hariLabel.getStyleClass().add("label-muted");
        hariLabel.setStyle("-fx-font-size:13px; -fx-padding:6 0 0 0;");
        timeBox.getChildren().addAll(jamLabel, hariLabel);

        jamRow.getChildren().addAll(clock, timeBox);
        card.getChildren().addAll(header, divider, jamRow);

        appendBotNode(card);

        appendFollowUp("Ada yang bisa dibantu lagi?",
            new String[]{"Lokasi Kedai", "lokasi"});
    }

    // ──────────────────────────────────────────────────────────────
    //  TOPIK: LOKASI  →  layar "User-Chat-Lokasi"
    // ──────────────────────────────────────────────────────────────

    private void showLokasi() {
        updateScreenTag("User-Chat-Lokasi");
        appendBotMessage("Berikut lokasi Kedai Kopi Blessed:");

        VBox card = makeCard();
        card.setMaxWidth(360);
        VBox.setMargin(card, new Insets(0));

        // Alamat
        HBox alamatRow = new HBox(8);
        alamatRow.setAlignment(Pos.CENTER_LEFT);
        Label pin = new Label("📍");
        pin.setStyle("-fx-font-size:20px; -fx-text-fill:#C8956C;");
        VBox alamatInfo = new VBox(2);
        Label namaKedai = new Label("Kedai Kopi \"Blessed\"");
        namaKedai.getStyleClass().add("label-subtitle");
        namaKedai.setStyle("-fx-font-size:15px;");
        Label alamat = new Label("Jl. Anggrek No.10, Jogja");
        alamat.getStyleClass().add("label-accent");
        alamatInfo.getChildren().addAll(namaKedai, alamat);
        alamatRow.getChildren().addAll(pin, alamatInfo);

        // Map placeholder
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

        // Tombol Maps
        Button btnMaps = new Button("Buka di Google Maps");
        btnMaps.getStyleClass().add("btn-secondary");
        btnMaps.setMaxWidth(Double.MAX_VALUE);
        btnMaps.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(
                    new java.net.URI("https://maps.google.com/?q=Kedai+Kopi+Blessed+Jogja")
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(alamatRow, mapBox, btnMaps);
        appendBotNode(card);

        appendFollowUp("Ada yang bisa dibantu lagi?",
            new String[]{"Jam Operasional", "jam"},
            new String[]{"Menu & Harga",    "menu"});
    }

    // ──────────────────────────────────────────────────────────────
    //  TOPIK TIDAK DIKENALI
    // ──────────────────────────────────────────────────────────────

    private void showUnknown() {
        appendBotMessage("Maaf, saya belum memahami pertanyaan itu. "
            + "Coba tanyakan tentang Menu & Harga, Fasilitas, Jam Operasional, atau Lokasi kami ya!");
        appendFollowUp("Pilih topik:",
            new String[]{"Menu & Harga",    "menu"},
            new String[]{"Fasilitas",        "fasilitas"},
            new String[]{"Jam Operasional",  "jam"},
            new String[]{"Lokasi Kedai",     "lokasi"});
    }

    // ──────────────────────────────────────────────────────────────
    //  HELPER: APPEND BUBBLE
    // ──────────────────────────────────────────────────────────────

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

    /** Append node (card, grid, dll.) di sisi kiri seperti balasan bot */
    private void appendBotNode(Node node) {
        HBox wrap = new HBox(10);
        wrap.setAlignment(Pos.CENTER_LEFT);
        Label botIcon = new Label("🤖");
        botIcon.setStyle("-fx-font-size:22px; -fx-padding:0 0 60 0;");
        wrap.getChildren().addAll(botIcon, node);
        chatContainer.getChildren().add(wrap);
        scrollToBottom();
    }

    /**
     * Append follow-up quick-reply buttons di sisi kanan.
     * @param label   teks label di atas tombol
     * @param options pasangan {"label tombol", "topik"} — varargs
     */
    private void appendFollowUp(String label, String[]... options) {
        HBox wrap = new HBox();
        wrap.setAlignment(Pos.CENTER_RIGHT);

        VBox vbox = new VBox(8);
        vbox.setAlignment(Pos.CENTER_RIGHT);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-subtitle");
        lbl.setStyle("-fx-font-size:14px;");
        vbox.getChildren().add(lbl);

        for (String[] opt : options) {
            Button btn = new Button(opt[0]);
            btn.getStyleClass().add("quick-reply-btn");
            String topic = opt[1];
            btn.setOnAction(e -> {
                appendUserBubble(opt[0]);
                routeToTopic(topic);
            });
            vbox.getChildren().add(btn);
        }

        wrap.getChildren().add(vbox);
        chatContainer.getChildren().add(wrap);
        scrollToBottom();
    }

    // ──────────────────────────────────────────────────────────────
    //  HELPER: UI UTILITIES
    // ──────────────────────────────────────────────────────────────

    /** Update label topbar sesuai layar aktif */
    private void updateScreenTag(String screenName) {
        if (lblScreenTag != null) lblScreenTag.setText(screenName);
    }

    /** Buat VBox styled sebagai card */
    private VBox makeCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        return card;
    }

    /** Buat tab button */
    private Button makeTabBtn(String text, boolean active) {
        Button btn = new Button(text);
        btn.getStyleClass().add(active ? "tab-btn-active" : "tab-btn");
        return btn;
    }

    /** Set satu tab aktif, sisanya nonaktif */
    private void setActiveTab(HBox tabs, Button active) {
        for (javafx.scene.Node n : tabs.getChildren()) {
            if (n instanceof Button b) {
                b.getStyleClass().removeAll("tab-btn-active", "tab-btn");
                b.getStyleClass().add(b == active ? "tab-btn-active" : "tab-btn");
            }
        }
    }

    /** Scroll ke bawah setelah konten ditambahkan */
    private void scrollToBottom() {
        javafx.application.Platform.runLater(() ->
            chatScrollPane.setVvalue(1.0));
    }
}