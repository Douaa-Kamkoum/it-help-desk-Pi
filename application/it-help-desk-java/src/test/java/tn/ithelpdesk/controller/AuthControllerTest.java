package tn.ithelpdesk.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.ithelpdesk.config.SecurityConfig;
import tn.ithelpdesk.model.AppUser;
import tn.ithelpdesk.model.Role;
import tn.ithelpdesk.repository.UserRepository;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, PageController.class})
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;

    @MockitoBean UserRepository users;

    // ---------- Pages publiques ----------

    @Test
    void laPageDeConnexionEstAccessibleSansConnexion() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void laPageDeConnexionAfficheLeMessageDErreur() throws Exception {
        mvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Identifiants invalides")));
    }

    @Test
    void laPageDInscriptionContientLeFormulaire() throws Exception {
        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerForm"));
    }

    @Test
    void laRacineRedirigeVersLeTableauDeBord() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    // ---------- Inscription ----------

    @Test
    void uneInscriptionValideCreeUnCompteUserAvecMotDePasseHache() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "nouveau")
                        .param("email", "nouveau@example.com")
                        .param("password", "MotDePasse1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(captor.capture());
        AppUser enregistre = captor.getValue();

        assertEquals("nouveau", enregistre.getUsername());
        assertEquals(Role.USER, enregistre.getRole());
        assertNotEquals("MotDePasse1", enregistre.getPassword());
        assertTrue(encoder.matches("MotDePasse1", enregistre.getPassword()));
    }

    @Test
    void unNomDejaUtiliseEstRefuse() throws Exception {
        when(users.existsByUsername("admin")).thenReturn(true);

        mvc.perform(post("/register").with(csrf())
                        .param("username", "admin")
                        .param("email", "autre@example.com")
                        .param("password", "MotDePasse1"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "username"));

        verify(users, never()).save(any());
    }

    @Test
    void unEmailDejaUtiliseEstRefuse() throws Exception {
        when(users.existsByEmail("pris@example.com")).thenReturn(true);

        mvc.perform(post("/register").with(csrf())
                        .param("username", "nouveau")
                        .param("email", "pris@example.com")
                        .param("password", "MotDePasse1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registerForm", "email"));

        verify(users, never()).save(any());
    }

    @Test
    void desDonneesInvalidesSontRefusees() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "ab")
                        .param("email", "pas-un-email")
                        .param("password", "court"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "username", "email", "password"));

        verify(users, never()).save(any());
    }

    @Test
    void uneInscriptionSansJetonCsrfEstRefusee() throws Exception {
        mvc.perform(post("/register")
                        .param("username", "nouveau")
                        .param("email", "nouveau@example.com")
                        .param("password", "MotDePasse1"))
                .andExpect(status().isForbidden());

        verify(users, never()).save(any());
    }
}