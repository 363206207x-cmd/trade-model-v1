package org.example.trademodel.security;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserIdResolverTest {
    @Mock
    private PersonalUserMapper personalUserMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesNormalizedUsernameToCanonicalUserId() {
        PersonalUserDO canonical = canonicalUser(41L, "owner.a");
        when(personalUserMapper.findByUsername("owner.a")).thenReturn(canonical);
        authenticate(User.withUsername(" Owner.A ").password("ignored").roles("OPERATOR").build());

        Long userId = new AuthenticatedUserIdResolver(personalUserMapper).requireCurrentUserId();

        assertThat(userId).isEqualTo(41L);
        verify(personalUserMapper).findByUsername("owner.a");
    }

    @Test
    void missingAuthenticationFailsClosedWithoutUserLookup() {
        assertResolutionFails();
        verify(personalUserMapper, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void anonymousAuthenticationFailsClosedWithoutUserLookup() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertResolutionFails();
        verify(personalUserMapper, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void unexpectedPrincipalTypeFailsClosed() {
        authenticate("owner.a");

        assertResolutionFails();
        verify(personalUserMapper, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void invalidUsernameFailsClosedWithoutCanonicalLookup() {
        authenticate(User.withUsername("owner A").password("ignored").roles("OPERATOR").build());

        assertResolutionFails();
        verify(personalUserMapper, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void unknownCanonicalUsernameFailsClosedWithoutCreatingUser() {
        when(personalUserMapper.findByUsername("missing.user")).thenReturn(null);
        authenticate(User.withUsername("missing.user").password("ignored").roles("OPERATOR").build());

        assertResolutionFails();

        verify(personalUserMapper).findByUsername("missing.user");
        verify(personalUserMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidCanonicalIdFailsClosed() {
        when(personalUserMapper.findByUsername("owner.a")).thenReturn(canonicalUser(null, "owner.a"));
        authenticate(User.withUsername("owner.a").password("ignored").roles("OPERATOR").build());

        assertResolutionFails();
    }

    private void assertResolutionFails() {
        assertThatThrownBy(() -> new AuthenticatedUserIdResolver(personalUserMapper).requireCurrentUserId())
                .isInstanceOf(AuthenticatedUserResolutionException.class)
                .hasMessage("authentication required");
    }

    private static void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, "ignored", List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))));
    }

    private static PersonalUserDO canonicalUser(Long id, String username) {
        PersonalUserDO user = new PersonalUserDO();
        user.setId(id);
        user.setUsername(username);
        user.setEnabled(true);
        return user;
    }
}
