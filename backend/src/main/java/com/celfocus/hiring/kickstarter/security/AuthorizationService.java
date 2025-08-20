package com.celfocus.hiring.kickstarter.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationService {

    public boolean isCartOwner(String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            UserDetails principal = (UserDetails) auth.getPrincipal();
            if (principal != null && principal.getUsername().equals(username))
                return true;
        }
        return false;
    }


}
