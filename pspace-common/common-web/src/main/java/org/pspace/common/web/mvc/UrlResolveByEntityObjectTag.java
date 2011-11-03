package org.pspace.common.web.mvc;

import org.apache.taglibs.standard.tag.rt.core.UrlTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;

/**
 * @author Martin Pietsch
 */
public class UrlResolveByEntityObjectTag extends UrlTag {

    private static final long serialVersionUID = -1493357742073187977L;

    private static final Logger log = LoggerFactory.getLogger(UrlResolveByEntityObjectTag.class);

    private String operation = "READ";
    private Object object;

    @Override
    public int doEndTag() throws JspException {

        try {

            EntityOperation entityOperation = EntityOperation.valueOf(operation);
            // try to find mapping url for this entity
            ServletContext servletContext = pageContext.getServletContext();
            WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
            String url = UrlResolverHelper.resolveUrlByEntity(wac, object, entityOperation);

            this.setValue(url);
            return super.doEndTag();

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new Error(ex.getMessage());
        }
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
