package com.ringfulhealth.chatbotbook;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
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
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WatsonSpeechServlet extends HttpServlet {

    private String zencoder_apikey = "ZENCODER-KEY";
    private String username = "WATSON-T2S-USERNAME";
    private String password = "WATSON-T2S-PASSWORD";

    private String root_dir = "/var/www/html/chatbotbook/files/";
    private String root_url = "http://chatbotbook.ringfulhealth.com/files/";
    private boolean redirect_to_zencoder = false;

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String text = req.getParameter("t");
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String filename = MD5(text.trim());
        if (filename == null || filename.trim().isEmpty()) {
            return;
        }

        File f = new File(root_dir + filename + ".mp3");
        if(f.exists() && !f.isDirectory() && !redirect_to_zencoder) {
            resp.sendRedirect(root_url + filename + ".mp3");
            return;
        } else {

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
            );
            CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
            CloseableHttpResponse response = null;
            try {
                // get wav file from Watson
                HttpGet httpGet = new HttpGet("https://stream.watsonplatform.net/text-to-speech/api/v1/synthesize?accept=audio/wav&voice=en-US_AllisonVoice&text=" + URLEncoder.encode(text));
                response = httpclient.execute(httpGet);

                HttpEntity entity = response.getEntity();
                byte[] respBodyBytes = EntityUtils.toByteArray(entity);
                response.close();

                FileOutputStream out = new FileOutputStream(root_dir + filename + ".wav");
                try {
                    out.write(respBodyBytes);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    out.close();
                }

                // zencoder for mp3
                HttpPost httpPost = new HttpPost("https://app.zencoder.com/api/v2/jobs");
                httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                httpPost.setHeader("Zencoder-Api-Key", zencoder_apikey);

                JSONObject json = new JSONObject();
                JSONArray outputs = new JSONArray ();
                JSONObject output = new JSONObject ();
                output.put("format", "mp3");
                outputs.put(output);
                json.put("input", root_url + filename + ".wav");
                json.put("outputs", outputs);
                System.out.println(json.toString());

                httpPost.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
                response = httpclient.execute(httpPost);
                response.close();

                entity = response.getEntity();
                String respBodyStr = EntityUtils.toString(entity);
                System.out.println(respBodyStr);

                json = new JSONObject(respBodyStr);
                long job_id = json.getLong("id");
                String zencoder_url = ((JSONObject) json.getJSONArray("outputs").get(0)).getString("url");

                while (!respBodyStr.startsWith("{\"state\":\"finished\"")) {
                    Thread.sleep(1000);
                    httpGet = new HttpGet("https://app.zencoder.com/api/v2/jobs/" + job_id + "/progress.json?api_key=" + zencoder_apikey);
                    response = httpclient.execute(httpGet);
                    entity = response.getEntity();
                    respBodyStr = EntityUtils.toString(entity);
                    response.close();
                    System.out.println(respBodyStr);
                }

                if (redirect_to_zencoder) {
                    resp.sendRedirect(zencoder_url);
                    
                } else {
                    // get and save zencoder mp3
                    httpGet = new HttpGet(zencoder_url);
                    response = httpclient.execute(httpGet);
                    entity = response.getEntity();
                    respBodyBytes = EntityUtils.toByteArray(entity);
                    response.close();

                    out = new FileOutputStream(root_dir + filename + ".mp3");
                    try {
                        out.write(respBodyBytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        out.close();
                    }

                    resp.sendRedirect(root_url + filename + ".mp3");
                }
                return;

                /*
                resp.setContentType("audio/wav");

                String range = req.getHeader("Range");
                if (range == null || range.equals("bytes=0-")) {
                    resp.setContentLength(respBody.length);
                    System.out.println("No Range");

                    OutputStream out = resp.getOutputStream();
                    out.write(respBody);
                    out.close();

                } else {
                    System.out.println("Range is " + range);
                    String[] ranges = range.split("=")[1].split("-");
                    int from = Integer.parseInt(ranges[0]);
                    int to = Integer.parseInt(ranges[1]);
                    // int to = respBody.length - 1;
                    int len = to - from + 1;

                    resp.setStatus(206);
                    resp.setContentLength(len);
                    resp.setHeader("Accept-Ranges", "bytes");
                    String responseRange = String.format("bytes %d-%d/%d", from, to, respBody.length);
                    response.setHeader("Content-Range", responseRange);
                    System.out.println("Content-Range: " + responseRange);

                    OutputStream out = resp.getOutputStream();
                    out.write(Arrays.copyOfRange(respBody, from, to + 1));
                    out.close();
                }
                */

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (httpclient != null) {
                        httpclient.close();
                    }
                } catch (Exception e) {
                }
                try {
                    if (response != null) {
                        response.close();
                    }
                } catch (Exception e) {
                }
            }
        }

    }

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

}
