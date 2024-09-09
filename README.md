# Quarkus issue reproduction: quarkus-opentelemetry clears MDC context of ManagedExecutor thread

Project to reproduce an issue observed in a Quarkus based project. In short: the behaviour observed is that for logging 
statements executed on a ManagedExecutor thread, the MDC (traceId) context is not available. 

## Project setup

This project was created using the quarkus CLI:

```shell
quarkus:create
quarkus ext add quarkus-opentelemetry
```

* See [GreetingResource](./src/main/java/org/acme/GreetingResource.java) for the code.
* See [application.properties](./src/main/resources/application.properties) for the configuration.
  * The mdc.traceId is added to logging statements using the `quarkus.log.console.format` configuration. 

## Running reproduction

```shell
./mvnw quarkus:dev

curl localhost:8080/hello
```

## Observed behaviour

```log
2024-09-09 10:12:49,785 INFO  traceId=e19013fa3e2b7e0ce8365af3149e09c0 [org.acm.GreetingResource] (executor-thread-2) hello() [traceId=e│
19013fa3e2b7e0ce8365af3149e09c0]                                                                                                        │
2024-09-09 10:12:49,787 INFO  traceId=e19013fa3e2b7e0ce8365af3149e09c0 [org.acm.GreetingResource] (executor-thread-1) hello() from Manag│
edExecutor before sleep [traceId=e19013fa3e2b7e0ce8365af3149e09c0]                                                                      │
2024-09-09 10:12:50,788 INFO  traceId= [org.acm.GreetingResource] (executor-thread-1) hello() from ManagedExecutor after sleep [traceId=│
e19013fa3e2b7e0ce8365af3149e09c0]
```

Observations: 

* In the ManagedExecutor thread, the mdc.traceId is _sometimes_ not included in the logging statement.
* On the contrary, tracing context (`Span.current().getSpanContext().getTraceId()`) is not cleared, thus always correctly available in the ManagedExecutor thread
* My investigation/understanding of the issue:
  * Vert.x duplicated context is used in the ManagedExecutor thread
  * VertxMDC is used as MDCProvider, stored in the Vert.x context
  * quarkus-opentelemetry clears the MDC data in this duplicated context (via `OpenTelemetryUtil#clearMDC(...)`)