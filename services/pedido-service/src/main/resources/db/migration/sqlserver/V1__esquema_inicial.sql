-- DER do pedido-service (Secao 8.1.1) - Azure SQL Database / SQL Server (padrao de producao).
-- Banco exclusivo deste servico (Database per Service).

CREATE TABLE comanda (
    id                    UNIQUEIDENTIFIER  NOT NULL PRIMARY KEY,
    tenant_id             NVARCHAR(80)      NOT NULL,
    mesa_id               NVARCHAR(80)      NOT NULL,          -- referencia logica: estabelecimento-service
    mesa_rotulo           NVARCHAR(80)      NULL,
    versao_cardapio_id    NVARCHAR(80)      NOT NULL,          -- referencia logica: catalogo-service
    status                NVARCHAR(20)      NOT NULL,
    subtotal_centavos     BIGINT            NULL,
    taxa_servico_centavos BIGINT            NULL,
    total_centavos        BIGINT            NULL,
    aberta_em             DATETIMEOFFSET(6) NOT NULL,
    fechada_em            DATETIMEOFFSET(6) NULL,
    versao_otimista       BIGINT            NOT NULL DEFAULT 0
);
CREATE INDEX ix_comanda_tenant_status ON comanda (tenant_id, status, aberta_em DESC);
-- RF05: no maximo uma comanda ABERTA por mesa (indice filtrado)
CREATE UNIQUE INDEX ux_comanda_aberta_por_mesa ON comanda (tenant_id, mesa_id) WHERE status = 'ABERTA';

CREATE TABLE item_comanda (
    id                          UNIQUEIDENTIFIER  NOT NULL PRIMARY KEY,
    comanda_id                  UNIQUEIDENTIFIER  NOT NULL REFERENCES comanda (id),
    item_cardapio_id            NVARCHAR(80)      NOT NULL,     -- referencia logica: catalogo-service
    descricao                   NVARCHAR(200)     NOT NULL,     -- congelada na adicao
    categoria                   NVARCHAR(80)      NULL,         -- usada no roteamento por categoria
    preco_unitario_centavos     BIGINT            NOT NULL,     -- congelado
    quantidade                  INT               NOT NULL,
    observacao                  NVARCHAR(MAX)     NULL,
    estacao_sugerida            NVARCHAR(60)      NOT NULL,
    tempo_alvo_preparo_segundos INT               NOT NULL,
    status                      NVARCHAR(20)      NOT NULL,
    chave_confirmacao           NVARCHAR(64)      NULL,
    motivo_cancelamento         NVARCHAR(200)     NULL,
    adicionado_em               DATETIMEOFFSET(6) NOT NULL,
    confirmado_em               DATETIMEOFFSET(6) NULL,
    entregue_em                 DATETIMEOFFSET(6) NULL,
    cancelado_em                DATETIMEOFFSET(6) NULL
);
CREATE INDEX ix_item_comanda_comanda ON item_comanda (comanda_id);

CREATE TABLE modificador_item (
    item_comanda_id          UNIQUEIDENTIFIER NOT NULL REFERENCES item_comanda (id) ON DELETE CASCADE,
    grupo                    NVARCHAR(80)     NOT NULL,
    opcao                    NVARCHAR(80)     NOT NULL,
    preco_adicional_centavos BIGINT           NOT NULL
);
CREATE INDEX ix_modificador_item ON modificador_item (item_comanda_id);

CREATE TABLE outbox_evento (
    evento_id    UNIQUEIDENTIFIER  NOT NULL PRIMARY KEY,
    tenant_id    NVARCHAR(80)      NOT NULL,
    tipo         NVARCHAR(80)      NOT NULL,
    routing_key  NVARCHAR(120)     NOT NULL,
    payload      NVARCHAR(MAX)     NOT NULL CHECK (ISJSON(payload) = 1),
    traceparent  NVARCHAR(80)      NULL,
    criado_em    DATETIMEOFFSET(6) NOT NULL,
    publicado_em DATETIMEOFFSET(6) NULL,
    tentativas   INT               NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_pendentes ON outbox_evento (criado_em) WHERE publicado_em IS NULL;

CREATE TABLE evento_processado (
    evento_id     UNIQUEIDENTIFIER  NOT NULL PRIMARY KEY,
    origem        NVARCHAR(80)      NOT NULL,
    processado_em DATETIMEOFFSET(6) NOT NULL
);

CREATE TABLE log_auditoria (
    id             UNIQUEIDENTIFIER  NOT NULL PRIMARY KEY,
    tenant_id      NVARCHAR(80)      NOT NULL,
    usuario_id     NVARCHAR(120)     NOT NULL,
    acao           NVARCHAR(60)      NOT NULL,
    comanda_id     UNIQUEIDENTIFIER  NULL,
    detalhe        NVARCHAR(MAX)     NULL CHECK (detalhe IS NULL OR ISJSON(detalhe) = 1),
    correlation_id NVARCHAR(80)      NULL,
    ocorrido_em    DATETIMEOFFSET(6) NOT NULL
);
CREATE INDEX ix_auditoria_tenant ON log_auditoria (tenant_id, ocorrido_em DESC);
