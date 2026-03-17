package com.financecoach.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(min = 2, max = 200, message = "Ürün adı 2-200 karakter arasında olmalıdır")
    private String productName;

    @Min(value = 1, message = "Miktar en az 1 olmalıdır")
    @Builder.Default
    private Integer quantity = 1;
}