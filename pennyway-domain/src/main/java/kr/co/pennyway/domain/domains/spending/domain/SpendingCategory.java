package kr.co.pennyway.domain.domains.spending.domain;

import jakarta.persistence.*;
import kr.co.pennyway.domain.common.model.DateAuditable;
import kr.co.pennyway.domain.domains.spending.type.SpendingIcon;
import kr.co.pennyway.domain.domains.user.domain.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "spending_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE spending_category SET deleted_at = NOW() WHERE id = ?")
public class SpendingCategory extends DateAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private SpendingIcon icon;
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private SpendingCategory(String name, SpendingIcon icon, User user) {
        this.name = name;
        this.icon = icon;
        this.user = user;
    }

    public static SpendingCategory of(String name, SpendingIcon icon, User user) {
        if (icon.equals(SpendingIcon.OTHER))
            throw new IllegalArgumentException("OTHER 아이콘은 커스텀 카테고리의 icon으로 사용할 수 없습니다.");
        return new SpendingCategory(name, icon, user);
    }
}
