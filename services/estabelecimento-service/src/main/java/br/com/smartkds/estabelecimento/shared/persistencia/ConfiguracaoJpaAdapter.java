package br.com.smartkds.estabelecimento.shared.persistencia;

import br.com.smartkds.estabelecimento.shared.dominio.Estabelecimento;
import br.com.smartkds.estabelecimento.shared.dominio.Estacao;
import br.com.smartkds.estabelecimento.shared.dominio.Mesa;
import br.com.smartkds.estabelecimento.shared.dominio.RegraDeRoteamento;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JPA de todas as portas do contexto Estabelecimento. */
@Component
public class ConfiguracaoJpaAdapter implements EstabelecimentoRepository, MesaRepository, EstacaoRepository,
        RegraRoteamentoRepository, ResolvedorDeQrToken {

    interface Estabelecimentos extends JpaRepository<Entidades.EstabelecimentoEntity, String> { }

    interface Mesas extends JpaRepository<Entidades.MesaEntity, UUID> {
        List<Entidades.MesaEntity> findByTenantIdOrderByRotuloAsc(String tenantId);

        Optional<Entidades.MesaEntity> findByTenantIdAndId(String tenantId, UUID id);

        Optional<Entidades.MesaEntity> findByQrToken(String qrToken);
    }

    interface Estacoes extends JpaRepository<Entidades.EstacaoEntity, UUID> {
        List<Entidades.EstacaoEntity> findByTenantIdOrderByOrdemExibicaoAsc(String tenantId);

        Optional<Entidades.EstacaoEntity> findByTenantIdAndCodigo(String tenantId, String codigo);
    }

    interface Regras extends JpaRepository<Entidades.RegraEntity, UUID> {
        List<Entidades.RegraEntity> findByTenantIdOrderByPrioridadeDesc(String tenantId);

        @Modifying
        long deleteByTenantId(String tenantId);
    }

    private final Estabelecimentos estabelecimentos;
    private final Mesas mesas;
    private final Estacoes estacoes;
    private final Regras regras;

    ConfiguracaoJpaAdapter(Estabelecimentos estabelecimentos, Mesas mesas, Estacoes estacoes, Regras regras) {
        this.estabelecimentos = estabelecimentos;
        this.mesas = mesas;
        this.estacoes = estacoes;
        this.regras = regras;
    }

    // ---------------- Estabelecimento ----------------
    @Override
    @Transactional
    public Estabelecimento salvar(Estabelecimento e) {
        var entidade = new Entidades.EstabelecimentoEntity();
        entidade.tenantId = e.tenantId().valor();
        entidade.nomeFantasia = e.nomeFantasia();
        entidade.fusoHorario = e.fusoHorario();
        entidade.percentualTaxaServico = e.percentualTaxaServico();
        entidade.estacaoPadrao = e.estacaoPadrao();
        entidade.ativo = e.ativo();
        estabelecimentos.save(entidade);
        return e;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Estabelecimento> buscar(TenantId tenantId) {
        return estabelecimentos.findById(tenantId.valor()).map(e -> new Estabelecimento(new TenantId(e.tenantId),
                e.nomeFantasia, e.fusoHorario, e.percentualTaxaServico, e.estacaoPadrao, e.ativo));
    }

    // ---------------- Mesa ----------------
    @Override
    @Transactional
    public Mesa salvar(Mesa m) {
        var entidade = new Entidades.MesaEntity();
        entidade.id = m.id();
        entidade.tenantId = m.tenantId().valor();
        entidade.rotulo = m.rotulo();
        entidade.qrToken = m.qrToken();
        entidade.ativa = m.ativa();
        mesas.save(entidade);
        return m;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mesa> listar(TenantId tenantId) {
        return mesas.findByTenantIdOrderByRotuloAsc(tenantId.valor()).stream().map(ConfiguracaoJpaAdapter::mesa).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Mesa> buscarPorId(TenantId tenantId, UUID mesaId) {
        return mesas.findByTenantIdAndId(tenantId.valor(), mesaId).map(ConfiguracaoJpaAdapter::mesa);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Mesa> resolver(String qrToken) {
        return mesas.findByQrToken(qrToken).map(ConfiguracaoJpaAdapter::mesa);
    }

    private static Mesa mesa(Entidades.MesaEntity e) {
        return new Mesa(e.id, new TenantId(e.tenantId), e.rotulo, e.qrToken, e.ativa);
    }

    // ---------------- Estacao ----------------
    @Override
    @Transactional
    public Estacao salvar(Estacao s) {
        var entidade = new Entidades.EstacaoEntity();
        entidade.id = s.id();
        entidade.tenantId = s.tenantId().valor();
        entidade.codigo = s.codigo();
        entidade.nome = s.nome();
        entidade.ordemExibicao = s.ordemExibicao();
        entidade.percentualAtencao = s.percentualAtencao();
        entidade.ativa = s.ativa();
        estacoes.save(entidade);
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Estacao> buscarPorCodigo(TenantId tenantId, String codigo) {
        return estacoes.findByTenantIdAndCodigo(tenantId.valor(), codigo).map(ConfiguracaoJpaAdapter::estacao);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Estacao> listarEstacoes(TenantId tenantId) {
        return estacoes.findByTenantIdOrderByOrdemExibicaoAsc(tenantId.valor()).stream()
                .map(ConfiguracaoJpaAdapter::estacao).toList();
    }

    private static Estacao estacao(Entidades.EstacaoEntity e) {
        return new Estacao(e.id, new TenantId(e.tenantId), e.codigo, e.nome, e.ordemExibicao, e.percentualAtencao,
                e.ativa);
    }

    // ---------------- Regras ----------------
    @Override
    @Transactional
    public void substituirTodas(TenantId tenantId, List<RegraDeRoteamento> novas) {
        regras.deleteByTenantId(tenantId.valor());
        regras.flush();
        regras.saveAll(novas.stream().map(r -> {
            var e = new Entidades.RegraEntity();
            e.id = r.id();
            e.tenantId = tenantId.valor();
            e.itemCardapioId = r.itemCardapioId();
            e.categoriaCardapio = r.categoria();
            e.estacaoCodigo = r.estacaoCodigo();
            e.prioridade = r.prioridade();
            return e;
        }).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegraDeRoteamento> listarRegras(TenantId tenantId) {
        return regras.findByTenantIdOrderByPrioridadeDesc(tenantId.valor()).stream()
                .map(e -> new RegraDeRoteamento(e.id, new TenantId(e.tenantId), e.itemCardapioId, e.categoriaCardapio,
                        e.estacaoCodigo, e.prioridade))
                .toList();
    }
}
