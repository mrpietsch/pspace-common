package org.pspace.common.web.mvc;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author peach
 */
public class EmailObfuscaterTag extends TagSupport {
    private static final long serialVersionUID = 6075275866294377112L;

    private String email;
    private String linkText = null;

    @Override
    public int doStartTag() throws JspException {
        try {
            String code = EmailAddressObfuscater.enkode_mail(email, linkText !=null ? linkText : email);
            pageContext.getOut().print(code);
        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }

        // Must return SKIP_BODY because we are not supporting a body for this tag.
        return SKIP_BODY;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

}
