package br.com.eyesproject.eyes_project_back.modules.scan.infrastructure;

import br.com.eyesproject.eyes_project_back.global.exceptions.ApiProblemFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class ScanRequestSizeFilter extends OncePerRequestFilter {
    private static final int MAX_BYTES = 65536;
    private final ApiProblemFactory problemFactory;
    private final ObjectMapper mapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/v1/scan-sessions") || !"POST".equals(request.getMethod());
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (body.length > MAX_BYTES) {
            response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
            response.setContentType("application/problem+json");
            mapper.writeValue(response.getOutputStream(), problemFactory.create(HttpStatus.CONTENT_TOO_LARGE,
                    "METADATA_TOO_LARGE", "Metadados excedem o limite", "O corpo de metadados deve ter até 64 KiB.", request));
            return;
        }
        chain.doFilter(new BoundedRequest(request, body), response);
    }
    private static final class BoundedRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        private BoundedRequest(HttpServletRequest request, byte[] body) { super(request); this.body = body; }
        @Override
        public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Synchronous metadata endpoint");
                }
            };
        }
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
