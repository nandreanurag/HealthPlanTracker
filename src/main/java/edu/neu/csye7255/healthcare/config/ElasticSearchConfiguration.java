package edu.neu.csye7255.healthcare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;


@Configuration
public class ElasticSearchConfiguration extends ElasticsearchConfiguration {

    @Override
    public ClientConfiguration clientConfiguration() {
        try {
            System.out.println("elastic connection successful");
            return ClientConfiguration.builder()
                    .connectedTo("localhost:9200").
                    build();
        } catch (Exception e) {
            System.out.println("Error connecting elastic " + e.getMessage());
        }
        return null;
    }
}
