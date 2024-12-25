/*
package com.hmall.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取request
        //2.判断是否需要做登录拦截
        //3.获取token
        //4.校验并且解析token
        // TODO 5.传递用户信息
        //6.放行

        return null;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
*/
