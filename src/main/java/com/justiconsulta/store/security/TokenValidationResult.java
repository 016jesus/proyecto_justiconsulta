package com.justiconsulta.store.security;

import java.util.Collections;
import java.util.List;

public class TokenValidationResult {
    private final boolean valid;
    private final String principal; // documentNumber or email or sub
    private final List<String> authorities;

    public TokenValidationResult(boolean valid, String principal, List<String> authorities) {
        this.valid = valid;
        this.principal = principal;
        this.authorities = authorities == null ? Collections.emptyList() : authorities;
    }

    public boolean isValid() {
        return valid;
    }

    public String getPrincipal() {
        return principal;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public static TokenValidationResult invalid() {
        return new TokenValidationResult(false, null, Collections.emptyList());
    }
}

