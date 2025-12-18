package jojo.jjdc.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.domain.member.Member;
import jojo.jjdc.dto.googlecalendar.SuggestDto;
import jojo.jjdc.domain.notity.Notity;
import jojo.jjdc.domain.notity.NotitySuggestion;
import jojo.jjdc.dto.NotityDto;
import jojo.jjdc.repository.NotityRepository;
import jojo.jjdc.repository.MemberRepository;
import jojo.jjdc.security.jwt.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotityService {

    private final NotityRepository notityRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public NotityDto upsert(Long memberId, String eventId, Instant startAt, Instant endAt, String summary, String content, List<SuggestDto> suggestList) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Notity notity = notityRepository.findByMemberIdAndCalendarEventId(memberId, eventId)
                .orElseGet(() -> new Notity(member, eventId, startAt, endAt, summary, content));
        notity.overwrite(startAt, endAt, summary, content, toSuggestions(suggestList));
        Notity saved = notityRepository.save(notity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<NotityDto> findAll(MemberPrincipal principal) {
        Long memberId = requireMemberId(principal);
        return notityRepository.findAllByMemberIdOrderByUpdatedAtDesc(memberId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(MemberPrincipal principal, Long notityId) {
        Long memberId = requireMemberId(principal);
        Notity notity = notityRepository.findById(notityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "알람 정보를 찾을 수 없습니다."));
        if (!notity.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        notityRepository.delete(notity);
    }

    private List<NotitySuggestion> toSuggestions(List<SuggestDto> suggestList) {
        List<SuggestDto> safeList = suggestList != null ? suggestList : List.of();
        List<NotitySuggestion> results = new ArrayList<>();
        for (SuggestDto dto : safeList) {
            results.add(new NotitySuggestion(
                    dto != null ? dto.suggest() : null,
                    dto != null ? dto.startAt() : null,
                    dto != null ? dto.endAt() : null
            ));
        }
        return results;
    }

    private NotityDto toDto(Notity entity) {
        return new NotityDto(
                entity.getId(),
                entity.getCalendarEventId(),
                entity.getSummary(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getContent(),
                entity.getSuggestions().stream()
                        .map(s -> new SuggestDto(s.getSuggest(), s.getStartAt(), s.getEndAt()))
                        .toList()
        );
    }

    private Long requireMemberId(MemberPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.memberId();
    }
}
