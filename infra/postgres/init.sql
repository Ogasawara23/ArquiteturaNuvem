-- Database per Service (Secao 7.2): um banco e um usuario NAO superusuario por servico.
-- Assim um acesso cruzado acidental falha localmente, e o Row Level Security e efetivamente aplicado.

CREATE ROLE pedido LOGIN PASSWORD 'pedido' NOSUPERUSER;
CREATE DATABASE smartkds_pedido OWNER pedido;

CREATE ROLE producao LOGIN PASSWORD 'producao' NOSUPERUSER;
CREATE DATABASE smartkds_producao OWNER producao;

CREATE ROLE estabelecimento LOGIN PASSWORD 'estabelecimento' NOSUPERUSER;
CREATE DATABASE smartkds_estabelecimento OWNER estabelecimento;

-- Nenhum usuario conecta no banco de outro servico.
REVOKE CONNECT ON DATABASE smartkds_pedido FROM PUBLIC;
REVOKE CONNECT ON DATABASE smartkds_producao FROM PUBLIC;
REVOKE CONNECT ON DATABASE smartkds_estabelecimento FROM PUBLIC;
GRANT CONNECT ON DATABASE smartkds_pedido TO pedido;
GRANT CONNECT ON DATABASE smartkds_producao TO producao;
GRANT CONNECT ON DATABASE smartkds_estabelecimento TO estabelecimento;
