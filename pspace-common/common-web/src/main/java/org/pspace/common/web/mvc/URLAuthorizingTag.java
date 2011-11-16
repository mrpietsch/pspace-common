package org.pspace.common.web.mvc;

import org.springframework.security.taglibs.authz.AuthorizeTag;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

/**
 * @author mrpietsch
 */
public abstract class URLAuthorizingTag extends AuthorizeTag {

    private String myurl = null;

    private boolean isAuthorized;

    protected abstract String resolveUrl() throws ClassNotFoundException;

    @Override
    public int doStartTag() throws JspException {

        try {
            myurl = resolveUrl();
            this.setUrl(myurl);
            int superResult = super.doStartTag();
            if (superResult == Tag.EVAL_BODY_INCLUDE) {
                isAuthorized = true;
            }
            return superResult;

        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }
    }


    public String getUrl() {
        return myurl;
    }

    protected boolean isAuthorized() {
        return isAuthorized;
    }

}
