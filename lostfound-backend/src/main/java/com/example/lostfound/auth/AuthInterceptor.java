package com.example.lostfound.auth;

import com.example.lostfound.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionStore sessionStore;

    public AuthInterceptor(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        String token = extractToken(request.getHeader("Authorization"));
        SessionUser sessionUser = token == null ? null : sessionStore.get(token);
        if (sessionUser != null) {
            AuthContext.set(sessionUser);
        }

        LoginRequired required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), LoginRequired.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), LoginRequired.class);
        }

        if (required == null) {
            return true;
        }

        if (sessionUser == null) {
            throw new BusinessException(401, "Please login first");
        }

        if (required.adminOnly() && !"ADMIN".equals(sessionUser.getRole())) {
            throw new BusinessException(403, "Admin privilege required");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            return authHeader.substring(7);
        }
        return null;
    }
}
