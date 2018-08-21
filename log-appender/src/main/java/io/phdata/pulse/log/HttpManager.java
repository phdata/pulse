/*
 * Copyright 2018 phData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.phdata.pulse.log;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.helpers.LogLog;

public class HttpManager {

    private CloseableHttpClient client;
    private URI address;

    public HttpManager(URI address) {
        this.address = address;

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(20);
        cm.setMaxTotal(200);

        this.client = HttpClients.custom().setConnectionManager(cm).build();
    }

    public boolean send(String logMessage) {
        HttpPost post = new HttpPost(address);
        post.setHeader("Content-type", "application/json");

        StringEntity strEntity = new StringEntity(logMessage, Charset.forName("UTF8"));
        post.setEntity(strEntity);
        LogLog.debug("Executing request: " +  post.getRequestLine());
        boolean isSuccessful = false;
        try {
            CloseableHttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            response.close();

            LogLog.debug("Response code: " + statusCode);

            isSuccessful = (200 <= statusCode && statusCode < 300);
        } catch (IOException ie) {
           LogLog.error("Request failed: " + ie, ie);
        }
        return isSuccessful;
    }

    public void close() throws IOException {
        client.close();
    }
}
