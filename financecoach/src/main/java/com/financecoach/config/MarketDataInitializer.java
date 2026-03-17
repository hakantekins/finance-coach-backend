package com.financecoach.config;

import com.financecoach.model.entity.MarketPrice;
import com.financecoach.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataInitializer implements CommandLineRunner {

    private final MarketPriceRepository marketPriceRepository;

    private static final LocalDate PRICE_DATE = LocalDate.of(2026, 3, 14);

    @Override
    public void run(String... args) {
        if (marketPriceRepository.count() > 0) {
            log.info("Market fiyatları zaten mevcut ({} kayıt), seed data atlanıyor.",
                    marketPriceRepository.count());
            return;
        }

        log.info("Market fiyatı seed data'sı yükleniyor (genişletilmiş v2)...");
        List<MarketPrice> prices = buildPriceList();
        marketPriceRepository.saveAll(prices);
        log.info("{} adet market fiyatı başarıyla yüklendi.", prices.size());
    }

    private List<MarketPrice> buildPriceList() {
        List<MarketPrice> list = new ArrayList<>();

        // ─── SÜT ÜRÜNLERİ ──────────────────────────────────────────
        //                                    BİM      A101     ŞOK      Migros
        addProduct(list, "Tam Yağlı Süt",     "1L",   "Süt Ürünleri", "34.90", "36.75", "35.50", "42.90");
        addProduct(list, "Yarım Yağlı Süt",   "1L",   "Süt Ürünleri", "32.50", "33.50", "33.00", "39.90");
        addProduct(list, "Günlük Süt",         "1L",   "Süt Ürünleri", "29.90", "31.50", "30.50", "36.90");
        addProduct(list, "Beyaz Peynir",       "1kg",  "Süt Ürünleri", "189.90", "199.00", "194.50", "229.90");
        addProduct(list, "Kaşar Peyniri",      "500g", "Süt Ürünleri", "149.90", "159.00", "154.50", "189.90");
        addProduct(list, "Taze Kaşar",         "500g", "Süt Ürünleri", "134.50", "139.00", "137.00", "169.90");
        addProduct(list, "Tereyağı",           "500g", "Süt Ürünleri", "109.90", "115.00", "112.50", "139.90");
        addProduct(list, "Yoğurt",             "1kg",  "Süt Ürünleri", "54.90", "59.00", "56.50", "69.90");
        addProduct(list, "Süzme Yoğurt",       "1kg",  "Süt Ürünleri", "79.90", "84.00", "82.00", "99.90");
        addProduct(list, "Ayran",              "1L",   "Süt Ürünleri", "24.90", "26.50", "25.50", "32.90");
        addProduct(list, "Krema",              "200ml","Süt Ürünleri", "34.90", "37.00", "36.00", "44.90");
        addProduct(list, "Labne Peynir",       "200g", "Süt Ürünleri", "39.90", "42.00", "41.00", "52.90");
        addProduct(list, "Tulum Peyniri",      "500g", "Süt Ürünleri", "169.90", "179.00", "174.50", "214.90");
        addProduct(list, "Lor Peyniri",        "500g", "Süt Ürünleri", "59.90", "64.00", "62.00", "79.90");

        // ─── ET & TAVUK ─────────────────────────────────────────────
        addProduct(list, "Dana Kıyma",         "1kg",  "Et & Tavuk",   "349.90", "369.00", "359.00", "429.90");
        addProduct(list, "Dana Kuşbaşı",       "1kg",  "Et & Tavuk",   "389.90", "399.00", "394.50", "469.90");
        addProduct(list, "Tavuk Göğsü",        "1kg",  "Et & Tavuk",   "134.90", "139.00", "137.00", "164.90");
        addProduct(list, "Tavuk Bütün",        "1kg",  "Et & Tavuk",   "89.90", "94.00", "92.00", "109.90");
        addProduct(list, "Tavuk Kanat",        "1kg",  "Et & Tavuk",   "99.90", "104.00", "102.00", "124.90");
        addProduct(list, "Sucuk",              "250g", "Et & Tavuk",   "64.90", "69.00", "67.00", "84.90");
        addProduct(list, "Salam",              "200g", "Et & Tavuk",   "49.90", "54.00", "52.00", "64.90");
        addProduct(list, "Sosis",              "300g", "Et & Tavuk",   "54.90", "59.00", "57.00", "69.90");

        // ─── KAHVALTILIK ────────────────────────────────────────────
        addProduct(list, "Yumurta",            "15'li","Kahvaltılık",  "89.90", "94.00", "91.50", "109.90");
        addProduct(list, "Yumurta",            "30'lu","Kahvaltılık",  "169.90", "179.00", "174.50", "209.90");
        addProduct(list, "Zeytin Yeşil",       "500g", "Kahvaltılık",  "79.90", "84.00", "82.00", "99.90");
        addProduct(list, "Zeytin Siyah",       "500g", "Kahvaltılık",  "89.90", "94.00", "92.00", "109.90");
        addProduct(list, "Bal",                "850g", "Kahvaltılık",  "149.90", "159.00", "154.50", "189.90");
        addProduct(list, "Reçel Kayısı",       "380g", "Kahvaltılık",  "44.90", "49.00", "47.00", "59.90");
        addProduct(list, "Reçel Çilek",        "380g", "Kahvaltılık",  "46.90", "51.00", "49.00", "62.90");
        addProduct(list, "Tahin Pekmez",       "750g", "Kahvaltılık",  "69.90", "74.00", "72.00", "89.90");
        addProduct(list, "Fındık Ezmesi",      "350g", "Kahvaltılık",  "54.90", "59.00", "57.00", "74.90");

        // ─── TEMEL GIDA ─────────────────────────────────────────────
        addProduct(list, "Ayçiçek Yağı",       "1L",   "Temel Gıda",  "69.90", "68.50", "71.90", "84.90");
        addProduct(list, "Ayçiçek Yağı",       "5L",   "Temel Gıda",  "329.90", "319.00", "339.90", "399.90");
        addProduct(list, "Zeytinyağı",         "1L",   "Temel Gıda",  "249.90", "259.00", "254.50", "299.90");
        addProduct(list, "Mısırözü Yağı",      "1L",   "Temel Gıda",  "74.90", "79.00", "77.00", "94.90");
        addProduct(list, "Un",                 "2kg",  "Temel Gıda",  "39.90", "42.00", "41.00", "49.90");
        addProduct(list, "Un",                 "5kg",  "Temel Gıda",  "89.90", "94.00", "92.00", "109.90");
        addProduct(list, "Şeker",              "1kg",  "Temel Gıda",  "39.90", "42.00", "41.00", "49.90");
        addProduct(list, "Şeker",              "3kg",  "Temel Gıda",  "109.90", "115.00", "112.50", "139.90");
        addProduct(list, "Pirinç",             "1kg",  "Temel Gıda",  "59.90", "62.00", "61.00", "74.90");
        addProduct(list, "Baldo Pirinç",       "1kg",  "Temel Gıda",  "79.90", "84.00", "82.00", "99.90");
        addProduct(list, "Bulgur",             "1kg",  "Temel Gıda",  "39.90", "42.00", "41.00", "49.90");
        addProduct(list, "Makarna",            "500g", "Temel Gıda",  "24.90", "26.90", "25.50", "31.90");
        addProduct(list, "Erişte",             "500g", "Temel Gıda",  "34.90", "37.00", "36.00", "44.90");
        addProduct(list, "Salça Domates",      "830g", "Temel Gıda",  "54.50", "57.00", "56.00", "69.90");
        addProduct(list, "Salça Biber",        "830g", "Temel Gıda",  "64.90", "67.00", "66.00", "79.90");
        addProduct(list, "Tuz",                "750g", "Temel Gıda",  "14.90", "16.00", "15.50", "19.90");
        addProduct(list, "Nohut",              "1kg",  "Temel Gıda",  "49.90", "52.00", "51.00", "64.90");
        addProduct(list, "Kırmızı Mercimek",   "1kg",  "Temel Gıda",  "54.90", "57.00", "56.00", "69.90");
        addProduct(list, "Yeşil Mercimek",     "1kg",  "Temel Gıda",  "59.90", "62.00", "61.00", "74.90");
        addProduct(list, "Kuru Fasulye",       "1kg",  "Temel Gıda",  "69.90", "74.00", "72.00", "89.90");
        addProduct(list, "Sirke",              "500ml","Temel Gıda",  "19.90", "22.00", "21.00", "27.90");

        // ─── EKMEK & UNLU MAMUL ─────────────────────────────────────
        addProduct(list, "Ekmek",              "500g", "Ekmek",       "12.50", "13.50", "12.90", "16.90");
        addProduct(list, "Tost Ekmeği",        "500g", "Ekmek",       "29.90", "32.00", "31.00", "39.90");
        addProduct(list, "Pide Ekmeği",        "1 Adet","Ekmek",      "19.90", "22.00", "21.00", "27.90");

        // ─── İÇECEKLER ──────────────────────────────────────────────
        addProduct(list, "Çay",                "1kg",  "İçecekler",   "159.90", "169.00", "164.50", "199.90");
        addProduct(list, "Türk Kahvesi",       "250g", "İçecekler",   "89.90", "94.00", "92.00", "109.90");
        addProduct(list, "Nescafe Gold",       "100g", "İçecekler",   "159.50", "164.00", "162.00", "189.90");
        addProduct(list, "Su",                 "6x1.5L","İçecekler",  "54.90", "57.00", "56.00", "64.90");
        addProduct(list, "Maden Suyu",         "6x200ml","İçecekler", "34.90", "37.00", "36.00", "44.90");
        addProduct(list, "Meyve Suyu",         "1L",   "İçecekler",   "29.90", "32.00", "31.00", "39.90");
        addProduct(list, "Limonata",           "1L",   "İçecekler",   "24.90", "27.00", "26.00", "34.90");

        // ─── TEMİZLİK ───────────────────────────────────────────────
        addProduct(list, "Bulaşık Deterjanı",  "1L",   "Temizlik",    "44.90", "47.00", "46.00", "59.90");
        addProduct(list, "Bulaşık Tableti",    "40'lı","Temizlik",    "169.90", "179.00", "174.50", "214.90");
        addProduct(list, "Çamaşır Deterjanı",  "4kg",  "Temizlik",    "169.90", "179.00", "174.50", "219.90");
        addProduct(list, "Çamaşır Suyu",       "2.5L", "Temizlik",    "34.90", "37.00", "36.00", "44.90");
        addProduct(list, "Yumuşatıcı",         "1.5L", "Temizlik",    "59.90", "64.00", "62.00", "79.90");
        addProduct(list, "Tuvalet Kağıdı",     "24'lü","Temizlik",    "179.90", "189.00", "184.50", "229.90");
        addProduct(list, "Kağıt Havlu",        "12'li","Temizlik",    "119.90", "124.00", "122.00", "149.90");
        addProduct(list, "Islak Mendil",       "100'lü","Temizlik",   "29.90", "32.00", "31.00", "39.90");
        addProduct(list, "Çöp Poşeti",         "20'li","Temizlik",    "19.90", "22.00", "21.00", "27.90");
        addProduct(list, "Yüzey Temizleyici",  "1L",   "Temizlik",    "39.90", "42.00", "41.00", "54.90");

        // ─── KİŞİSEL BAKIM ──────────────────────────────────────────
        addProduct(list, "Diş Macunu",         "100ml","Kişisel Bakım","49.90", "52.00", "51.00", "64.90");
        addProduct(list, "Diş Fırçası",        "1 Adet","Kişisel Bakım","24.90", "27.00", "26.00", "34.90");
        addProduct(list, "Şampuan",            "500ml","Kişisel Bakım","89.90", "94.00", "92.00", "119.90");
        addProduct(list, "Saç Kremi",          "350ml","Kişisel Bakım","59.90", "64.00", "62.00", "79.90");
        addProduct(list, "Duş Jeli",           "500ml","Kişisel Bakım","69.90", "74.00", "72.00", "89.90");
        addProduct(list, "Sıvı Sabun",         "500ml","Kişisel Bakım","39.90", "42.00", "41.00", "54.90");
        addProduct(list, "Deodorant",          "150ml","Kişisel Bakım","59.90", "64.00", "62.00", "79.90");
        addProduct(list, "Tıraş Köpüğü",      "200ml","Kişisel Bakım","44.90", "49.00", "47.00", "59.90");

        // ─── MEYVE & SEBZE ──────────────────────────────────────────
        addProduct(list, "Domates",            "1kg",  "Meyve & Sebze","29.90", "32.00", "31.00", "39.90");
        addProduct(list, "Biber Sivri",        "1kg",  "Meyve & Sebze","39.90", "42.00", "41.00", "54.90");
        addProduct(list, "Patates",            "1kg",  "Meyve & Sebze","19.90", "21.00", "20.50", "27.90");
        addProduct(list, "Soğan",              "1kg",  "Meyve & Sebze","14.90", "16.00", "15.50", "22.90");
        addProduct(list, "Sarımsak",           "500g", "Meyve & Sebze","49.90", "54.00", "52.00", "69.90");
        addProduct(list, "Elma",               "1kg",  "Meyve & Sebze","34.90", "37.00", "36.00", "44.90");
        addProduct(list, "Portakal",           "1kg",  "Meyve & Sebze","24.90", "27.00", "26.00", "34.90");
        addProduct(list, "Muz",                "1kg",  "Meyve & Sebze","69.90", "74.00", "72.00", "89.90");
        addProduct(list, "Salatalık",          "1kg",  "Meyve & Sebze","24.90", "27.00", "26.00", "34.90");
        addProduct(list, "Limon",              "1kg",  "Meyve & Sebze","29.90", "32.00", "31.00", "39.90");
        addProduct(list, "Havuç",              "1kg",  "Meyve & Sebze","14.90", "17.00", "16.00", "22.90");
        addProduct(list, "Kabak",              "1kg",  "Meyve & Sebze","19.90", "22.00", "21.00", "29.90");
        addProduct(list, "Patlıcan",           "1kg",  "Meyve & Sebze","24.90", "27.00", "26.00", "34.90");

        // ─── DONDURULMUŞ ────────────────────────────────────────────
        addProduct(list, "Donuk Bezelye",      "450g", "Dondurulmuş", "34.90", "37.00", "36.00", "44.90");
        addProduct(list, "Donuk Patates",      "1kg",  "Dondurulmuş", "49.90", "54.00", "52.00", "64.90");
        addProduct(list, "Donuk Pizza",        "1 Adet","Dondurulmuş","39.90", "42.00", "41.00", "54.90");
        addProduct(list, "Dondurma",           "1L",   "Dondurulmuş", "69.90", "74.00", "72.00", "94.90");
        addProduct(list, "Donuk Börek",        "500g", "Dondurulmuş", "54.90", "59.00", "57.00", "74.90");

        // ─── ATIŞTIIRMALIK ──────────────────────────────────────────
        addProduct(list, "Bisküvi",            "300g", "Atıştırmalık","24.90", "27.00", "26.00", "34.90");
        addProduct(list, "Çikolata Sütlü",    "80g",  "Atıştırmalık","19.90", "22.00", "21.00", "27.90");
        addProduct(list, "Cips",               "150g", "Atıştırmalık","34.90", "37.00", "36.00", "44.90");
        addProduct(list, "Kuruyemiş Karışık",  "200g", "Atıştırmalık","69.90", "74.00", "72.00", "89.90");
        addProduct(list, "Gofret",             "40g",  "Atıştırmalık","9.90", "11.00", "10.50", "14.90");
        addProduct(list, "Kraker",             "200g", "Atıştırmalık","19.90", "22.00", "21.00", "27.90");

        // ─── BEBEK ──────────────────────────────────────────────────
        addProduct(list, "Bebek Bezi",         "40'lı","Bebek",       "199.90", "209.00", "204.50", "249.90");
        addProduct(list, "Bebek Islak Mendili", "80'li","Bebek",      "39.90", "42.00", "41.00", "54.90");
        addProduct(list, "Bebek Şampuanı",     "400ml","Bebek",       "69.90", "74.00", "72.00", "94.90");
        addProduct(list, "Bebek Maması",       "250g", "Bebek",       "89.90", "94.00", "92.00", "119.90");

        return list;
    }

    /**
     * Tek ürün için 4 markete fiyat ekler.
     * Sıra: BİM, A101, ŞOK, Migros
     */
    private void addProduct(List<MarketPrice> list,
                            String productName, String unit, String category,
                            String bimPrice, String a101Price,
                            String sokPrice, String migrosPrice) {
        list.add(createPrice(productName, "BİM", bimPrice, unit, category));
        list.add(createPrice(productName, "A101", a101Price, unit, category));
        list.add(createPrice(productName, "ŞOK", sokPrice, unit, category));
        list.add(createPrice(productName, "Migros", migrosPrice, unit, category));
    }

    private MarketPrice createPrice(String productName, String marketName,
                                    String price, String unit, String category) {
        return MarketPrice.builder()
                .productName(productName)
                .marketName(marketName)
                .price(new BigDecimal(price))
                .priceDate(PRICE_DATE)
                .unit(unit)
                .category(category)
                .active(true)
                .build();
    }
}