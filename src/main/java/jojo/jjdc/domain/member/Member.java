package jojo.jjdc.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jojo.jjdc.domain.member.converter.StringListConverter;

@Getter
@Entity
@Table(name = "members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_provider_provider_id", columnNames = {"provider", "provider_id"})
})
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 128)
    private String providerId;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberStatus status = MemberStatus.PENDING;

    @Column(name = "telecom", length = 30)
    private String telecom;

    @Convert(converter = StringListConverter.class)
    @Column(name = "payments", columnDefinition = "text")
    private List<String> payments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Member(OAuthProvider provider, String providerId, String email) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.status = MemberStatus.PENDING;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateProfile(String telecom, List<String> payments, MemberStatus nextStatus) {
        this.telecom = telecom;
        this.payments = payments != null ? new ArrayList<>(payments) : new ArrayList<>();
        this.status = nextStatus;
    }

    public void updateStatus(MemberStatus nextStatus) {
        this.status = nextStatus;
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
