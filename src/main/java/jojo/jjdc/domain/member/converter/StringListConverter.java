package jojo.jjdc.domain.member.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

/**
 * 단순 문자열 리스트를 하나의 칼럼에 직렬화하기 위한 AttributeConverter 구현체.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = "::";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return new ArrayList<>();
        }
        return Stream.of(dbData.split(DELIMITER))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
