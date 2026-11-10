-- Tenant de demonstracao usado no video e nos testes ponta a ponta.
-- Os qrTokens de demonstracao sao legiveis; tokens reais sao gerados com 192 bits de entropia.

INSERT INTO estabelecimento (tenant_id, nome_fantasia, fuso_horario, percentual_taxa_servico, estacao_padrao)
VALUES ('demo', 'Trattoria SmartKDS (demo)', 'America/Sao_Paulo', 10.00, 'cozinha');

INSERT INTO estacao (id, tenant_id, codigo, nome, ordem_exibicao, percentual_atencao) VALUES
    ('00000000-0000-0000-0000-0000000000e1', 'demo', 'grill',     'Grill',     1, 70),
    ('00000000-0000-0000-0000-0000000000e2', 'demo', 'fritura',   'Fritura',   2, 70),
    ('00000000-0000-0000-0000-0000000000e3', 'demo', 'bar',       'Bar',       3, 60),
    ('00000000-0000-0000-0000-0000000000e4', 'demo', 'sobremesa', 'Sobremesa', 4, 70),
    ('00000000-0000-0000-0000-0000000000e5', 'demo', 'cozinha',   'Cozinha',   5, 70);

INSERT INTO mesa (id, tenant_id, rotulo, qr_token) VALUES
    ('00000000-0000-0000-0000-0000000000a1', 'demo', 'Mesa 1', 'demo-mesa-1'),
    ('00000000-0000-0000-0000-0000000000a2', 'demo', 'Mesa 2', 'demo-mesa-2'),
    ('00000000-0000-0000-0000-0000000000a3', 'demo', 'Mesa 3', 'demo-mesa-3'),
    ('00000000-0000-0000-0000-0000000000a4', 'demo', 'Mesa 4', 'demo-mesa-4'),
    ('00000000-0000-0000-0000-0000000000a5', 'demo', 'Balcao 1', 'demo-balcao-1');

INSERT INTO regra_roteamento (id, tenant_id, categoria_cardapio, item_cardapio_id, estacao_codigo, prioridade) VALUES
    ('00000000-0000-0000-0000-0000000000b1', 'demo', 'Bebidas',    NULL, 'bar',       1),
    ('00000000-0000-0000-0000-0000000000b2', 'demo', 'Sobremesas', NULL, 'sobremesa', 1);
