package org.acme;

import io.opentelemetry.api.trace.Span;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

@Path("/hello")
public class GreetingResource {

    private static final Logger log = Logger.getLogger(GreetingResource.class);

    @Inject
    ManagedExecutor managedExecutor;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        log.infof("hello() [traceId=%s]", Span.current().getSpanContext().getTraceId());

        managedExecutor.execute(() -> {

            // A first logging statement: in this case the mdc.traceId might or might not be available, probably
            // depending on the timing at which OpenTelemetryUtil#clearMdcData(...) is executed on the main thread
            log.infof("hello() from ManagedExecutor before sleep [traceId=%s]", Span.current().getSpanContext().getTraceId());

            executeWorkOnWorkerThread();

            // To illustrate the behaviour, a second logging statement is added after a sleep (simulating work executed
            // on the ManagedExecutor thread). In this case, the mdc.traceId is most likely not available anymore.
            log.infof("hello() from ManagedExecutor after sleep [traceId=%s]", Span.current().getSpanContext().getTraceId());
        });

        return "Hello from Quarkus REST";
    }

    private void executeWorkOnWorkerThread() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
