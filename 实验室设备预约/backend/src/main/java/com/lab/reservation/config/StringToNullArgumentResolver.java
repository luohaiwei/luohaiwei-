package com.lab.reservation.config;

import javax.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 全局参数解析器：处理前端 axios 将 null/空值以空字符串传给后端导致的 400 错误。
 *
 * <p>当 @RequestParam 参数类型为 Integer/Long 等包装类型时，
 * 若请求中该参数为空字符串（""），Spring 无法将其转为 null 而抛出 400 Bad Request。
 *
 * <p>本解析器在参数绑定前将空字符串转为 null，使 required=false 的参数正常工作。
 * 目前处理：Integer, Long, Short, Byte, Float, Double。
 */
public class StringToNullArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> type = parameter.getParameterType();
        return type == Integer.class || type == Long.class
            || type == Short.class || type == Byte.class
            || type == Float.class || type == Double.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 只有 @RequestParam 且 required=false 才走此解析器
        RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
        if (ann != null && ann.required()) {
            // required=true 走默认逻辑，让 Spring 抛 400（参数缺失）
            return null;
        }
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) return null;
        String name = parameter.getParameterName();
        String val = request.getParameter(name);
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            return convert(parameter.getParameterType(), val.trim());
        } catch (NumberFormatException e) {
            // 非法数字格式时返回 null，不抛 400（前端传了垃圾值时应友好提示，而非直接报错）
            return null;
        }
    }

    private Object convert(Class<?> type, String val) {
        if (type == Integer.class)    return Integer.parseInt(val);
        if (type == Long.class)        return Long.parseLong(val);
        if (type == Short.class)       return Short.parseShort(val);
        if (type == Byte.class)        return Byte.parseByte(val);
        if (type == Float.class)       return Float.parseFloat(val);
        if (type == Double.class)      return Double.parseDouble(val);
        return null;
    }
}
