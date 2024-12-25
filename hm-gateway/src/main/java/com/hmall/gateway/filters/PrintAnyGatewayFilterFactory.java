package com.hmall.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class PrintAnyGatewayFilterFactory extends AbstractGatewayFilterFactory {
    /*
    下面这种是比较友好的过滤器写法
    这里使用了 OrderedGatewayFilter 这个方法
    不仅仅载入了我们新定义的过滤器，还提供了参数选项
    解决了匿名内部类不能继承 Ordered 接口的问题
        */
    @Override
    public GatewayFilter apply(Object config) {
        return new OrderedGatewayFilter(new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                System.out.println("目前执行到了PrintAnyGatewayFilterFactory");
                return chain.filter(exchange);
            }
        }, 1);

        /*
        这是自定义过滤器的普通写法
        这种通过匿名内部类的实现方式没有办法指定过滤器在过滤链中的执行顺序
        所以这里注释掉了，请看上面没有被注释掉的方法
            return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                // 编写过滤器逻辑
                System.out.println("目前执行到了PrintAnyGatewayFilterFactory");
                //放行
                return chain.filter(exchange);
            }
        };*/
    }
}
