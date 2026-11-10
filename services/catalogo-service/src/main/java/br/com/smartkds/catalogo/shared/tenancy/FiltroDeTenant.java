package br.com.smartkds.catalogo.shared.tenancy;

import br.com.smartkds.catalogo.shared.dominio.TenantId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class FiltroDeTenant extends OncePerRequestFilter {

    public static final String CABECALHO_TENANT = "X-Tenant-Id";
    public static final String CABECALHO_CORRELACAO = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        String tenant = requisicao.getHeader(CABECALHO_TENANT);
        String correlacao = requisicao.getHeader(CABECALHO_CORRELACAO);
        if (correlacao == null || correlacao.isBlank()) {
            correlacao = UUID.randomUUID().toString();
        }
        try {
            if (tenant != null && !tenant.isBlank()) {
                ContextoDoTenant.definir(new TenantId(tenant));
                MDC.put("tenant_id", tenant);
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
