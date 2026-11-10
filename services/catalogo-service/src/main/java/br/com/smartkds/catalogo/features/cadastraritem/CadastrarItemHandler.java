package br.com.smartkds.catalogo.features.cadastraritem;

import br.com.smartkds.catalogo.shared.dominio.ItemCardapio;
import br.com.smartkds.catalogo.shared.persistencia.ItemCardapioRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
class CadastrarItemHandler {

    private final ItemCardapioRepository repositorio;
    private final Clock relogio;

    CadastrarItemHandler(ItemCardapioRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public CadastrarItemResponse executar(CadastrarItemCommand comando) {
        Instant agora = Instant.now(relogio);
        ItemCardapio item = ItemCardapio.cadastrar(comando.tenantId(), comando.dados(), agora);
        repositorio.salvar(item);
        return new CadastrarItemResponse(item.id(), item.atualizadoEm());
    }
}
