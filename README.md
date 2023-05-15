# HttpClient 5 logger

Request/Response logger for Apache HttpClient 5

## Getting started

**Add dependency**

```
dependencies {
    implementation 'ee.datanor.httpclient.logger:httpclient-logger:1.0.0'
}
```

**Configure logger**

```

        List<RequestLogProcessor> requestLogProcessors = List.of(
                new RequestTimeLogProcessor(),
                new RequestHashLogProcessor(),
                new RequestLineLogProcessor(),
                new RequestHeadersLogProcessor(),
                new RequestBodyLogProcessor(2048, Set.of())
        );

        List<ResponseLogProcessor> responseLogProcessors = List.of(
                new ResponseStatusLogProcessor(),
                new ResponseHeadersLogProcessor(),
                new ResponseBodyLogProcessor()
        );
        httpClientLogger = new HttpClientLogger(requestLogProcessors, responseLogProcessors);
```


**Attach logger to httpClient**
```
HttpClientBuilder.create()
    .addRequestInterceptorLast((HttpRequest request, EntityDetails entityDetails, HttpContext context) -> {
        httpClientLogger.cleanup();
        httpClientLogger.logRequest(request, context);
    }).addResponseInterceptorLast((HttpResponse response, EntityDetails entityDetails, HttpContext context) -> {
        httpClientLogger.logResponse(response, context);
        httpClientLogger.cleanup();
    });
```


**Log4j2 xml configuration example**
```
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="INFO">

    <Properties>
        <Property name="httpclient-request-log-pattern">
            REQ\t%date{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}\t%X{HC_REQUEST_HASH}\t%X{HC_REQUEST_LINE}\t%X{HC_REQUEST_HEADERS}\t%replace{%X{HC_REQUEST_BODY}}{^$}{-}%n
        </Property>
        <Property name="httpclient-response-log-pattern">
            RES\t%date{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}\t%X{HC_REQUEST_HASH}\t%X{HC_REQUEST_LINE}\t%X{HC_RESPONSE_STATUS}\t%X{HC_RESPONSE_HEADERS}\t%X{HC_RESPONSE_BODY_LENGTH}\t%X{HC_RESPONSE_BODY}%n
        </Property>

        ...

    </Properties>

    <Appenders>
        <Console name="httpclient-request-logger-console" target="SYSTEM_OUT">
            <PatternLayout pattern="${httpclient-request-log-pattern}"/>
        </Console>

        <Console name="httpclient-response-logger-console" target="SYSTEM_OUT">
            <PatternLayout pattern="${httpclient-response-log-pattern}"/>
        </Console>

        ...

    </Appenders>

    <Loggers>

        <Logger name="httpclient-request-log" additivity="false" level="DEBUG">
            <AppenderRef ref="httpclient-request-logger-console"/>
        </Logger>

        <Logger name="httpclient-response-log" additivity="false" level="DEBUG">
            <AppenderRef ref="httpclient-response-logger-console"/>
        </Logger>

        ...

    </Loggers>

</Configuration>


```
