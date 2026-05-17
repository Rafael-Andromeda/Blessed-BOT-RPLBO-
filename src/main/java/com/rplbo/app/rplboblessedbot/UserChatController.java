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

import java.io.File;
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
import java.util.Map.Entry;

public class UserChatController {

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox       chatContainer;
    @FXML private TextField  inputField;
    @FXML private Label      lblScreenTag;

    private Map<String, List<String[]>> menuByKategori = new LinkedHashMap<>();
    private List<String[]> fasilitasList = new ArrayList<>();

    private String jamBuka   = "08:00";
    private String jamTutup  = "22:00";
    private String hariOps   = "Setiap Hari";
    private String namaKedai = "Kedai Kopi Blessed";
    private String alamat    = "";
    private String mapsUrl   = "";
    private String patokan   = "";

    @FXML
    public void initialize() {
        loadAllDataFromDb();

        // Auto-scroll setiap kali tinggi chatContainer berubah (termasuk saat gambar selesai dimuat)
        chatContainer.heightProperty().addListener((obs, oldH, newH) ->
                chatScrollPane.setVvalue(1.0));

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
                    fasilitasList.add(new String[]{ rs.getString("icon"), rs.getString("nama") });
                }
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT nama_kedai, jam_buka, jam_tutup, hari_operasi, alamat, maps_url, patokan " +
                                 "FROM informasi_kedai LIMIT 1")) {
                if (rs.next()) {
                    namaKedai = rs.getString("nama_kedai");
                    jamBuka   = rs.getString("jam_buka");
                    jamTutup  = rs.getString("jam_tutup");
                    hariOps   = rs.getString("hari_operasi");
                    alamat    = rs.getString("alamat");
                    mapsUrl   = rs.getString("maps_url");
                    String dbPatokan = rs.getString("patokan");
                    patokan = (dbPatokan != null && !dbPatokan.isBlank()) ? dbPatokan : "";
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
                new String[]{ "Americano",  "Espresso + air panas",    "Rp18.000", null },
                new String[]{ "Long Black", "Espresso kuat & bold",    "Rp20.000", null },
                new String[]{ "V60",        "Pour-over filter coffee", "Rp22.000", null }
        ));
        menuByKategori.put("Kopi Susu", List.of(
                new String[]{ "Latte",      "Espresso + susu lembut",   "Rp25.000", null },
                new String[]{ "Cappuccino", "Espresso + foam tebal",    "Rp23.000", null },
                new String[]{ "Flat White", "Double shot + microfoam",  "Rp26.000", null }
        ));
        menuByKategori.put("Kopi Gula", List.of(
                new String[]{ "Kopi Gula Aren", "Espresso + gula aren", "Rp24.000", null },
                new String[]{ "Es Kopi Susu",   "Kopi susu dingin",     "Rp22.000", null },
                new String[]{ "Dalgona Coffee", "Kopi kocok creamy",    "Rp27.000", null }
        ));
        fasilitasList = List.of(
                new String[]{ "📶", "Free Wi-Fi"   },
                new String[]{ "🚪", "Private Room" },
                new String[]{ "🚗", "Parkir Area"  },
                new String[]{ "☂",  "Outdoor"      }
        );
        mapsUrl  = "https://maps.google.com/?q=Kedai+Kopi+Blessed+Jogja";
        alamat   = "Jl. Anggrek No.10, Jogja";
        patokan  = "Dekat Malioboro, 500m dari Stasiun";
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

    @FXML public void onMenuDanHarga()   { appendUserBubble("Menu & Harga");    showMenuDanHarga();     }
    @FXML public void onFasilitas()      { appendUserBubble("Fasilitas");       showFasilitas();        }
    @FXML public void onJamOperasional() { appendUserBubble("Jam Operasional"); showJamOperasional();   }
    @FXML public void onLokasiKedai()    { appendUserBubble("Lokasi Kedai");    showLokasi();           }

    @FXML
    private void onLogoutUser() {
        Navigator.goTo(inputField, "/com/rplbo/app/rplboblessedbot/Welcome.fxml");
    }

    private String detectTopic(String text) {
        // ── 1. Perbandingan dua menu harus dicek PALING AWAL ───────────────
        // Contoh yang sekarang bisa:
        // "latte sama cappuccino"
        // "bedain latte sama cappuccino"
        // "latte vs cappuccino"
        // "mending americano atau long black"
        // "kopi gula aren sama es kopi susu bedanya apa"
        List<String> menuDitemukanUntukCompare = findAllMenusInText(text);

        if (menuDitemukanUntukCompare.size() >= 2 &&
                containsAny(text,
                        "beda", "bedain", "perbedaan", "vs",
                        "dibanding", "dibandingkan", "lebih baik",
                        "mending", "mending mana", "pilih", "pilih mana",
                        "rekomendasi antara", "sama", "atau", "antara")) {

            return "bandingkan_menu:" +
                    menuDitemukanUntukCompare.get(0) + "|" +
                    menuDitemukanUntukCompare.get(1);
        }

        // ── 2. Patokan lokasi ──────────────────────────────────────────────
        if (containsAny(text, "patokan", "petunjuk arah", "ciri khas tempat",
                "dekat apa", "deket apa", "landmark", "tanda", "patokannya")) {
            return "patokan";
        }

        // ── 3. Pertanyaan spesifik tentang satu menu ───────────────────────
        // Penting: bagian ini harus SETELAH perbandingan.
        // Kalau tidak, "latte sama cappuccino" akan kebaca sebagai detail Latte.
        String menuDikenali = findMenuInText(text);
        if (menuDikenali != null) {
            boolean adaKataDetail = containsAny(text,
                    "apa", "apakah", "itu", "jelaskan", "cerita",
                    "gimana", "bagaimana", "info", "informasi", "detail",
                    "deskripsi", "kaya apa", "kayak apa", "seperti apa",
                    "rasanya", "rasa", "bahan", "isinya", "isi", "tentang");

            boolean tidakAdaTopikLain = !containsAny(text,
                    "beda", "bedain", "perbedaan", "vs", "dibanding", "dibandingkan",
                    "lebih baik", "mending", "mending mana", "pilih", "pilih mana",
                    "rekomendasi antara", "sama", "atau", "antara",
                    "harga", "semua menu", "daftar", "list");

            if (adaKataDetail || tidakAdaTopikLain) {
                return "detail_menu:" + menuDikenali;
            }
        }

        // ── 4. Kopi ringan + nugas ─────────────────────────────────────────
        if (containsAny(text, "ga terlalu pahit", "tidak terlalu pahit", "ga pahit",
                "tidak pahit", "kurang pahit", "mild", "ringan") &&
                containsAny(text, "nugas", "laptop", "kerja", "belajar")) {
            return "kopi_ringan_nugas";
        }

        // ── 5. Kopi ringan ─────────────────────────────────────────────────
        if (containsAny(text, "ga terlalu pahit", "tidak terlalu pahit", "ga pahit",
                "tidak pahit", "kurang pahit", "mild", "ringan")) {
            return "kopi_ringan";
        }

        // ── 6. Kopi manis ──────────────────────────────────────────────────
        if (containsAny(text, "manis", "gula aren", "creamy", "susu") &&
                containsAny(text, "kopi", "coffee", "minum")) {
            return "kopi_manis";
        }

        // ── 7. Sarapan ─────────────────────────────────────────────────────
        if (containsAny(text, "sarapan", "breakfast", "pagi")) {
            return "sarapan";
        }

        // ── 8. Fasilitas laptop ────────────────────────────────────────────
        if (containsAny(text, "laptop", "nugas", "kerja", "belajar",
                "colokan", "charger", "wifi", "wi-fi")) {
            return "fasilitas_laptop";
        }

        // ── 9. Topik umum ──────────────────────────────────────────────────
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

    /**
     * Kembalikan nama menu pertama yang cocok dalam teks, atau null jika tidak ada.
     * Pencocokan dilakukan secara case-insensitive pada versi lowercase teks.
     */
    private String findMenuInText(String lowerText) {
        for (List<String[]> items : menuByKategori.values()) {
            for (String[] item : items) {
                if (lowerText.contains(item[0].toLowerCase())) {
                    return item[0];
                }
            }
        }
        // Alias umum yang diketik tanpa nama lengkap
        Map<String, String> alias = Map.of(
                "gula aren",    "Kopi Gula Aren",
                "es kopi",      "Es Kopi Susu",
                "dalgona",      "Dalgona Coffee",
                "flat white",   "Flat White",
                "long black",   "Long Black",
                "roti bakar",   "Roti Bakar Coklat",
                "sandwich",     "Sandwich Telur",
                "kentang",      "Kentang Goreng",
                "pisang goreng","Pisang Goreng Crispy"
        );
        for (Map.Entry<String, String> e : alias.entrySet()) {
            if (lowerText.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    /**
     * Kembalikan semua nama menu yang muncul dalam teks (untuk perbandingan).
     */
    private List<String> findAllMenusInText(String lowerText) {
        List<String> found = new ArrayList<>();

        // Cari dari data menu yang dimuat dari database/fallback
        for (List<String[]> items : menuByKategori.values()) {
            for (String[] item : items) {
                String menuName = item[0].toLowerCase();

                if (lowerText.contains(menuName) && !found.contains(item[0])) {
                    found.add(item[0]);
                }
            }
        }

        // Alias supaya user tidak harus mengetik nama menu 100% sama
        Map<String, String> alias = Map.ofEntries(
                Map.entry("latte", "Latte"),
                Map.entry("cappuccino", "Cappuccino"),
                Map.entry("americano", "Americano"),
                Map.entry("long black", "Long Black"),
                Map.entry("v60", "V60"),
                Map.entry("flat white", "Flat White"),

                Map.entry("gula aren", "Kopi Gula Aren"),
                Map.entry("kopi gula aren", "Kopi Gula Aren"),
                Map.entry("es kopi", "Es Kopi Susu"),
                Map.entry("es kopi susu", "Es Kopi Susu"),
                Map.entry("dalgona", "Dalgona Coffee"),
                Map.entry("dalgona coffee", "Dalgona Coffee"),

                Map.entry("roti bakar", "Roti Bakar Coklat"),
                Map.entry("sandwich", "Sandwich Telur"),
                Map.entry("sandwich telur", "Sandwich Telur"),
                Map.entry("kentang", "Kentang Goreng"),
                Map.entry("kentang goreng", "Kentang Goreng"),
                Map.entry("pisang goreng", "Pisang Goreng Crispy")
        );

        for (Map.Entry<String, String> e : alias.entrySet()) {
            if (lowerText.contains(e.getKey()) && !found.contains(e.getValue())) {
                found.add(e.getValue());
            }
        }

        return found;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void routeToTopic(String topic) {
        if (topic.startsWith("detail_menu:")) {
            String namaMenu = topic.substring("detail_menu:".length());
            showDetailMenu(namaMenu);
            return;
        }
        if (topic.startsWith("bandingkan_menu:")) {
            String[] parts = topic.substring("bandingkan_menu:".length()).split("\\|");
            showBandingkanMenu(parts[0], parts.length > 1 ? parts[1] : "");
            return;
        }
        switch (topic) {
            case "patokan"                 -> showPatokan();
            case "rekomendasi"             -> showRekomendasiMenuHariIni();
            case "kopi_ringan"             -> showRekomendasiKopiRingan();
            case "kopi_ringan_nugas"       -> showRekomendasiKopiRinganUntukNugas();
            case "kopi_manis"              -> showRekomendasiKopiManis();
            case "sarapan"                 -> showRekomendasiSarapan();
            case "beda_latte_cappuccino"   -> showBandingkanMenu("Latte", "Cappuccino");
            case "fasilitas_laptop"        -> showFasilitasLaptop();
            case "menu"                    -> showMenuDanHarga();
            case "fasilitas"               -> showFasilitas();
            case "jam"                     -> showJamOperasional();
            case "lokasi"                  -> showLokasi();
            default                         -> showUnknown();
        }
    }


    private void showRekomendasiKopiRingan() {
        updateScreenTag("User-Chat-Rekomendasi-Kopi-Ringan");
        appendBotMessage("Kalau kamu cari kopi yang tidak terlalu pahit, aku rekomendasikan Latte, Kopi Gula Aren, atau Es Kopi Susu. Rasanya lebih smooth, creamy, dan lebih aman buat yang kurang suka kopi strong ☕");
        showMenuDanHarga();
    }

    private void showRekomendasiKopiRinganUntukNugas() {
        updateScreenTag("User-Chat-Rekomendasi-Kopi-Nugas");
        appendBotMessage("Buat nugas sambil minum kopi yang tidak terlalu pahit, pilihan yang cocok itu Latte, Kopi Gula Aren, atau Es Kopi Susu ☕💻");
        appendBotMessage("Kalau bawa laptop juga bisa. Tempatnya cocok buat kerja atau belajar, apalagi kalau kamu butuh WiFi dan suasana santai.");
        showFasilitas();
    }

    private void showRekomendasiKopiManis() {
        updateScreenTag("User-Chat-Rekomendasi-Kopi-Manis");
        appendBotMessage("Kalau mau kopi yang manis tapi masih terasa kopinya, aku saranin Kopi Gula Aren atau Es Kopi Susu. Kopinya tetap terasa, tapi lebih creamy dan tidak terlalu pahit ☕");
        showMenuDanHarga();
    }

    private void showRekomendasiSarapan() {
        updateScreenTag("User-Chat-Rekomendasi-Sarapan");
        appendBotMessage("Untuk sarapan, pilihan yang cocok biasanya Latte atau Cappuccino dipasangkan dengan Roti Bakar, Sandwich Telur, Pisang Goreng, atau Kentang Goreng 🍞☕");
        appendBotMessage("Kalau mau yang ringan, pilih Latte + Roti Bakar. Kalau mau agak mengenyangkan, pilih Cappuccino + Sandwich Telur.");
        showMenuDanHarga();
    }

    // ── Tampilkan detail satu menu ────────────────────────────────────────
    private void showDetailMenu(String namaMenu) {
        updateScreenTag("User-Chat-Detail-Menu");

        // Cari item menu
        String[] foundItem = null;
        String foundKategori = null;
        outer:
        for (Map.Entry<String, List<String[]>> entry : menuByKategori.entrySet()) {
            for (String[] item : entry.getValue()) {
                if (item[0].equalsIgnoreCase(namaMenu)) {
                    foundItem    = item;
                    foundKategori = entry.getKey();
                    break outer;
                }
            }
        }

        if (foundItem == null) {
            appendBotMessage("Maaf, saya tidak menemukan informasi tentang menu \"" + namaMenu + "\". " +
                    "Coba cek daftar menu lengkap kami ya!");
            showMenuDanHarga();
            return;
        }

        appendBotMessage("Berikut informasi tentang " + foundItem[0] + ":");

        VBox card = makeCard();
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color:#FFF8F2; -fx-background-radius:14;" +
                "-fx-border-color:#C8956C; -fx-border-radius:14; -fx-border-width:1.5;" +
                "-fx-padding:14 16 14 16;");

        // Badge kategori
        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label("📂  " + foundKategori);
        badge.setStyle("-fx-background-color:#E8D5C0; -fx-text-fill:#7A5C3A;" +
                "-fx-background-radius:20; -fx-padding:3 10 3 10;" +
                "-fx-font-size:11px; -fx-font-weight:bold;");
        badgeRow.getChildren().add(badge);

        // Nama + Harga
        HBox namaHargaRow = new HBox();
        namaHargaRow.setAlignment(Pos.CENTER_LEFT);
        Label lblNama = new Label("☕  " + foundItem[0]);
        lblNama.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");
        HBox.setHgrow(lblNama, Priority.ALWAYS);
        Label lblHarga = new Label(foundItem[2]);
        lblHarga.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#C8956C;");
        namaHargaRow.getChildren().addAll(lblNama, lblHarga);

        // Gambar (jika ada)
        String imgUrl = (foundItem.length > 3 && foundItem[3] != null) ? foundItem[3] : null;
        if (imgUrl != null && !imgUrl.isBlank()) {
            try {
                File imgFile = new File("images/menu/" + imgUrl);
                String imageUri = imgFile.exists() ? imgFile.toURI().toString() : imgUrl;
                ImageView iv = new ImageView(new Image(imageUri, 320, 160, true, true, true));
                iv.setFitWidth(320);
                iv.setFitHeight(160);
                iv.setStyle("-fx-background-radius:10;");
                card.getChildren().addAll(badgeRow, namaHargaRow, iv);
            } catch (Exception e) {
                card.getChildren().addAll(badgeRow, namaHargaRow);
            }
        } else {
            card.getChildren().addAll(badgeRow, namaHargaRow);
        }

        // Deskripsi
        Region divider = new Region();
        divider.setStyle("-fx-background-color:#E2C4A0; -fx-pref-height:1;");
        Label lblDesc = new Label(foundItem[1]);
        lblDesc.setStyle("-fx-font-size:13px; -fx-text-fill:#5A3E2B;");
        lblDesc.setWrapText(true);

        card.getChildren().addAll(divider, lblDesc);
        appendBotNode(card);
    }

    // ── Perbandingan dua menu (generik) ──────────────────────────────────
    private void showBandingkanMenu(String namaA, String namaB) {
        updateScreenTag("User-Chat-Perbandingan-Menu");

        String[] itemA = findItemByName(namaA);
        String[] itemB = findItemByName(namaB);

        if (itemA == null || itemB == null) {
            // Fallback teks bawaan untuk latte vs cappuccino jika DB belum siap
            if ((namaA.equalsIgnoreCase("Latte") && namaB.equalsIgnoreCase("Cappuccino")) ||
                    (namaA.equalsIgnoreCase("Cappuccino") && namaB.equalsIgnoreCase("Latte"))) {
                appendBotMessage("Latte dan Cappuccino sama-sama berbahan espresso dan susu, tapi rasanya beda. " +
                        "Latte lebih creamy dan lembut karena susunya lebih banyak. Cappuccino punya foam lebih tebal " +
                        "dan rasa kopinya biasanya lebih terasa ☕");
                appendBotMessage("Kalau kamu tidak suka terlalu pahit, pilih Latte. " +
                        "Kalau mau rasa kopi yang lebih kuat tapi tetap ada susu, pilih Cappuccino.");
            } else {
                appendBotMessage("Maaf, saya tidak bisa menemukan salah satu atau kedua menu yang ingin kamu bandingkan. " +
                        "Coba lihat daftar menu lengkap kami ya!");
                showMenuDanHarga();
            }
            return;
        }

        appendBotMessage("Yuk, bandingkan " + itemA[0] + " vs " + itemB[0] + " ☕");

        // Card perbandingan
        VBox card = makeCard();
        card.setMaxWidth(500);

        // Header
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER);
        Label lblTitle = new Label("⚖  Perbandingan Menu");
        lblTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");
        headerRow.getChildren().add(lblTitle);
        card.getChildren().add(headerRow);

        Region div0 = new Region();
        div0.setStyle("-fx-background-color:#E2C4A0; -fx-pref-height:1;");
        card.getChildren().add(div0);

        // Dua kolom: itemA | vs | itemB
        HBox cols = new HBox(10);
        cols.setAlignment(Pos.TOP_CENTER);

        for (String[] item : new String[][]{ itemA, itemB }) {
            VBox col = new VBox(6);
            col.setAlignment(Pos.TOP_CENTER);
            col.setPrefWidth(185);
            col.setStyle("-fx-background-color:#FFF8F2; -fx-background-radius:10; -fx-padding:10;");

            // Gambar atau emoji
            String imgUrl = (item.length > 3 && item[3] != null) ? item[3] : null;
            if (imgUrl != null && !imgUrl.isBlank()) {
                try {
                    File imgFile = new File("images/menu/" + imgUrl);
                    String imageUri = imgFile.exists() ? imgFile.toURI().toString() : imgUrl;
                    ImageView iv = new ImageView(new Image(imageUri, 120, 80, true, true, true));
                    iv.setFitWidth(120);
                    iv.setFitHeight(80);
                    col.getChildren().add(iv);
                } catch (Exception ex) {
                    Label emoji = new Label("☕");
                    emoji.setStyle("-fx-font-size:32px;");
                    col.getChildren().add(emoji);
                }
            } else {
                Label emoji = new Label("☕");
                emoji.setStyle("-fx-font-size:32px;");
                col.getChildren().add(emoji);
            }

            Label lblNama = new Label(item[0]);
            lblNama.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");
            lblNama.setWrapText(true);
            lblNama.setAlignment(Pos.CENTER);

            Label lblHarga = new Label(item[2]);
            lblHarga.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#C8956C;");

            Label lblDesc = new Label(item[1]);
            lblDesc.setStyle("-fx-font-size:11px; -fx-text-fill:#7A5C3A;");
            lblDesc.setWrapText(true);
            lblDesc.setAlignment(Pos.CENTER);

            col.getChildren().addAll(lblNama, lblHarga, lblDesc);
            cols.getChildren().add(col);

            if (item == itemA) {
                Label vs = new Label("VS");
                vs.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#C8956C;" +
                        "-fx-padding:30 0 0 0;");
                cols.getChildren().add(vs);
            }
        }

        card.getChildren().add(cols);

        // Saran pilihan
        Region div1 = new Region();
        div1.setStyle("-fx-background-color:#E2C4A0; -fx-pref-height:1;");
        card.getChildren().add(div1);

        Label lblSaran = new Label(buildSaranPerbandingan(itemA, itemB));
        lblSaran.setStyle("-fx-font-size:12px; -fx-text-fill:#5A3E2B; -fx-font-style:italic;");
        lblSaran.setWrapText(true);
        card.getChildren().add(lblSaran);

        appendBotNode(card);
    }

    /** Buat teks saran perbandingan otomatis berdasarkan harga & deskripsi */
    private String buildSaranPerbandingan(String[] a, String[] b) {
        int hargaA = extractHarga(a[2]);
        int hargaB = extractHarga(b[2]);

        String murah  = hargaA <= hargaB ? a[0] : b[0];
        String mahal  = hargaA <= hargaB ? b[0] : a[0];

        if (hargaA == hargaB) {
            return "💡 Harganya sama! Pilih sesuai selera — coba baca deskripsinya untuk menentukan mana yang lebih cocok buat kamu.";
        }
        return "💡 " + murah + " lebih terjangkau dibanding " + mahal + ". Keduanya memiliki cita rasa khas masing-masing — pilih sesuai mood dan selera kamu!";
    }

    private int extractHarga(String hargaStr) {
        try { return Integer.parseInt(hargaStr.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }

    /** Cari item menu berdasarkan nama (case-insensitive) */
    private String[] findItemByName(String nama) {
        for (List<String[]> items : menuByKategori.values()) {
            for (String[] item : items) {
                if (item[0].equalsIgnoreCase(nama)) return item;
            }
        }
        return null;
    }

    // ── Tampilkan patokan lokasi kedai ───────────────────────────────────
    private void showPatokan() {
        updateScreenTag("User-Chat-Patokan");

        if (patokan == null || patokan.isBlank()) {
            appendBotMessage("Maaf, informasi patokan untuk kedai kami belum tersedia saat ini. " +
                    "Kamu bisa lihat lokasi lengkapnya lewat Google Maps ya!");
            showLokasi();
            return;
        }

        appendBotMessage("Ini patokan lokasi " + namaKedai + " agar mudah ditemukan:");

        VBox card = makeCard();
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color:#FFF3E0; -fx-background-radius:14;" +
                "-fx-border-color:#C8956C; -fx-border-radius:14; -fx-border-width:1.5;" +
                "-fx-padding:14 16 14 16;");

        // Judul
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label flagIcon = new Label("🚩");
        flagIcon.setStyle("-fx-font-size:22px;");
        Label lblJudul = new Label("Patokan Lokasi");
        lblJudul.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");
        titleRow.getChildren().addAll(flagIcon, lblJudul);

        Region divider = new Region();
        divider.setStyle("-fx-background-color:#E2C4A0; -fx-pref-height:1;");

        // Isi patokan
        Label lblPatokan = new Label(patokan);
        lblPatokan.setStyle("-fx-font-size:13px; -fx-text-fill:#5A3E2B;");
        lblPatokan.setWrapText(true);

        // Alamat lengkap
        HBox alamatRow = new HBox(6);
        alamatRow.setAlignment(Pos.CENTER_LEFT);
        Label pinIcon = new Label("📍");
        pinIcon.setStyle("-fx-font-size:14px;");
        Label lblAlamat = new Label(alamat);
        lblAlamat.setStyle("-fx-font-size:11px; -fx-text-fill:#A07850;");
        lblAlamat.setWrapText(true);
        alamatRow.getChildren().addAll(pinIcon, lblAlamat);

        // Tombol Maps
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

        card.getChildren().addAll(titleRow, divider, lblPatokan, alamatRow, btnMaps);
        appendBotNode(card);
    }

    private void showFasilitasLaptop() {
        updateScreenTag("User-Chat-Fasilitas-Laptop");
        appendBotMessage("Bisa kok kalau mau bawa laptop 💻 Tempatnya cocok buat nugas, kerja, atau belajar santai. Kamu juga bisa cek fasilitas yang tersedia di bawah ini.");
        showFasilitas();
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
        List<Button> tabButtons   = new ArrayList<>();

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
                scrollToBottom();
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
                    File imgFile  = new File("images/menu/" + imgUrl);
                    String imageUri = imgFile.exists() ? imgFile.toURI().toString() : imgUrl;
                    ImageView iv  = new ImageView(new Image(imageUri, 56, 56, true, true, true));
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
            appendBotMessage("Belum ada rekomendasi menu untuk hari " + hariIni + " ini. " +
                    "Silakan tanyakan menu lainnya!");
            return;
        }

        VBox card = makeCard();
        card.setMaxWidth(370);
        card.setStyle("-fx-background-color:#FFF3E0; -fx-background-radius:14;" +
                "-fx-border-color:#C8956C; -fx-border-radius:14; -fx-border-width:1.5;" +
                "-fx-padding:14 16 14 16;");

        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label("⭐  Pilihan Hari " + hariIni);
        badge.setStyle("-fx-background-color:#C8956C; -fx-text-fill:white;" +
                "-fx-background-radius:20; -fx-padding:3 10 3 10;" +
                "-fx-font-size:11px; -fx-font-weight:bold;");
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
            Label lbl = new Label(f[1]);
            lbl.getStyleClass().add("facility-card-text");

            card.getChildren().addAll(icon, lbl);
            GridPane.setColumnIndex(card, col);
            GridPane.setRowIndex(card, row);
            grid.getChildren().add(card);

            col++;
            if (col > 1) { col = 0; row++; }
        }

        ColumnConstraints cc  = new ColumnConstraints();
        ColumnConstraints cc2 = new ColumnConstraints();
        cc.setPercentWidth(50);
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

        // Baris Alamat
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

        // Baris Patokan (dari DB, sinkron dengan admin)
        HBox patokanRow = new HBox(8);
        patokanRow.setAlignment(Pos.CENTER_LEFT);
        if (patokan != null && !patokan.isBlank()) {
            Label flagIcon = new Label("🚩");
            flagIcon.setStyle("-fx-font-size:16px;");

            VBox patokanInfo = new VBox(1);
            Label patokanTitle = new Label("Patokan");
            patokanTitle.setStyle("-fx-font-size:11px; -fx-text-fill:#A07850; -fx-font-weight:bold;");
            Label patokanLbl = new Label(patokan);
            patokanLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#3B2414;");
            patokanLbl.setWrapText(true);
            patokanInfo.getChildren().addAll(patokanTitle, patokanLbl);

            patokanRow.getChildren().addAll(flagIcon, patokanInfo);
        }

        // Map Placeholder
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

        // Tombol Google Maps
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

        // Patokan hanya ditampilkan kalau tidak kosong
        if (!patokanRow.getChildren().isEmpty()) {
            card.getChildren().addAll(alamatRow, patokanRow, mapBox, btnMaps);
        } else {
            card.getChildren().addAll(alamatRow, mapBox, btnMaps);
        }

        appendBotNode(card);
    }

    private void showUnknown() {
        appendBotMessage("Maaf, saya belum memahami pertanyaan itu. " +
                "Coba tanyakan tentang Menu & Harga, Fasilitas, Jam Operasional, atau Lokasi kami ya!");
    }

    // ── Bubble & Node Helpers ─────────────────────────────────────

    private void appendUserBubble(String text) {
        HBox wrap = new HBox();
        wrap.setAlignment(Pos.CENTER_RIGHT);
        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-bubble-user");
        bubble.setStyle("-fx-font-size:14px; -fx-text-fill:#3B2414;" +
                "-fx-background-color:#E0D5C8;" +
                "-fx-background-radius:16 2 16 16;" +
                "-fx-padding:10 14 10 14;" +
                "-fx-max-width:280px;");
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

    // ── UI Helpers ────────────────────────────────────────────────

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
                javafx.application.Platform.runLater(() ->
                        javafx.application.Platform.runLater(() ->
                                chatScrollPane.setVvalue(1.0))));
    }
}