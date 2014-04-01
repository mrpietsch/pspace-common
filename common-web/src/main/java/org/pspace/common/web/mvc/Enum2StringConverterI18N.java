package org.pspace.common.web.mvc;

import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.converter.Converter;

import java.util.Locale;
import java.util.ResourceBundle;

public class Enum2StringConverterI18N implements Converter<Enum<?>, String> {

    public String getResourceBundleName() {
        return resourceBundleName;
    }

    public void setResourceBundleName(String resourceBundleName) {
        this.resourceBundleName = resourceBundleName;
    }

    private String resourceBundleName = "ApplicationResources";

    public Enum2StringConverterI18N(String resourceBundleName) {
        this.resourceBundleName = resourceBundleName;
    }

    public Enum2StringConverterI18N() {
    }

    @Override
    public String convert(Enum<?> value) {
        String output = value.toString();
        try {
            Locale locale = LocaleContextHolder.getLocale();
            final String key = value.getClass().getCanonicalName() + "." + value.name();
            return ResourceBundle.getBundle(resourceBundleName, locale).getString(key);
        } catch (NoSuchMessageException e) {
            System.err.println("No message resource found for " + value + " add this to the resource bundle");
        }
        return output;
    }
}
