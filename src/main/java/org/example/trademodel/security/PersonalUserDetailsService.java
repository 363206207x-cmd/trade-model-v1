package org.example.trademodel.security;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PersonalUserDetailsService implements UserDetailsService {
    private final PersonalUserMapper personalUserMapper;

    public PersonalUserDetailsService(PersonalUserMapper personalUserMapper) {
        this.personalUserMapper = personalUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = PersonalUsernamePolicy.normalize(username);
        if (!PersonalUsernamePolicy.isValid(normalized)) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        PersonalUserDO user = personalUserMapper.findByUsername(normalized);
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        PersonalUserRole role;
        try {
            role = PersonalUserRole.valueOf(user.getRole());
        } catch (RuntimeException invalidRole) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return new PersonalUserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(), role,
                true, user.getSessionVersion() == null ? 0L : user.getSessionVersion());
    }
}
