package br.com.smartkds.catalogo.shared.persistencia;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Ponteiro versaoAtivaId do tenant (atualizado na publicacao, Figura 20). */
@Document("cardapio_ativo")
record PonteiroVersaoAtivaDocumento(@Id String tenantId, String versaoAtivaId) {
}
