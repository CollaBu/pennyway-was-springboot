package kr.co.pennyway.api.common.security.authorization;

import kr.co.pennyway.domain.domains.spending.service.SpendingCustomCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("spendingCategoryManager")
@RequiredArgsConstructor
public class SpendingCategoryManager {
    private final SpendingCustomCategoryService spendingCustomCategoryService;

    /**
     * 사용자가 커스텀 지출 카테고리에 대한 권한이 있는지 확인한다. <br>
     * -1L이면 서비스에서 제공하는 기본 카테고리를 사용하는 것이므로 무시한다.
     *
     * @return 권한이 있으면 true, 없으면 false
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, Long categoryId) {
        if (categoryId.equals(-1L)) {
            return true;
        }

        return spendingCustomCategoryService.isExistsSpendingCustomCategory(userId, categoryId);
    }
}
