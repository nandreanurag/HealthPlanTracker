package edu.neu.csye7255.healthcare.controller;

import edu.neu.csye7255.healthcare.exeception.*;
import edu.neu.csye7255.healthcare.service.JsonSchemaValidationService;
import edu.neu.csye7255.healthcare.service.PlanService;
import edu.neu.csye7255.healthcare.util.EtagProvider;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPlan(@RequestBody(required = false) String planObject) {

        try {
            if (planObject == null || planObject.isBlank()) throw new BadRequestException("Request body is missing!");
            jsonSchemaService.validateJson(planObject);
            JSONObject plan = new JSONObject(planObject);
            String key = plan.getString("objectType") + ":" + plan.getString("planType") + ":" + plan.getString("objectId");
            if (planService.isKeyPresent(key)) throw new PlanAlreadyExists("Plan already exists!");

            String eTag = planService.createPlan(plan, key);

//            return new ResponseEntity<>("{\"objectId\": \"" + plan.getString("objectId") + "\"}", headersToSend, HttpStatus.CREATED);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setETag(eTag);

            return new ResponseEntity<>("Created plan with key : " + key, responseHeaders, HttpStatus.CREATED);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
//            System.out.println(e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping(value = "/{objectType}/{planType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPlan(@PathVariable String objectType, @PathVariable String objectId,
                                     @PathVariable String planType,
                                     @RequestHeader HttpHeaders headers) {

        try {
            if (objectId == null || objectId.isBlank() || planType == null || planType.isBlank())
                throw new BadRequestException("PlanType or objectId is missing!");

            String key = objectType + ":" + planType + ":" + objectId;
            if (!planService.isKeyPresent(key)) throw new ResourceNotFoundException("Plan not found!");

            // Check if the ETag provided is not corrupt
            List<String> ifNoneMatch;
            try {
                ifNoneMatch = headers.getIfNoneMatch();
            } catch (Exception e) {
                throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
            }

            Map<String, Object> objectToReturn = planService.getPlan(key);
            String eTag = objectToReturn.get("eTag").toString();
            ;
            HttpHeaders headersToSend = new HttpHeaders();
            headersToSend.setETag(eTag);


            if (objectType.equals("plan") && ifNoneMatch.contains(eTag))
                return new ResponseEntity<>(null, headersToSend, HttpStatus.NOT_MODIFIED);

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
                                        @PathVariable String planType,
                                        @RequestHeader HttpHeaders headers) {

        try {
            if (objectId == null || objectId.isBlank() || planType == null || planType.isBlank())
                throw new BadRequestException("PlanType or objectId is missing!");

            String key = objectType + ":" + planType + ":" + objectId;
            if (!planService.isKeyPresent(key)) throw new ResourceNotFoundException("Plan not found!");
            planService.deletePlan(key);
            return new ResponseEntity<>("{\"message\": \"Plan deleted successfully\"}",
                    HttpStatus.OK);
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
                                        @PathVariable String planType,
                                        @RequestBody(required = false) String planObject,
                                        @RequestHeader HttpHeaders headers) {
        String currentETag = "";
        try {
            if (planObject == null || planObject.isBlank()) throw new BadRequestException("Request body is missing!");
            jsonSchemaService.validateJson(planObject);
            JSONObject plan = new JSONObject(planObject);
            String key = objectType + ":" + planType + ":" + objectId;
            if (!planService.isKeyPresent(key)) throw new ResourceNotFoundException("Plan not found!");

            Map<String, Object> objectToReturn = planService.getPlan(key);
            currentETag = objectToReturn.get("eTag").toString();
            List<String> ifMatch;
            try {
                ifMatch = headers.getIfMatch();
            } catch (Exception e) {
                throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
            }

            if (ifMatch.isEmpty()) throw new ETagParseException("ETag is not provided with request!");
            if (!ifMatch.contains(currentETag)) throw new ResourceNotModifiedException("ETag does not match!");

            planService.deletePlan(key);
            String updatedETag = planService.createPlan(plan, key);

            HttpHeaders headersToSend = new HttpHeaders();
            headersToSend.setETag(updatedETag);
            return new ResponseEntity<>("{\"message\": \"Plan updated successfully\"}", headersToSend, HttpStatus.OK);
        } catch (BadRequestException | ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ResourceNotModifiedException e) {
            return ResponseEntity.status(412).eTag(currentETag).body("Precondition Failed: The plan has changed.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
