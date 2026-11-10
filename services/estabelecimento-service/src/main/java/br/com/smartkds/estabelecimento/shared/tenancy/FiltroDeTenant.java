package br.com.smartkds.estabelecimento.shared.tenancy;

import br.com.smartkds.estabelecimento.shared.dominio.TenantId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroDeTenant extends OncePerRequestFilter {

    public static final String CABECALHO_TENANT = "X-Tenant-Id";
    public static final String CABECALHO_USUARIO = "X-User-Id";
    public static final String CABECALHO_CORRELACAO = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        String tenant = requisicao.getHeader(CABECALHO_TENANT);
        String correlacao = requisicao.getHeader(CABECALHO_CORRELACAO);
        if (correlacao == null || correlacao.isBlank()) {
            correlacao = UUID.randomUUID().toString();
        }
        TenantId tenantId = tenant == null || tenant.isBlank() ? null : new TenantId(tenant);
        try {
            ContextoDoTenant.definir(new ContextoDoTenant.Dados(tenantId,
                    requisicao.getHeader(CABECALHO_USUARIO), correlacao, requisicao.getHeader("traceparent")));
            if (tenantId != null) {
                MDC.put("tenant_id", tenantId.valor());
            }
            MDC.put("correlation_id", correlacao);
            resposta.setHeader(CABECALHO_CORRELACAO, correlacao);
            cadeia.doFilter(requisicao, resposta);
        } finally {
            ContextoDoTenant.limpar();
            MDC.clear();
        }
    }
}
