package jojo.jjdc.domain.notity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotitySuggestion {

    @Column(name = "suggest_detail", columnDefinition = "TEXT")
    private String suggest;

    @Column(name = "suggest_start_at")
    private OffsetDateTime startAt;

    @Column(name = "suggest_end_at")
    private OffsetDateTime endAt;

    public NotitySuggestion(String suggest, OffsetDateTime startAt, OffsetDateTime endAt) {
        this.suggest = suggest;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}
