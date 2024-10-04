package edu.neu.csye7255.healthcare.repository;

import org.json.JSONObject;

import java.util.Optional;


public interface CacheRepository {

    void put(String key, JSONObject value);


    Optional<JSONObject> get(String key);

    void remove(String key);

}