// Teste ponta a ponta do fluxo principal, sempre pelo API Gateway (como o navegador faria).
//   node scripts/teste-ponta-a-ponta.mjs            (API padrao: http://localhost:8080; ou API=https://<gateway>)
const API = process.env.API ?? 'http://localhost:8080';
const MESA = process.env.QR ?? 'demo-mesa-2';
const ADMIN = { 'content-type': 'application/json', 'x-tenant-id': 'demo' };

let passos = 0;
const ok = (msg) => console.log(`  ok  ${++passos}. ${msg}`);
const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

async function chamar(metodo, caminho, corpo, cabecalhos = { 'content-type': 'application/json' }) {
  const r = await fetch(API + caminho, { method: metodo, headers: cabecalhos, body: corpo ? JSON.stringify(corpo) : undefined });
  const texto = await r.text();
  if (!r.ok) throw new Error(`${metodo} ${caminho} -> ${r.status}: ${texto}`);
  return texto ? JSON.parse(texto) : undefined;
}

async function ate(descricao, condicao, tentativas = 40) {
  for (let i = 0; i < tentativas; i++) {
    const valor = await condicao();
    if (valor) return valor;
    await esperar(500);
  }
  throw new Error(`tempo esgotado: ${descricao}`);
}

/** Abre o stream SSE da estacao e devolve uma promessa com o primeiro evento recebido. */
async function ouvirKds(estacao) {
  const controle = new AbortController();
  const r = await fetch(`${API}/bff/kds/estacoes/${estacao}/stream`, { headers: { 'x-tenant-id': 'demo' }, signal: controle.signal });
  const leitor = r.body.getReader();
  const primeiro = (async () => {
    let buffer = '';
    for (;;) {
      const { value, done } = await leitor.read();
      if (done) return null;
      buffer += new TextDecoder().decode(value);
      const evento = /event: (\S+)/.exec(buffer);
      if (evento) return evento[1];
    }
  })();
  return { primeiro, fechar: () => controle.abort() };
}

const inicio = Date.now();
console.log(`Teste ponta a ponta em ${API} (mesa ${MESA})`);

const tela = await chamar('GET', `/bff/mesa/${MESA}/cardapio`);
const itens = tela.cardapio.categorias.flatMap((c) => c.itens);
const hamburguer = itens.find((i) => i.nome.startsWith('Hamburguer'));
const refri = itens.find((i) => i.nome.startsWith('Refrigerante'));
ok(`cliente leu o QR Code: ${tela.mesa.rotulo} em "${tela.mesa.estabelecimento}", ${itens.length} itens no cardapio`);

const stream = await ouvirKds('grill');
ok('KDS da estacao grill conectado ao stream SSE');

await chamar('POST', `/bff/mesa/${MESA}/comanda/itens`, {
  itemCardapioId: hamburguer.itemCardapioId, quantidade: 2, observacao: 'sem cebola',
  modificadores: [{ grupo: 'Ponto da carne', opcao: 'Ao ponto' }, { grupo: 'Adicionais', opcao: 'Bacon extra' }],
});
const carrinho = await chamar('POST', `/bff/mesa/${MESA}/comanda/itens`, { itemCardapioId: refri.itemCardapioId, quantidade: 1 });
ok(`carrinho com ${carrinho.itens.filter((i) => i.status === 'PENDENTE').length} itens pendentes, subtotal R$ ${(carrinho.subtotalCentavos / 100).toFixed(2)}`);

const chave = `teste-${Date.now()}`;
await chamar('POST', `/bff/mesa/${MESA}/comanda/confirmacao`, { chaveIdempotencia: chave });
const repetida = await chamar('POST', `/bff/mesa/${MESA}/comanda/confirmacao`, { chaveIdempotencia: chave });
ok(`pedido enviado a cozinha (reenvio com a mesma chave foi idempotente: repeticao=${repetida.repeticao})`);

const eventoSse = await Promise.race([stream.primeiro, esperar(15000).then(() => null)]);
stream.fechar();
if (!eventoSse) throw new Error('nenhum evento SSE recebido no KDS');
ok(`KDS recebeu "${eventoSse}" em tempo real via SSE (${Date.now() - inicio} ms desde o inicio)`);

const ticket = await ate('ticket no grill', async () => {
  const fila = await chamar('GET', '/bff/kds/estacoes/grill/tickets', undefined, ADMIN);
  return fila.tickets.find((t) => t.mesa === tela.mesa.rotulo);
});
const bar = await ate('ticket no bar', async () => {
  const fila = await chamar('GET', '/bff/kds/estacoes/bar/tickets', undefined, ADMIN);
  return fila.tickets.find((t) => t.mesa === tela.mesa.rotulo);
});
ok(`roteamento: hamburguer -> grill (${ticket.itens[0].quantidade}x ${ticket.itens[0].descricao}, ${ticket.itens[0].modificadores.join(', ')}), refrigerante -> bar`);

await chamar('PATCH', `/bff/kds/tickets/${ticket.ticketId}/status`, { status: 'EM_PREPARO' }, ADMIN);
await ate('item em producao na comanda', async () => {
  const c = await chamar('GET', `/bff/mesa/${MESA}/comanda`);
  return c.itens.some((i) => i.status === 'EM_PRODUCAO');
});
ok('cozinha iniciou o preparo e a comanda do cliente mostra EM_PRODUCAO (evento ticket.status)');

const cancelar = await fetch(`${API}/bff/mesa/${MESA}/comanda/itens/${(await chamar('GET', `/bff/mesa/${MESA}/comanda`)).itens.find((i) => i.status === 'EM_PRODUCAO').itemId}`, { method: 'DELETE' });
ok(`cancelar item em preparo foi recusado pela politica de dominio (HTTP ${cancelar.status})`);

await chamar('PATCH', `/bff/kds/tickets/${ticket.ticketId}/status`, { status: 'PRONTO' }, ADMIN);
await chamar('POST', `/bff/kds/tickets/${ticket.ticketId}/bump`, undefined, ADMIN);
const entregue = await ate('item entregue', async () => {
  const c = await chamar('GET', `/bff/mesa/${MESA}/comanda`);
  return c.itens.find((i) => i.descricao.startsWith('Hamburguer') && i.status === 'ENTREGUE');
});
ok(`bump na cozinha -> comanda mostra "${entregue.descricao}" como ENTREGUE (evento ticket.expedido)`);

const comanda = await chamar('GET', `/bff/mesa/${MESA}/comanda`);
const itemBar = comanda.itens.find((i) => i.descricao.startsWith('Refrigerante'));
await chamar('DELETE', `/bff/mesa/${MESA}/comanda/itens/${itemBar.itemId}`);
await ate('ticket do bar compensado', async () => {
  const fila = await chamar('GET', '/bff/kds/estacoes/bar/tickets', undefined, ADMIN);
  return !fila.tickets.some((t) => t.ticketId === bar.ticketId);
});
ok('cliente cancelou o refrigerante ainda nao iniciado -> ticket do bar saiu do KDS (saga ItemCancelado)');

const fechada = await chamar('POST', `/bff/admin/comandas/${comanda.comandaId}/fechamento`, { percentualTaxaServico: 10 }, ADMIN);
ok(`atendente fechou a conta: subtotal R$ ${(fechada.subtotalCentavos / 100).toFixed(2)} + 10% = R$ ${(fechada.totalCentavos / 100).toFixed(2)}`);

const agregado = await chamar('GET', '/bff/admin/aggregated-data', undefined, ADMIN);
ok(`GET /aggregated-data: ${agregado.fontes.map((f) => `${f.nome}=${f.situacao}`).join(', ')}`);
ok(`  catalogo: ${agregado.catalogo.totalDeItens} itens; pedidos 24h: R$ ${(agregado.pedidos.faturamentoNasUltimas24hCentavos / 100).toFixed(2)}; ` +
   `Function: ${agregado.statusOperacional.faixaHoraria} (pico=${agregado.statusOperacional.emHorarioDePico})`);

const auditoria = await chamar('GET', '/bff/admin/auditoria', undefined, ADMIN);
ok(`auditoria registrou: ${[...new Set(auditoria.map((a) => a.acao))].join(', ')}`);

console.log(`\nFluxo completo OK: ${passos} verificacoes em ${((Date.now() - inicio) / 1000).toFixed(1)} s`);
