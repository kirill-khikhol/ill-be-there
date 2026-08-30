package com.illbethere.security;

import com.illbethere.config.AppProperties;
import com.illbethere.domain.AppUser;
import com.illbethere.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenEncryptor tokenEncryptor;
    private final AppProperties properties;
    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClientService;
    private final ObjectProvider<OAuth2AuthorizedClientRepository> authorizedClientRepository;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            TokenEncryptor tokenEncryptor,
            AppProperties properties,
            ObjectProvider<OAuth2AuthorizedClientService> authorizedClientService,
            ObjectProvider<OAuth2AuthorizedClientRepository> authorizedClientRepository) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.tokenEncryptor = tokenEncryptor;
        this.properties = properties;
        this.authorizedClientService = authorizedClientService;
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String sub = principal.getAttribute("sub");
        if (sub == null && principal instanceof OidcUser oidcUser) {
            sub = oidcUser.getSubject();
        }
        if (sub == null || sub.isBlank()) {
            throw new IllegalStateException("Google не вернул идентификатор пользователя");
        }
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        AppUser user = userRepository.findByGoogleSub(sub).orElseGet(AppUser::new);
        user.setGoogleSub(sub);
        user.setEmail(email);
        user.setName(name != null ? name : email);
        user.setAvatarUrl(picture);

        OAuth2AuthorizedClient client = loadAuthorizedClient(authentication, request);
        if (client != null) {
            OAuth2RefreshToken refreshToken = client.getRefreshToken();
            if (refreshToken != null && refreshToken.getTokenValue() != null) {
                user.setEncryptedRefreshToken(tokenEncryptor.encrypt(refreshToken.getTokenValue()));
                log.info("Stored Google refresh token for {}", email);
            } else {
                log.warn("Google did not return a refresh token for {}. Calendar writes will use the short-lived access token.",
                        email);
            }
            OAuth2AccessToken accessToken = client.getAccessToken();
            if (accessToken != null && accessToken.getTokenValue() != null) {
                user.setEncryptedAccessToken(tokenEncryptor.encrypt(accessToken.getTokenValue()));
                user.setAccessTokenExpiresAt(accessToken.getExpiresAt());
            }
        } else {
            log.warn("No OAuth2 authorized client in session for {}. Calendar will not work until re-login.", email);
        }

        userRepository.save(user);
        String jwt = jwtService.createToken(user);
        String target = properties.getFrontendOrigin() + "/auth/callback?token="
                + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        response.sendRedirect(target);
    }

    private OAuth2AuthorizedClient loadAuthorizedClient(Authentication authentication, HttpServletRequest request) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            return null;
        }
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClientRepository repository = authorizedClientRepository.getIfAvailable();
        if (repository != null) {
            OAuth2AuthorizedClient fromRepo = repository.loadAuthorizedClient(registrationId, authentication, request);
            if (fromRepo != null) {
                return fromRepo;
            }
        }
        OAuth2AuthorizedClientService service = authorizedClientService.getIfAvailable();
        if (service == null) {
            return null;
        }
        OAuth2AuthorizedClient byTokenName = service.loadAuthorizedClient(registrationId, oauthToken.getName());
        if (byTokenName != null) {
            return byTokenName;
        }
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return service.loadAuthorizedClient(registrationId, oauth2User.getName());
        }
        return null;
    }
}
