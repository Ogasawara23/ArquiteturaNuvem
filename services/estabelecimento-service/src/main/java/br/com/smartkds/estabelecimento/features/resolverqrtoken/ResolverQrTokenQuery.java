package br.com.smartkds.estabelecimento.features.resolverqrtoken;

import br.com.smartkds.estabelecimento.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.estabelecimento.shared.persistencia.EstabelecimentoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.ResolvedorDeQrToken;
import org.springframework.stereotype.Component;

/** Token inexistente, mesa inativa e estabelecimento inativo sao indistinguiveis para quem chama (404). */
@Component
class ResolverQrTokenQuery {

    private final ResolvedorDeQrToken resolvedor;
    private final EstabelecimentoRepository estabelecimentos;

    ResolverQrTokenQuery(ResolvedorDeQrToken resolvedor, EstabelecimentoRepository estabelecimentos) {
        this.resolvedor = resolvedor;
        this.estabelecimentos = estabelecimentos;
    }

    public MesaResolvidaDto executar(String qrToken) {
        var naoEncontrado = new RecursoNaoEncontradoException("QR Code", "invalido ou expirado");
        var mesa = resolvedor.resolver(qrToken).filter(m -> m.ativa()).orElseThrow(() -> naoEncontrado);
        var estabelecimento = estabelecimentos.buscar(mesa.tenantId()).filter(e -> e.ativo())
                .orElseThrow(() -> naoEncontrado);
        return new MesaResolvidaDto(mesa.tenantId().valor(), mesa.id(), mesa.rotulo(),
                estabelecimento.nomeFantasia(), estabelecimento.percentualTaxaServico());
    }
}
