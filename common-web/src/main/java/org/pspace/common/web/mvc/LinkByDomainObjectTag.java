package org.pspace.common.web.mvc;

import javax.servlet.jsp.JspException;

/**
 * @author peach
 * @deprecated use spring-hateoas
 */
@Deprecated
public class LinkByDomainObjectTag extends AuthByDomainObjectTag {
    private static final long serialVersionUID = -1493357742073187977L;

    private String title = "";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int doStartTag() throws JspException {

        try {
            int superResult = super.doStartTag();
            LinkTag.printLinkStart(this, title, pageContext);
            return superResult;
        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            LinkTag.printLinkEnd(this, pageContext);
        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }
        return super.doEndTag();
    }
}
