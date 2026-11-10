package br.com.smartkds.pedido.features.registrareventorejeitado;

record RegistrarEventoRejeitadoCommand(String eventoId, String tipo, String fila, String traceparent,
                                       String trechoDoCorpo) {
}
