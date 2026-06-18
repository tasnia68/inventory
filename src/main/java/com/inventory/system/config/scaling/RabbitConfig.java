package com.inventory.system.config.scaling;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for async Shopify sync. Only created when {@code app.scaling.enabled}
 * — without it (dev/test) no listener containers start, so no broker connection is needed.
 * A dead-letter queue captures jobs that exhaust retries for later inspection.
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "app.scaling.enabled", havingValue = "true")
public class RabbitConfig {

    public static final String EXCHANGE = "masterinventory.shopify";
    public static final String SYNC_QUEUE = "masterinventory.shopify.sync";
    public static final String SYNC_ROUTING_KEY = "shopify.sync";
    public static final String DLQ = "masterinventory.shopify.sync.dlq";
    public static final String DLQ_ROUTING_KEY = "shopify.sync.dlq";

    @Bean
    public DirectExchange shopifyExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue shopifySyncQueue() {
        return QueueBuilder.durable(SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue shopifySyncDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding shopifySyncBinding(Queue shopifySyncQueue, DirectExchange shopifyExchange) {
        return BindingBuilder.bind(shopifySyncQueue).to(shopifyExchange).with(SYNC_ROUTING_KEY);
    }

    @Bean
    public Binding shopifySyncDlqBinding(Queue shopifySyncDlq, DirectExchange shopifyExchange) {
        return BindingBuilder.bind(shopifySyncDlq).to(shopifyExchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }
}
