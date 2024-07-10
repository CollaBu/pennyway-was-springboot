package kr.co.pennyway.domain.domains.spending.service;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import kr.co.pennyway.common.annotation.DomainService;
import kr.co.pennyway.domain.common.repository.QueryHandler;
import kr.co.pennyway.domain.common.util.SliceUtil;
import kr.co.pennyway.domain.domains.spending.domain.QSpending;
import kr.co.pennyway.domain.domains.spending.domain.QSpendingCustomCategory;
import kr.co.pennyway.domain.domains.spending.domain.Spending;
import kr.co.pennyway.domain.domains.spending.dto.TotalSpendingAmount;
import kr.co.pennyway.domain.domains.spending.repository.SpendingRepository;
import kr.co.pennyway.domain.domains.spending.type.SpendingCategory;
import kr.co.pennyway.domain.domains.user.domain.QUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@DomainService
@RequiredArgsConstructor
public class SpendingService {
    private final SpendingRepository spendingRepository;

    private final QUser user = QUser.user;
    private final QSpending spending = QSpending.spending;
    private final QSpendingCustomCategory spendingCustomCategory = QSpendingCustomCategory.spendingCustomCategory;

    @Transactional
    public Spending createSpending(Spending spending) {
        return spendingRepository.save(spending);
    }

    @Transactional(readOnly = true)
    public Optional<Spending> readSpending(Long spendingId) {
        return spendingRepository.findById(spendingId);
    }

    @Transactional(readOnly = true)
    public Optional<TotalSpendingAmount> readTotalSpendingAmountByUserId(Long userId, LocalDate date) {
        return spendingRepository.findTotalSpendingAmountByUserId(userId, date.getYear(), date.getMonthValue());
    }

    @Transactional(readOnly = true)
    public List<Spending> readSpendings(Long userId, int year, int month) {
        return spendingRepository.findByYearAndMonth(userId, year, month);
    }

    @Transactional(readOnly = true)
    public int readSpendingTotalCountByCategoryId(Long userId, Long categoryId) {
        return spendingRepository.countByUser_IdAndSpendingCustomCategory_Id(userId, categoryId);
    }

    @Transactional(readOnly = true)
    public int readSpendingTotalCountByCategory(Long userId, SpendingCategory spendingCategory) {
        return spendingRepository.countByUser_IdAndCategory(userId, spendingCategory);
    }

    /**
     * 사용자 정의 카테고리 ID로 지출 내역 리스트를 조회한다.
     *
     * @return 지출 내역 리스트를 {@link Slice}에 담아서 반환한다.
     */
    @Transactional(readOnly = true)
    public Slice<Spending> readSpendingsSliceByCategoryId(Long userId, Long categoryId, Pageable pageable) {
        Predicate predicate = spending.user.id.eq(userId).and(spendingCustomCategory.id.eq(categoryId));

        QueryHandler queryHandler = query -> query
                .leftJoin(spending.spendingCustomCategory, spendingCustomCategory).fetchJoin()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1);

        Sort sort = pageable.getSort();

        return SliceUtil.toSlice(spendingRepository.findList(predicate, queryHandler, sort), pageable);
    }

    /**
     * 시스템 제공 카테고리 code로 지출 내역 리스트를 조회한다.
     *
     * @return 지출 내역 리스트를 {@link Slice}에 담아서 반환한다.
     */
    @Transactional(readOnly = true)
    public Slice<Spending> readSpendingsSliceByCategory(Long userId, SpendingCategory spendingCategory, Pageable pageable) {
        if (spendingCategory.equals(SpendingCategory.CUSTOM) || spendingCategory.equals(SpendingCategory.OTHER)) {
            throw new IllegalArgumentException("지출 카테고리가 시스템 제공 카테고리가 아닙니다.");
        }

        Predicate predicate = spending.user.id.eq(userId).and(spending.category.eq(spendingCategory));

        QueryHandler queryHandler = query -> query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1);

        Sort sort = pageable.getSort();

        return SliceUtil.toSlice(spendingRepository.findList(predicate, queryHandler, sort), pageable);
    }

    @Transactional(readOnly = true)
    public List<TotalSpendingAmount> readTotalSpendingsAmountByUserId(Long userId) {
        Predicate predicate = user.id.eq(userId);

        QueryHandler queryHandler = query -> query.leftJoin(spending).on(user.id.eq(spending.user.id))
                .groupBy(spending.spendAt.year(), spending.spendAt.month());

        Sort sort = Sort.by(Sort.Order.desc("year(spendAt)"), Sort.Order.desc("month(spendAt)"));

        Map<String, Expression<?>> bindings = new LinkedHashMap<>();
        bindings.put("year", spending.spendAt.year());
        bindings.put("month", spending.spendAt.month());
        bindings.put("totalSpending", spending.amount.sum());

        return spendingRepository.selectList(predicate, TotalSpendingAmount.class, bindings, queryHandler, sort);
    }

    @Transactional
    public void deleteSpendingsByCategoryIdInQuery(Long categoryId) {
        spendingRepository.deleteAllByCategoryIdAndDeletedAtNullInQuery(categoryId);
    }

    @Transactional(readOnly = true)
    public boolean isExistsSpending(Long userId, Long spendingId) {
        return spendingRepository.existsByIdAndUser_Id(spendingId, userId);
    }

    @Transactional
    public void deleteSpending(Spending spending) {
        spendingRepository.delete(spending);
    }

    @Transactional
    public void deleteSpendingsInQuery(List<Long> spendingIds) {
        spendingRepository.deleteAllByIdAndDeletedAtNullInQuery(spendingIds);
    }

    @Transactional(readOnly = true)
    public long countByUserIdAndIdIn(Long userId, List<Long> spendingIds) {
        return spendingRepository.countByUserIdAndIdIn(userId, spendingIds);
    }

    @Transactional
    public void migrateSpendingsByCategoryId(Long fromCategoryId, Long toCategoryId) {
        spendingRepository.updateSpendingCustomCategoryInQuery(fromCategoryId, toCategoryId);
    }

    @Transactional
    public void migrateSpendingsByCategory(Long fromCategoryId, SpendingCategory toCategory) {
        spendingRepository.updateCategoryInQuery(fromCategoryId, toCategory);
    }
}
