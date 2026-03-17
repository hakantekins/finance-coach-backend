package com.financecoach.service.scraper;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * FMCG (Hızlı Tüketim Ürünleri) Filtresi — Whitelist ağırlıklı.
 *
 * Strateji:
 * 1. Blacklist kontrolü — kesinlikle istemediğimiz ürünler (hızlı eleme)
 * 2. Whitelist kontrolü — ürün adında FMCG anahtar kelimesi var mı
 * 3. Kategori kontrolü — izin verilen kategori mi
 *
 * Üçünden en az biri geçmezse ürün reddedilir.
 */
public class FmcgFilter {

    // ─── KESİNLİKLE REDDET (blacklist) ───────────────────────────────────────

    private static final List<String> BLACKLIST = List.of(
            // Elektronik
            "televizyon", "tv ", " tv", "smart tv", "led tv", "oled", "qled",
            "telefon", "iphone", "samsung galaxy", "xiaomi", "tablet", "ipad",
            "bilgisayar", "laptop", "notebook", "monitor", "ekran",
            "kulaklık", "hoparlör", "bluetooth", "powerbank", "earbuds", "tws",
            "playstation", "xbox", "konsol", "joystick", "gamepad",
            "kamera", "drone", "akıllı saat", "smartwatch", "fitness",
            "şarj", "adaptör", "hdmi", "usb", "flash bellek", "hard disk", "ssd",
            "yazıcı", "tarayıcı", "modem", "router", "sim kart",
            // Beyaz Eşya & Küçük Ev Aletleri
            "buzdolabı", "çamaşır makinesi", "bulaşık makinesi",
            "fırın", "mikrodalga", "klima", "aspiratör", "davlumbaz",
            "ankastre", "termosifon", "kombi", "kurutma makinesi",
            "süpürge", "robot süpürge", "cyclone", "fantom",
            "ütü", "tost makinesi", "waffle", "airfryer", "fritöz",
            "blender", "mutfak robotu", "mikser",
            "saç kurutma", "düzleştirici", "epilatör", "tıraş makinesi",
            "dikiş makinesi", "singer", "delonghi", "philips",
            "espresso", "kahve makinesi", "çay makinesi", "kettle",
            "ekmek yapma", "buharlı temizleyici",
            "manikür", "masaj aleti", "tartı", "baskül",
            // Mobilya & Ev Eşyası
            "masa", "sandalye", "koltuk", "yatak", "çekyat", "baza", "şilte",
            "dolap", "gardırop", "raf ", "sehpa", "tv ünitesi", "mobilya",
            "berjer", "puf ", "tabure",
            // Giyim & Ayakkabı
            "tişört", "pantolon", "gömlek", "elbise", "ceket", "mont", "kaban",
            "ayakkabı", "terlik", "bot ", "çizme", "sandalet", "sneaker",
            "birkenstock", "crocs", "adidas", "nike", "puma", "skechers",
            "iç çamaşırı", "pijama", "eşofman", "şort", "tayt", "etek",
            // Hırdavat & Yapı Market
            "matkap", "vidalama", "testere", "çekiç", "pense", "tornavida",
            "boya", "vernik", "dübel", "vida", "somun", "cıvata",
            "kompresör", "jeneratör", "kaynak", "merdiven", "el arabası",
            // Bahçe & Outdoor
            "çim biçme", "bahçe", "sulama", "tırmık", "çit",
            "barbekü", "mangal", "şemsiye", "hamak", "çadır", "uyku tulumu",
            // Otomotiv
            "motosiklet", "moped", "bisiklet", "scooter", "elektrikli araç",
            "lastik", "akü", "motor yağı", "oto ",
            // Ev Tekstili & Dekorasyon
            "perde", "halı", "kilim", "nevresim", "pike", "yastık", "yorgan",
            "battaniye", "ayna", "tablo", "vazo", "çerçeve",
            // Oyuncak & Hobi & Müzik
            "oyuncak", "lego", "puzzle", "hot wheels", "barbie",
            "piyano", "gitar", "org ", "davul",
            // Mutfak Eşyası (yeniden kullanılabilir, FMCG değil)
            "tencere", "tava", "çaydanlık", "cezve", "bıçak seti",
            "tabak", "bardak", "kase seti", "çatal kaşık",
            "stanley", "termos", "matara", "suluk", "mug",
            "saklama kabı set", "erzak kabı", "düzenleyici", "organizer",
            "kesme tahtası", "pano", "tepsi", "fırın tepsisi",
            "süzgeç", "kevgir", "spatula", "doğrayıcı",
            // Spor
            "dambıl", "koşu bandı", "pilates", "yoga matı",
            // Diğer
            "valiz", "çanta", "sırt çantası", "cüzdan",
            "altın", "gram altın", "çeyrek altın",
            "hediye çeki", "indirim kuponu",
            "kitap", "dergi", "roman",
            "çakmak", "tütün",
            "bebek arabası", "mama sandalye", "oto koltuğu",
            "güneş kremi", "bronzlaştırıcı"
    );

    // ─── KESİNLİKLE KABUL ET (whitelist — ürün adında bunlar varsa FMCG) ────

    private static final List<String> WHITELIST = List.of(
            // Süt & Süt Ürünleri
            "süt", "peynir", "yoğurt", "tereyağ", "ayran", "kefir", "krema", "kaymak",
            "lor ", "çökelek", "labne",
            // Et & Tavuk & Şarküteri
            "kıyma", "kuşbaşı", "pirzola", "biftek", "antrikot",
            "tavuk", "hindi", "but ", "kanat", "göğüs",
            "sucuk", "salam", "sosis", "pastırma", "jambon", "füme",
            "balık", "somon", "levrek", "çipura", "hamsi", "karides",
            // Yumurta
            "yumurta",
            // Ekmek & Unlu Mamul
            "ekmek", "tost ekmeği", "bazlama", "lavaş", "pide",
            "simit", "poğaça", "börek", "kruvasan",
            // Temel Gıda
            "un ", "şeker", "tuz", "pirinç", "bulgur", "makarna", "erişte",
            "salça", "domates salçası", "biber salçası",
            "sıvı yağ", "ayçiçek yağı", "zeytinyağı", "mısırözü yağı",
            "nohut", "mercimek", "fasulye", "barbunya", "bezelye",
            "sirke", "limon sosu",
            // Kahvaltılık
            "zeytin", "bal ", "reçel", "pekmez", "tahin",
            "nutella", "fıstık ezmesi", "çikolata krema",
            // İçecek
            "su ", "maden suyu", "soda", "ayran",
            "çay", "kahve", "nescafe", "türk kahvesi", "filtre kahve",
            "meyve suyu", "şerbet", "limonata",
            "kola", "gazoz", "enerji içeceği",
            // Meyve & Sebze
            "domates", "biber", "patlıcan", "kabak", "salatalık", "marul",
            "patates", "soğan", "sarımsak", "havuç", "brokoli", "ıspanak",
            "elma", "portakal", "mandalina", "muz", "üzüm", "çilek",
            "limon", "karpuz", "kavun", "armut", "kayısı", "şeftali", "kiraz",
            "avokado", "nar ", "kivi",
            // Atıştırmalık
            "cips", "kraker", "bisküvi", "çikolata", "gofret",
            "kuruyemiş", "fındık", "fıstık", "badem", "ceviz",
            "kek ", "muffin", "kurabiye",
            // Dondurulmuş
            "dondurma", "donuk", "dondurulmuş",
            // Temizlik
            "deterjan", "yumuşatıcı", "çamaşır suyu", "tuz ruhu",
            "bulaşık deterjanı", "elde yıkama", "makine deterjanı",
            "yüzey temizleyici", "cam temizleyici", "banyo temizleyici",
            "çöp poşeti", "çöp torbası", "eldiven",
            // Kağıt Ürünleri
            "tuvalet kağıdı", "kağıt havlu", "peçete", "mendil",
            "ıslak mendil", "kağıt", "streç film", "folyo", "bulaşık süngeri",
            // Kişisel Bakım
            "şampuan", "saç kremi", "duş jeli", "sabun", "sıvı sabun",
            "diş macunu", "diş fırçası", "ağız bakım",
            "deodorant", "roll-on", "parfüm",
            "pamuk", "kulak çubuğu", "jilet",
            "ped ", "tampon", "günlük ped",
            // Bebek
            "bebek bezi", "bebek maması", "biberon", "emzik",
            "bebek şampuanı", "bebek kremi",
            // Evcil Hayvan
            "kedi maması", "köpek maması", "kedi kumu",
            // Hazır Gıda
            "konserve", "ton balığı", "hazır çorba", "noodle",
            "ketçap", "mayonez", "hardal",
            "baharat", "karabiber", "kırmızı biber", "kimyon", "kekik",
            "vanilya", "kabartma tozu", "maya"
    );

    // ─── İZİN VERİLEN KATEGORİLER ───────────────────────────────────────────

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "süt ürünleri", "süt", "süt ve süt ürünleri",
            "et & tavuk", "et ve tavuk", "et ve tavuk ve şarküteri",
            "temel gıda", "yemeklik malzemeler", "bakliyat",
            "kahvaltılık",
            "ekmek", "ekmek & pastane", "ekmek ve pastane",
            "meyve & sebze", "meyve ve sebze",
            "içecekler", "içecek",
            "temizlik",
            "kağıt ürünleri", "kağıt ürünler",
            "kişisel bakım", "kişisel bakım ve kozmetik",
            "anne & bebek", "anne-bebek ve çocuk",
            "dondurulmuş", "dondurulmuş ürünler", "dondurulmuş gıda",
            "atıştırmalıklar", "atıştırmalık",
            "evcil dostlar", "evcil hayvan",
            "aktüel"
    );

    private static final Pattern BLACKLIST_PATTERN;
    private static final Pattern WHITELIST_PATTERN;

    static {
        String blackJoined = String.join("|", BLACKLIST.stream()
                .map(s -> Pattern.quote(s.trim()))
                .toList());
        BLACKLIST_PATTERN = Pattern.compile("(?iu)(" + blackJoined + ")");

        String whiteJoined = String.join("|", WHITELIST.stream()
                .map(s -> Pattern.quote(s.trim()))
                .toList());
        WHITELIST_PATTERN = Pattern.compile("(?iu)(" + whiteJoined + ")");
    }

    // ─── PUBLIC API ──────────────────────────────────────────────────────────

    /**
     * Bu ürün FMCG mı?
     *
     * Mantık:
     * 1. Blacklist'te varsa → KESİN RED
     * 2. Whitelist'te varsa → KESİN KABUL
     * 3. Kategori izin listesindeyse → KABUL
     * 4. Hiçbiri değilse → RED
     */
    public static boolean isFmcg(String productName, String category) {
        if (productName == null || productName.isBlank()) return false;

        String lowerName = productName.toLowerCase();

        // 1. Blacklist — kesin red
        if (BLACKLIST_PATTERN.matcher(lowerName).find()) {
            return false;
        }

        // 2. Whitelist — kesin kabul
        if (WHITELIST_PATTERN.matcher(lowerName).find()) {
            return true;
        }

        // 3. Kategori kontrolü
        if (category != null && !category.isBlank()) {
            String lowerCategory = category.toLowerCase().trim();
            if (ALLOWED_CATEGORIES.contains(lowerCategory)) {
                return true;
            }
        }

        // 4. Hiçbirine uymuyorsa reddet
        return false;
    }
}