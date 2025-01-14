package kr.co.pennyway.api.apis.ledger.service;

import kr.co.pennyway.domain.context.finance.service.TargetAmountService;
import kr.co.pennyway.domain.domains.target.domain.TargetAmount;
import kr.co.pennyway.domain.domains.target.exception.TargetAmountErrorCode;
import kr.co.pennyway.domain.domains.target.exception.TargetAmountErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TargetAmountSearchService {
    private final TargetAmountService targetAmountService;

    @Transactional(readOnly = true)
    public List<TargetAmount> readTargetAmountsByUserId(Long userId) {
        return targetAmountService.readTargetAmountsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public TargetAmount readTargetAmountThatMonth(Long userId, LocalDate date) {
        return targetAmountService.readTargetAmountThatMonth(userId, date).orElseThrow(() -> new TargetAmountErrorException(TargetAmountErrorCode.NOT_FOUND_TARGET_AMOUNT));
    }

    @Transactional(readOnly = true)
    public Optional<TargetAmount> readRecentTargetAmount(Long userId) {
        return targetAmountService.readRecentTargetAmount(userId);
    }
}
