package org.pspace.common.web.mvc;

import org.apache.taglibs.standard.tag.common.core.UrlSupport;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author peach
 */
public class LinkTag extends TagSupport {

    private String title = "";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int doStartTag() throws JspException {
        URLAuthorizingTag parent = (URLAuthorizingTag) findAncestorWithClass(this, URLAuthorizingTag.class);
        printLinkStart(parent, title, pageContext);
        return Tag.EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        URLAuthorizingTag parent = (URLAuthorizingTag) findAncestorWithClass(this, URLAuthorizingTag.class);
        printLinkEnd(parent, pageContext);
        return super.doEndTag();
    }

    public static void printLinkStart(URLAuthorizingTag authorizingTag, String title, PageContext pageContext) {
        try {
            if (authorizingTag.isAuthorized()) {
                String resolvedUrl = UrlSupport.resolveUrl(authorizingTag.getUrl(), null, pageContext);
                HttpServletResponse response = ((HttpServletResponse) pageContext.getResponse());
                pageContext.getOut().println(String.format("<a href=\"%s\" title=\"%s\">", response.encodeURL(resolvedUrl), title));
            }
        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }
    }

    public static void printLinkEnd(URLAuthorizingTag authorizingTag, PageContext pageContext) {
        try {
            if (authorizingTag.isAuthorized()) pageContext.getOut().print("</a>");
        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }

    }
}
