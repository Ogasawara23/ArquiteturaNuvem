package br.com.smartkds.pedido.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Topologia do broker vista pelo pedido-service (Secao 7.2): publica em smartkds.pedidos,
 * consome de smartkds.producao, com uma dead letter queue por fila de consumo.
 */
@Configuration
class MensageriaConfig {

    static final String EXCHANGE_PEDIDOS = "smartkds.pedidos";
    static final String EXCHANGE_PRODUCAO = "smartkds.producao";
    static final String EXCHANGE_DLX = "smartkds.dlx";

    @Bean
    Declarables topologia() {
        TopicExchange pedidos = new TopicExchange(EXCHANGE_PEDIDOS, true, false);
        TopicExchange producao = new TopicExchange(EXCHANGE_PRODUCAO, true, false);
        DirectExchange dlx = new DirectExchange(EXCHANGE_DLX, true, false);

        List<org.springframework.amqp.core.Declarable> declaraveis = new ArrayList<>(List.of(pedidos, producao, dlx));
        declaraveis.addAll(filaComDlq("pedido.producao", producao, "ticket.expedido", dlx));
        declaraveis.addAll(filaComDlq("pedido.producao.status", producao, "ticket.status", dlx));
        return new Declarables(declaraveis);
    }

    private static List<org.springframework.amqp.core.Declarable> filaComDlq(String nome, TopicExchange origem,
                                                                            String chave, DirectExchange dlx) {
        Queue fila = QueueBuilder.durable(nome)
                .deadLetterExchange(dlx.getName())
                .deadLetterRoutingKey(nome + ".dlq")
                .build();
        Queue dlq = QueueBuilder.durable(nome + ".dlq").build();
        Binding ligacao = BindingBuilder.bind(fila).to(origem).with(chave);
        Binding ligacaoDlq = BindingBuilder.bind(dlq).to(dlx).with(nome + ".dlq");
        return List.of(fila, dlq, ligacao, ligacaoDlq);
    }
}
