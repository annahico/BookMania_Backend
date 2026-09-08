package com.bookmania.bookmania.Configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void translatesRailwayDatabaseUrlIntoJdbcProperties() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgresql://myuser:my%40pass@containers-us-west-42.railway.app:6543/railway");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://containers-us-west-42.railway.app:6543/railway");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("myuser");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("my@pass");
    }

    @Test
    void defaultsToStandardPostgresPortWhenMissing() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgresql://user:pass@db.example.com/mydb");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.example.com:5432/mydb");
    }

    @Test
    void preservesQueryString() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgresql://user:pass@db.example.com:5432/mydb?sslmode=require");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.example.com:5432/mydb?sslmode=require");
    }

    @Test
    void leavesEnvironmentUntouchedWhenDatabaseUrlIsAbsent() {
        MockEnvironment env = new MockEnvironment();

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }

    @Test
    void ignoresMalformedDatabaseUrlInsteadOfFailingStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "not a valid uri");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }
}
