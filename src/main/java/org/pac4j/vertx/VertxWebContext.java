package org.pac4j.vertx;

import java.util.HashMap;
import java.util.Map;

import org.pac4j.core.context.WebContext;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

public class VertxWebContext implements WebContext {

    private final HttpServerRequest request;

    private final HttpServerResponse response;

    public VertxWebContext(HttpServerRequest request) {
        this.request = request;
        this.response = request.response();
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
        String sessionId = StorageHelper.getOrCreateSessionId(request);
        StorageHelper.save(sessionId, name, value);
    }

    @Override
    public Object getSessionAttribute(String name) {
        String sessionId = StorageHelper.getOrCreateSessionId(request);
        return StorageHelper.get(sessionId, name);
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
