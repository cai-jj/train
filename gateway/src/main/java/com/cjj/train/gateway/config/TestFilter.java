//package com.cjj.train.gateway.config;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//@Component
////@Order
//public class TestFilter implements GlobalFilter {
//    private static final Logger LOG = LoggerFactory.getLogger(TestFilter.class);
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        LOG.info("test filter");
//        return chain.filter(exchange);
//    }
//}
