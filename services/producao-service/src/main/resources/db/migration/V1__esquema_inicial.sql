-- DER do producao-service (Secao 8.1.2). Banco exclusivo deste servico.

CREATE TABLE ticket_producao (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(80)  NOT NULL,
    comanda_id          UUID         NOT NULL,          -- referencia logica: pedido-service
    mesa_rotulo         VARCHAR(80)  NOT NULL,          -- denormalizado do evento
    estacao_id          VARCHAR(60)  NOT NULL,          -- referencia logica: estabelecimento-service
    status              VARCHAR(20)  NOT NULL,
    tempo_alvo_segundos INT          NOT NULL,
    criado_em           TIMESTAMPTZ  NOT NULL,
    iniciado_em         TIMESTAMPTZ,
    pronto_em           TIMESTAMPTZ,
    expedido_em         TIMESTAMPTZ,
    cancelado_em        TIMESTAMPTZ,
    versao_otimista     BIGINT       NOT NULL DEFAULT 0
);
-- fila do KDS
CREATE INDEX ix_ticket_fila_kds ON ticket_producao (tenant_id, estacao_id, status, criado_em);
-- indicadores
CREATE INDEX ix_ticket_indicadores ON ticket_producao (tenant_id, estacao_id, expedido_em);

CREATE TABLE item_ticket (
    id              UUID PRIMARY KEY,
    ticket_id       UUID         NOT NULL REFERENCES ticket_producao (id),
    item_comanda_id UUID         NOT NULL,              -- usado na compensacao
    descricao       VARCHAR(200) NOT NULL,              -- denormalizada do evento
    quantidade      INT          NOT NULL,
    modificadores   TEXT,
    observacao      TEXT,
    cancelado       BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_item_ticket_ticket ON item_ticket (ticket_id);
CREATE INDEX ix_item_ticket_item_comanda ON item_ticket (item_comanda_id);

CREATE TABLE evento_processado (
    evento_id     UUID PRIMARY KEY,
    origem        VARCHAR(80) NOT NULL,
    processado_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE outbox_evento (
    evento_id    UUID PRIMARY KEY,
    tenant_id    VARCHAR(80)  NOT NULL,
    tipo         VARCHAR(80)  NOT NULL,
    routing_key  VARCHAR(120) NOT NULL,
    payload      JSONB        NOT NULL,
    traceparent  VARCHAR(80),
    criado_em    TIMESTAMPTZ  NOT NULL,
    publicado_em TIMESTAMPTZ,
    tentativas   INT          NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_pendentes ON outbox_evento (criado_em) WHERE publicado_em IS NULL;
