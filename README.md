HealthPlanTracker


docker run -it --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4.0-management

docker run -p 9200:9200 \
-e "discovery.type=single-node" \
-e "xpack.security.enabled=false" \
docker.elastic.co/elasticsearch/elasticsearch:8.8.1


docker run -p 5601:5601 \
-e "ELASTICSEARCH_HOSTS=http://localhost:9200" \
-e "XPACK_SECURITY_ENABLED=false" \
docker.elastic.co/kibana/kibana:8.8.1


GET _cat/indices?v

GET plan-index/_search
{
"query": {
"match_all": {}
}
}


docker compose up

docker compose down




PUT /plan-index
{
"mappings": {
"properties": {
"plan_join": {
"type": "join",
"relations": {
"plan": ["linkedPlanServices", "planCostShares"]
}
},
"planCostShares": {
"properties": {
"deductible": { "type": "long" },
"_org": { "type": "text" },
"copay": { "type": "long" },
"objectId": { "type": "keyword" },
"objectType": { "type": "text" }
}
},
"linkedPlanServices": {
"type": "nested",
"properties": {
"linkedService": {
"properties": {
"_org": { "type": "text" },
"objectId": { "type": "keyword" },
"objectType": { "type": "text" },
"name": { "type": "text" }
}
},
"planserviceCostShares": {
"properties": {
"deductible": { "type": "long" },
"_org": { "type": "text" },
"copay": { "type": "long" },
"objectId": { "type": "keyword" },
"objectType": { "type": "text" }
}
},
"_org": { "type": "text" },
"objectId": { "type": "keyword" },
"objectType": { "type": "text" }
}
},
"_org": { "type": "text" },
"objectId": { "type": "keyword" },
"objectType": { "type": "text" },
"planType": { "type": "text" },
"creationDate": {
"type": "date",
"format": "MM-dd-yyyy"
}
}
}
}


DELETE plan-index/_doc/12xvxc345ssdsds-508

GET /plan-index/_mapping