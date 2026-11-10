package br.com.smartkds.producao.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/** Consome de smartkds.pedidos e publica em smartkds.producao, com DLQ por fila (Secao 7.2). */
@Configuration
class MensageriaConfig {

    @Bean
    Declarables topologia() {
        TopicExchange pedidos = new TopicExchange("smartkds.pedidos", true, false);
        TopicExchange producao = new TopicExchange("smartkds.producao", true, false);
        DirectExchange dlx = new DirectExchange("smartkds.dlx", true, false);
        List<Declarable> declaraveis = new ArrayList<>(List.of(pedidos, producao, dlx));
        declaraveis.addAll(filaComDlq("producao.pedidos", pedidos, "pedido.confirmado", dlx));
        declaraveis.addAll(filaComDlq("producao.cancelamentos", pedidos, "pedido.item.cancelado", dlx));
        return new Declarables(declaraveis);
    }

    private static List<Declarable> filaComDlq(String nome, TopicExchange origem, String chave, DirectExchange dlx) {
        Queue fila = QueueBuilder.durable(nome).deadLetterExchange(dlx.getName()).deadLetterRoutingKey(nome + ".dlq").build();
        Queue dlq = QueueBuilder.durable(nome + ".dlq").build();
        return List.of(fila, dlq, BindingBuilder.bind(fila).to(origem).with(chave),
                BindingBuilder.bind(dlq).to(dlx).with(nome + ".dlq"));
    }
}
