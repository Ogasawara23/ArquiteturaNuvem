package br.com.smartkds.pedido.features.adicionaritem;

import br.com.smartkds.pedido.shared.web.RequisicaoInvalidaException;

/** Validacao sintatica especifica desta fatia; as regras de negocio ficam no agregado. */
class AdicionarItemValidator {

    void validar(AdicionarItemCommand comando) {
        for (var selecao : comando.modificadores()) {
            if (selecao.grupo() == null || selecao.grupo().isBlank()
                    || selecao.opcao() == null || selecao.opcao().isBlank()) {
                throw new RequisicaoInvalidaException("cada modificador exige grupo e opcao");
            }
        }
    }
}
