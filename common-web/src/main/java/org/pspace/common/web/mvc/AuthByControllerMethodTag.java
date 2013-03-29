package org.pspace.common.web.mvc;

/**
 * @author peach
 */
public class AuthByControllerMethodTag extends URLAuthorizingTag {
    private static final long serialVersionUID = -1493357742073187977L;

    private String controller;
    private String methodName;
    private Object object;

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

    @Override
    protected String resolveUrl() throws ClassNotFoundException {
        assert controller != null && methodName != null;
        Class<?> controllerClass = Class.forName(controller);
        return UrlResolverHelper.resolveUrlByControllerMethod(controllerClass, methodName, object);
    }
}
