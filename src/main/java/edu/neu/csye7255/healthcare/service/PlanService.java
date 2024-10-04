package edu.neu.csye7255.healthcare.service;

import edu.neu.csye7255.healthcare.exeception.CacheException;
import edu.neu.csye7255.healthcare.exeception.ResourceNotFoundException;
import edu.neu.csye7255.healthcare.exeception.ResourceNotModifiedException;
import edu.neu.csye7255.healthcare.repository.CacheRepository;
import edu.neu.csye7255.healthcare.repository.PlanCacheRepository;
import edu.neu.csye7255.healthcare.util.EtagProvider;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PlanService {

    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);
    @Autowired
    private CacheRepository cacheRepository;

    @Autowired
    private EtagProvider etagProvider;

    public String createPlan(JSONObject plan, String key) {
        String eTag = etagProvider.getETag(plan);
        try {
            cacheRepository.put(key, plan);
        } catch (CacheException e) {
            logger.error("Failed to create plan: {}", e.getMessage(), e);
            throw e;
        }
        return eTag;
    }

    public boolean isKeyPresent(String key) {
        return cacheRepository.get(key).isPresent();
    }

    public Map<String, Object> getPlan(String key) {

        Optional<JSONObject> plan = cacheRepository.get(key);
        if (plan.isEmpty()) {
            throw new ResourceNotFoundException("Plan not found!");
        }
        JSONObject planObject = plan.get();
        Map<String, Object> response = new HashMap<>();
        response.put("plan", planObject.toMap());
        response.put("eTag", etagProvider.getETag(planObject));
        return response;
    }

    public void deletePlan(String key) {
        try {
            cacheRepository.remove(key);
        } catch (CacheException e) {
            logger.error("Failed to delete plan: {}", e.getMessage(), e);
            throw e;
        }
    }
}
