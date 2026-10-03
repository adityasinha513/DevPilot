package devPilot.backend.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {

    private final DatabaseConfig config = new DatabaseConfig();

    @Test
    void testStandardJdbcUrlPassedThrough() {
        DataSource ds = config.dataSource("jdbc:postgresql://localhost:5433/devpilot", "myuser", "mypass", "org.postgresql.Driver");
        assertNotNull(ds);
        assertTrue(ds instanceof HikariDataSource);
        HikariDataSource hikari = (HikariDataSource) ds;
        assertEquals("jdbc:postgresql://localhost:5433/devpilot", hikari.getJdbcUrl());
        assertEquals("myuser", hikari.getUsername());
        assertEquals("mypass", hikari.getPassword());
        hikari.close();
    }

    @Test
    void testPostgresqlUriConvertedToJdbc() {
        DataSource ds = config.dataSource("postgresql://render_user:render_secret@dpg-abc12345-a.oregon-postgres.render.com:5432/devpilot", "", "", "org.postgresql.Driver");
        assertNotNull(ds);
        assertTrue(ds instanceof HikariDataSource);
        HikariDataSource hikari = (HikariDataSource) ds;
        assertEquals("jdbc:postgresql://dpg-abc12345-a.oregon-postgres.render.com:5432/devpilot", hikari.getJdbcUrl());
        assertEquals("render_user", hikari.getUsername());
        assertEquals("render_secret", hikari.getPassword());
        hikari.close();
    }

    @Test
    void testPostgresUriConvertedToJdbc() {
        DataSource ds = config.dataSource("postgres://render_user:render_secret@dpg-abc12345-a:5432/devpilot_db?sslmode=require", "", "", "org.postgresql.Driver");
        assertNotNull(ds);
        assertTrue(ds instanceof HikariDataSource);
        HikariDataSource hikari = (HikariDataSource) ds;
        assertEquals("jdbc:postgresql://dpg-abc12345-a:5432/devpilot_db?sslmode=require", hikari.getJdbcUrl());
        assertEquals("render_user", hikari.getUsername());
        assertEquals("render_secret", hikari.getPassword());
        hikari.close();
    }
}
