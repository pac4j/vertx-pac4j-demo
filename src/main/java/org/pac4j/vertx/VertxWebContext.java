/*
  Copyright 2014 - 2014 Michael Remond

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.vertx;

import java.util.HashMap;
import java.util.Map;

import org.pac4j.core.context.WebContext;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;

/**
 * WebContext implementation for Vert.x.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class VertxWebContext implements WebContext {

    private final HttpServerRequest request;

    private final JsonObject attributes;

    private final HttpServerResponse response;

    private final String sessionId;

    public VertxWebContext(HttpServerRequest request, String sessionId, JsonObject attributes) {
        this.request = request;
        this.response = request.response();
        this.sessionId = sessionId;
        this.attributes = attributes;
    }

    @Override
    public String getRequestParameter(String name) {
        String param = request.params().get(name);
        if (param == null & request.params().get(Constants.FORM_ATTRIBUTES) != null) {
            param = request.formAttributes().get(name);
            if (param != null) {
                // FIX for Vert.x
                param = param.replaceAll("\\s", "+");
            }
        }
        return param;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        final Map<String, String[]> map = new HashMap<>();
        if (request.params().get(Constants.FORM_ATTRIBUTES) != null) {
            for (String name : request.formAttributes().names()) {
                map.put(name, request.formAttributes().getAll(name).toArray(new String[0]));
            }
        }
        for (String name : request.params().names()) {
            map.put(name, request.params().getAll(name).toArray(new String[0]));
        }
        return map;
    }

    @Override
    public String getRequestHeader(String name) {
        return request.headers().get(name);
    }

    @Override
    public void setSessionAttribute(String name, Object value) {
        //System.out.println("set attribute " + name + "=" + value + " => " + JsonObjectConverter.encode(value));
        attributes.putValue(name, JsonObjectConverter.encode(value));
    }

    public JsonObject getOutAttributes() {
        return attributes;
    }

    @Override
    public Object getSessionAttribute(String name) {
        return JsonObjectConverter.decode(attributes.getValue(name));
    }

    @Override
    public String getRequestMethod() {
        return request.method();
    }

    @Override
    public void writeResponseContent(String content) {
        response.end(content);
    }

    @Override
    public void setResponseStatus(int code) {
        response.setStatusCode(code);
    }

    @Override
    public void setResponseHeader(String name, String value) {
        response.headers().set(name, value);
    }

    @Override
    public String getServerName() {
        return getRequestHeader(Constants.HOST_HEADER).split(":")[0];
    }

    @Override
    public int getServerPort() {
        String[] tab = getRequestHeader(Constants.HOST_HEADER).split(":");
        if (tab.length > 1) {
            return Integer.parseInt(tab[1]);
        }
        if ("http".equals(getScheme())) {
            return 80;
        } else {
            return 443;
        }
    }

    @Override
    public String getScheme() {
        return request.absoluteURI().getScheme();
    }

    @Override
    public String getFullRequestURL() {
        return getScheme() + "://" + getRequestHeader(Constants.HOST_HEADER) + request.uri();
    }

}
