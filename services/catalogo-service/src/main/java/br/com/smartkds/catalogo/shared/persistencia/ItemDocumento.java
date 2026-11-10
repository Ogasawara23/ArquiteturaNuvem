package br.com.smartkds.catalogo.shared.persistencia;

import br.com.smartkds.catalogo.shared.dominio.GrupoModificador;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/** Documento da colecao "itens" (Secao 8.1.4): grupos e opcoes sao arrays embutidos. */
@Document("itens")
@CompoundIndex(name = "tenant_categoria", def = "{'tenantId': 1, 'categoria': 1, 'nome': 1}")
record ItemDocumento(@Id String id,
                     String tenantId,
                     String nome,
                     String descricao,
                     String categoria,
                     long precoCentavos,
                     String estacaoSugerida,
                     int tempoAlvoPreparoSegundos,
                     boolean disponivel,
                     String imagemUrl,
                     List<GrupoModificador> gruposModificadores,
                     Instant atualizadoEm) {
}
