package com.ivedi.ivedikuryemanagement.common.config;

import com.ivedi.ivedikuryemanagement.realtime.impl.RedisMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Slf4j
public class RedisConfig {

    public static final String CHANNEL_ORDER_STATUS     = "ivedi:order-status";
    public static final String CHANNEL_ASSIGNMENT       = "ivedi:assignment";
    public static final String CHANNEL_COURIER_LOCATION = "ivedi:courier-location";

    /**
     * Only created when realtime.redis.enabled=true (default).
     * Set to false in local dev environments where Redis is not running.
     */
    @Bean
    @ConditionalOnProperty(name = "realtime.redis.enabled", havingValue = "true")
    public RedisMessageListenerContainer redisListenerContainer(
            RedisConnectionFactory factory,
            RedisMessageHandler handler
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(handler, new ChannelTopic(CHANNEL_ORDER_STATUS));
        container.addMessageListener(handler, new ChannelTopic(CHANNEL_ASSIGNMENT));
        container.addMessageListener(handler, new ChannelTopic(CHANNEL_COURIER_LOCATION));
        container.setRecoveryInterval(10_000L);
        container.setErrorHandler(e ->
                log.error("Redis listener error — will retry in {}ms: {}", 10_000, e.getMessage()));
        return container;
    }
}
