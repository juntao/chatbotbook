package com.ringfulhealth.chatbotbook.skype;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EchoBasicServlet extends HttpServlet {

    String app_id = "YOUR-MS-APP-ID";
    String app_secret = "YOUR-MS-APP-SECRET";
    static String token = null;
    static Date token_exp_date = new Date ();

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        // Read POST body from request
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String body = buffer.toString();
        System.out.println(body);

        // Expire the token if needed
        if ((new Date ()).after(token_exp_date)) {
            token = null;
        }
        if (token == null || token.trim().isEmpty()) {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response = null;
            try {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("client_id", app_id));
                nvps.add(new BasicNameValuePair("client_secret", app_secret));
                nvps.add(new BasicNameValuePair("grant_type", "client_credentials"));
                nvps.add(new BasicNameValuePair("scope", "https://graph.microsoft.com/.default"));

                HttpPost httpPost = new HttpPost("https://login.microsoftonline.com/common/oauth2/v2.0/token");
                httpPost.setEntity(new UrlEncodedFormEntity(nvps));
                response = httpclient.execute(httpPost);

                HttpEntity entity = response.getEntity();
                String resp_body = EntityUtils.toString(entity);
                System.out.println(resp_body);

                JSONObject json = new JSONObject(resp_body);
                token = json.getString("access_token");

                int expires_in = json.getInt("expires_in");
                if (expires_in > 10) {
                    expires_in = expires_in - 10;
                }
                token_exp_date = new Date(System.currentTimeMillis() + expires_in * 1000L);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (httpclient != null) {
                        httpclient.close();
                    }
                } catch (Exception e) {}
                try {
                    if (response != null) {
                        response.close();
                    }
                } catch (Exception e) {}
            }
        }

        if (token != null && !token.trim().isEmpty()) {
            try {
                JSONObject entry = new JSONObject(body);
                if (!"message".equals(entry.getString("type")) && !"message/text".equals(entry.getString("type"))) {
                    // We will only process text messages
                    return;
                }
                String content = entry.getString("text");
                String cid = entry.getJSONObject("conversation").getString("id");
                String fromDisplayName = entry.getJSONObject("from").getString("name");

                System.out.println("TOKEN: " + token);
                System.out.println("CONTENT: " + content);
                System.out.println("FROM: " + fromDisplayName);

                CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = null;
                try {
                    JSONObject json = new JSONObject();
                    json.put("type", "message/text");
                    json.put("text", "You said: " + content);

                    HttpPost httpPost = new HttpPost("https://df-apis.skype.com/v3/conversations/" + cid + "/activities");
                    httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                    httpPost.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
                    response = httpclient.execute(httpPost);
                    System.out.println("STATUS: " + response.getStatusLine());

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (httpclient != null) {
                            httpclient.close();
                        }
                    } catch (Exception e) {}
                    try {
                        if (response != null) {
                            response.close();
                        }
                    } catch (Exception e) {}
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

}
