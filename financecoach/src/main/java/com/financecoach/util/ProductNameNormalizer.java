package com.financecoach.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ürün adı normalizasyonu.
 * Farklı marketlerden gelen aynı ürünlerin eşleştirilmesi için kullanılır.
 *
 * Örnekler:
 *   "Tam Yağlı Süt 1 Lt"   → "tam yağlı süt 1l"
 *   "Tam Yağlı Süt 1L"     → "tam yağlı süt 1l"
 *   "Tam Yağlı Süt 1 Litre"→ "tam yağlı süt 1l"
 *   "Tereyağı 500 gr"       → "tereyağı 500g"
 *   "Tereyağı 500g"         → "tereyağı 500g"
 */
public final class ProductNameNormalizer {

    private ProductNameNormalizer() {}

    // ── Birim normalizasyon haritası (sıra önemli: uzun olanlar önce) ──────
    private static final LinkedHashMap<Pattern, String> UNIT_PATTERNS = new LinkedHashMap<>();

    static {
        // Hacim
        UNIT_PATTERNS.put(Pattern.compile("(\\d+)\\s*(?:litre|lt|ltr)", Pattern.CASE_INSENSITIVE), "$1l");
        UNIT_PATTERNS.put(Pattern.compile("(\\d+)\\s*(?:mililitre|mlt|ml)", Pattern.CASE_INSENSITIVE), "$1ml");
        UNIT_PATTERNS.put(Pattern.compile("(\\d+)\\s*(?:cl)", Pattern.CASE_INSENSITIVE), "$1cl");

        // Ağırlık
        UNIT_PATTERNS.put(Pattern.compile("(\\d+)\\s*(?:kilogram|kilo|kg)", Pattern.CASE_INSENSITIVE), "$1kg");
        UNIT_PATTERNS.put(Pattern.compile("(\\d+)\\s*(?:gram|gr|g)\\b", Pattern.CASE_INSENSITIVE), "$1g");

        // Adet
        UNIT_PATTERNS.put(Pattern.compile("(\\d+)\\s*(?:adet|ad)\\b", Pattern.CASE_INSENSITIVE), "$1 adet");

        // Paket
        UNIT_PATTERNS.put(Pattern.compile("(\\d+)\\s*(?:paket|pk)\\b", Pattern.CASE_INSENSITIVE), "$1 paket");

        // '... lı / ... li / ... lu / ... lü' suffix (4'lü, 6'lı vb.)
        UNIT_PATTERNS.put(Pattern.compile("(\\d+)\\s*[''`]?\\s*(?:lı|li|lu|lü)", Pattern.CASE_INSENSITIVE), "$1li");
    }

    // ── Eş anlamlı kelime normalizasyonu ──────────────────────────────────
    private static final Map<String, String> SYNONYM_MAP = Map.ofEntries(
            Map.entry("y.yağlı", "yarım yağlı"),
            Map.entry("yarim yağlı", "yarım yağlı"),
            Map.entry("yarim yagli", "yarım yağlı"),
            Map.entry("t.yağlı", "tam yağlı"),
            Map.entry("tam yagli", "tam yağlı"),
            Map.entry("tereyag", "tereyağı"),
            Map.entry("tereyagi", "tereyağı"),
            Map.entry("peynır", "peynir"),
            Map.entry("domates salcasi", "domates salçası"),
            Map.entry("salca", "salça"),
            Map.entry("seker", "şeker"),
            Map.entry("un ", "un "),
            Map.entry("tuvalet kagidi", "tuvalet kağıdı"),
            Map.entry("kagit havlu", "kağıt havlu"),
            Map.entry("bulasik", "bulaşık"),
            Map.entry("camasir", "çamaşır"),
            Map.entry("sampuan", "şampuan")
    );

    /**
     * Ürün adını normalize eder.
     * Null-safe: null girişte boş string döner.
     */
    public static String normalize(String productName) {
        if (productName == null || productName.isBlank()) return "";

        String result = productName.trim();

        // 1. Türkçe lowercase
        result = turkishLowerCase(result);

        // 2. Fazla boşlukları tek boşluğa indir
        result = result.replaceAll("\\s+", " ");

        // 3. Birim normalizasyonu
        for (Map.Entry<Pattern, String> entry : UNIT_PATTERNS.entrySet()) {
            result = entry.getKey().matcher(result).replaceAll(entry.getValue());
        }

        // 4. Eş anlamlı kelime normalizasyonu
        for (Map.Entry<String, String> entry : SYNONYM_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // 5. Son temizlik: baştaki-sondaki boşluk + tekrar eden boşluklar
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    /**
     * İki ürün adının normalize edilmiş hali eşleşiyor mu?
     */
    public static boolean matches(String name1, String name2) {
        return normalize(name1).equals(normalize(name2));
    }

    /**
     * Normalize edilmiş ad, diğerini içeriyor mu? (fuzzy partial match)
     */
    public static boolean containsNormalized(String haystack, String needle) {
        return normalize(haystack).contains(normalize(needle));
    }

    /**
     * Türkçe'ye uygun lowercase.
     * Java'nın default toLowerCase() Türkçe İ/I dönüşümünü yanlış yapar.
     */
    private static String turkishLowerCase(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case 'İ' -> sb.append('i');
                case 'I' -> sb.append('ı');
                case 'Ş' -> sb.append('ş');
                case 'Ğ' -> sb.append('ğ');
                case 'Ü' -> sb.append('ü');
                case 'Ö' -> sb.append('ö');
                case 'Ç' -> sb.append('ç');
                default -> sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}