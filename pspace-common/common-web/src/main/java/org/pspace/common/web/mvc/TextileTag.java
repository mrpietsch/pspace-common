package org.pspace.common.web.mvc;

import net.sf.textile4j.Textile;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author peach
 */
public class TextileTag extends TagSupport {
    private static final long serialVersionUID = 6075275866294377112L;

    private static final Textile textile = new Textile();

    private Object value;
    private Boolean enclosingParagraph = false;

    @Override
    public int doStartTag() throws JspException {
        try {
            String code = textile.process(value.toString());

            if (!(enclosingParagraph || code.isEmpty())) code = code.substring(4, code.length() - 4);

            pageContext.getOut().print(code);
        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }

        // Must return SKIP_BODY because we are not supporting a body for this tag.
        return SKIP_BODY;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setEnclosingParagraph(Boolean enclosingParagraph) {
        this.enclosingParagraph = enclosingParagraph;
    }
}
