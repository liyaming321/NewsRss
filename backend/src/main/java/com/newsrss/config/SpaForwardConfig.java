package com.newsrss.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 前端单页应用路由配置，用于支持打包进后端后的浏览器刷新。
 */
@Configuration
public class SpaForwardConfig implements WebMvcConfigurer {

    /**
     * 注册前端路由转发规则，不拦截 API、Actuator 和静态资源文件。
     *
     * @param registry 视图控制器注册器
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/reader").setViewName("forward:/index.html");
        registry.addViewController("/feeds").setViewName("forward:/index.html");
        registry.addViewController("/parser-templates").setViewName("forward:/index.html");
        registry.addViewController("/fetch-logs").setViewName("forward:/index.html");
        registry.addViewController("/settings").setViewName("forward:/index.html");
    }
}
