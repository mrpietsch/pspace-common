package org.pspace.common.web.mvc;

import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author peach
 */
public class CollectionToStringTag extends TagSupport {
    private static final long serialVersionUID = 6075275866294377112L;

    private Object collection;
    private String delim = ", ";
    private String prefix = "";
    private String suffix = "";

    @Override
    public int doStartTag() throws JspException {
        try {

            final String code;
            if ( collection instanceof Collection ) {
                code = StringUtils.collectionToDelimitedString((Collection) collection, delim, prefix, suffix);
            } else if (collection instanceof Object[]) {
                Collection<Object> objects = Arrays.asList((Object[]) collection);
                code = StringUtils.collectionToDelimitedString(objects, delim, prefix, suffix);
            } else {
                throw new JspException("Given object is neither a collection nor an array!");
            }

            pageContext.getOut().print(code);
        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }

        // Must return SKIP_BODY because we are not supporting a body for this tag.
        return SKIP_BODY;
    }

    public Object getCollection() {
        return collection;
    }

    public void setCollection(Object collection) {
        this.collection = collection;
    }

    public void setDelim(String delim) {
        this.delim = delim;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}