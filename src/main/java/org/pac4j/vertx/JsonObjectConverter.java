package org.pac4j.vertx;

import java.util.HashMap;
import java.util.Map;

import org.scribe.model.Token;
import org.vertx.java.core.json.JsonObject;

public class JsonObjectConverter {

    private static Map<String, Converter<? extends Object>> map;
    static {
        map = new HashMap<>();
        map.put("org.scribe.model.Token", new TokenConverter());
    }

    public static void addConverter(String className, Converter<? extends Object> converter) {
        map.put(className, converter);
    }

    public static Object encode(Object value) {
        if (value == null) {
            return null;
        }
        Converter converter = map.get(value.getClass().getName());
        if (converter != null) {
            return new JsonObject().putString("class", value.getClass().getName()).putValue("value",
                    converter.encode(value));
        } else {
            return value.toString();
        }
    }

    public static Object decode(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonObject) {
            JsonObject json = (JsonObject) value;
            Converter<? extends Object> converter = map.get(json.getString("class"));
            if (converter != null) {
                return converter.decode(json.getValue("value"));
            }
        }
        return value.toString();
    }

    public static interface Converter<T extends Object> {

        Object encode(T t);

        T decode(Object o);

    }

    public static class TokenConverter implements Converter<Token> {

        @Override
        public Object encode(Token t) {
            return new JsonObject().putString("token", t.getToken()).putString("secret", t.getSecret());
        }

        @Override
        public Token decode(Object o) {
            JsonObject json = (JsonObject) o;
            return new Token(json.getString("token"), json.getString("secret"));
        }

    }

}
