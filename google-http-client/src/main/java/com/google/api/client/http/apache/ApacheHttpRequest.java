/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.http.apache;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.util.Preconditions;
import java.io.IOException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/** @author Yaniv Inbar */
final class ApacheHttpRequest extends LowLevelHttpRequest {
  private final HttpClient httpClient;
  private final HttpRequestBase httpRequest;

  ApacheHttpRequest(HttpClient httpClient, HttpRequestBase httpRequest) {
    this.httpClient = httpClient;
    this.httpRequest = httpRequest;
  }

  @Override
  public void addHeader(String headerName, String headerValue) {
    httpRequest.addHeader(headerName, headerValue);
  }

  @Override
  public void setTimeout(int connectTimeout, int readTimeout) throws IOException {
    HttpParams params = httpRequest.getParams();
    ConnManagerParams.setTimeout(params, connectTimeout);
    HttpConnectionParams.setConnectionTimeout(params, connectTimeout);
    HttpConnectionParams.setSoTimeout(params, readTimeout);
  }

  @Override
  public LowLevelHttpResponse execute() throws IOException {
    if (getStreamingContent() != null) {
      boolean isHttpEntityEnclosingRequest = httpRequest instanceof HttpEntityEnclosingRequest;
      Preconditions.checkState(
              isHttpEntityEnclosingRequest,
              "Apache HTTP client does not support %s requests with content.",
              httpRequest.getRequestLine().getMethod()
      );

      ContentEntity entity = new ContentEntity(getContentLength(), getStreamingContent());
      setRequestEntity(entity);
    }

    return new ApacheHttpResponse(httpRequest, httpClient.execute(httpRequest));
  }

  private void setRequestEntity(ContentEntity entity) {
    entity.setContentEncoding(getContentEncoding());
    entity.setContentType(getContentType());
    if (getContentLength() == -1) {
      entity.setChunked(true);
    }

    HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
    httpEntityEnclosingRequest.setEntity(entity);
  }

}

