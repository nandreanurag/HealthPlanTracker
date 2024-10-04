package edu.neu.csye7255.healthcare.util;

import org.json.JSONObject;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

public class JSONObjectRedisSerializer implements RedisSerializer<JSONObject> {

    @Override
    public byte[] serialize(JSONObject jsonObject) throws SerializationException {
        if (jsonObject == null) {
            return null;
        }
        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public JSONObject deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    }
}
