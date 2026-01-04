package com.qyl.v2trade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * RestTemplate 配置
 *
 * @author qyl
 */
@Configuration
public class RestTemplateConfig {

    @Value("${http.proxy.host:}")
    private String proxyHost;

    @Value("${http.proxy.port:0}")
    private int proxyPort;



    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时: 10秒
        factory.setConnectTimeout(10000);

        // 读取超时: 30秒
        factory.setReadTimeout(30000);

        // 配置代理
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
        }

        return new RestTemplate(factory);
    }

}