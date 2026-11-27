package tn.ithelpdesk.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.ithelpdesk.config.SecurityConfig;
import tn.ithelpdesk.model.AppUser;
import tn.ithelpdesk.model.Category;
import tn.ithelpdesk.model.Role;
import tn.ithelpdesk.model.Ticket;
import tn.ithelpdesk.model.TicketStatus;
import tn.ithelpdesk.repository.CategoryRepository;
import tn.ithelpdesk.repository.TicketRepository;
import tn.ithelpdesk.repository.UserRepository;
import tn.ithelpdesk.service.TicketService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@Import(SecurityConfig.class)
class TicketControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean TicketService service;
    @MockitoBean CategoryRepository categories;
    @MockitoBean TicketRepository tickets;
    @MockitoBean UserRepository users; // requis par SecurityConfig

    // ---------- Tableau de bord ----------

    @Test
    void sansConnexionOnEstRenvoyeVersLaPageDeConnexion() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void unUtilisateurVoitSonTableauDeBordSansLesOutilsAdmin() throws Exception {
        when(service.findForUser("testuser", false)).thenReturn(List.of());

        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("isAdmin", false))
                .andExpect(model().attribute("username", "testuser"))
                .andExpect(content().string(not(containsString("/admin/categories"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unAdminVoitLaGestionDesCategories() throws Exception {
        when(service.findForUser("admin", true)).thenReturn(List.of());

        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isAdmin", true))
                .andExpect(content().string(containsString("/admin/categories")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void leTableauDeBordAfficheLesTickets() throws Exception {
        // Si ce test échoue sur l'affichage de la page, c'est un vrai bug du modèle HTML.
        Ticket t = new Ticket("Imprimante en panne", "Plus de papier",
                new AppUser("testuser", "testuser@example.com", "hash", Role.USER),
                new Category("Printers"));
        when(service.findForUser("testuser", false)).thenReturn(List.of(t));

        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Imprimante en panne")))
                .andExpect(content().string(containsString("Annuler")));
    }

    // ---------- Création d'un ticket ----------

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void leFormulaireDeNouveauTicketListeLesCategories() throws Exception {
        when(categories.findAll()).thenReturn(List.of(new Category("Hardware")));

        mvc.perform(get("/tickets/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("ticket-form"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(content().string(containsString("Hardware")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void creerUnTicketValideRedirigeVersLeTableauDeBord() throws Exception {
        mvc.perform(post("/tickets").with(csrf())
                        .param("title", "Ecran noir")
                        .param("description", "Ne s'allume plus")
                        .param("categoryId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?created"));

        verify(service).create("testuser", "Ecran noir", "Ne s'allume plus", 1L);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void creerUnTicketSansJetonCsrfEstRefuse() throws Exception {
        mvc.perform(post("/tickets")
                        .param("title", "Ecran noir")
                        .param("description", "Ne s'allume plus")
                        .param("categoryId", "1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void unTitreVideEstRefuse() throws Exception {
        // Si ce test est rouge (302 au lieu de 400), la validation n'est pas active : voir APP-04.
        mvc.perform(post("/tickets").with(csrf())
                        .param("title", "")
                        .param("description", "Une description")
                        .param("categoryId", "1"))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void unTitreTropLongEstRefuse() throws Exception {
        mvc.perform(post("/tickets").with(csrf())
                        .param("title", "x".repeat(141))
                        .param("description", "Une description")
                        .param("categoryId", "1"))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(), any(), any(), any());
    }

    // ---------- Annulation ----------

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void annulerSonTicket() throws Exception {
        mvc.perform(post("/tickets/5/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(service).cancelOwnTicket(5L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void annulerLeTicketDUnAutreEstInterdit() throws Exception {
        doThrow(new AccessDeniedException("Accès interdit"))
                .when(service).cancelOwnTicket(5L, "testuser");

        mvc.perform(post("/tickets/5/cancel").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ---------- Changement de statut (admin) ----------

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void unSimpleUtilisateurNePeutPasChangerUnStatut() throws Exception {
        mvc.perform(post("/admin/tickets/3/status").with(csrf())
                        .param("status", "RESOLVED"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unAdminPeutChangerLeStatut() throws Exception {
        mvc.perform(post("/admin/tickets/3/status").with(csrf())
                        .param("status", "RESOLVED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?updated"));

        verify(service).updateStatus(3L, TicketStatus.RESOLVED);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unStatutInconnuEstRefuse() throws Exception {
        mvc.perform(post("/admin/tickets/3/status").with(csrf())
                        .param("status", "INCONNU"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}