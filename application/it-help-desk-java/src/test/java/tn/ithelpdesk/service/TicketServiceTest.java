package tn.ithelpdesk.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.ithelpdesk.model.*;
import tn.ithelpdesk.repository.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository tickets;
    @Mock UserRepository users;
    @Mock CategoryRepository categories;
    @InjectMocks TicketService service;

    private AppUser alice() {
        return new AppUser("alice", "alice@example.com", "hash", Role.USER);
    }

    private Ticket ticketDe(AppUser auteur) {
        return new Ticket("Imprimante en panne", "Plus de papier", auteur, new Category("Printers"));
    }

    // ---------- findForUser ----------

    @Test
    void unUtilisateurNeVoitQueSesTickets() {
        Ticket t = ticketDe(alice());
        when(tickets.findByUserUsernameOrderByCreatedAtDesc("alice")).thenReturn(List.of(t));

        List<Ticket> resultat = service.findForUser("alice", false);

        assertEquals(List.of(t), resultat);
        verify(tickets, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void unAdminVoitTousLesTickets() {
        Ticket t = ticketDe(alice());
        when(tickets.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(t));

        List<Ticket> resultat = service.findForUser("admin", true);

        assertEquals(List.of(t), resultat);
        verify(tickets, never()).findByUserUsernameOrderByCreatedAtDesc(anyString());
    }

    // ---------- create ----------

    @Test
    void creerUnTicketRogneLesEspacesEtLeRattacheAuBonUtilisateur() {
        AppUser auteur = alice();
        Category categorie = new Category("Hardware");
        when(users.findByUsername("alice")).thenReturn(Optional.of(auteur));
        when(categories.findById(1L)).thenReturn(Optional.of(categorie));
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket cree = service.create("alice", "  Écran noir  ", "  Ne s'allume plus  ", 1L);

        assertEquals("Écran noir", cree.getTitle());
        assertEquals("Ne s'allume plus", cree.getDescription());
        assertSame(auteur, cree.getUser());
        assertSame(categorie, cree.getCategory());
        assertEquals(TicketStatus.NEW, cree.getStatus());
    }

    @Test
    void creerUnTicketPourUnUtilisateurInconnuEchoue() {
        when(users.findByUsername("fantome")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.create("fantome", "Titre", "Description", 1L));
        verify(tickets, never()).save(any());
    }

    @Test
    void creerUnTicketAvecUneCategorieInconnueEchoue() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice()));
        when(categories.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.create("alice", "Titre", "Description", 99L));
        verify(tickets, never()).save(any());
    }

    // ---------- updateStatus ----------

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void leStatutPeutPasserParTousLesEtats(TicketStatus nouveau) {
        Ticket t = ticketDe(alice());
        when(tickets.findById(7L)).thenReturn(Optional.of(t));

        service.updateStatus(7L, nouveau);

        assertEquals(nouveau, t.getStatus());
    }

    @Test
    void changerLeStatutDUnTicketInconnuEchoue() {
        when(tickets.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.updateStatus(404L, TicketStatus.RESOLVED));
    }

    // ---------- cancelOwnTicket ----------

    @Test
    void unUtilisateurPeutAnnulerSonTicketNeuf() {
        Ticket t = ticketDe(alice());
        when(tickets.findById(7L)).thenReturn(Optional.of(t));

        service.cancelOwnTicket(7L, "alice");

        assertEquals(TicketStatus.CANCELLED, t.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"IN_PROGRESS", "RESOLVED", "CANCELLED"})
    void unTicketDejaTraiteNEstPasAnnule(TicketStatus statut) {
        Ticket t = ticketDe(alice());
        t.setStatus(statut);
        when(tickets.findById(7L)).thenReturn(Optional.of(t));

        service.cancelOwnTicket(7L, "alice");

        assertEquals(statut, t.getStatus());
    }

    @Test
    void onNePeutPasAnnulerLeTicketDUnAutreUtilisateur() {
        Ticket t = ticketDe(alice());
        when(tickets.findById(7L)).thenReturn(Optional.of(t));

        assertThrows(AccessDeniedException.class, () -> service.cancelOwnTicket(7L, "bob"));
        assertEquals(TicketStatus.NEW, t.getStatus());
    }

    @Test
    void annulerUnTicketInconnuEchoue() {
        when(tickets.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.cancelOwnTicket(404L, "alice"));
    }
}