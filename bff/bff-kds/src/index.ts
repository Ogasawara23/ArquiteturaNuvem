import { config } from './config.js';
import { iniciarConsumidor } from './eventos/consumidorDeEventos.js';
import { LIGACOES_KDS, rotearEventoParaKds } from './eventos/consumidorDeProducao.js';
import { criarServidor, dependenciasPadrao } from './servidor.js';

const deps = dependenciasPadrao();
const app = await criarServidor(deps);

let pararConsumidor: () => Promise<void> = async () => undefined;
if (config.rabbitmqUrl) {
  pararConsumidor = await iniciarConsumidor({
    url: config.rabbitmqUrl,
    prefixoFila: 'kds.stream',
    ligacoes: LIGACOES_KDS,
    aoReceber: (evento) => rotearEventoParaKds(evento, deps.streams),
    log: app.log,
  });
} else {
  app.log.warn('RABBITMQ_URL ausente: o KDS funciona sem atualizacao em tempo real');
}

const encerrar = async () => {
  deps.streams.fechar();
  await pararConsumidor();
  await app.close();
  process.exit(0);
};
process.on('SIGTERM', encerrar);
process.on('SIGINT', encerrar);

await app.listen({ port: config.porta, host: '0.0.0.0' });
