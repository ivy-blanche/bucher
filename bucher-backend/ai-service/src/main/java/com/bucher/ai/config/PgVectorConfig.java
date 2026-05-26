package com.bucher.ai.config;

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PGVector 向量存储配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pgvector")
public class PgVectorConfig {

    private String host = "localhost";
    private int port = 5432;
    private String database = "bucher_vector";
    private String username = "postgres";
    private String password = "postgres";
    private String embeddingTable = "ai_embeddings";
    private int dimension = 1024;

    @Bean
    public PgVectorEmbeddingStore embeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(username)
                .password(password)
                .table(embeddingTable)
                .dimension(dimension)
                .useIndex(true)
                .indexListSize(100)
                .createTable(true)
                .dropTableFirst(false)
                .build();
    }
}
