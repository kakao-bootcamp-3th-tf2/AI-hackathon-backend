package jojo.jjdc.security.oauth;

import java.util.Map;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;

public record GoogleOAuth2User(String email, String providerId) {

    public static GoogleOAuth2User fromAttributes(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String sub = (String) attributes.get("sub");
        if (email == null || sub == null) {
            throw new BusinessException(ErrorCode.OAUTH_CLIENT_NOT_FOUND, "구글 프로필 정보 부족");
        }
        return new GoogleOAuth2User(email, sub);
    }
}
