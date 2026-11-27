package tn.ithelpdesk.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.ithelpdesk.config.SecurityConfig;
import tn.ithelpdesk.model.Category;
import tn.ithelpdesk.repository.CategoryRepository;
import tn.ithelpdesk.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean CategoryRepository categories;
    @MockitoBean UserRepository users; // requis par SecurityConfig

    // ---------- Contrôle d'accès ----------

    @Test
    void sansConnexionOnEstRenvoyeVersLaConnexion() throws Exception {
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "Reseau"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(categories);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void unSimpleUtilisateurNePeutPasGererLesCategories() throws Exception {
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "Reseau"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categories);
    }

    // ---------- Ajout d'une catégorie ----------

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unAdminAjouteUneCategorieSansEspacesParasites() throws Exception {
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "  Reseau  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categories).save(captor.capture());
        assertEquals("Reseau", captor.getValue().getName());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void uneCategorieDejaExistanteNEstPasAjoutee() throws Exception {
        when(categories.existsByNameIgnoreCase("Hardware")).thenReturn(true);

        mvc.perform(post("/admin/categories").with(csrf()).param("name", "Hardware"))
                .andExpect(redirectedUrl("/dashboard"));

        verify(categories, never()).save(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void uneCategorieVideNEstPasAjoutee() throws Exception {
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "   "))
                .andExpect(redirectedUrl("/dashboard"));

        verify(categories, never()).save(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void uneCategorieTropLongueNEstPasAjoutee() throws Exception {
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "x".repeat(81)))
                .andExpect(redirectedUrl("/dashboard"));

        verify(categories, never()).save(any());
    }

    // ---------- Suppression ----------

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unAdminSupprimeUneCategorie() throws Exception {
        mvc.perform(post("/admin/categories/3/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(categories).deleteById(3L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void supprimerUneCategorieUtiliseeNeCassePasLaPage() throws Exception {
        // Comportement actuel : l'erreur est ignorée en silence (amélioration possible :
        // afficher un message à l'administrateur).
        doThrow(new DataIntegrityViolationException("catégorie liée à un ticket"))
                .when(categories).deleteById(3L);

        mvc.perform(post("/admin/categories/3/delete").with(csrf()))
                .andExpect(redirectedUrl("/dashboard"));
    }
}