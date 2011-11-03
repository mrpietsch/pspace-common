package org.pspace.common.web.mvc;

import org.apache.taglibs.standard.tag.rt.core.UrlTag;

import javax.servlet.jsp.JspException;

/**
 * @author peach
 */
public class UrlResolveByControllerMethodTag extends UrlTag {
    private static final long serialVersionUID = -1493357742073187977L;

    private String controller;
    private String methodName;
    private Object object;

    @Override
    public int doEndTag() throws JspException {

        try {
            assert controller != null && methodName != null;
            Class<?> controllerClass = Class.forName(controller);
            String url = UrlResolverHelper.resolveUrlByControllerMethod(controllerClass, methodName, object);
            this.setValue(url);
            return super.doEndTag();
        } catch (Exception ex) {
            throw new Error(ex.getMessage());
        }
    }


    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
