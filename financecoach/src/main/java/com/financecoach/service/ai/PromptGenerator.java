package com.financecoach.service.ai;

import com.financecoach.dto.response.MarketPriceResponse;
import com.financecoach.dto.response.TransactionResponse;
import com.financecoach.model.enums.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PromptGenerator {

    /**
     * Her prompt'un sonuna eklenen Türkçe zorlama suffix'i.
     * System prompt'taki kuralları user mesajında da tekrarlayarak
     * modelin Türkçe kalma olasılığını artırır.
     */
    private static final String TURKISH_SUFFIX = """

            ÖNEMLİ: Yanıtını YALNIZCA TÜRKÇE yaz. \
            İngilizce veya başka dilde tek kelime bile kullanma. \
            En fazla 3 kısa cümle yaz, madde işareti kullanma.""";

    // ─── GENEL FİNANSAL TAVSİYE PROMPT'U ────────────────────────────────────

    public String buildGeneralAdvicePrompt(List<TransactionResponse> transactions, int limit) {
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(TransactionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(TransactionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fixedExpense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && t.isFixed())
                .map(TransactionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variableExpense = totalExpense.subtract(fixedExpense);
        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        Map<String, BigDecimal> categoryTotals = buildCategoryTotals(transactions);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Aşağıdaki finansal veriler bana aittir:\n\n");
        prompt.append(String.format("- Toplam Gelir: %.2f TL%n", totalIncome));
        prompt.append(String.format("- Toplam Gider: %.2f TL%n", totalExpense));

        if (fixedExpense.compareTo(BigDecimal.ZERO) > 0) {
            prompt.append(String.format("  · Sabit Giderler: %.2f TL (kira, fatura vb.)%n", fixedExpense));
            prompt.append(String.format("  · Değişken Giderler: %.2f TL%n", variableExpense));
        }

        prompt.append(String.format("- Net Bakiye: %.2f TL%n%n", netBalance));

        if (!categoryTotals.isEmpty()) {
            prompt.append("Değişken harcama kategorilerim:\n");
            categoryTotals.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .forEach(e -> prompt.append(String.format(
                            "  · %s: %.2f TL%n", e.getKey(), e.getValue()
                    )));
            prompt.append("\n");
        }

        if (!transactions.isEmpty()) {
            prompt.append("Son değişken harcamalarım:\n");
            transactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE && !t.isFixed())
                    .limit(limit)
                    .forEach(t -> prompt.append(String.format(
                            "  · %s - %.2f TL (%s)%n",
                            t.getCategory() != null ? t.getCategory() : "Diğer",
                            t.getAmount(),
                            t.getTransactionDate()
                    )));
            prompt.append("\n");
        }

        prompt.append("Bu verilere dayanarak somut tasarruf önerisi ver. ");
        prompt.append("Sabit giderleri (kira, fatura) hariç tut, sadece değişken harcamaları analiz et. ");
        prompt.append("Tasarruf fırsatlarını ve dikkat edilecek kategorileri belirt.");
        prompt.append(TURKISH_SUFFIX);

        return prompt.toString();
    }

    // ─── KATEGORİ BAZLI UYARI PROMPT'U ──────────────────────────────────────

    public String buildCategoryAlertPrompt(List<TransactionResponse> transactions) {
        Map<String, BigDecimal> categoryTotals = buildCategoryTotals(transactions);

        if (categoryTotals.isEmpty()) {
            return buildGeneralAdvicePrompt(transactions, 10);
        }

        Map.Entry<String, BigDecimal> topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        BigDecimal totalExpense = categoryTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal categoryShare = totalExpense.compareTo(BigDecimal.ZERO) > 0
                ? topCategory.getValue()
                .divide(totalExpense, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.debug("En yüksek değişken harcama kategorisi: '{}' → {} TL (%{})",
                topCategory.getKey(), topCategory.getValue(), categoryShare);

        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format(
                "En çok harcama yaptığım kategori '%s' ve değişken giderlerimin %%%s'ini oluşturuyor (%.2f TL).%n%n",
                topCategory.getKey(), categoryShare, topCategory.getValue()
        ));

        prompt.append("Değişken harcama dağılımım:\n");
        categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(e -> {
                    BigDecimal share = e.getValue()
                            .divide(totalExpense, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP);
                    prompt.append(String.format(
                            "  · %s: %.2f TL (%%%s)%n",
                            e.getKey(), e.getValue(), share
                    ));
                });

        prompt.append(String.format(
                "%n'%s' kategorisinde %.2f TL harcıyorum. Bu kategoriyi azaltmak için 3 somut öneri ver.",
                topCategory.getKey(), topCategory.getValue()
        ));
        prompt.append(TURKISH_SUFFIX);

        return prompt.toString();
    }

    // ─── MARKET KARŞILAŞTIRMASI PROMPT'U ─────────────────────────────────────

    public String buildMarketComparisonPrompt(
            List<TransactionResponse> transactions,
            List<MarketPriceResponse> marketPrices,
            String productName
    ) {
        if (marketPrices.isEmpty()) {
            log.warn("'{}' için market fiyatı bulunamadı, genel prompt'a dönülüyor", productName);
            return buildGeneralAdvicePrompt(transactions, 10);
        }

        MarketPriceResponse cheapest = marketPrices.stream()
                .min(Comparator.comparing(MarketPriceResponse::getPrice))
                .orElseThrow();

        MarketPriceResponse mostExpensive = marketPrices.stream()
                .max(Comparator.comparing(MarketPriceResponse::getPrice))
                .orElseThrow();

        BigDecimal savingsPct = BigDecimal.ZERO;
        if (mostExpensive.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            savingsPct = mostExpensive.getPrice()
                    .subtract(cheapest.getPrice())
                    .divide(mostExpensive.getPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("'%s' ürünü için market fiyatları:%n%n", productName));

        marketPrices.stream()
                .sorted(Comparator.comparing(MarketPriceResponse::getPrice))
                .forEach(mp -> prompt.append(String.format(
                        "  · %s: %.2f TL%n",
                        mp.getMarketName(), mp.getPrice()
                )));

        prompt.append(String.format(
                "%nEn ucuz: %s (%.2f TL), En pahalı: %s (%.2f TL), Fark: %%%s%n",
                cheapest.getMarketName(), cheapest.getPrice(),
                mostExpensive.getMarketName(), mostExpensive.getPrice(),
                savingsPct
        ));

        prompt.append(String.format(
                "%n'%s' ürününü en uygun marketten alarak ne kadar tasarruf edebileceğimi hesapla.",
                productName
        ));
        prompt.append(TURKISH_SUFFIX);

        log.debug("Market karşılaştırma prompt'u üretildi: ürün='{}', en ucuz={}({}TL)",
                productName, cheapest.getMarketName(), cheapest.getPrice());

        return prompt.toString();
    }

    // ─── SİMÜLASYON MODU ─────────────────────────────────────────────────────

    public String simulateAdvice(String prompt) {
        log.info("Simülasyon modu: gerçek LLM API çağrısı yapılmıyor");

        if (prompt.contains("market fiyatları")) {
            return "En ucuz marketi tercih ederek bu üründe tasarruf yapabilirsiniz. " +
                    "Alışveriş listenizi önceden hazırlayarak gereksiz harcamalardan kaçının.";
        }

        if (prompt.contains("en çok harcama yaptığım kategori")) {
            return "Bu kategorideki harcamalarınız bütçenizde önemli yer tutuyor. " +
                    "Toplu alım yaparak birim maliyeti düşürebilir, alternatif ürünleri değerlendirebilirsiniz. " +
                    "Küçük değişiklikler zamanla büyük tasarruf sağlar.";
        }

        return "Değişken harcamalarınızı kategorize ederek bütçe takibi yapmanız tasarruf hedefinize " +
                "ulaşmanızı kolaylaştırır. İsteğe bağlı harcamalarınızı gözden geçirerek aylık gelirinizin " +
                "en az %10'unu kenara ayırmanızı öneririm.";
    }

    // ─── Private Yardımcılar ─────────────────────────────────────────────────

    public Map<String, BigDecimal> buildCategoryTotals(List<TransactionResponse> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && !t.isFixed())
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "Diğer",
                        Collectors.reducing(BigDecimal.ZERO,
                                TransactionResponse::getAmount,
                                BigDecimal::add)
                ));
    }

    public String findTopCategory(List<TransactionResponse> transactions) {
        return buildCategoryTotals(transactions).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Diğer");
    }
}