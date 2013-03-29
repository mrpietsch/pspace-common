package org.pspace.common.web.mvc;

import org.pspace.common.api.ObjectWithID;
import org.pspace.common.api.GenericManager;
import org.springframework.format.Formatter;

import java.beans.PropertyEditorSupport;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Locale;

/**
 * @author Martin Pietsch
 */
public class ObjectWithIdEditor<T extends ObjectWithID<? extends Serializable>> extends PropertyEditorSupport implements Formatter<T> {

    /* fields */

    // protected final static Log log = LogFactory.getLog(ObjectWithIdEditor.class);

    private final GenericManager<T, Serializable> manager;

    public ObjectWithIdEditor(GenericManager<T, Serializable> manager) {
        assert manager != null;
        this.manager = manager;
    }

    /* implementation */

    @Override
    @SuppressWarnings("unchecked")
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setValue(parse(text, Locale.getDefault()));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }

    }

    @Override
    public T parse(String text, Locale locale) throws ParseException {
        Long id = Long.parseLong(text);
        return this.manager.get(id);
    }

    @Override
    public String print(T object, Locale locale) {
        return object.getId().toString();
    }
}
