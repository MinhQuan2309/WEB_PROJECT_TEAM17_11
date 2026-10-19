package com.utetra.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sau khi dang nhap, day moi vai ve dung khu vuc cua minh.
 */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String target = "/";
        if (roles.contains("ROLE_ADMIN")) {
            target = "/admin";
        } else if (roles.contains("ROLE_MANAGER")) {
            target = "/manager";
        } else if (roles.contains("ROLE_VENDOR")) {
            target = "/vendor";
        } else if (roles.contains("ROLE_SHIPPER")) {
            target = "/shipper";
        }

        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
