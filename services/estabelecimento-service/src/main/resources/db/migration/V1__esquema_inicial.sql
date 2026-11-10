-- DER do estabelecimento-service (Secao 8.1.3). Banco exclusivo deste servico.

CREATE TABLE estabelecimento (
    tenant_id               VARCHAR(80) PRIMARY KEY,
    nome_fantasia           VARCHAR(120) NOT NULL,
    fuso_horario            VARCHAR(60)  NOT NULL,
    percentual_taxa_servico NUMERIC(5,2) NOT NULL,
    estacao_padrao          VARCHAR(60),
    ativo                   BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE mesa (
    id        UUID PRIMARY KEY,
    tenant_id VARCHAR(80)  NOT NULL REFERENCES estabelecimento (tenant_id),
    rotulo    VARCHAR(80)  NOT NULL,
    qr_token  VARCHAR(120) NOT NULL UNIQUE,   -- opaco e rotacionavel
    ativa     BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_mesa_rotulo ON mesa (tenant_id, rotulo);

CREATE TABLE estacao (
    id                 UUID PRIMARY KEY,
    tenant_id          VARCHAR(80) NOT NULL REFERENCES estabelecimento (tenant_id),
    codigo             VARCHAR(60) NOT NULL,   -- ex.: grill, fritura, bar (o mesmo usado em estacaoSugerida)
    nome               VARCHAR(80) NOT NULL,
    ordem_exibicao     INT         NOT NULL,
    percentual_atencao INT         NOT NULL DEFAULT 70,
    ativa              BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_estacao_codigo ON estacao (tenant_id, codigo);

CREATE TABLE regra_roteamento (
    id                 UUID PRIMARY KEY,
    tenant_id          VARCHAR(80) NOT NULL REFERENCES estabelecimento (tenant_id),
    categoria_cardapio VARCHAR(80),
    item_cardapio_id   VARCHAR(80),
    estacao_codigo     VARCHAR(60) NOT NULL,
    prioridade         INT         NOT NULL DEFAULT 0
);
CREATE INDEX ix_regra_tenant ON regra_roteamento (tenant_id);

CREATE TABLE usuario_lotacao (
    id                UUID PRIMARY KEY,
    tenant_id         VARCHAR(80)  NOT NULL REFERENCES estabelecimento (tenant_id),
    usuario_id        VARCHAR(120) NOT NULL,   -- sub do Keycloak
    papel             VARCHAR(20)  NOT NULL,
    estacao_padrao_id UUID REFERENCES estacao (id)
);
