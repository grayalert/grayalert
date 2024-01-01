package com.github.grayalert.http;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * this client can post messages to a Microsoft Teams channel.
 */
public interface MSTeamsClient {

    @RequestLine("POST {path}")
    @Headers("Content-Type: application/json")
    String postMessage(@Param("path") String path, MSTeamsMessage message);

}
