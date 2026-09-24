package br.com.eyesproject.eyes_project_back.modules.audit.application.models;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditQueryTest {

    @Test
    void normalizesOptionalActorAndAcceptsValidPeriod() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 30, 23, 59);

        String actorUserId = UUID.randomUUID().toString();
        AuditQuery query = new AuditQuery(0, 20, "  " + actorUserId + "  ", null, null, from, to);

        assertEquals(actorUserId, query.actorUserId());
        assertEquals(from, query.occurredFrom());
        assertEquals(to, query.occurredTo());
        assertNull(new AuditQuery(0, 20, "  ", null, null, null, null).actorUserId());
    }

    @Test
    void rejectsInvalidPaginationAndInvertedPeriod() {
        assertThrows(DomainException.class,
                () -> new AuditQuery(-1, 20, null, null, null, null, null));
        assertThrows(DomainException.class,
                () -> new AuditQuery(0, 0, null, null, null, null, null));
        assertThrows(DomainException.class,
                () -> new AuditQuery(0, 101, null, null, null, null, null));
        assertThrows(DomainException.class,
                () -> new AuditQuery(0, 20, "invalid-actor", null, null, null, null));
        LocalDateTime invertedStart = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime invertedEnd = LocalDateTime.of(2026, 9, 1, 0, 0);
        assertThrows(DomainException.class, () -> new AuditQuery(
                0,
                20,
                null,
                null,
                null,
                invertedStart,
                invertedEnd
        ));
    }
}
