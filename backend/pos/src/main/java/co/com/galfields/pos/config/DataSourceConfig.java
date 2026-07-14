package co.com.galfields.pos.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * The DataSource JPA/Hibernate actually use: routes to the replica for
     * read-only transactions, the primary otherwise (see RoutingDataSource).
     */
    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("replicaDataSource") DataSource replicaDataSource
    ) {
        RoutingDataSource routingDataSource = new RoutingDataSource();
    
        routingDataSource.setTargetDataSources(Map.of(
                RoutingDataSource.PRIMARY, primaryDataSource,
                RoutingDataSource.REPLICA, replicaDataSource));
    
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }
}
