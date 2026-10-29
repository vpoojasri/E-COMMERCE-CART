package com.ecommerce.util;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.exception.ForbiddenException;
import com.ecommerce.exception.UnauthorizedException;
import jakarta.servlet.http.HttpSession;

public final class SessionAuthUtil {

    public static final String SESSION_USER_ID = "AUTH_USER_ID";
    public static final String SESSION_USER_ROLE = "AUTH_USER_ROLE";
    public static final String SESSION_USER_EMAIL = "AUTH_USER_EMAIL";

    private SessionAuthUtil() {}

    public static void setAuthenticatedUser(HttpSession session, User user) {
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_USER_ROLE, user.getRole().name());
        session.setAttribute(SESSION_USER_EMAIL, user.getEmail());
    }

    public static Long requireUserId(HttpSession session) {
        if (session == null) {
            throw new UnauthorizedException("Authentication is required to access this resource.");
        }
        Object uid = session.getAttribute(SESSION_USER_ID);
        if (!(uid instanceof Long)) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        return (Long) uid;
    }

    public static Role requireRole(HttpSession session) {
        requireUserId(session);
        Object roleStr = session.getAttribute(SESSION_USER_ROLE);
        if (roleStr == null) {
            throw new UnauthorizedException("User role not found in session.");
        }
        return Role.valueOf(roleStr.toString());
    }

    public static void requireAdmin(HttpSession session) {
        Role role = requireRole(session);
        if (role != Role.ADMIN) {
            throw new ForbiddenException("Administrator privileges are required to perform this action.");
        }
    }

    public static String getSessionEmail(HttpSession session) {
        if (session == null) return "System";
        Object email = session.getAttribute(SESSION_USER_EMAIL);
        return email != null ? email.toString() : "System";
    }
}

