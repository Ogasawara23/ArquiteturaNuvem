package br.com.smartkds.pedido.features;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.MesaId;
import br.com.smartkds.pedido.shared.dominio.RegistroAuditoria;
import br.com.smartkds.pedido.shared.dominio.StatusComanda;
import br.com.smartkds.pedido.shared.dominio.TenantId;
import br.com.smartkds.pedido.shared.eventos.EventoDeDominio;
import br.com.smartkds.pedido.shared.inbox.EventoProcessadoRepository;
import br.com.smartkds.pedido.shared.outbox.PublicadorDeEventos;
import br.com.smartkds.pedido.shared.persistencia.AuditoriaRepository;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Fakes das portas do shared: os testes de fatia rodam com JUnit puro (Secao 8.8.6). */
public final class Fakes {

    private Fakes() { }

    public static Clock relogio() {
        return Clock.fixed(Instant.parse("2026-10-15T15:40:00Z"), ZoneOffset.UTC);
    }

    public static final class Comandas implements ComandaRepository {
        public final Map<ComandaId, Comanda> dados = new HashMap<>();
        public int salvamentos;

        @Override
        public Comanda salvar(Comanda comanda) {
            salvamentos++;
            dados.put(comanda.id(), comanda);
            return comanda;
        }

        @Override
        public Optional<Comanda> buscarPorId(TenantId tenantId, ComandaId comandaId) {
            return Optional.ofNullable(dados.get(comandaId)).filter(c -> c.tenantId().equals(tenantId));
        }

        @Override
        public Optional<Comanda> buscarAbertaPorMesa(TenantId tenantId, MesaId mesaId) {
            return dados.values().stream().filter(c -> c.tenantId().equals(tenantId) && c.mesaId().equals(mesaId)
                    && c.status() == StatusComanda.ABERTA).findFirst();
        }

        @Override
        public List<Comanda> listar(TenantId tenantId, StatusComanda status, MesaId mesa) {
            return dados.values().stream().filter(c -> c.tenantId().equals(tenantId)).toList();
        }
    }

    public static final class Publicador implements PublicadorDeEventos {
        public final List<EventoDeDominio> publicados = new ArrayList<>();

        @Override
        public void publicar(EventoDeDominio evento) {
            publicados.add(evento);
        }
    }

    public static final class Inbox implements EventoProcessadoRepository {
        public final Set<UUID> processados = new HashSet<>();

        @Override
        public boolean jaProcessado(UUID eventoId) {
            return processados.contains(eventoId);
        }

        @Override
        public void marcarProcessado(UUID eventoId, String origem, Instant agora) {
            processados.add(eventoId);
        }
    }

    public static final class Auditoria implements AuditoriaRepository {
        public final List<RegistroAuditoria> registros = new ArrayList<>();

        @Override
        public void salvarRegistro(RegistroAuditoria registro) {
            registros.add(registro);
        }

        @Override
        public List<RegistroAuditoria> listar(TenantId tenantId, int pagina, int tamanho) {
            return registros;
        }
    }
}
