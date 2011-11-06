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
public class AuthByDomainObjectTag extends URLAuthorizingTag {
    private static final long serialVersionUID = -1493357742073187977L;

    private String operation = "READ";
    private Object object;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    @Override
    protected String resolveUrl() {
        EntityOperation entityOperation = EntityOperation.valueOf(operation);
        // try to find mapping url for this entity
        ServletContext servletContext = pageContext.getServletContext();
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        return UrlResolverHelper.resolveUrlByEntity(wac, object, entityOperation);
    }
}
