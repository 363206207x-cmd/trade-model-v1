package org.example.trademodel.security;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserIdResolver {
    private final PersonalUserMapper personalUserMapper;

    public AuthenticatedUserIdResolver(PersonalUserMapper personalUserMapper) {
        this.personalUserMapper = personalUserMapper;
    }

    public Long requireCurrentUserId() {
        return requireCurrentUser().getId();
    }

    public PersonalUserDO requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof UserDetails details)) {
            throw new AuthenticatedUserResolutionException();
        }
        String username = PersonalUsernamePolicy.normalize(details.getUsername());
        if (!PersonalUsernamePolicy.isValid(username)) {
            throw new AuthenticatedUserResolutionException();
        }
        PersonalUserDO user = personalUserMapper.findByUsername(username);
        if (user == null || user.getId() == null || user.getId() <= 0) {
            throw new AuthenticatedUserResolutionException();
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new AuthenticatedUserResolutionException();
        }
        return user;
    }

    public PersonalUserDO requireOwner() {
        PersonalUserDO user = requireCurrentUser();
        if (!"OWNER".equals(user.getRole())) {
            throw new AuthenticatedUserResolutionException();
        }
        return user;
    }
}
