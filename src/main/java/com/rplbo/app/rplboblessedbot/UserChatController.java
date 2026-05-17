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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserChatController {

    // ── FXML Bindings ─────────────────────────────────────────────

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox       chatContainer;
    @FXML private TextField  inputField;
    @FXML private Label      lblScreenTag;

    // ── Data dari DB ──────────────────────────────────────────────

    /** Semua menu: key = nama (lowercase), value = [nama, deskripsi, hargaStr, gambar_url, kategori] */
    private final Map<String, String[]> menuMap       = new LinkedHashMap<>();
    /** Menu dikelompokkan per kategori: key = nama kategori, value = list [nama, desk, harga, url] */
    private final Map<String, List<String[]>> menuByKategori = new LinkedHashMap<>();
    private final List<String[]> fasilitasList        = new ArrayList<>();

    private String jamBuka   = "08:00";
    private String jamTutup  = "22:00";
    private String hariOps   = "Setiap Hari";
    private String namaKedai = "Kedai Kopi Blessed";
    private String alamat    = "";
    private String mapsUrl   = "";
    private String patokan   = "";

    // ── Intent Enum ───────────────────────────────────────────────

    private enum Intent {
        REKOMENDASI_HARI,       // rekomendasi menu hari ini
        REKOMENDASI_PREFERENSI, // rekomendasiin yang manis/pahit/dingin/dll
        CARI_MENU,              // ada menu X ga? / nyari nama menu
        BANDINGKAN_MENU,        // bedain A sama B
        HARGA_MENU,             // harga X berapa?
        DAFTAR_MENU,            // lihat semua menu / menu & harga
        FASILITAS,              // tanya fasilitas umum
        FASILITAS_SPESIFIK,     // ada wifi? bisa bawa laptop?
        JAM_OPERASIONAL,        // jam buka/tutup
        LOKASI,                 // alamat / lokasi
        SAPAAN,                 // halo, hi, selamat pagi
        TERIMA_KASIH,           // makasih, thanks
        UNKNOWN                 // tidak dikenali
    }

    // ── Tag rasa/sifat untuk rekomendasi preferensi ───────────────

    private static final Map<String, List<String>> PREFERENSI_TAG = new LinkedHashMap<>();
    static {
        PREFERENSI_TAG.put("pahit",   List.of("espresso", "americano", "long black", "v60"));
        PREFERENSI_TAG.put("kuat",    List.of("espresso", "long black", "v60"));
        PREFERENSI_TAG.put("manis",   List.of("dalgona", "gula aren", "kopi susu", "latte", "cappuccino"));
        PREFERENSI_TAG.put("creamy",  List.of("latte", "cappuccino", "flat white", "dalgona"));
        PREFERENSI_TAG.put("dingin",  List.of("es kopi", "dalgona", "kopi susu", "gula aren"));
        PREFERENSI_TAG.put("panas",   List.of("v60", "americano", "espresso", "long black"));
        PREFERENSI_TAG.put("ringan",  List.of("latte", "flat white", "cappuccino", "americano"));
        PREFERENSI_TAG.put("murah",   List.of("americano", "long black", "espresso", "kopi susu"));
        PREFERENSI_TAG.put("premium", List.of("flat white", "v60", "dalgona", "cappuccino"));
        PREFERENSI_TAG.put("susu",    List.of("latte", "cappuccino", "flat white", "kopi susu"));
        PREFERENSI_TAG.put("filter",  List.of("v60", "americano", "long black"));
        PREFERENSI_TAG.put("sarapan", List.of("sandwich telur", "pisang goreng", "kentang goreng", "americano", "latte"));
        PREFERENSI_TAG.put("cemilan", List.of("pisang goreng", "kentang goreng", "robak", "sandwich"));
        PREFERENSI_TAG.put("makan",   List.of("sandwich telur", "pisang goreng", "kentang goreng", "robak"));
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    @FXML
    public void initialize() {
        loadAllDataFromDb();

        chatContainer.heightProperty().addListener((obs, oldH, newH) ->
                chatScrollPane.setVvalue(1.0));

        javafx.application.Platform.runLater(this::showRekomendasiMenuHariIni);
    }

    // ── Load data dari DB ─────────────────────────────────────────

    private void loadAllDataFromDb() {
        try {
            Connection conn = DatabaseHelper.getConnection();

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT k.nama_kategori, m.nama, m.deskripsi, m.harga, m.gambar_url " +
                         "FROM menu m JOIN kategori_menu k ON m.kategori_id = k.id " +
                         "WHERE m.tersedia = 1 ORDER BY k.urutan, m.id")) {
                while (rs.next()) {
                    String kat      = rs.getString("nama_kategori");
                    String nama     = rs.getString("nama");
                    String desk     = rs.getString("deskripsi");
                    String hargaStr = "Rp" + String.format("%,.0f", (double) rs.getInt("harga"))
                                              .replace(",", ".");
                    String gambar   = rs.getString("gambar_url");

                    menuByKategori.computeIfAbsent(kat, x -> new ArrayList<>())
                            .add(new String[]{ nama, desk, hargaStr, gambar });
                    // menuMap: key lowercase untuk pencarian fuzzy
                    menuMap.put(nama.toLowerCase(), new String[]{ nama, desk, hargaStr, gambar, kat });
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
                    namaKedai        = rs.getString("nama_kedai");
                    jamBuka          = rs.getString("jam_buka");
                    jamTutup         = rs.getString("jam_tutup");
                    hariOps          = rs.getString("hari_operasi");
                    alamat           = rs.getString("alamat");
                    mapsUrl          = rs.getString("maps_url");
                    String dbPatokan = rs.getString("patokan");
                    patokan = (dbPatokan != null && !dbPatokan.isBlank()) ? dbPatokan : "";
                }
            }

            System.out.println("✅ UserChat: data berhasil dimuat dari DB (" + menuMap.size() + " menu)");
        } catch (Exception e) {
            System.err.println("UserChat: gagal load DB — " + e.getMessage());
            loadFallbackData();
        }
    }

    private void loadFallbackData() {
        String[][] fallback = {
            { "Americano",     "Espresso + air panas",     "Rp18.000", null, "Kopi Hitam" },
            { "Long Black",    "Espresso kuat & bold",     "Rp20.000", null, "Kopi Hitam" },
            { "V60",           "Pour-over filter coffee",  "Rp22.000", null, "Kopi Hitam" },
            { "Latte",         "Espresso + susu lembut",   "Rp25.000", null, "Kopi Susu"  },
            { "Cappuccino",    "Espresso + foam tebal",    "Rp23.000", null, "Kopi Susu"  },
            { "Flat White",    "Double shot + microfoam",  "Rp26.000", null, "Kopi Susu"  },
            { "Kopi Gula Aren","Espresso + gula aren",     "Rp24.000", null, "Kopi Gula"  },
            { "Es Kopi Susu",  "Kopi susu dingin segar",   "Rp22.000", null, "Kopi Gula"  },
            { "Dalgona Coffee","Kopi kocok creamy",         "Rp27.000", null, "Kopi Gula"  },
        };
        for (String[] m : fallback) {
            menuByKategori.computeIfAbsent(m[4], x -> new ArrayList<>())
                    .add(new String[]{ m[0], m[1], m[2], m[3] });
            menuMap.put(m[0].toLowerCase(), m);
        }
        fasilitasList.addAll(List.of(
            new String[]{ "📶", "Free Wi-Fi" },
            new String[]{ "🚪", "Private Room" },
            new String[]{ "🚗", "Parkir Area" },
            new String[]{ "☂",  "Outdoor" }
        ));
        mapsUrl  = "https://maps.google.com/?q=Kedai+Kopi+Blessed+Jogja";
        alamat   = "Jl. Anggrek No.10, Jogja";
        patokan  = "Dekat Malioboro, 500m dari Stasiun";
    }

    // ═════════════════════════════════════════════════════════════
    //  INTENT DETECTION ENGINE
    // ═════════════════════════════════════════════════════════════

    /**
     * Analisis kalimat user → tentukan intent utama.
     * Urutan pengecekan dari yang paling spesifik ke paling umum.
     */
    private Intent detectIntent(String raw) {
        String t = raw.toLowerCase().trim();
        t = t.replaceAll("[?!.,]", "");

        // Sapaan
        if (t.matches("(halo|halo|hai|hi|hey|selamat (pagi|siang|sore|malam)|assalamu|salam|permisi).*"))
            return Intent.SAPAAN;

        // Terima kasih
        if (t.matches(".*(makasih|terima kasih|thanks|thx|tq|thank you).*"))
            return Intent.TERIMA_KASIH;

        // Bandingkan dua menu → "bedain A sama B", "apa bedanya A dan B", "A vs B"
        if (t.matches(".*(beda(in|nya|)|bedain|vs|versus|disbanding|dibanding|lebih (enak|manis|pahit)|perbandingan).*(sama|dan|dengan|atau|&).*") ||
            t.matches(".*(sama|dan|atau).*(beda(in|nya|)|vs|bedain).*"))
            return Intent.BANDINGKAN_MENU;

        // Harga spesifik → "harga latte berapa", "latte harganya"
        if (t.matches(".*(harga|berapa|cost|bayar).*(nya|)|.*(nya) (harga|berapa).*") &&
            findMenuInText(t) != null)
            return Intent.HARGA_MENU;

        // Cari menu spesifik → "ada latte ga", "jual v60", "punya americano"
        if ((t.matches(".*(ada|jual|punya|tersedia|ada ga|ada gak|ngejual|sedia).+") ||
             t.matches(".*(ga ada|gak ada|ga jual|tidak ada).+")) &&
            findMenuInText(t) != null)
            return Intent.CARI_MENU;

        // Rekomendasi dengan preferensi → "rekomen yang manis", "mau yang ga terlalu pahit"
        if (t.matches(".*(rekomen|saranin|suggest|pilih|pilihkan|mau (yang|kopi)|coba (yang|kopi)|" +
                        "ga terlalu|tidak terlalu|yang (manis|pahit|enak|creamy|dingin|panas|ringan|kuat|murah)|" +
                        "cocok buat|buat sarapan|buat cemilan|cocok|enaknya).*"))
            return Intent.REKOMENDASI_PREFERENSI;

        // Rekomendasi hari ini
        if (t.matches(".*(rekomendasi|rekomen|saran|pilihan hari|menu hari|hari ini|featured).*"))
            return Intent.REKOMENDASI_HARI;

        // Fasilitas spesifik → "ada wifi ga", "bisa bawa laptop", "ada colokan", "ada toilet"
        if (t.matches(".*(wifi|wi.fi|internet|colokan|stop kontak|laptop|charge|parkir|" +
                        "toilet|wc|mushola|ac|outdoor|indoor|private|smoking|rokok|hewan|anjing|kucing).*"))
            return Intent.FASILITAS_SPESIFIK;

        // Fasilitas umum
        if (t.matches(".*(fasilitas|fitur|layanan|service).*"))
            return Intent.FASILITAS;

        // Jam operasional
        if (t.matches(".*(jam|buka|tutup|operasional|waktu|kapan|berapa lama|sampai jam|dari jam).*"))
            return Intent.JAM_OPERASIONAL;

        // Lokasi
        if (t.matches(".*(lokasi|alamat|dimana|di mana|mana|tempat|maps|google maps|jalan|rute|arah).*"))
            return Intent.LOKASI;

        // Daftar menu / harga
        if (t.matches(".*(menu|harga|kopi|minum|makan|pesan|beli|ada apa|tersedia|daftar|list|" +
                        "apa aja|apa saja|semua menu|selain|lainnya|minuman|makanan).*"))
            return Intent.DAFTAR_MENU;

        return Intent.UNKNOWN;
    }

    /**
     * Cari nama menu yang disebut di dalam teks.
     * Pakai fuzzy matching: cukup substring dari nama menu.
     */
    private String[] findMenuInText(String text) {
        // Coba exact atau substring match dulu
        for (Map.Entry<String, String[]> e : menuMap.entrySet()) {
            if (text.contains(e.getKey())) return e.getValue();
        }
        // Coba per kata (min 4 huruf) dari nama menu
        for (Map.Entry<String, String[]> e : menuMap.entrySet()) {
            String[] parts = e.getKey().split("\\s+");
            for (String part : parts) {
                if (part.length() >= 4 && text.contains(part)) return e.getValue();
            }
        }
        return null;
    }

    /**
     * Cari DUA menu yang disebutkan dalam teks (untuk perbandingan).
     */
    private List<String[]> findTwoMenusInText(String text) {
        List<String[]> found = new ArrayList<>();
        Set<String> addedKeys = new HashSet<>();
        for (Map.Entry<String, String[]> e : menuMap.entrySet()) {
            if (text.contains(e.getKey()) && addedKeys.add(e.getKey())) {
                found.add(e.getValue());
                if (found.size() == 2) break;
            }
        }
        // Kalau belum 2, coba per kata
        if (found.size() < 2) {
            for (Map.Entry<String, String[]> e : menuMap.entrySet()) {
                if (addedKeys.contains(e.getKey())) continue;
                String[] parts = e.getKey().split("\\s+");
                for (String part : parts) {
                    if (part.length() >= 4 && text.contains(part) && addedKeys.add(e.getKey())) {
                        found.add(e.getValue());
                        break;
                    }
                }
                if (found.size() == 2) break;
            }
        }
        return found;
    }

    /**
     * Deteksi preferensi rasa dari teks → kembalikan daftar preferensi yang cocok.
     */
    private List<String> detectPreferensi(String text) {
        List<String> result = new ArrayList<>();
        for (String tag : PREFERENSI_TAG.keySet()) {
            if (text.contains(tag)) result.add(tag);
        }
        // Alias tambahan
        if (text.contains("tidak pahit") || text.contains("ga pahit") || text.contains("gak pahit"))
            result.add("manis");
        if (text.contains("tidak manis") || text.contains("ga manis") || text.contains("gak manis"))
            result.add("pahit");
        return result;
    }

    // ═════════════════════════════════════════════════════════════
    //  ROUTING & RESPONSE
    // ═════════════════════════════════════════════════════════════

    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.clear();

        appendUserBubble(text);

        String textLower = text.toLowerCase().replaceAll("[?!.,]", "").trim();
        Intent intent = detectIntent(textLower);

        PauseTransition delay = new PauseTransition(Duration.millis(350));
        delay.setOnFinished(e -> handleIntent(intent, textLower, text));
        delay.play();

        logChatToDb(text, intent.name());
    }

    private void handleIntent(Intent intent, String textLower, String originalText) {
        switch (intent) {
            case SAPAAN               -> showSapaan();
            case TERIMA_KASIH         -> showTerimaKasih();
            case REKOMENDASI_HARI     -> showRekomendasiMenuHariIni();
            case REKOMENDASI_PREFERENSI -> showRekomendasiPreferensi(textLower);
            case CARI_MENU            -> showCariMenu(textLower);
            case HARGA_MENU           -> showHargaMenu(textLower);
            case BANDINGKAN_MENU      -> showBandingkanMenu(textLower);
            case DAFTAR_MENU          -> showMenuDanHarga();
            case FASILITAS            -> showFasilitas();
            case FASILITAS_SPESIFIK   -> showFasilitasSpesifik(textLower);
            case JAM_OPERASIONAL      -> showJamOperasional();
            case LOKASI               -> showLokasi();
            default                   -> showUnknown(textLower);
        }
    }

    // ── Sapaan & Basa-basi ────────────────────────────────────────

    private void showSapaan() {
        String[] greetings = {
            "Halo! 👋 Selamat datang di " + namaKedai + "!\n" +
            "Saya BlessBot, asisten virtual kamu di sini. Mau tanya apa?\n" +
            "Kamu bisa tanya soal menu, harga, fasilitas, jam buka, atau lokasi kami 😊",

            "Hai! Seneng banget kamu mampir ke " + namaKedai + " ☕\n" +
            "Ada yang bisa saya bantu? Mau lihat menu, rekomendasi kopi, atau info lainnya?",

            "Selamat datang! Apa yang bisa BlessBot bantu hari ini? 🤗\n" +
            "Bisa tanya menu & harga, rekomendasi, fasilitas, jam buka, atau lokasi ya!"
        };
        appendBotMessage(greetings[new Random().nextInt(greetings.length)]);
    }

    private void showTerimaKasih() {
        String[] responses = {
            "Sama-sama! Semoga kopinya enak ya ☕ Kalau ada yang mau ditanyain lagi, BlessBot siap!",
            "Dengan senang hati! 😊 Ada yang bisa saya bantu lagi?",
            "Siap! Selamat menikmati, semoga harimu menyenangkan 🌟"
        };
        appendBotMessage(responses[new Random().nextInt(responses.length)]);
    }

    // ── Rekomendasi Preferensi ────────────────────────────────────

    private void showRekomendasiPreferensi(String text) {
        updateScreenTag("User-Chat-Rekomendasi-Preferensi");
        List<String> preferensiList = detectPreferensi(text);

        if (preferensiList.isEmpty()) {
            // Tidak ada preferensi spesifik → fallback ke rekomendasi hari ini
            appendBotMessage("Hmm, mau yang seperti apa kopinya? Biar saya kasih rekomendasi yang pas! ☕\n" +
                             "Kamu bisa bilang misalnya:\n" +
                             "• \"yang ga terlalu pahit\"\n" +
                             "• \"yang manis dan creamy\"\n" +
                             "• \"yang dingin-dingin\"\n" +
                             "• \"yang cocok buat sarapan\"");
            return;
        }

        // Kumpulkan skor menu berdasarkan preferensi yang match
        Map<String, Integer> skor = new LinkedHashMap<>();
        for (String pref : preferensiList) {
            List<String> candidates = PREFERENSI_TAG.getOrDefault(pref, List.of());
            for (String candidate : candidates) {
                // Cari menu yang namanya mengandung kata kandidat
                for (String menuKey : menuMap.keySet()) {
                    if (menuKey.contains(candidate) || candidate.contains(menuKey.split("\\s")[0])) {
                        skor.merge(menuKey, 1, Integer::sum);
                    }
                }
            }
        }

        if (skor.isEmpty()) {
            appendBotMessage("Wah, belum ada menu yang cocok banget dengan preferensimu saat ini. " +
                             "Tapi coba lihat semua menu dulu yuk — siapa tau ada yang menarik! 😊");
            showMenuDanHarga();
            return;
        }

        // Sort berdasarkan skor tertinggi
        List<Map.Entry<String, Integer>> sorted = skor.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .collect(Collectors.toList());

        String prefText = preferensiList.stream()
                .map(p -> "\"" + p + "\"")
                .collect(Collectors.joining(", "));

        appendBotMessage("Untuk yang " + prefText + ", ini rekomendasiku: ☕");

        VBox container = new VBox(10);
        container.setMaxWidth(480);

        int shown = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (shown >= 3) break;
            String[] menuData = menuMap.get(entry.getKey());
            if (menuData == null) continue;

            container.getChildren().add(buildMenuCard(menuData, shown == 0));
            shown++;
        }

        if (shown > 0) {
            appendBotNode(container);
            if (shown == 1) {
                appendBotMessage("Itu paling cocok menurutku! Mau tahu lebih detail atau lihat menu lainnya?");
            } else {
                appendBotMessage("Yang paling saya rekomendasiin adalah yang pertama! Tapi semuanya enak kok 😄");
            }
        }
    }

    // ── Cari Menu Spesifik ────────────────────────────────────────

    private void showCariMenu(String text) {
        updateScreenTag("User-Chat-Cari-Menu");
        String[] menuData = findMenuInText(text);

        boolean tanyaAda = text.contains("ada") || text.contains("jual") ||
                           text.contains("punya") || text.contains("tersedia") || text.contains("sedia");
        boolean tanyaTidakAda = text.contains("ga ada") || text.contains("gak ada") ||
                                text.contains("tidak ada") || text.contains("ga jual");

        if (menuData == null) {
            // Coba ekstrak kata yang dicari
            String[] stopwords = { "ada", "ga", "gak", "tidak", "apa", "yang", "kamu", "kalian",
                                   "jual", "punya", "tersedia", "menu", "mau", "minta" };
            String cleaned = text;
            for (String sw : stopwords) cleaned = cleaned.replace(sw, "").trim();
            cleaned = cleaned.replaceAll("\\s+", " ").trim();

            if (!cleaned.isEmpty()) {
                appendBotMessage("Hmm, saya belum nemu menu \"" + cleaned + "\" di daftar kami. 🤔\n" +
                                 "Mau lihat menu lengkap yang kami punya?");
            } else {
                appendBotMessage("Menu apa yang kamu cari? Bisa sebutin nama menunya biar saya bantu cek! 😊");
            }
            return;
        }

        String nama = menuData[0];

        if (tanyaTidakAda) {
            appendBotMessage("Ada kok! \"" + nama + "\" tersedia di menu kami 😊\n" +
                             "Ini detailnya:");
        } else if (tanyaAda) {
            appendBotMessage("Ada! \"" + nama + "\" tersedia di " + namaKedai + " ✅");
        } else {
            appendBotMessage("Ketemu! Ini info \"" + nama + "\":");
        }

        appendBotNode(buildMenuCard(menuData, true));
        appendBotMessage("Mau tahu menu lain atau ada yang ditanyain lagi?");
    }

    // ── Harga Menu Spesifik ───────────────────────────────────────

    private void showHargaMenu(String text) {
        updateScreenTag("User-Chat-Harga-Menu");
        String[] menuData = findMenuInText(text);

        if (menuData == null) {
            appendBotMessage("Menu mana yang mau kamu tahu harganya? Sebutin namanya ya, nanti saya cariin 😊");
            return;
        }

        String nama  = menuData[0];
        String harga = menuData[2];

        VBox card = makeCard();
        card.setMaxWidth(320);

        Label lblNama  = new Label("☕  " + nama);
        lblNama.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");

        Label lblHarga = new Label(harga);
        lblHarga.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:#C8956C;");

        Label lblKat = new Label("Kategori: " + (menuData.length > 4 ? menuData[4] : "-"));
        lblKat.setStyle("-fx-font-size:12px; -fx-text-fill:#8B6E5A;");

        Region div = new Region();
        div.setStyle("-fx-background-color:#E2D0B5; -fx-pref-height:1;");

        Label lblDesk = new Label(menuData[1] != null ? menuData[1] : "");
        lblDesk.setStyle("-fx-font-size:13px; -fx-text-fill:#7A5C3A;");
        lblDesk.setWrapText(true);

        card.getChildren().addAll(lblNama, lblHarga, div, lblDesk, lblKat);
        appendBotNode(card);
        appendBotMessage("Kalau mau tahu harga menu lain, tanyain aja ya! 😊");
    }

    // ── Bandingkan Dua Menu ───────────────────────────────────────

    private void showBandingkanMenu(String text) {
        updateScreenTag("User-Chat-Bandingkan-Menu");
        List<String[]> dua = findTwoMenusInText(text);

        if (dua.size() < 2) {
            if (dua.size() == 1) {
                appendBotMessage("Mau bandingin \"" + dua.get(0)[0] + "\" sama menu apa? " +
                                 "Sebutin keduanya ya, misalnya:\n\"Bedain latte sama cappuccino apa?\"");
            } else {
                appendBotMessage("Mau bandingin menu mana? Sebutin dua nama menu yang mau dibandingkan ya! 😊\n" +
                                 "Contoh: \"Bedain Latte sama Cappuccino apa?\"");
            }
            return;
        }

        String[] menuA = dua.get(0);
        String[] menuB = dua.get(1);

        appendBotMessage("Oke, kita bandingin " + menuA[0] + " vs " + menuB[0] + "! ☕");

        HBox compareBox = new HBox(12);
        compareBox.setAlignment(Pos.TOP_CENTER);
        compareBox.setMaxWidth(520);

        compareBox.getChildren().addAll(
                buildCompareCard(menuA, "#FFF8F0", "#C8956C"),
                buildVsLabel(),
                buildCompareCard(menuB, "#F0F5FF", "#5C7EC8")
        );

        appendBotNode(compareBox);

        // Narasi perbedaan
        String narasi = generateKomparasi(menuA, menuB);
        appendBotMessage(narasi);
    }

    private Node buildVsLabel() {
        Label vs = new Label("VS");
        vs.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#9E7050; -fx-padding:60 0 0 0;");
        return vs;
    }

    private VBox buildCompareCard(String[] menuData, String bgColor, String accentColor) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:" + bgColor + "; -fx-background-radius:12; " +
                      "-fx-border-color:" + accentColor + "; -fx-border-radius:12; " +
                      "-fx-border-width:1.5; -fx-padding:12; -fx-pref-width:200;");

        // Gambar (jika ada)
        String gambar = menuData[3];
        if (gambar != null && !gambar.isBlank()) {
            File f = new File("images/menu/" + gambar);
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 80, 80, true, true));
                iv.setFitWidth(80);
                iv.setFitHeight(80);
                StackPane imgBox = new StackPane(iv);
                imgBox.setAlignment(Pos.CENTER);
                card.getChildren().add(imgBox);
            }
        }

        Label lblNama = new Label(menuData[0]);
        lblNama.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#3B2414; " +
                         "-fx-wrap-text:true;");
        lblNama.setWrapText(true);

        Label lblHarga = new Label(menuData[2]);
        lblHarga.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + accentColor + ";");

        Label lblDesk = new Label(menuData[1] != null ? menuData[1] : "-");
        lblDesk.setStyle("-fx-font-size:11px; -fx-text-fill:#7A5C3A;");
        lblDesk.setWrapText(true);

        card.getChildren().addAll(lblNama, lblHarga, lblDesk);
        return card;
    }

    /**
     * Buat narasi perbandingan otomatis berdasarkan deskripsi dan harga.
     */
    private String generateKomparasi(String[] a, String[] b) {
        StringBuilder sb = new StringBuilder();

        // Perbandingan harga
        int hargaA = parseHarga(a[2]);
        int hargaB = parseHarga(b[2]);
        if (hargaA != hargaB) {
            String lebihMurah = hargaA < hargaB ? a[0] : b[0];
            String selisih = "Rp" + String.format("%,.0f", (double) Math.abs(hargaA - hargaB)).replace(",", ".");
            sb.append("💰 Harga: ").append(lebihMurah).append(" lebih murah selisih ").append(selisih).append(".\n\n");
        } else {
            sb.append("💰 Harga keduanya sama!\n\n");
        }

        // Narasi deskripsi
        sb.append("☕ ").append(a[0]).append(": ")
          .append(a[1] != null ? a[1] : "menu andalan kami").append(".\n");
        sb.append("☕ ").append(b[0]).append(": ")
          .append(b[1] != null ? b[1] : "menu andalan kami").append(".\n\n");

        sb.append("Kalau suka yang lebih klasik dan bold, coba ").append(a[0])
          .append(". Kalau mau yang sedikit berbeda, ").append(b[0]).append(" bisa jadi pilihan! 😊");

        return sb.toString();
    }

    private int parseHarga(String hargaStr) {
        try {
            return Integer.parseInt(hargaStr.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Fasilitas Spesifik ────────────────────────────────────────

    private void showFasilitasSpesifik(String text) {
        updateScreenTag("User-Chat-Fasilitas-Spesifik");

        // Database jawaban fasilitas spesifik
        if (text.matches(".*(wifi|wi.fi|internet|online).*")) {
            boolean adaWifi = fasilitasList.stream()
                    .anyMatch(f -> f[1].toLowerCase().contains("wi-fi") || f[1].toLowerCase().contains("wifi"));
            if (adaWifi) {
                appendBotMessage("Ada! 📶 Kamu bisa akses Free Wi-Fi di " + namaKedai + " tanpa biaya tambahan. " +
                                 "Cocok banget buat yang mau kerja atau belajar sambil ngopi!");
            } else {
                appendBotMessage("Maaf, untuk info Wi-Fi silakan konfirmasi langsung ke kedai ya. " +
                                 "Nanti aku tanyain dulu ke admin 🙏");
            }

        } else if (text.matches(".*(laptop|kerja|belajar|colokan|stop.kontak|charge|cas).*")) {
            boolean adaPrivate = fasilitasList.stream()
                    .anyMatch(f -> f[1].toLowerCase().contains("private") || f[1].toLowerCase().contains("room"));
            appendBotMessage("Boleh banget bawa laptop! 💻\n" +
                             (adaPrivate ? "Kami juga punya Private Room yang cocok untuk fokus kerja/belajar. " : "") +
                             "Tersedia juga Wi-Fi gratis. " +
                             "Untuk colokan/stop kontak, ada di beberapa titik di dalam kedai.");

        } else if (text.matches(".*(parkir|motor|mobil|kendaraan).*")) {
            boolean adaParkir = fasilitasList.stream()
                    .anyMatch(f -> f[1].toLowerCase().contains("parkir"));
            appendBotMessage(adaParkir
                    ? "Tersedia area parkir untuk motor dan mobil 🚗\nParkirnya luas dan aman kok!"
                    : "Untuk info parkir, silakan konfirmasi ke kedai langsung ya 🙏");

        } else if (text.matches(".*(toilet|wc|kamar mandi|wc|rest room).*")) {
            appendBotMessage("Ada toilet yang tersedia di dalam kedai 🚻\nBersih dan nyaman!");

        } else if (text.matches(".*(mushola|sholat|ibadah).*")) {
            appendBotMessage("Ada area mushola untuk sholat 🕌\nSilakan tanya ke staf untuk lokasinya ya!");

        } else if (text.matches(".*(outdoor|luar|terbuka|taman|open.air).*")) {
            boolean adaOutdoor = fasilitasList.stream()
                    .anyMatch(f -> f[1].toLowerCase().contains("outdoor"));
            appendBotMessage(adaOutdoor
                    ? "Ada area outdoor yang asik buat ngopi sambil nikmatin suasana! ☂\nCocok banget pas cuaca cerah."
                    : "Untuk info area outdoor, cek langsung ke kedai ya! 😊");

        } else if (text.matches(".*(indoor|dalam|ber.ac|ac|dingin|sejuk).*")) {
            appendBotMessage("Ada area indoor yang ber-AC, nyaman dan adem! ❄️\nCocok buat yang ga tahan panas.");

        } else if (text.matches(".*(private|vip|khusus|ruangan).*")) {
            boolean adaPrivate = fasilitasList.stream()
                    .anyMatch(f -> f[1].toLowerCase().contains("private"));
            appendBotMessage(adaPrivate
                    ? "Ada Private Room tersedia! 🚪\nCocok untuk meeting kecil atau kerja yang butuh fokus.\nUntuk booking, hubungi kami langsung ya."
                    : "Untuk info ruangan private, silakan hubungi kami langsung ya 🙏");

        } else if (text.matches(".*(rokok|smoking|smoke).*")) {
            appendBotMessage("Untuk area merokok, silakan tanya langsung ke staf ya — " +
                             "bisa jadi ada area khusus outdoor untuk perokok. 🚬");

        } else if (text.matches(".*(hewan|anjing|kucing|pet|binatang).*")) {
            appendBotMessage("Untuk kebijakan membawa hewan peliharaan, sebaiknya konfirmasi dulu ke kedai langsung ya! " +
                             "Biasanya diperbolehkan di area outdoor 🐾");

        } else {
            // Tidak ketemu keyword spesifik → tampilkan semua fasilitas
            appendBotMessage("Ini fasilitas yang ada di " + namaKedai + ":");
            showFasilitas();
        }
    }

    // ── Unknown dengan Smart Suggest ─────────────────────────────

    private void showUnknown(String text) {
        // Coba cek apakah ada nama menu di kalimat ini
        String[] menuData = findMenuInText(text);
        if (menuData != null) {
            appendBotMessage("Hmm, saya rasa kamu nanya soal \"" + menuData[0] + "\"? Ini infonya:");
            appendBotNode(buildMenuCard(menuData, false));
            return;
        }

        // Tidak ketemu sama sekali
        appendBotMessage("Maaf, saya belum ngerti maksud kamu 😅\n\n" +
                         "Coba tanyakan tentang:\n" +
                         "• 📋 Menu & Harga  — \"Tampilkan semua menu\"\n" +
                         "• ☕ Cari menu     — \"Ada Latte ga?\"\n" +
                         "• 💡 Rekomendasi   — \"Rekomendasiin kopi yang ga pahit\"\n" +
                         "• ⚖️ Bandingkan    — \"Bedain Latte sama Cappuccino\"\n" +
                         "• 📶 Fasilitas     — \"Ada wifi ga?\"\n" +
                         "• 🕐 Jam buka      — \"Buka sampai jam berapa?\"\n" +
                         "• 📍 Lokasi        — \"Dimana lokasi kedainya?\"");
    }

    // ── Quick Button Handlers ─────────────────────────────────────

    @FXML public void onMenuDanHarga()   { appendUserBubble("Menu & Harga");     showMenuDanHarga();           }
    @FXML public void onFasilitas()      { appendUserBubble("Fasilitas");        showFasilitas();              }
    @FXML public void onJamOperasional() { appendUserBubble("Jam Operasional");  showJamOperasional();         }
    @FXML public void onLokasiKedai()    { appendUserBubble("Lokasi Kedai");     showLokasi();                 }
    @FXML public void onRekomendasiMenu(){ appendUserBubble("Rekomendasi Menu"); showRekomendasiMenuHariIni(); }

    @FXML
    private void onLogoutUser() {
        Navigator.goTo(inputField, "/com/rplbo/app/rplboblessedbot/Welcome.fxml");
    }

    // ═════════════════════════════════════════════════════════════
    //  SHOW METHODS (UI Response)
    // ═════════════════════════════════════════════════════════════

    private void showMenuDanHarga() {
        updateScreenTag("User-Chat-Menu-dan-Harga");
        appendBotMessage("Berikut semua menu yang tersedia di " + namaKedai + ":");

        VBox card = makeCard();
        card.setMaxWidth(520);

        HBox tabs     = new HBox(4);
        tabs.setMaxWidth(430);
        VBox menuList = new VBox(8);

        List<String>  kategoriList = new ArrayList<>(menuByKategori.keySet());
        List<Button>  tabButtons   = new ArrayList<>();

        for (int i = 0; i < kategoriList.size(); i++) {
            Button btn = makeTabBtn(kategoriList.get(i), i == 0);
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
        appendBotMessage("Mau tahu detail atau rekomendasi dari menu di atas? Tanyain aja! 😊");
    }

    private void populateMenuList(VBox container, List<String[]> items) {
        container.getChildren().clear();
        if (items == null) return;

        for (String[] item : items) {
            HBox row = new HBox(12);
            row.getStyleClass().add("menu-item-row");
            row.setAlignment(Pos.CENTER_LEFT);

            StackPane icon = new StackPane();
            icon.getStyleClass().add("image-placeholder");
            icon.setPrefSize(56, 56);
            icon.setMinSize(56, 56);
            icon.setMaxSize(56, 56);

            String imgUrl = (item.length > 3 && item[3] != null) ? item[3] : null;
            if (imgUrl != null && !imgUrl.isBlank()) {
                try {
                    File imgFile    = new File("images/menu/" + imgUrl);
                    String imageUri = imgFile.exists() ? imgFile.toURI().toString() : imgUrl;
                    ImageView iv    = new ImageView(new Image(imageUri, 56, 56, true, true, true));
                    iv.setFitWidth(56);
                    iv.setFitHeight(56);
                    icon.getChildren().add(iv);
                } catch (Exception e) {
                    icon.getChildren().add(emojiLabel("☕"));
                }
            } else {
                icon.getChildren().add(emojiLabel("☕"));
            }

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label name = new Label(item[0]);
            name.getStyleClass().add("menu-item-name");
            Label desc = new Label(item[1] != null ? item[1] : "");
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

        String namaMenu = null, deskripsi = null, catatan = null, hargaStr = null, gambar = null;
        try {
            Connection conn = DatabaseHelper.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT m.nama, m.deskripsi, m.harga, m.gambar_url, r.catatan " +
                    "FROM rekomendasi_menu r " +
                    "JOIN menu m ON r.menu_id = m.id " +
                    "WHERE r.hari = ?")) {
                ps.setString(1, hariIni);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    namaMenu  = rs.getString("nama");
                    deskripsi = rs.getString("deskripsi");
                    catatan   = rs.getString("catatan");
                    gambar    = rs.getString("gambar_url");
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
                             "Silakan tanyakan menu lainnya atau lihat daftar menu lengkap!");
            return;
        }

        VBox card = makeCard();
        card.setMaxWidth(380);
        card.setStyle("-fx-background-color:#FFF3E0; -fx-background-radius:14;" +
                      "-fx-border-color:#C8956C; -fx-border-radius:14; -fx-border-width:1.5;" +
                      "-fx-padding:14 16 14 16;");

        // Badge hari
        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label("⭐  Pilihan Hari " + hariIni);
        badge.setStyle("-fx-background-color:#C8956C; -fx-text-fill:white;" +
                       "-fx-background-radius:20; -fx-padding:3 10 3 10;" +
                       "-fx-font-size:11px; -fx-font-weight:bold;");
        badgeRow.getChildren().add(badge);

        // Gambar (jika ada)
        if (gambar != null && !gambar.isBlank()) {
            File f = new File("images/menu/" + gambar);
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 120, 120, true, true));
                iv.setFitWidth(120);
                iv.setFitHeight(120);
                StackPane imgBox = new StackPane(iv);
                imgBox.setAlignment(Pos.CENTER);
                card.getChildren().add(imgBox);
            }
        }

        // Nama & Harga
        HBox namaHargaRow = new HBox();
        namaHargaRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(namaHargaRow, Priority.ALWAYS);

        Label lblNama = new Label("☕  " + namaMenu);
        lblNama.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");
        HBox.setHgrow(lblNama, Priority.ALWAYS);

        Label lblHarga = new Label(hargaStr);
        lblHarga.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#C8956C;");
        namaHargaRow.getChildren().addAll(lblNama, lblHarga);

        // Deskripsi
        Label lblDesc = new Label(deskripsi != null ? deskripsi : "");
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size:13px; -fx-text-fill:#7A5C3A;");

        card.getChildren().addAll(badgeRow, namaHargaRow, lblDesc);

        // Catatan dari admin
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
        appendBotMessage("Mau tahu info lain tentang " + namaMenu + " atau menu lainnya? Tanyain aja! 😊");
    }

    private void showFasilitas() {
        updateScreenTag("User-Chat-Fasilitas");
        appendBotMessage("Fasilitas yang tersedia di " + namaKedai + ":");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setMaxWidth(300);

        ColumnConstraints cc  = new ColumnConstraints();
        ColumnConstraints cc2 = new ColumnConstraints();
        cc.setPercentWidth(50);
        cc2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(cc, cc2);

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

        appendBotNode(grid);
        appendBotMessage("Ada pertanyaan lebih lanjut soal fasilitas? Misalnya \"Ada colokan ga?\" atau \"Bisa bawa laptop?\" 😊");
    }

    private void showJamOperasional() {
        updateScreenTag("User-Chat-Jam-Operasional");
        appendBotMessage("Jam Operasional " + namaKedai + ":");

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
        appendBotMessage("Kalau ada info lain yang kamu butuhkan, tanyain aja ya! 😊");
    }

    private void showLokasi() {
        updateScreenTag("User-Chat-Lokasi");
        appendBotMessage("Ini lokasi " + namaKedai + ":");

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

        // Baris Patokan
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

        if (!patokanRow.getChildren().isEmpty()) {
            card.getChildren().addAll(alamatRow, patokanRow, mapBox, btnMaps);
        } else {
            card.getChildren().addAll(alamatRow, mapBox, btnMaps);
        }

        appendBotNode(card);
    }

    // ═════════════════════════════════════════════════════════════
    //  UI BUILDERS
    // ═════════════════════════════════════════════════════════════

    /**
     * Build kartu menu satu item.
     * @param menuData [nama, deskripsi, hargaStr, gambar_url, (kategori)]
     * @param highlight true = tampilkan dengan highlight border emas
     */
    private VBox buildMenuCard(String[] menuData, boolean highlight) {
        VBox card = new VBox(6);
        card.setMaxWidth(400);

        String border = highlight ? "#C8956C" : "#E2D0B5";
        String bg     = highlight ? "#FFF8F0" : "#FAFAFA";
        card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12; " +
                      "-fx-border-color:" + border + "; -fx-border-radius:12; " +
                      "-fx-border-width:1.5; -fx-padding:12;");

        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);

        // Gambar atau emoji fallback
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(64, 64);
        imgBox.setMinSize(64, 64);
        imgBox.setMaxSize(64, 64);
        imgBox.setStyle("-fx-background-radius:8; -fx-background-color:#F0E8DC;");

        String gambar = (menuData.length > 3) ? menuData[3] : null;
        if (gambar != null && !gambar.isBlank()) {
            File f = new File("images/menu/" + gambar);
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 64, 64, true, true));
                iv.setFitWidth(64);
                iv.setFitHeight(64);
                imgBox.getChildren().add(iv);
            } else {
                imgBox.getChildren().add(emojiLabel("☕"));
            }
        } else {
            imgBox.getChildren().add(emojiLabel("☕"));
        }

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblNama = new Label(menuData[0]);
        lblNama.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#3B2414;");

        Label lblHarga = new Label(menuData[2]);
        lblHarga.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#C8956C;");

        Label lblDesk = new Label(menuData[1] != null ? menuData[1] : "");
        lblDesk.setStyle("-fx-font-size:12px; -fx-text-fill:#7A5C3A;");
        lblDesk.setWrapText(true);

        info.getChildren().addAll(lblNama, lblHarga, lblDesk);

        if (menuData.length > 4 && menuData[4] != null) {
            Label lblKat = new Label(menuData[4]);
            lblKat.setStyle("-fx-font-size:10px; -fx-text-fill:#FFFFFF; -fx-background-color:#9B7E67; " +
                            "-fx-background-radius:10; -fx-padding:1 6 1 6;");
            info.getChildren().add(lblKat);
        }

        content.getChildren().addAll(imgBox, info);
        card.getChildren().add(content);
        return card;
    }

    private Label emojiLabel(String emoji) {
        Label lbl = new Label(emoji);
        lbl.setStyle("-fx-font-size:24px;");
        return lbl;
    }

    // ── Chat Bubble Helpers ───────────────────────────────────────

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
        bubble.setMaxWidth(360);
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

    // ── General UI Helpers ────────────────────────────────────────

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

    // ── Log Chat ke DB ────────────────────────────────────────────

    private void logChatToDb(String pesan, String intent) {
        try {
            Connection conn = DatabaseHelper.getConnection();
            conn.prepareStatement(
                    "INSERT INTO chat_log (pesan_user, topik, balasan_bot) VALUES ('" +
                    pesan.replace("'", "''") + "','" + intent + "','Bot membalas intent: " + intent + "')")
                    .executeUpdate();
        } catch (Exception e) {
            System.err.println("Gagal log chat: " + e.getMessage());
        }
    }
}
