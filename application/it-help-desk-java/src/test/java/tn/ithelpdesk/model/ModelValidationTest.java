package tn.ithelpdesk.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ModelValidationTest {

    private static Validator validator;

    @BeforeAll
    static void initialiserLeValidateur() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<String> champsEnErreur(Object objet) {
        return validator.validate(objet).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private AppUser utilisateur() {
        return new AppUser("alice", "alice@example.com", "hash", Role.USER);
    }

    @Test
    void unUtilisateurValideNAAucuneErreur() {
        assertTrue(champsEnErreur(utilisateur()).isEmpty());
    }

    @Test
    void unNomDUtilisateurVideEstRefuse() {
        AppUser u = utilisateur();
        u.setUsername("   ");
        assertTrue(champsEnErreur(u).contains("username"));
    }

    @Test
    void unEmailInvalideEstRefuse() {
        AppUser u = utilisateur();
        u.setEmail("pas-un-email");
        assertTrue(champsEnErreur(u).contains("email"));
    }

    @Test
    void unTicketValideNAAucuneErreur() {
        Ticket t = new Ticket("Titre", "Description", utilisateur(), new Category("Hardware"));
        assertTrue(champsEnErreur(t).isEmpty());
    }

    @Test
    void unTicketSansTitreEstRefuse() {
        Ticket t = new Ticket("  ", "Description", utilisateur(), new Category("Hardware"));
        assertTrue(champsEnErreur(t).contains("title"));
    }

    @Test
    void unTicketSansDescriptionEstRefuse() {
        Ticket t = new Ticket("Titre", "", utilisateur(), new Category("Hardware"));
        assertTrue(champsEnErreur(t).contains("description"));
    }

    @Test
    void uneCategorieSansNomEstRefusee() {
        assertTrue(champsEnErreur(new Category(" ")).contains("name"));
    }

    @Test
    void valeursParDefaut() {
        assertEquals(Role.USER, new AppUser().getRole());

        Ticket t = new Ticket("Titre", "Description", utilisateur(), new Category("Hardware"));
        assertEquals(TicketStatus.NEW, t.getStatus());
        assertNotNull(t.getCreatedAt());
    }
}