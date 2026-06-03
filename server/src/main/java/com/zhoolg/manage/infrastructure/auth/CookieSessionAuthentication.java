package com.zhoolg.manage.infrastructure.auth;

import com.zhoolg.manage.entity.base.CurrentUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Objects;
import java.util.stream.Collectors;

public class CookieSessionAuthentication extends AbstractAuthenticationToken {
    private final CurrentUser principal;

    public CookieSessionAuthentication(CurrentUser principal) {
        super(principal == null ? null : principal.permissions().stream()
                .filter(Objects::nonNull)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "cookie-session";
    }

    @Override
    public CurrentUser getPrincipal() {
        return principal;
    }
}
