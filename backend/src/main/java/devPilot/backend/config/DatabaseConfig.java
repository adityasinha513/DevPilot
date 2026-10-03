package devPilot.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String rawUrl,
            @Value("${spring.datasource.username:}") String username,
            @Value("${spring.datasource.password:}") String password,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName) {

        String jdbcUrl = rawUrl;
        String resolvedUsername = username;
        String resolvedPassword = password;

        if (rawUrl != null && (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            try {
                URI uri = new URI(rawUrl.replace("postgres://", "postgresql://"));
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath() != null ? uri.getPath() : "";
                String query = uri.getQuery();

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null ? "?" + query : "");

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    if (userInfo.length > 0 && (resolvedUsername == null || resolvedUsername.isBlank())) {
                        resolvedUsername = userInfo[0];
                    }
                    if (userInfo.length > 1 && (resolvedPassword == null || resolvedPassword.isBlank())) {
                        resolvedPassword = userInfo[1];
                    }
                }
                log.info("Normalized PostgreSQL connection URL for host: {}:{}", host, port);
            } catch (URISyntaxException e) {
                if (!rawUrl.startsWith("jdbc:")) {
                    jdbcUrl = "jdbc:" + rawUrl;
                }
            }
        }

        if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql://")) {
            if ((jdbcUrl.contains("supabase.co") || jdbcUrl.contains("supabase.com")) && !jdbcUrl.contains("sslmode=")) {
                jdbcUrl = jdbcUrl.contains("?") ? jdbcUrl + "&sslmode=require" : jdbcUrl + "?sslmode=require";
            }
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        if (resolvedUsername != null && !resolvedUsername.isBlank()) {
            ds.setUsername(resolvedUsername);
        }
        if (resolvedPassword != null && !resolvedPassword.isBlank()) {
            ds.setPassword(resolvedPassword);
        }
        ds.setDriverClassName(driverClassName);
        return ds;
    }
}
