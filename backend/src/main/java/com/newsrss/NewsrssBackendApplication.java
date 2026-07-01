package com.newsrss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class NewsrssBackendApplication {

	/**
	 * 启动 NewsRss 后端服务。
	 *
	 * @param args 命令行启动参数
	 */
	public static void main(String[] args) {
		SpringApplication.run(NewsrssBackendApplication.class, args);
	}

}
