package org.pspace.common.web.mvc;

import org.apache.taglibs.standard.tag.common.core.UrlSupport;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 * @author peach
 */
public class UrlTag extends TagSupport {

    @Override
    public int doStartTag() throws JspException {
        try {
            URLAuthorizingTag parent = (URLAuthorizingTag) findAncestorWithClass(this, URLAuthorizingTag.class);
            String resolvedUrl = UrlSupport.resolveUrl(parent.getUrl(), null, pageContext);
            pageContext.getOut().print(resolvedUrl);
            return Tag.EVAL_BODY_INCLUDE;
        } catch (IOException e) {
            throw new Error(e);
        }
    }


}
