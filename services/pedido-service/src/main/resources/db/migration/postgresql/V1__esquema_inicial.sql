-- DER do pedido-service (Secao 8.1.1) - variante PostgreSQL 16 (o padrao de producao e o Azure SQL, ver ../sqlserver).
-- Banco exclusivo deste servico (Database per Service).

CREATE TABLE comanda (
    id                    UUID PRIMARY KEY,
    tenant_id             VARCHAR(80)  NOT NULL,
    mesa_id               VARCHAR(80)  NOT NULL,          -- referencia logica: estabelecimento-service
    mesa_rotulo           VARCHAR(80),
    versao_cardapio_id    VARCHAR(80)  NOT NULL,          -- referencia logica: catalogo-service
    status                VARCHAR(20)  NOT NULL,
    subtotal_centavos     BIGINT,
    taxa_servico_centavos BIGINT,
    total_centavos        BIGINT,
    aberta_em             TIMESTAMPTZ  NOT NULL,
    fechada_em            TIMESTAMPTZ,
    versao_otimista       BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX ix_comanda_tenant_status ON comanda (tenant_id, status, aberta_em DESC);
-- RF05: no maximo uma comanda ABERTA por mesa
CREATE UNIQUE INDEX ux_comanda_aberta_por_mesa ON comanda (tenant_id, mesa_id) WHERE status = 'ABERTA';

CREATE TABLE item_comanda (
    id                          UUID PRIMARY KEY,
    comanda_id                  UUID         NOT NULL REFERENCES comanda (id),
    item_cardapio_id            VARCHAR(80)  NOT NULL,     -- referencia logica: catalogo-service
    descricao                   VARCHAR(200) NOT NULL,     -- congelada na adicao
    categoria                   VARCHAR(80),               -- usada no roteamento por categoria
    preco_unitario_centavos     BIGINT       NOT NULL,     -- congelado
    quantidade                  INT          NOT NULL,
    observacao                  TEXT,
    estacao_sugerida            VARCHAR(60)  NOT NULL,
    tempo_alvo_preparo_segundos INT          NOT NULL,
    status                      VARCHAR(20)  NOT NULL,
    chave_confirmacao           VARCHAR(64),
    motivo_cancelamento         VARCHAR(200),
    adicionado_em               TIMESTAMPTZ  NOT NULL,
    confirmado_em               TIMESTAMPTZ,
    entregue_em                 TIMESTAMPTZ,
    cancelado_em                TIMESTAMPTZ
);
CREATE INDEX ix_item_comanda_comanda ON item_comanda (comanda_id);

CREATE TABLE modificador_item (
    item_comanda_id          UUID         NOT NULL REFERENCES item_comanda (id) ON DELETE CASCADE,
    grupo                    VARCHAR(80)  NOT NULL,
    opcao                    VARCHAR(80)  NOT NULL,
    preco_adicional_centavos BIGINT       NOT NULL
);
CREATE INDEX ix_modificador_item ON modificador_item (item_comanda_id);

CREATE TABLE outbox_evento (
    evento_id    UUID PRIMARY KEY,
    tenant_id    VARCHAR(80)  NOT NULL,
    tipo         VARCHAR(80)  NOT NULL,
    routing_key  VARCHAR(120) NOT NULL,
    payload      TEXT         NOT NULL CHECK (payload IS JSON),
    traceparent  VARCHAR(80),
    criado_em    TIMESTAMPTZ  NOT NULL,
    publicado_em TIMESTAMPTZ,
    tentativas   INT          NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_pendentes ON outbox_evento (criado_em) WHERE publicado_em IS NULL;

CREATE TABLE evento_processado (
    evento_id     UUID PRIMARY KEY,
    origem        VARCHAR(80) NOT NULL,
    processado_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE log_auditoria (
    id             UUID PRIMARY KEY,
    tenant_id      VARCHAR(80)  NOT NULL,
    usuario_id     VARCHAR(120) NOT NULL,
    acao           VARCHAR(60)  NOT NULL,
    comanda_id     UUID,
    detalhe        TEXT CHECK (detalhe IS NULL OR detalhe IS JSON),
    correlation_id VARCHAR(80),
    ocorrido_em    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_auditoria_tenant ON log_auditoria (tenant_id, ocorrido_em DESC);
