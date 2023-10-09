package com.kakao.sunsuwedding._core.utils;

import com.kakao.sunsuwedding.match.Quotation.Quotation;
import com.kakao.sunsuwedding.match.Quotation.QuotationStatus;
import com.kakao.sunsuwedding.portfolio.PortfolioResponse;

import java.util.List;

public class PriceCalculator {
    public static Long calculatePortfolioPrice(List<PortfolioResponse.PriceItemDTO> priceItemDTOS) {
        return priceItemDTOS.stream().mapToLong(PortfolioResponse.PriceItemDTO::itemPrice).sum();
    }

    public static Long calculateQuotationPrice(List<Quotation> quotations) {
        return quotations.stream().mapToLong(Quotation::getPrice).sum();
    }

    public static Long calculateConfirmedQuotationPrice(List<Quotation> quotations) {
        return quotations.stream().mapToLong(quotation -> {
            if (quotation.getStatus().equals(QuotationStatus.CONFIRMED)) {
                return quotation.getPrice();
            }
            else return 0L;
        })
        .sum();
    }
}
