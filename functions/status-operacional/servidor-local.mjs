// Host HTTP minimo para executar a Azure Function localmente quando o Azure Functions Core Tools
// (`func start`) nao estiver instalado. Usa o MESMO handler publicado na nuvem.
//   npm run build && node servidor-local.mjs      ->  http://localhost:7071/api/status-operacional
import { createServer } from 'node:http';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { HttpRequest, InvocationContext } = require('@azure/functions');
const { statusOperacional } = require('./dist/src/functions/statusOperacional.js');

const porta = Number(process.env.PORT ?? 7071);

createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${porta}`);
  if (url.pathname !== '/api/status-operacional' || req.method !== 'GET') {
    res.writeHead(404).end();
    return;
  }
  try {
    const requisicao = new HttpRequest({ method: 'GET', url: url.toString(), headers: req.headers });
    const contexto = new InvocationContext({ functionName: 'status-operacional', invocationId: crypto.randomUUID() });
    const resposta = await statusOperacional(requisicao, contexto);
    res.writeHead(resposta.status ?? 200, { 'content-type': 'application/json', ...(resposta.headers ?? {}) });
    res.end(JSON.stringify(resposta.jsonBody ?? null));
  } catch (erro) {
    res.writeHead(500, { 'content-type': 'application/json' }).end(JSON.stringify({ erro: String(erro) }));
  }
}).listen(porta, () => console.log(`status-operacional (host local) em http://localhost:${porta}/api/status-operacional`));
