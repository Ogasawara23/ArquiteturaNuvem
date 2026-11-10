package br.com.smartkds.producao.features.registrareventorejeitado;

record RegistrarEventoRejeitadoCommand(String eventoId, String tipo, String fila, String traceparent,
                                       String trechoDoCorpo) {
}
