package edu.neu.csye7255.healthcare.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
public class IndexingListener {
    private ElasticsearchOperations elasticsearchOperations;
    private ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "plan-index";
    private static LinkedHashMap<String, Map<String, Object>> Mapd = new LinkedHashMap<>();
    private static ArrayList<String> keylist = new ArrayList<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public IndexingListener(ElasticsearchOperations elasticsearchOperations, ElasticsearchClient elasticsearchClient) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.elasticsearchClient = elasticsearchClient;
    }

    @RabbitListener(queues = "indexing-queue-info7255")
    public void receiveMessage(Map<String, String> message) throws IOException {

        try {
            System.out.println("Message received: " + message);
            if (this.elasticsearchOperations == null) {
                System.out.println("ElasticsearchOperations is null");
            }
            if (!isElasticsearchConnectionOpen()) {
                System.out.println("Elasticsearch connection is not open");
                return;
            }

            String operation = message.get("operation");
            String body = message.get("body");


            if (operation.equals("SAVE")) {
                JSONObject jsonBody = new JSONObject(body);
                postDocument(jsonBody);
            } else if (operation.equals("DELETE")) deleteDocument(body);
        } catch (Exception e) {
            System.out.println("Error processing message: " + e.getMessage());
        }
    }

    private boolean isElasticsearchConnectionOpen() {
        try {
            return elasticsearchClient.ping().value();
        } catch (IOException e) {
            System.out.println("Error pinging Elasticsearch: " + e.getMessage());
            return false;
        }
    }

    public void postDocument(JSONObject jsonObject) {
        String parentId = jsonObject.getString("objectId");

        // Step 1: Insert Parent Document (plan)
        jsonObject.put("relations", Map.of("name", "plan"));
        insertDocument(jsonObject.toMap(), parentId, null);

        // Step 2: Insert planCostShares child
        if (jsonObject.has("planCostShares")) {
            JSONObject planCostShares = jsonObject.getJSONObject("planCostShares");
            planCostShares.put("relations", Map.of("name", "planCostShares", "parent", parentId));
            insertDocument(planCostShares.toMap(), planCostShares.getString("objectId"), parentId);
        }

        // Step 3: Insert linkedPlanServices and their children
        if (jsonObject.has("linkedPlanServices")) {
            for (Object obj : jsonObject.getJSONArray("linkedPlanServices")) {
                JSONObject linkedService = (JSONObject) obj;
                String linkedServiceId = linkedService.getString("objectId");

                linkedService.put("relations", Map.of("name", "linkedPlanServices", "parent", parentId));
                insertDocument(linkedService.toMap(), linkedServiceId, parentId);

                // Insert grandchildren
                if (linkedService.has("linkedService")) {
                    JSONObject grandChildService = linkedService.getJSONObject("linkedService");
                    grandChildService.put("relations", Map.of("name", "linkedService", "parent", linkedServiceId));
                    insertDocument(grandChildService.toMap(), grandChildService.getString("objectId"), linkedServiceId);
                }
                if (linkedService.has("planserviceCostShares")) {
                    JSONObject planserviceCostShares = linkedService.getJSONObject("planserviceCostShares");
                    planserviceCostShares.put("relations", Map.of("name", "planserviceCostShares", "parent", linkedServiceId));
                    insertDocument(planserviceCostShares.toMap(), planserviceCostShares.getString("objectId"), linkedServiceId);
                }
            }
        }
    }

    /**
     * Insert a document into Elasticsearch.
     */
    private void insertDocument(Map<String, Object> document, String id, String routing) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(INDEX_NAME);

        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(id)
                .withObject(document)
                .withRouting(routing)
                .build();

        elasticsearchOperations.index(indexQuery, indexCoordinates);
    }


    public void deleteDocument(String parentId) {
        try {
            // Step 1: Query all child documents using the join field
            Query childQuery = Query.of(q -> q
                    .term(t -> t
                            .field("relations.parent")
                            .value(parentId)
                    )
            );

            SearchRequest searchRequest = SearchRequest.of(sr -> sr
                    .index(INDEX_NAME)
                    .query(childQuery)
            );

            SearchResponse<Object> searchResponse = elasticsearchClient.search(searchRequest, Object.class);

            // Step 2: Delete all child documents
            List<Hit<Object>> hits = searchResponse.hits().hits();
            for (Hit<Object> hit : hits) {
                String childId = hit.id();
                deleteDocument(childId, parentId); // Delete each child document
            }

            // Step 3: Delete the parent document
            deleteDocument(parentId, null); // Delete the parent document
            System.out.println("Parent and all child documents deleted successfully.");
        } catch (Exception e) {
            System.err.println("Error while deleting parent and children: " + e.getMessage());
        }
    }

    public void deleteDocument(String objectId, String routing) {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(builder -> {
                builder.index(INDEX_NAME).id(objectId); // Set index and document ID
                if (routing != null && !routing.isEmpty()) {
                    builder.routing(routing); // Set routing for child documents
                }
                return builder;
            });

            elasticsearchClient.delete(deleteRequest);
            System.out.println("Document deleted with ID: " + objectId);
        } catch (IOException e) {
            System.err.println("Error while deleting document with ID " + objectId + ": " + e.getMessage());
        }
    }


    public void createElasticIndex() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME));

        // Delete the index if it already exists (optional)
        if (indexOperations.exists()) {
            indexOperations.delete();
        }

        // Define Settings
        Map<String, Object> settings = Map.of(
                "index.number_of_shards", 1,
                "index.number_of_replicas", 1
        );

        // Define Mapping
        Map<String, Object> mapping = getMapping();

        // Convert Mapping to Document
        Document mappingDocument = Document.from(mapping);

        // Create Index with Settings
        indexOperations.create(settings);

        // Apply Mapping
        boolean mappingAcknowledged = indexOperations.putMapping(mappingDocument);
        if (mappingAcknowledged) {
            System.out.println("Mapping successfully applied.");
        } else {
            System.out.println("Failed to apply mapping.");
        }
    }


    private Map<String, Object> getMapping() {
        return Map.of(
                "properties", Map.of(
                        "relations", Map.of(
                                "type", "join",
                                "relations", Map.of(
                                        "plan", List.of("planCostShares", "linkedPlanServices"),
                                        "linkedPlanServices", List.of("linkedService", "planserviceCostShares")
                                )
                        ),
                        "planType", Map.of("type", "keyword"),
                        "creationDate", Map.of("type", "date", "format", "MM-dd-yyyy"),
                        "deductible", Map.of("type", "double"),
                        "copay", Map.of("type", "double"),
                        "_org", Map.of("type", "keyword"),
                        "objectId", Map.of("type", "keyword"),
                        "objectType", Map.of("type", "keyword"),
                        "name", Map.of("type", "text")
                )
        );
    }

}
