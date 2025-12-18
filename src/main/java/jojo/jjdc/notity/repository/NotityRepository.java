package jojo.jjdc.notity.repository;

import java.util.List;
import java.util.Optional;
import jojo.jjdc.notity.domain.Notity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotityRepository extends JpaRepository<Notity, Long> {
    List<Notity> findAllByMemberIdOrderByUpdatedAtDesc(Long memberId);
    Optional<Notity> findByMemberIdAndCalendarEventId(Long memberId, String calendarEventId);
}
