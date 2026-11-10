package br.com.smartkds.catalogo.shared.persistencia;

import br.com.smartkds.catalogo.shared.dominio.ItemNaVersao;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/** Documento fechado da versao: embute a copia imutavel dos itens. */
@Document("versoes_cardapio")
@CompoundIndex(name = "tenant_versao", def = "{'tenantId': 1, '_id': 1}")
record VersaoDocumento(@Id String versaoId,
                       String tenantId,
                       Instant publicadaEm,
                       String publicadaPor,
                       int quantidadeDeItens,
                       List<ItemNaVersao> itens) {
}
