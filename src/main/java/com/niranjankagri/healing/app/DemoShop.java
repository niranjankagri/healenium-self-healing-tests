package com.niranjankagri.healing.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * The system under test: a tiny shop served from the classpath by the JDK's built-in HTTP server.
 *
 * <p>{@link #deploy(Release)} switches the files behind the same URL, the way a real deployment would, so the tests
 * cannot tell the releases apart by address. Healenium does not key its saved elements by URL here
 * ({@code KEY_SELECTOR_URL=false} in infra/docker-compose.yml): it uses the locator and the calling method, which is
 * why a new random port on every run does not matter.
 */
public final class DemoShop implements AutoCloseable {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8");

    private final HttpServer server;
    private volatile Release release;

    private DemoShop(HttpServer server, Release release) {
        this.server = server;
        this.release = release;
    }

    /**
     * Starts the shop on a free port of the loopback address.
     *
     * @param release the release to serve first
     * @return the running shop; close it to stop the server
     */
    public static DemoShop start(Release release) {
        try {
            // Port 0: the OS picks a free port, so parallel builds on one machine never clash
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            DemoShop shop = new DemoShop(server, release);
            server.createContext("/", shop::serve);
            server.start();
            return shop;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start the demo shop", e);
        }
    }

    /** Replaces the files behind the URL with another release. Pages already open keep their old HTML until reloaded. */
    public void deploy(Release next) {
        this.release = next;
    }

    /** The release currently served. */
    public Release release() {
        return release;
    }

    /** Base URL, ending in a slash, for example {@code http://localhost:51234/}. */
    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void serve(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String file = path.equals("/") ? "index.html" : path.substring(1);
            // Only plain file names: no folders, so nothing outside the release folder can be requested
            if (!file.matches("[a-z-]+\\.(html|css|js)")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            try (InputStream in = DemoShop.class.getResourceAsStream("/shop/" + release.folder() + "/" + file)) {
                if (in == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] body = in.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPES.get(file.substring(file.lastIndexOf('.') + 1)));
                // No caching: after deploy() the browser must fetch the new release, not reuse the old files
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        }
    }
}
