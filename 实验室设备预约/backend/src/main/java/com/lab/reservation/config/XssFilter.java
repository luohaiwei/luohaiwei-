package com.lab.reservation.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * XSS攻击防护过滤器
 * 防止跨站脚本攻击（XSS），仅过滤GET查询参数和表单参数。
 * 注意：JSON请求体的XSS过滤由Spring的消息转换器处理，不在此过滤器中处理。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter {

    /**
     * XSS攻击常用模式匹配规则
     */
    private static final Pattern[] XSS_PATTERNS = {
        // script标签
        Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // javascript伪协议
        Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
        // vbscript伪协议
        Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE),
        // onload等事件处理器
        Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onerror事件
        Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onClick事件
        Pattern.compile("onclick(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onmouseover事件
        Pattern.compile("onmouseover(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onfocus事件
        Pattern.compile("onfocus(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onblur事件
        Pattern.compile("onblur(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onchange事件
        Pattern.compile("onchange(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onsubmit事件
        Pattern.compile("onsubmit(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onkeydown事件
        Pattern.compile("onkeydown(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onkeyup事件
        Pattern.compile("onkeyup(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // onkeypress事件
        Pattern.compile("onkeypress(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // iframe标签
        Pattern.compile("<iframe(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // object标签
        Pattern.compile("<object(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // embed标签
        Pattern.compile("<embed(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // applet标签
        Pattern.compile("<applet(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // style标签
        Pattern.compile("<style(.*?)</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // expression表达式
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        // CSS表达式
        Pattern.compile("behavior\\s*:", Pattern.CASE_INSENSITIVE),
        // @import引入
        Pattern.compile("@import", Pattern.CASE_INSENSITIVE),
        // base64编码的script
        Pattern.compile("base64\\s*,", Pattern.CASE_INSENSITIVE),
        // 数据URI
        Pattern.compile("data\\s*:", Pattern.CASE_INSENSITIVE),
        // SVG脚本
        Pattern.compile("<svg(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 仅对 GET 请求的 query string 进行 XSS 过滤
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String queryString = httpRequest.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            // 如果 query string 包含 XSS 特征，使用包装器过滤
            // 否则直接放行，避免不必要的包装开销
            if (containsXssPattern(queryString)) {
                chain.doFilter(new XssQueryStringRequestWrapper(httpRequest), response);
                return;
            }
        }
        // 对于表单参数（POST/PUT application/x-www-form-urlencoded），
        // getParameter 已被重写，会自动过滤
        chain.doFilter(httpRequest, response);
    }

    /**
     * 快速检查是否包含XSS模式（避免对所有请求都创建包装器）
     */
    private boolean containsXssPattern(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        // 销毁
    }

    /**
     * 带XSS过滤的请求包装器，仅过滤 query string 和表单参数
     */
    private static class XssQueryStringRequestWrapper extends HttpServletRequestWrapper {

        public XssQueryStringRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getQueryString() {
            String queryString = super.getQueryString();
            return filterXss(queryString);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return filterXss(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] filteredValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                filteredValues[i] = filterXss(values[i]);
            }
            return filteredValues;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return filterXss(value);
        }

        /**
         * HTML特殊字符转义
         */
        private String escapeHtml(String value) {
            if (value == null) {
                return null;
            }
            return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
        }

        /**
         * XSS过滤主方法
         */
        private String filterXss(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            // 第一步：HTML特殊字符转义
            String escaped = escapeHtml(value);
            // 第二步：移除XSS攻击模式
            String filtered = escaped;
            for (Pattern pattern : XSS_PATTERNS) {
                filtered = pattern.matcher(filtered).replaceAll("");
            }
            return filtered;
        }
    }
}
