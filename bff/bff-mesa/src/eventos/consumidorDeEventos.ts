import amqp from 'amqplib';
import { hostname } from 'node:os';

export interface Ligacao {
  exchange: string;
  chave: string;
}

export interface EventoRecebido {
  chave: string;
  tipo: string;
  corpo: Record<string, unknown>;
}

interface Registro {
  info(msg: string): void;
  warn(msg: string): void;
}

/**
 * Consumidor AMQP para alimentar o SSE. Cada replica usa uma fila exclusiva e temporaria
 * (todas as replicas recebem todos os eventos, pois cada uma tem seus proprios clientes SSE).
 * Reconecta sozinho; sem RABBITMQ_URL o BFF funciona sem tempo real (degradacao parcial).
 */
export async function iniciarConsumidor(opcoes: {
  url: string;
  prefixoFila: string;
  ligacoes: Ligacao[];
  aoReceber: (evento: EventoRecebido) => void;
  log: Registro;
}): Promise<() => Promise<void>> {
  let encerrado = false;
  let conexao: Awaited<ReturnType<typeof amqp.connect>> | undefined;

  const conectar = async (): Promise<void> => {
    if (encerrado) return;
    try {
      conexao = await amqp.connect(opcoes.url);
      conexao.on('error', (e) => opcoes.log.warn(`amqp: ${e.message}`));
      conexao.on('close', () => {
        if (!encerrado) setTimeout(conectar, 5000).unref();
      });
      const canal = await conexao.createChannel();
      const fila = await canal.assertQueue(`${opcoes.prefixoFila}.${hostname()}.${process.pid}`, {
        exclusive: true,
        autoDelete: true,
        durable: false,
      });
      for (const { exchange, chave } of opcoes.ligacoes) {
        await canal.assertExchange(exchange, 'topic', { durable: true });
        await canal.bindQueue(fila.queue, exchange, chave);
      }
      await canal.consume(fila.queue, (msg) => {
        if (!msg) return;
        try {
          const corpo = JSON.parse(msg.content.toString('utf-8')) as Record<string, unknown>;
          opcoes.aoReceber({
            chave: msg.fields.routingKey,
            tipo: String(msg.properties.type ?? corpo.tipo ?? msg.fields.routingKey),
            corpo,
          });
        } catch (e) {
          opcoes.log.warn(`mensagem ignorada: ${(e as Error).message}`);
        }
        canal.ack(msg);
      });
      opcoes.log.info(`consumidor AMQP ativo na fila ${fila.queue}`);
    } catch (e) {
      opcoes.log.warn(`RabbitMQ indisponivel (${(e as Error).message}); nova tentativa em 5 s`);
      setTimeout(conectar, 5000).unref();
    }
  };

  await conectar();
  return async () => {
    encerrado = true;
    await conexao?.close().catch(() => undefined);
  };
}
