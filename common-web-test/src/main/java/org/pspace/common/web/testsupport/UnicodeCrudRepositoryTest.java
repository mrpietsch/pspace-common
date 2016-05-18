package org.pspace.common.web.testsupport;

import org.junit.Test;
import org.pspace.common.api.ObjectWithID;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public abstract class UnicodeCrudRepositoryTest<E extends ObjectWithID, R extends CrudRepository<E, Long>>
        extends CrudRepositoryTest<E, R> {

    final static private String EMOJI = "\uD83D\uDE03";

    protected abstract List<Setter<E>> unicodeCapableColumns();

    @Test
    public void itShouldBeAbleToStoreUnicodeCharacters() {
        R repository = getRepository();

        for (Setter<E> colSetter : unicodeCapableColumns()) {
            E e = exampleEntity();
            colSetter.manipulateString(e, EMOJI);

            repository.save(e);
            assertThat(e.getId(), notNullValue());

            E reloaded = repository.findOne(e.getId());
            assertThat(reloaded, equalTo(e));
        }

    }

    protected interface Setter<E> {
        void manipulateString(E e, String s);
    }

}
