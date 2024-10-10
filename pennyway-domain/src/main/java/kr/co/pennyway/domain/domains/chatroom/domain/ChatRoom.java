package kr.co.pennyway.domain.domains.chatroom.domain;

import jakarta.persistence.*;
import kr.co.pennyway.domain.common.model.DateAuditable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "chat_room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE chat_room SET deleted_at = NOW() WHERE id = ?")
public class ChatRoom extends DateAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String backgroundImageUrl;
    private Integer password;

    @ColumnDefault("NULL")
    private LocalDateTime deletedAt;

    @Builder
    public ChatRoom(String title, String description, String backgroundImageUrl, Integer password) {
        validate(title, description, password);

        this.title = title;
        this.description = description;
        this.backgroundImageUrl = backgroundImageUrl;
        this.password = password;
    }

    public void update(String title, String description, String backgroundImageUrl, Integer password) {
        validate(title, description, password);

        this.title = title;
        this.description = description;
        this.backgroundImageUrl = backgroundImageUrl;
        this.password = password;
    }

    private void validate(String title, String description, Integer password) {
        if (!StringUtils.hasText(title) || title.length() > 50) {
            throw new IllegalArgumentException("제목은 null이거나 빈 문자열이 될 수 없으며, 50자 이하로 제한됩니다.");
        }

        if (description != null && description.length() > 100) {
            throw new IllegalArgumentException("설명은 null이거나 빈 문자열이 될 수 있으며, 100자 이하로 제한됩니다.");
        }

        if (password != null && password < 0 && password.toString().length() != 6) {
            throw new IllegalArgumentException("비밀번호는 null이거나, 6자리 정수여야 하며, 음수는 허용하지 않습니다.");
        }
    }

    @Override
    public String toString() {
        return "ChatRoom{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", backgroundImageUrl='" + backgroundImageUrl + '\'' +
                ", password=" + password +
                ", deletedAt=" + deletedAt +
                '}';
    }
}