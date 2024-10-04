package edu.neu.csye7255.healthcare.repository;

import edu.neu.csye7255.healthcare.exeception.CacheException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PlanCacheRepository implements CacheRepository {

    private static final Logger logger = LoggerFactory.getLogger(PlanCacheRepository.class);
    @Autowired
    private RedisTemplate<String, JSONObject> redisTemplate;


    @Override
    public void put(String key, JSONObject value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            logger.error("Error while putting value in Redis: {}", e.getMessage(), e);
            throw new CacheException("Failed to put value in Redis", e);
        }
    }

    @Override
    public Optional<JSONObject> get(String key) {
        try {
            JSONObject value = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            logger.error("Error while getting value from Redis: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void remove(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.error("Error while removing value from Redis: {}", e.getMessage(), e);
            throw new CacheException("Failed to delete plan", e);
        }
    }
}
