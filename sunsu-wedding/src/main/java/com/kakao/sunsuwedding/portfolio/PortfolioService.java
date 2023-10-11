package com.kakao.sunsuwedding.portfolio;

import com.kakao.sunsuwedding._core.errors.BaseException;
import com.kakao.sunsuwedding._core.errors.exception.Exception400;
import com.kakao.sunsuwedding._core.errors.exception.Exception403;
import com.kakao.sunsuwedding._core.errors.exception.Exception404;
import com.kakao.sunsuwedding.match.Match;
import com.kakao.sunsuwedding.match.MatchJPARepository;
import com.kakao.sunsuwedding.match.Quotation.Quotation;
import com.kakao.sunsuwedding.match.Quotation.QuotationJPARepository;
import com.kakao.sunsuwedding.portfolio.image.ImageEncoder;
import com.kakao.sunsuwedding.portfolio.image.ImageItem;
import com.kakao.sunsuwedding.portfolio.image.ImageItemJPARepository;
import com.kakao.sunsuwedding.portfolio.price.PriceItem;
import com.kakao.sunsuwedding.portfolio.price.PriceItemJPARepository;
import com.kakao.sunsuwedding.user.constant.Role;
import com.kakao.sunsuwedding.user.planner.Planner;
import com.kakao.sunsuwedding.user.planner.PlannerJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

@RequiredArgsConstructor
@Service
public class PortfolioService {

    private final PortfolioJPARepository portfolioJPARepository;
    private final ImageItemJPARepository imageItemJPARepository;
    private final PriceItemJPARepository priceItemJPARepository;
    private final MatchJPARepository matchJPARepository;
    private final QuotationJPARepository quotationJPARepository;
    private final PlannerJPARepository plannerJPARepository;

    public Pair<Portfolio, Planner> addPortfolio(PortfolioRequest.addDTO request, Long plannerId) {
        // 요청한 플래너 탐색
        Planner planner = plannerJPARepository.findById(plannerId)
                .orElseThrow(() -> new Exception404(BaseException.USER_NOT_FOUND.getMessage()));

        Portfolio existPortfolio = portfolioJPARepository.findByPlannerId(plannerId)
                .orElse(new Portfolio());

        // 해당 플래너가 생성한 포트폴리오가 이미 있는 경우 예외처리
        if (existPortfolio.getId() != null)
            throw new Exception400(BaseException.PORTFOLIO_ALREADY_EXIST.getMessage());

        // 필요한 계산값 연산
        Long totalPrice =  request.getItems().stream()
                .mapToLong(PortfolioRequest.addDTO.ItemDTO::getItemPrice)
                .sum();

        // 포트폴리오 엔티티에 저장
        Portfolio portfolio = Portfolio.builder()
                .planner(planner)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .career(request.getCareer())
                .partnerCompany(request.getPartnerCompany())
                .totalPrice(totalPrice)
                .contractCount(0L)
                .avgPrice(0L)
                .minPrice(0L)
                .maxPrice(0L)
                .build();
        portfolioJPARepository.save(portfolio);

        // 가격 항목 엔티티에 저장
        List<PriceItem> priceItems = new ArrayList<>();
        for (PortfolioRequest.addDTO.ItemDTO item : request.getItems()) {
            PriceItem priceItem = PriceItem.builder()
                    .portfolio(portfolio)
                    .itemTitle(item.getItemTitle())
                    .itemPrice(item.getItemPrice())
                    .build();
            priceItems.add(priceItem);
        }
        priceItemJPARepository.saveAll(priceItems);

        // 이미지 처리 로직에 활용하기 위해 포트폴리오 객체 리턴
        return Pair.of(portfolio, planner);
    }

    public List<PortfolioResponse.findAllBy> getPortfolios(PageRequest pageRequest) {
        List<Portfolio> portfolios = portfolioJPARepository.findAllByOrderByCreatedAtDesc(pageRequest).getContent();

        List<String> images = imageItemJPARepository.findAllByThumbnailAndPortfolioInOrderByPortfolioCreatedAtDesc(true, portfolios)
                .stream()
                .map(ImageEncoder::encode)
                .toList();

        return PortfolioDTOConverter.toListItemDTO(portfolios, images);
    }

    public PortfolioResponse.findById getPortfolioById(Long id) {
        List<ImageItem> imageItems = imageItemJPARepository.findByPortfolioId(id);
        if (imageItems.isEmpty()) {
            throw new Exception404(BaseException.PORTFOLIO_NOT_FOUND.getMessage());
        }

        List<String> images = imageItems
                .stream()
                .map(ImageEncoder::encode)
                .toList();

        List<PriceItem> priceItems = priceItemJPARepository.findAllByPortfolioId(id);
        Portfolio portfolio = imageItems.get(0).getPortfolio();
        Planner planner = portfolio.getPlanner();

        // 거래 내역 조회를 위한 매칭 내역, 견적서 가져오기
        List<Match> matches = matchJPARepository.findLatestTenByPlanner(planner);
        List<Long> matchIds = matches.stream().map(Match::getId).toList();
        List<Quotation> quotations = quotationJPARepository.findAllByMatchIds(matchIds);

        return PortfolioDTOConverter.toPortfolioDTO(planner, portfolio, images, priceItems, matches, quotations);
    }

    @Transactional
    public Pair<Portfolio,Planner> updatePortfolio(PortfolioRequest.updateDTO request, Long plannerId) {
        // 요청한 플래너 탐색
        Planner planner = plannerJPARepository.findById(plannerId)
                .orElseThrow(() -> new Exception404(BaseException.USER_NOT_FOUND.getMessage()));

        // 플래너의 포트폴리오 탐색
        Portfolio portfolio = portfolioJPARepository.findByPlannerId(plannerId)
                .orElseThrow(() -> new Exception400(BaseException.PORTFOLIO_NOT_FOUND.getMessage()));

        // 필요한 계산값 연산
        Long totalPrice =  request.getItems().stream()
                .mapToLong(PortfolioRequest.updateDTO.ItemDTO::getItemPrice)
                .sum();

        // 불변 객체 패턴을 고려한 포트폴리오 변경사항 업데이트
        Portfolio updatedPortfolio = Portfolio.builder()
                .id(portfolio.getId())
                .planner(planner)
                .title(request.getTitle() != null ? request.getTitle() : portfolio.getTitle())
                .description(request.getDescription() != null ? request.getDescription() : portfolio.getDescription())
                .location(request.getLocation() != null ? request.getLocation() : portfolio.getLocation())
                .career(request.getCareer() != null ? request.getCareer() : portfolio.getCareer())
                .partnerCompany(request.getPartnerCompany() != null ? request.getPartnerCompany() : portfolio.getPartnerCompany())
                .totalPrice(totalPrice)
                .contractCount(portfolio.getContractCount())
                .avgPrice(portfolio.getAvgPrice())
                .minPrice(portfolio.getMinPrice())
                .maxPrice(portfolio.getMaxPrice())
                .build();
        portfolioJPARepository.save(updatedPortfolio);

        // 해당하는 가격 아이템 탐색 & 업데이트
        List<PriceItem> existPriceItems = priceItemJPARepository.findByPortfolioId(portfolio.getId());
        List<PriceItem> updatedPriceItems = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            PriceItem priceItem = existPriceItems.get(i);
            PortfolioRequest.updateDTO.ItemDTO item = request.getItems().get(i);

            PriceItem updatedPriceItem = PriceItem.builder()
                    .id(priceItem.getId())
                    .portfolio(portfolio)
                    .itemTitle(item.getItemTitle() != null ? item.getItemTitle() : priceItem.getItemTitle())
                    .itemPrice(item.getItemPrice() != null ? item.getItemPrice() : priceItem.getItemPrice())
                    .build();
            updatedPriceItems.add(updatedPriceItem);
        }
        priceItemJPARepository.saveAll(updatedPriceItems);

        // 이미지 처리 로직에 활용하기 위해 포트폴리오 객체 리턴
        return Pair.of(updatedPortfolio, planner);

    }

    @Transactional
    public void updateConfirmedPrices(Planner planner, Long contractCount, Long avgPrice, Long minPrice, Long maxPrice) {
        Portfolio portfolio = portfolioJPARepository.findByPlannerId(planner.getId())
                .orElseThrow(() -> new Exception404(BaseException.PORTFOLIO_NOT_FOUND.getMessage()));

        portfolio.updateConfirmedPrices(contractCount, avgPrice, minPrice, maxPrice);
        portfolioJPARepository.save(portfolio);
    }

    @Transactional
    public void deletePortfolio(Pair<String, Long> info) {
        if (!info.getFirst().equals(Role.PLANNER.getRoleName())) {
            throw new Exception403(BaseException.PERMISSION_DENIED_METHOD_ACCESS.getMessage());
        }

        Planner planner = Planner.builder().id(info.getSecond()).build();
        priceItemJPARepository.deleteAllByPortfolioPlannerId(planner.getId());
        imageItemJPARepository.deleteAllByPortfolioPlannerId(planner.getId());
        portfolioJPARepository.deleteByPlanner(planner);
    }

    /*
    public PortfolioResponse.myPortfolioDTO myPortfolio(Long plannerId) {
        // 플래너의 포트폴리오 탐색
        Portfolio portfolio = portfolioJPARepository.findByPlannerId(plannerId)
                .orElseThrow(() -> new Exception400(BaseException.PORTFOLIO_NOT_FOUND.getMessage()));

        List<ImageItem> imageItems = imageItemJPARepository.findByPortfolioId(portfolio.getId());
        if (imageItems.isEmpty()) {
            throw new Exception404(BaseException.PORTFOLIO_NOT_FOUND.getMessage());
        }

        List<String> images = imageItems
                .stream()
                .map(ImageEncoder::encode)
                .toList();

        List<PriceItem> priceItems = priceItemJPARepository.findAllByPortfolioId(id);
        Portfolio portfolio = imageItems.get(0).getPortfolio();
        Planner planner = portfolio.getPlanner();


        return PortfolioDTOConverter.toMyPortfolioDTO();
    }
    */
}
