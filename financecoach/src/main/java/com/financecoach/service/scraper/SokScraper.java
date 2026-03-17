package com.financecoach.service.scraper;

import com.financecoach.model.entity.MarketPrice;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ŞOK Market (Cepte Şok) Scraper — sokmarket.com.tr
 *
 * DİKKAT: ŞOK'ta fiyat formatı diğerlerinden farklı!
 * A101/BİM: ₺24,50 (₺ solda)
 * ŞOK:      49,90₺  (₺ sağda)
 */
@Service
@Slf4j
public class SokScraper implements MarketScraper {

    private static final String BASE_URL = "https://www.sokmarket.com.tr";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 15_000;
    private static final int DELAY_MS = 2_000;

    // ŞOK'ta ₺ SAĞDA: 49,90₺
    private static final Pattern PRICE_PATTERN = Pattern.compile("([\\d.]+,\\d{2})₺");
    private static final Pattern ID_PATTERN = Pattern.compile("-p-(\\d+)");
    private static final Pattern WIN_PARA_PATTERN = Pattern.compile("\\+\\d+\\s*win\\s*Para", Pattern.CASE_INSENSITIVE);

    private static final List<String> CATEGORY_PATHS = List.of(
            "/meyve-ve-sebze-c-20",
            "/et-ve-tavuk-ve-sarkuteri-c-160",
            "/sut-ve-sut-urunleri-c-460",
            "/kahvaltilik-c-890",
            "/yemeklik-malzemeler-c-1770",
            "/atistirmaliklar-c-20376",
            "/icecek-c-20505",
            "/temizlik-c-20647",
            "/kisisel-bakim-ve-kozmetik-c-20395"
    );

    @Override
    public String getMarketName() {
        return "ŞOK";
    }

    @Override
    public List<MarketPrice> scrape() {
        List<MarketPrice> allProducts = new ArrayList<>();
        log.info("ŞOK scraping başlatılıyor...");

        for (String path : CATEGORY_PATHS) {
            try {
                List<MarketPrice> products = scrapeCategory(path);
                List<MarketPrice> filtered = products.stream()
                        .filter(p -> FmcgFilter.isFmcg(p.getProductName(), p.getCategory()))
                        .toList();
                allProducts.addAll(filtered);
                log.info("  ŞOK {} → {} ürün (filtrelenmeden: {})", path, filtered.size(), products.size());
                Thread.sleep(DELAY_MS);
            } catch (Exception e) {
                log.error("  ŞOK {} hatası: {}", path, e.getMessage());
            }
        }

        log.info("ŞOK scraping tamamlandı: {} ürün", allProducts.size());
        return allProducts;
    }

    private List<MarketPrice> scrapeCategory(String path) throws Exception {
        List<MarketPrice> products = new ArrayList<>();
        String url = BASE_URL + path;

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .header("Accept-Language", "tr-TR,tr;q=0.9")
                .get();

        // ŞOK ürün kartları: a[href*='-p-']
        Elements cards = doc.select("a[href*='-p-']");

        for (Element card : cards) {
            try {
                MarketPrice product = parseCard(card, path);
                if (product != null) {
                    products.add(product);
                }
            } catch (Exception e) {
                log.debug("ŞOK ürün parse hatası: {}", e.getMessage());
            }
        }

        return products;
    }

    private MarketPrice parseCard(Element card, String categoryPath) {
        // Ürün adı: h2
        String name = null;
        Element h2 = card.selectFirst("h2");
        if (h2 != null) name = h2.text().trim();
        if (name == null || name.isBlank()) {
            Element img = card.selectFirst("img[alt]");
            if (img != null) name = img.attr("alt").trim();
        }
        if (name == null || name.isBlank()) return null;

        // Fiyat: XX,XX₺ — "win Para" taglarını temizle, son fiyat geçerli
        String cardText = WIN_PARA_PATTERN.matcher(card.text()).replaceAll("");
        List<BigDecimal> prices = new ArrayList<>();
        Matcher matcher = PRICE_PATTERN.matcher(cardText);
        while (matcher.find()) {
            try {
                String priceStr = matcher.group(1).replace(".", "").replace(",", ".");
                prices.add(new BigDecimal(priceStr));
            } catch (NumberFormatException ignored) {}
        }
        if (prices.isEmpty()) return null;
        BigDecimal price = prices.get(prices.size() - 1);
        if (price.compareTo(BigDecimal.ZERO) <= 0) return null;

        // Önceki fiyat (indirimli ürünlerde)
        BigDecimal previousPrice = prices.size() >= 2 ? prices.get(0) : null;

        // External ID
        String href = card.attr("href");
        String externalId = "SOK-" + href.hashCode();
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (idMatcher.find()) externalId = "SOK-" + idMatcher.group(1);

        // Ürün URL
        String productUrl = href.startsWith("http") ? href : BASE_URL + href;

        // Resim
        String imageUrl = null;
        Element img = card.selectFirst("img[src*='images.ceptesok.com']");
        if (img != null) imageUrl = img.attr("abs:src");

        // Kategori
        String category = mapCategory(categoryPath);

        // Birim
        String unit = extractUnit(name);

        return MarketPrice.builder()
                .productName(name)
                .marketName("ŞOK")
                .price(price)
                .previousPrice(previousPrice)
                .priceDate(LocalDate.now())
                .unit(unit)
                .externalId(externalId)
                .productUrl(productUrl)
                .imageUrl(imageUrl)
                .category(category)
                .active(true)
                .scrapedAt(OffsetDateTime.now())
                .build();
    }

    private String mapCategory(String path) {
        if (path.contains("meyve-ve-sebze")) return "Meyve & Sebze";
        if (path.contains("et-ve-tavuk")) return "Et & Tavuk";
        if (path.contains("sut-ve-sut-urunleri")) return "Süt Ürünleri";
        if (path.contains("kahvaltilik")) return "Kahvaltılık";
        if (path.contains("yemeklik-malzemeler")) return "Temel Gıda";
        if (path.contains("atistirmaliklar")) return "Atıştırmalıklar";
        if (path.contains("icecek")) return "İçecekler";
        if (path.contains("temizlik")) return "Temizlik";
        if (path.contains("kisisel-bakim")) return "Kişisel Bakım";
        return "Diğer";
    }

    private String extractUnit(String name) {
        Pattern unitPattern = Pattern.compile(
                "(\\d+[.,]?\\d*)\\s*(kg|gr|g|ml|lt|litre|l|adet|'(?:l[iıuü]|lu|lü))",
                Pattern.CASE_INSENSITIVE);
        Matcher m = unitPattern.matcher(name);
        return m.find() ? m.group(0).trim() : null;
    }
}