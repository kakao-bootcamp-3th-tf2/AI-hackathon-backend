package jojo.jjdc.notity.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jojo.jjdc.domain.member.Member;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "calendar_event_id", nullable = false, length = 128)
    private String calendarEventId;

    @Column(length = 255)
    private String summary;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(name = "notity_suggestions", joinColumns = @JoinColumn(name = "notity_id"))
    private List<NotitySuggestion> suggestions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Notity(Member member, String calendarEventId, Instant startAt, Instant endAt, String summary, String content) {
        this.member = member;
        this.calendarEventId = calendarEventId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.summary = summary;
        this.content = content;
    }

    public void overwrite(Instant startAt, Instant endAt, String summary, String content, List<NotitySuggestion> newSuggestions) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.summary = summary;
        this.content = content;
        this.suggestions.clear();
        if (newSuggestions != null) {
            this.suggestions.addAll(newSuggestions);
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
