package com.zifang.z.camuda.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * SPA history fallback。
 *
 * <p>z-camuda 路由表用绝对路径 {@code /z-camuda/**}，后端 context-path 默认 {@code /}，
 * 前后端同源同端口。浏览器在子路由上直接刷新时 Spring 找不到静态资源，返回 JSON 404，
 * 浏览器拿不到 index.html 直接白屏。vite dev/preview 自带 fallback，本地测不出来，只在生产现形。
 *
 * <p>{@code api/} 与 {@code assets/} 前缀必须原样返 null：回落成 index.html 会把
 * 「接口 404」变成「返回一段 HTML」，前端 axios 会报一个更难查的解析错。
 */
@Configuration
public class ZCamudaWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("assets/")) {
                            return null;
                        }
                        // 带扩展名的不是前端路由：vite base 配错时 index.html 会去请求
                        // /xxx/assets/x.js，回 index.html 会得到一个 200 text/html 的假成功，
                        // 浏览器只报 "Failed to fetch dynamically imported module"，看不出是 base 错了。
                        if (resourcePath.lastIndexOf('.') > resourcePath.lastIndexOf('/')) {
                            return null;
                        }
                        return location.createRelative("index.html");
                    }
                });
    }
}
