package tn.ithelpdesk.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.ithelpdesk.model.AppUser;
import tn.ithelpdesk.model.Category;
import tn.ithelpdesk.model.Role;
import tn.ithelpdesk.repository.CategoryRepository;
import tn.ithelpdesk.repository.UserRepository;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DataInitializerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void creeLesComptesEtLesCategoriesAuPremierDemarrage() throws Exception {
        when(encoder.encode(anyString())).thenAnswer(inv -> "HASH(" + inv.getArgument(0) + ")");

        new DataInitializer().seed(users, categories, encoder).run();

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(users, times(2)).save(captor.capture());
        Map<String, AppUser> parNom = captor.getAllValues().stream()
                .collect(Collectors.toMap(AppUser::getUsername, u -> u));

        assertEquals(Role.ADMIN, parNom.get("admin").getRole());
        assertEquals(Role.USER, parNom.get("testuser").getRole());
        // Les mots de passe ne sont jamais enregistrés en clair
        parNom.values().forEach(u -> assertTrue(u.getPassword().startsWith("HASH(")));

        verify(categories, times(5)).save(any(Category.class));
    }

    @Test
    void neRecreeRienSiLesDonneesExistentDeja() throws Exception {
        when(users.existsByUsername(anyString())).thenReturn(true);
        when(categories.existsByNameIgnoreCase(anyString())).thenReturn(true);

        new DataInitializer().seed(users, categories, encoder).run();

        verify(users, never()).save(any());
        verify(categories, never()).save(any());
    }
}