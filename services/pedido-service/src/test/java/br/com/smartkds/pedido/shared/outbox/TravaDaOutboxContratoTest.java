package br.com.smartkds.pedido.shared.outbox;

import br.com.smartkds.pedido.shared.eventos.ItemCancelado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Duas replicas do OutboxRelay nunca travam o mesmo evento: enquanto a transacao A segura
 * dois eventos, a transacao B (concorrente) recebe apenas os demais (SKIP LOCKED / READPAST).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "smartkds.outbox.intervalo-ms=3600000"
})
abstract class TravaDaOutboxContratoTest {

    @Autowired
    OutboxSpringData outbox;
    @Autowired
    PublicadorDeEventos publicador;
    @Autowired
    TransactionTemplate transacao;

    @Test
    void replicas_concorrentes_nao_travam_o_mesmo_evento() throws Exception {
        transacao.executeWithoutResult(s -> {
            for (int i = 0; i < 3; i++) {
                publicador.publicar(ItemCancelado.novo("t-trava", UUID.randomUUID(), UUID.randomUUID(), "x", Instant.now()));
            }
        });

        CountDownLatch aTravou = new CountDownLatch(1);
        CountDownLatch bTerminou = new CountDownLatch(1);
        CompletableFuture<List<UUID>> replicaA = CompletableFuture.supplyAsync(() -> transacao.execute(s -> {
            List<UUID> ids = outbox.travarPendentes(PageRequest.of(0, 2)).stream().map(r -> r.eventoId).toList();
            aTravou.countDown();
            aguardar(bTerminou);
            return ids;
        }));
        assertThat(aTravou.await(30, TimeUnit.SECONDS)).isTrue();
        List<UUID> deB = transacao.execute(s ->
                outbox.travarPendentes(PageRequest.of(0, 50)).stream().map(r -> r.eventoId).toList());
        bTerminou.countDown();
        List<UUID> deA = replicaA.get(30, TimeUnit.SECONDS);

        assertThat(deA).hasSize(2);
        assertThat(deB).isNotEmpty().doesNotContainAnyElementsOf(deA);
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
