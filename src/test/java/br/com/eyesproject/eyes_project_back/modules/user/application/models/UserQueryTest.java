package br.com.eyesproject.eyes_project_back.modules.user.application.models;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserQueryTest {

    @Test
    void shouldNormalizeSearchAndApplyDefaults() {
        UserQuery query = new UserQuery(0, 20, "  Ana  ", null, null, null, null);
        assertEquals("Ana", query.search());
        assertEquals(UserQuery.SortField.NAME, query.sortBy());
        assertEquals(UserQuery.SortDirection.ASC, query.direction());

        assertNull(new UserQuery(0, 20, "   ", null, null, null, null).search());
    }

    @Test
    void shouldRejectInvalidPageParameters() {
        assertThrows(DomainException.class,
                () -> new UserQuery(-1, 20, null, null, null, null, null));
        assertThrows(DomainException.class,
                () -> new UserQuery(0, 0, null, null, null, null, null));
        assertThrows(DomainException.class,
                () -> new UserQuery(0, 101, null, null, null, null, null));
    }
}
