import { config } from './config.js';
import { criarServidor } from './servidor.js';

const app = await criarServidor();

const encerrar = async () => {
  await app.close();
  process.exit(0);
};
process.on('SIGTERM', encerrar);
process.on('SIGINT', encerrar);

await app.listen({ port: config.porta, host: '0.0.0.0' });
