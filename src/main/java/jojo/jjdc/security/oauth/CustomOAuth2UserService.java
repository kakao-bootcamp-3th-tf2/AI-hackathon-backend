package jojo.jjdc.security.oauth;

import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User delegate = super.loadUser(userRequest);
        Map<String, Object> attributes = delegate.getAttributes();
        GoogleOAuth2User.fromAttributes(attributes); // fail fast for invalid payload
        return delegate;
    }
}
