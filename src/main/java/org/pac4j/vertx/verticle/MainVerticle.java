package org.pac4j.vertx.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

import java.io.*;

/**
 * Main verticle to load the config from the classpath (so we can, for example, store facebook config within
 * the built jar) and then pass it into the verticles we'll actually deploy for the purpose of the demo
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class MainVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();

        Vertx rxVertx = new Vertx(vertx);
        final Observable<String> deploymentIdObservable = rxVertx.<String>executeBlockingObservable(future -> {
            final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config_demo.json");
            final InputStreamReader inputStreamReader;
            final char[] buffer = new char[1000]; // we know the config file will be small so we should be ok
            final StringBuilder builder = new StringBuilder();
            try {
                inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                for (; ; ) {
                    int rsz = bufferedReader.read(buffer, 0, buffer.length);
                    if (rsz < 0)
                        break;
                    builder.append(buffer, 0, rsz);
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 not supported", e);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read config file", e);
            }
            future.complete(builder.toString());

        })
        .map(s -> new JsonObject(s))
        .map(conf -> new DeploymentOptions().setConfig(conf))
        .flatMap(options -> rxVertx.deployVerticleObservable(DemoServerVerticle.class.getName(), options));

        deploymentIdObservable.subscribe(string -> {
            LOG.info("Demo server verticle deployed with deployment id '" + string + "'");
            startFuture.complete();
        });
    }
}