package com.kakao.sunsuwedding.match;

import com.kakao.sunsuwedding._core.errors.BaseException;
import com.kakao.sunsuwedding._core.errors.exception.Exception400;
import com.kakao.sunsuwedding._core.errors.exception.Exception403;
import com.kakao.sunsuwedding._core.errors.exception.Exception404;
import com.kakao.sunsuwedding.match.Quotation.Quotation;
import com.kakao.sunsuwedding.match.Quotation.QuotationJPARepository;
import com.kakao.sunsuwedding.match.Quotation.QuotationStatus;
import com.kakao.sunsuwedding.user.constant.Role;
import com.kakao.sunsuwedding.user.couple.Couple;
import com.kakao.sunsuwedding.user.couple.CoupleJPARepository;
import com.kakao.sunsuwedding.user.planner.Planner;
import com.kakao.sunsuwedding.user.planner.PlannerJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class MatchService {
    private final CoupleJPARepository coupleJPARepository;
    private final PlannerJPARepository plannerJPARepository;
    private final MatchJPARepository matchJPARepository;
    private final QuotationJPARepository quotationJPARepository;

    // Match Update : 확정 상태, 가격, 확정 날짜
    @Transactional
    public void confirmAll(Pair<String, Long> info, Long matchId) {
        Match match = matchJPARepository.findById(matchId).orElseThrow(
                () -> new Exception404(BaseException.MATCHING_NOT_FOUND.getMessage()));

        // 유저 본인의 채팅방이 맞는지 확인
        permissionCheck(info, match);
        // 플래너가 1개씩 전부 확정한 후에 예비 부부가 전체 확정 가능
        Pair<Boolean, Long> result = isAllConfirmed(match);

        // 모든 견적서 확정 완료 시
        if (result.getFirst()) {
            match.updateStatus(MatchStatus.CONFIRMED);
            match.updateConfirmedPrice(result.getSecond());
            match.updateConfirmedAt(LocalDateTime.now());
        }
        // 확정되지 않은 견적서가 있을 때
        else {
            throw new Exception400(BaseException.QUOTATIONS_NOT_ALL_CONFIRMED.getMessage());
        }
    }

    // Match Delete : isActive 필드 false로
    // 채팅방 삭제 - 전체 확정 후 / 견적서 없을 때
    @Transactional
    public void deleteChat(Pair<String, Long> info, Long matchId) {
        Match match = matchJPARepository.findById(matchId).orElseThrow(
                () -> new Exception404(BaseException.MATCHING_NOT_FOUND.getMessage()));

        // 유저 본인의 채팅방이 맞는지 확인
        permissionCheck(info, match);

        List<Quotation> quotations = quotationJPARepository.findAllByMatch(match);
        // 견적서 존재하는데 전체 확정이 되지 않은 경우, 채팅방 삭제 불가
        if ((!quotations.isEmpty()) && (match.getStatus().equals(MatchStatus.UNCONFIRMED))) {
            throw new Exception400(BaseException.NOT_CONFIRMED_ALL_QUOTATIONS.getMessage());
        }
        // 전체확정 됐거나, 견적서가 없는 경우 채팅방 삭제
        matchJPARepository.delete(match);
    }

    private Pair<Boolean, Long> isAllConfirmed(Match match) {
        List<Quotation> quotations = quotationJPARepository.findAllByMatch(match);
        if (quotations.isEmpty()) {
            throw new Exception400(BaseException.NO_QUOTATION_TO_CONFIRM.getMessage());
        }
        else {
            // 모든 견적서 확정 됐는지 여부 구하기
            Boolean allConfirmed = quotations.stream().allMatch(quotation -> quotation.getStatus().equals(QuotationStatus.CONFIRMED));
            quotations.stream().forEach(quotation -> System.out.println(quotation.getStatus()));

            // Total Price 구하기
            Long totalPrice = quotations.stream().mapToLong(Quotation::getPrice).sum();

            return Pair.of(allConfirmed, totalPrice);
        }
    }

    public void addChat(Pair<String, Long> user, MatchRequest.AddMatchDTO requestDTO) {
        Long coupleId = user.getSecond();
        Long plannerId = requestDTO.getPlannerId();

        Couple couple = coupleJPARepository.findById(coupleId).orElseThrow(
                () -> new Exception404(BaseException.USER_NOT_FOUND.getMessage() + " couple")
        );
        Planner planner = plannerJPARepository.findById(plannerId).orElseThrow(
                () -> new Exception404(BaseException.USER_NOT_FOUND.getMessage() + " planner")
        );
        matchJPARepository.save(requestDTO.toMatchEntity(couple, planner));
    }

    private void permissionCheck(Pair<String, Long> info, Match match) {
        String role = info.getFirst();
        Long id = info.getSecond();
        if (role.equals(Role.PLANNER.getRoleName())) {
            if (!match.getPlanner().getId().equals(id))
                throw new Exception403(BaseException.PERMISSION_DENIED_METHOD_ACCESS.getMessage());
        }
        else {
            if (!match.getCouple().getId().equals(id))
                throw new Exception403(BaseException.PERMISSION_DENIED_METHOD_ACCESS.getMessage());
        }
    }
}