package tn.ithelpdesk.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.ithelpdesk.model.AppUser;
import tn.ithelpdesk.model.Role;
import tn.ithelpdesk.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void leMotDePasseEstHacheEtVerifiable() {
        PasswordEncoder encoder = config.passwordEncoder();

        String hash = encoder.encode("Secret123!");

        assertNotEquals("Secret123!", hash);
        assertTrue(encoder.matches("Secret123!", hash));
        assertFalse(encoder.matches("MauvaisMotDePasse", hash));
    }

    @Test
    void deuxHachagesDuMemeMotDePasseSontDifferents() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertNotEquals(encoder.encode("Secret123!"), encoder.encode("Secret123!"));
    }

    @Test
    void lUtilisateurChargeAUnRoleSpringSecurity() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", "admin@example.com", "hash", Role.ADMIN)));

        UserDetails details = config.userDetailsService(users).loadUserByUsername("admin");

        assertEquals("admin", details.getUsername());
        assertEquals("hash", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void unUtilisateurInconnuEstRefuse() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsername("fantome")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> config.userDetailsService(users).loadUserByUsername("fantome"));
    }
}