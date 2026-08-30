package com.illbethere.security;

import com.illbethere.config.AppProperties;
import com.illbethere.domain.AppUser;
import com.illbethere.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenEncryptor tokenEncryptor;
    private final AppProperties properties;
    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClientService;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            TokenEncryptor tokenEncryptor,
            AppProperties properties,
            ObjectProvider<OAuth2AuthorizedClientService> authorizedClientService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.tokenEncryptor = tokenEncryptor;
        this.properties = properties;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String sub = principal.getAttribute("sub");
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        AppUser user = userRepository.findByGoogleSub(sub).orElseGet(AppUser::new);
        user.setGoogleSub(sub);
        user.setEmail(email);
        user.setName(name != null ? name : email);
        user.setAvatarUrl(picture);

        OAuth2AuthorizedClientService clients = authorizedClientService.getIfAvailable();
        if (clients != null && authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2AuthorizedClient client = clients.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
            if (client != null) {
                OAuth2RefreshToken refreshToken = client.getRefreshToken();
                if (refreshToken != null && refreshToken.getTokenValue() != null) {
                    user.setEncryptedRefreshToken(tokenEncryptor.encrypt(refreshToken.getTokenValue()));
                }
            }
        }

        userRepository.save(user);
        String jwt = jwtService.createToken(user);
        String target = properties.getFrontendOrigin() + "/auth/callback?token="
                + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        response.sendRedirect(target);
    }
}
