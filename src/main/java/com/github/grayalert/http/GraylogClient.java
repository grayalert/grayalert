package com.github.grayalert.http;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;

public interface GraylogClient {
    @RequestLine("GET /api/search/universal/relative/export?fields={fields}&query={query}&range={range}")
    @Headers("Accept: text/csv")
    Response getLogs(@Param("fields") String fields, @Param("query") String query, @Param("range") int range);
}