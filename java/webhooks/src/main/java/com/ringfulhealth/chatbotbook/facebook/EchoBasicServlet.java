package com.ringfulhealth.chatbotbook.facebook;

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

public class EchoBasicServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if (req.getParameter("hub.verify_token") != null) {
            resp.setContentType("text/plain");
            resp.getOutputStream().println(req.getParameter("hub.challenge"));
            return;
        }

        String page_access_token = "YOUR-FB-PAGE-ACCESS-TOKEN";

        // Read POST body from request
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String body = buffer.toString();
        System.out.println(body);

        try {
            JSONObject json = new JSONObject(body);
            JSONArray entries = json.getJSONArray("entry");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                JSONArray messagings = entry.getJSONArray("messaging");
                for (int j = 0; j < messagings.length(); j++) {
                    JSONObject messaging = messagings.getJSONObject(j);

                    String sender_id = messaging.getJSONObject("sender").getString("id");
                    String message = messaging.getJSONObject("message").getString("text");

                    JSONObject r = new JSONObject();
                    r.put("recipient", (new JSONObject()).put("id", sender_id));
                    r.put("message", (new JSONObject()).put("text", message));
                    System.out.println(r.toString());

                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    CloseableHttpResponse response = null;
                    try {
                        HttpPost httpPost = new HttpPost("https://graph.facebook.com/v2.6/me/messages?access_token=" + page_access_token);
                        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                        httpPost.setEntity(new StringEntity(r.toString(), StandardCharsets.UTF_8));
                        response = httpclient.execute(httpPost);

                        HttpEntity entity = response.getEntity();
                        System.out.println(EntityUtils.toString(entity));

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
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
