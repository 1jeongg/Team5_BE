package com.kakao.sunsuwedding.portfolio;

import java.util.List;

public class PortfolioResponse {
    public record FindAllDTO(
            Long id,
            String imagePath,
            String title,
            String plannerName,
            Long price,
            String location,
            Long contractCount,
            Boolean isLiked
    ) {
    }

    public record FindByIdDTO(
            Long id,
            Long userId,
            List<String> imagePaths,
            String title,
            String plannerName,
            Long contractCount,
            PriceDTO priceInfo,
            String location,
            String description,
            String career,
            String partnerCompany,
            PaymentHistoryDTO paymentsHistory,
            Boolean isLiked) {
    }

    public record PriceDTO(
            Long totalPrice,
            List<PriceItemDTO> items
    ) {
    }

    public record PriceItemDTO(
            String itemTitle,
            Long itemPrice
    ) {
    }

    public record PaymentHistoryDTO(
            Long avgPrice,
            Long minPrice,
            Long maxPrice,
            List<PaymentDTO> payments
    ) {
    }

    public record PaymentDTO(
            Long price,
            String confirmedAt,
            List<PaymentItemDTO> paymentItems
    ) {
    }

    public record PaymentItemDTO(
            String paymentTitle,
            Long paymentPrice,
            String paymentCompany,
            String paymentDescription
    ) {
    }

    public record MyPortfolioDTO(
            String plannerName,
            List<String> imagePaths,
            List<PriceItemDTO> priceItems,
            String title,
            String description,
            String location,
            String career,
            String partnerCompany
    ) {
    }
}
