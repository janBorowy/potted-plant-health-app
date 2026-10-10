package pl.student.sensors.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        log.info("[REQUEST] {} {}{}", request.getMethod(), request.getRequestURI(), query);
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("[RESPONSE] {} {}{} status={}", request.getMethod(), request.getRequestURI(), query,
                    response.getStatus());
        }
    }
}
