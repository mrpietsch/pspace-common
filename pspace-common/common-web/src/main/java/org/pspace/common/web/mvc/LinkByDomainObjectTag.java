package org.pspace.common.web.mvc;

import org.apache.taglibs.standard.tag.common.core.UrlSupport;
import org.springframework.security.taglibs.authz.AuthorizeTag;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.Tag;

/**
 * @author peach
 */
public class LinkByDomainObjectTag extends AuthByDomainObjectTag {
    private static final long serialVersionUID = -1493357742073187977L;

    @Override
    public int doStartTag() throws JspException {

        try {
            int superResult = super.doStartTag();

            if (isAuthorized()) {
                String resolvedUrl = UrlSupport.resolveUrl(this.getUrl(), null, pageContext);
                HttpServletResponse response = ((HttpServletResponse) pageContext.getResponse());
                JspWriter out = pageContext.getOut();
                out.println(String.format("<a href=\"%s\">", response.encodeURL(resolvedUrl)));
            }

            return superResult;

        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            if (this.isAuthorized()) pageContext.getOut().print("</a>");
        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }
        return super.doEndTag();
    }
}
