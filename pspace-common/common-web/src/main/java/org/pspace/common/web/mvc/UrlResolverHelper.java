package org.pspace.common.web.mvc;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.support.HandlerMethodResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author peach
 */
public class UrlResolverHelper {

    private final static Pattern PATTERN = Pattern.compile("\\{(\\w+)\\}");

    public static String resolveUrlByControllerMethod(Class<?> controllerClass, String methodName, Object o) {
        for (Method m : controllerClass.getMethods()) {
            if (m.getName().equals(methodName)) {
                // find take the first one as default
                String mapping = getMappingByControllerAndMethod(controllerClass, m);
                return replaceProperties(mapping, m, o);
            }
        }
        return null;
    }

    public static String resolveUrlByEntity(ApplicationContext wac, Object o, EntityOperation entityOperation) {
        Map<String, Object> controllers = wac.getBeansWithAnnotation(Controller.class);


        for (Object controller : controllers.values()) {
            Class<?> controllerClass = controller.getClass();
            for (Method m : controllerClass.getMethods()) {

                EntityHandler em = AnnotationUtils.findAnnotation(m, EntityHandler.class);
                if (em != null && em.entity().isAssignableFrom(o.getClass()) && em.operation() == entityOperation) {
                    // find take the first one as default
                    String mapping = getMappingByControllerAndMethod(controllerClass, m);
                    return replaceProperties(mapping, m, o);
                }
            }
        }
        throw new RuntimeException("No entity handler could be resolved for class " + o.getClass() + " and operation " + entityOperation);
    }

    private static String getMappingByControllerAndMethod(Class<?> controllerClass, Method m) {
        RequestMapping classMappingAnnotation = AnnotationUtils.findAnnotation(controllerClass, RequestMapping.class);
        String baseMapping = classMappingAnnotation != null ? classMappingAnnotation.value()[0] : "";
        RequestMapping mappingAnnotation = AnnotationUtils.findAnnotation(m, RequestMapping.class);
        return baseMapping + mappingAnnotation.value()[0];
    }

    private static String replaceProperties(String mapping, Method m, Object o) {

        // find all variables in this url, interpret them as properties and replace them with their values
        if (o != null) {
            Matcher matcher = PATTERN.matcher(mapping);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                // try to find a property with this name
                String param = matcher.group(1);
                String encodedValue = getValueOfParam(param, o);
                matcher.appendReplacement(sb, encodedValue);
            }
            matcher.appendTail(sb);

            // find all @RequestParam annotations at method parameters
            StringBuilder queryParams = new StringBuilder();

            Class[] initBinderParams = m.getParameterTypes();
            Object[] initBinderArgs = new Object[initBinderParams.length];

            HandlerMethodResolver methodResolver = new HandlerMethodResolver();
            methodResolver.getTypeLevelMapping();

            for (int i = 0; i < initBinderArgs.length; i++) {
                MethodParameter methodParam = new MethodParameter(m, i);
                Annotation[] paramAnns = methodParam.getParameterAnnotations();

                for (Annotation paramAnn : paramAnns) {
                    if (RequestParam.class.isInstance(paramAnn)) {
                        RequestParam requestParam = (RequestParam) paramAnn;
                        // TODO only required parameters
                        String param = requestParam.value();
                        String encodedValue = getValueOfParam(param, o);
                        queryParams.append(param);
                        queryParams.append("=");
                        queryParams.append(encodedValue);
                    }
                }
            }

            if (queryParams.length() != 0) {
                sb.append("?");
                sb.append(queryParams);
            }

            return sb.toString();
        } else {
            return mapping;
        }
    }

    private static String replaceProperties2(String mapping, Method m, Object o) {

        // find all variables in this url, interpret them as properties and replace them with their values
        if (o != null) {
            Matcher matcher = PATTERN.matcher(mapping);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                // try to find a property with this name
                String param = matcher.group(1);
                String encodedValue = getValueOfParam(param, o);
                matcher.appendReplacement(sb, encodedValue);
            }
            matcher.appendTail(sb);


            // find all @RequestParam annotations at method parameters
            StringBuilder queryParams = new StringBuilder();
            Annotation[][] requestParams = m.getParameterAnnotations();
            for (Annotation[] parametersAnnotations : requestParams) {
                for (Annotation annotation : parametersAnnotations) {
                    if (annotation instanceof RequestParam) {
                        RequestParam requestParam = (RequestParam) annotation;
                        String param = requestParam.value();
                        String encodedValue = getValueOfParam(param, o);
                        queryParams.append(param);
                        queryParams.append("=");
                        queryParams.append(encodedValue);
                    }
                }
            }

            if (queryParams.length() != 0) {
                sb.append("?");
                sb.append(queryParams);
            }

            return sb.toString();
        } else {
            return mapping;
        }
    }

    private static String getValueOfParam(String param, Object o) {
        DirectFieldAccessor accessor = new DirectFieldAccessor(o);
        String value = accessor.getPropertyValue(param).toString();
        return URLEncoder.encode(value).replace("+", "%20");
    }

}
