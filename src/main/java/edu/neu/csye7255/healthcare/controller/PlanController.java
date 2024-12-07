package edu.neu.csye7255.healthcare.controller;

import edu.neu.csye7255.healthcare.exeception.*;
import edu.neu.csye7255.healthcare.service.JsonSchemaValidationService;
import edu.neu.csye7255.healthcare.service.PlanService;
import edu.neu.csye7255.healthcare.util.EtagProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/plan")
public class PlanController {

    @Autowired
    private JsonSchemaValidationService jsonSchemaService;

    @Autowired
    private PlanService planService;

    @Autowired
    private EtagProvider etagProvider;

    @Autowired
    private RabbitTemplate template;

    @Value("${rabbitmq.queue}")
    private String queueName;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPlan(@RequestBody(required = false) String planObject) {

        try {
            if (planObject == null || planObject.isBlank())
                throw new BadRequestException("Request body is missing!");
            jsonSchemaService.validateJson(planObject);
            JSONObject plan = new JSONObject(planObject);
            String key = plan.getString("objectType") + ":" + plan.getString("planType") + ":"
                    + plan.getString("objectId");
            if (planService.isKeyPresent(key))
                throw new PlanAlreadyExists("Plan already exists!");

            String eTag = planService.createPlan(plan, key);

            Map<String, String> message = new HashMap<>();
            message.put("operation", "SAVE");
            message.put("body", planObject);

            System.out.println("Sending message: " + message);
            template.convertAndSend(queueName, message);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setETag(eTag);

            return new ResponseEntity<>("Created plan with key : " + key, responseHeaders, HttpStatus.CREATED);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping(value = "/{objectType}/{planType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPlan(@PathVariable String objectType, @PathVariable String objectId,
                                     @PathVariable String planType, @RequestHeader HttpHeaders headers) {

        try {
            if (objectId == null || objectId.isBlank() || planType == null || planType.isBlank())
                throw new BadRequestException("PlanType or objectId is missing!");

            String key = objectType + ":" + planType + ":" + objectId;
            if (!planService.isKeyPresent(key))
                throw new ResourceNotFoundException("Plan not found!");
            List<String> ifNoneMatch;
            try {
                ifNoneMatch = headers.getIfNoneMatch();
            } catch (Exception e) {
                throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
            }
            System.out.println(ifNoneMatch);

            List<String> ifMatch;
            try {
                ifMatch = headers.getIfMatch();
            } catch (Exception e) {
                throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
            }
            System.out.println(ifMatch);

            Map<String, Object> objectToReturn = planService.getPlan(key);
            String eTag = objectToReturn.get("eTag").toString();

            HttpHeaders headersToSend = new HttpHeaders();
            headersToSend.setETag(eTag);

            System.out.println(eTag);
            if (objectType.equals("plan") && ifNoneMatch.contains(eTag))
                return new ResponseEntity<>(null, headersToSend, HttpStatus.NOT_MODIFIED);

            if (objectType.equals("plan") && !ifMatch.isEmpty() && !ifMatch.contains(eTag))
                return new ResponseEntity<>(null, headersToSend, HttpStatus.PRECONDITION_FAILED);

            if (objectType.equals("plan"))
                return new ResponseEntity<>(objectToReturn, headersToSend, HttpStatus.OK);

            return new ResponseEntity<>(objectToReturn, HttpStatus.OK);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/{objectType}/{planType}/{objectId}")
    public ResponseEntity<?> deletePlan(@PathVariable String objectType, @PathVariable String objectId,
                                        @PathVariable String planType, @RequestHeader HttpHeaders headers) {

        try {
            if (objectId == null || objectId.isBlank() || planType == null || planType.isBlank())
                throw new BadRequestException("PlanType or objectId is missing!");

            String key = objectType + ":" + planType + ":" + objectId;
            if (!planService.isKeyPresent(key))
                throw new ResourceNotFoundException("Plan not found!");
            planService.deletePlan(key);
            Map<String, String> message = new HashMap<>();
            message.put("operation", "DELETE");
            message.put("body", objectId);

            System.out.println("Sending message: " + message);
            template.convertAndSend(queueName, message);
            return new ResponseEntity<>("{\"message\": \"Plan deleted successfully\"}", HttpStatus.OK);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping(value = "/{objectType}/{planType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updatePlan(@PathVariable String objectType, @PathVariable String objectId,
                                        @PathVariable String planType, @RequestBody(required = false) String planObject,
                                        @RequestHeader HttpHeaders headers) {
        String currentETag = "";
        try {
            if (planObject == null || planObject.isBlank())
                throw new BadRequestException("Request body is missing!");
            jsonSchemaService.validateJson(planObject);
            JSONObject plan = new JSONObject(planObject);
            String key = objectType + ":" + planType + ":" + objectId;
            if (!planService.isKeyPresent(key))
                throw new ResourceNotFoundException("Plan not found!");

            Map<String, Object> objectToReturn = planService.getPlan(key);
            currentETag = objectToReturn.get("eTag").toString();
            List<String> ifMatch;
            try {
                ifMatch = headers.getIfMatch();
            } catch (Exception e) {
                throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
            }

            if (ifMatch.isEmpty())
                throw new ETagParseException("ETag is not provided with request!");
            if (!ifMatch.contains(currentETag))
                throw new ResourceNotModifiedException("ETag does not match!");

            planService.deletePlan(key);
            String updatedETag = planService.createPlan(plan, key);

            HttpHeaders headersToSend = new HttpHeaders();
            headersToSend.setETag(updatedETag);
            return new ResponseEntity<>("{\"message\": \"Plan updated successfully\"}", headersToSend, HttpStatus.OK);
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ResourceNotModifiedException e) {
            return ResponseEntity.status(412).eTag(currentETag).body("Precondition Failed: The plan has changed.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PatchMapping(value = "/{objectType}/{planType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> patchPlan(@PathVariable String objectType, @PathVariable String objectId,
                                       @PathVariable String planType, @RequestBody(required = false) String planObject,
                                       @RequestHeader HttpHeaders headers) {
        String currentETag = "";
        try {
            if (planObject == null || planObject.isBlank())
                throw new BadRequestException("Request body is missing!");
//            jsonSchemaService.validateJson(planObject);

            JSONObject newPlanFields = new JSONObject(planObject);
            System.out.println(objectId + " " + newPlanFields.get("objectId"));
            if (!objectId.equals(newPlanFields.get("objectId")))
                throw new BadRequestException("objectId in request body does not match the objectId in the URL!");
//            if (newPlanFields.get("objectId") != objectId)
//                throw new BadRequestException("objectId in request body does not match the objectId in the URL!");
            System.out.println(newPlanFields);
            String key = objectType + ":" + planType + ":" + objectId;
            if (!planService.isKeyPresent(key))
                throw new ResourceNotFoundException("Plan not found!");

            Map<String, Object> existingPlanMap = planService.getPlan(key);
            System.out.println(existingPlanMap);
//            JSONObject existingPlan = new JSONObject(existingPlanMap.get("plan").toString());
            String existingPlanString = existingPlanMap.get("plan").toString();
            System.out.println(existingPlanString);

            JSONObject existingPlan = planService.getPlanObject(key);
            System.out.println(existingPlan);
            currentETag = existingPlanMap.get("eTag").toString();
            List<String> ifMatch;
            try {
                ifMatch = headers.getIfMatch();
            } catch (Exception e) {
                throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
            }

            if (ifMatch.isEmpty())
                throw new ETagParseException("ETag is not provided with request!");
            if (!ifMatch.contains(currentETag))
                throw new ResourceNotModifiedException("ETag does not match!");
            System.out.println(213);

            for (String keyField : newPlanFields.keySet()) {
                if (keyField.equals("linkedPlanServices")) {
                    // Handle linkedPlanServices separately
                    JSONArray newServicesArray = newPlanFields.getJSONArray(keyField);
                    JSONArray existingServicesArray = existingPlan.getJSONArray(keyField);

                    for (int i = 0; i < newServicesArray.length(); i++) {
                        JSONObject newService = newServicesArray.getJSONObject(i);
                        boolean exists = false;

                        for (int j = 0; j < existingServicesArray.length(); j++) {
                            JSONObject existingService = existingServicesArray.getJSONObject(j);

                            // Check if objectId in both linkedService and planserviceCostShares matches
                            boolean linkedServiceIdMatches = existingService.getJSONObject("linkedService").getString("objectId")
                                    .equals(newService.getJSONObject("linkedService").getString("objectId"));
                            boolean planserviceCostSharesIdMatches = existingService.getJSONObject("planserviceCostShares").getString("objectId")
                                    .equals(newService.getJSONObject("planserviceCostShares").getString("objectId"));

                            if (linkedServiceIdMatches && planserviceCostSharesIdMatches) {
                                // Update existing service contents
                                for (String serviceKey : newService.keySet()) {
                                    existingService.put(serviceKey, newService.get(serviceKey));
                                }
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            // Append new service to the array if objectId is different
                            existingServicesArray.put(newService);
                        }
                    }

                } else if (keyField.equals("planCostShares")) {
                    // Handle planCostShares separately
                    JSONObject newPlanCostShares = newPlanFields.getJSONObject(keyField);
                    JSONObject existingPlanCostShares = existingPlan.getJSONObject(keyField);

                    // Check if the objectId matches
                    if (!existingPlanCostShares.getString("objectId").equals(newPlanCostShares.getString("objectId"))) {
                        throw new BadRequestException("Bad Request: New objectId in planCostShares is not allowed.");
                    }

                    // Update planCostShares if objectId matches
                    for (String costShareKey : newPlanCostShares.keySet()) {
                        existingPlanCostShares.put(costShareKey, newPlanCostShares.get(costShareKey));
                    }

                } else {
                    // Update other fields directly
                    existingPlan.put(keyField, newPlanFields.get(keyField));
                }
            }

            System.out.println(existingPlan);


            System.out.println(existingPlan);
            planService.deletePlan(key);

            Map<String, String> message = new HashMap<>();
            message.put("operation", "DELETE");
            message.put("body", objectId);

            System.out.println("Sending message: " + message);
            template.convertAndSend(queueName, message);

            String updatedETag = planService.createPlan(existingPlan, key);

            message = new HashMap<>();
            message.put("operation", "SAVE");
            message.put("body", existingPlan.toString());

            System.out.println("Sending message: " + message);
            template.convertAndSend(queueName, message);

            HttpHeaders headersToSend = new HttpHeaders();
            headersToSend.setETag(updatedETag);

            return new ResponseEntity<>("{\"message\": \"Plan updated successfully\"}", headersToSend, HttpStatus.OK);
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ResourceNotModifiedException e) {
            return ResponseEntity.status(412).eTag(currentETag).body("Precondition Failed: The plan has changed.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

}
