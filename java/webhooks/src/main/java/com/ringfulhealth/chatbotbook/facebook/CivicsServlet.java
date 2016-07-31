package com.ringfulhealth.chatbotbook.facebook;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CivicsServlet extends BaseServlet {

    Random rand = new Random ();

    public CivicsServlet () {
        page_access_token = "FB-CIVICS-PAGE-TOKEN";

        zencoder_apikey = "ZENCODER-KEY";
        audio_bluemix_username = "BLUEMIX-S2T-USERNAME";
        audio_bluemix_password = "BLUEMIX-T2S-PASSWORD";
    }

    public Object converse (String human, ConcurrentHashMap<String, Object> context) {
        System.out.println("CivicsServlet converse");

        // ALWAYS STOP even if dup
        if ("stop".equalsIgnoreCase(human) || "bye".equalsIgnoreCase(human) || "finish".equalsIgnoreCase(human)) {
            context.remove("question");
            try {
                int corrects = ((List <JSONObject>) context.get("corrects")).size();
                int errors = ((List <JSONObject>) context.get("errors")).size();
                return "Okay. You answered " + corrects + " questions correctly out of " + (errors + corrects) + " total questions. Your score is " + (int) (100. * corrects / (errors + corrects)) + "%. At least 60% is needed to pass the exam. This quiz session has stopped. Please text GO to start a new session.";
            } catch (Exception e) {
                e.printStackTrace();
                return "Okay.";
            }
        }

        if ("test".equalsIgnoreCase(human)) {
            List replies = new ArrayList();
            replies.add ("Audio file: http://michaelyuan.com/download/bots/SampleAudio_0.7mb.mp3");
            replies.add ("Audio <a href=\"http://michaelyuan.com/download/bots/SampleAudio_0.7mb.mp3\">link</a>");
            replies.add ("Video file: http://michaelyuan.com/download/bots/SampleVideo_720x480_10mb.mp4");
            replies.add ("YouTube: https://www.youtube.com/watch?v=UxB11eAl-YE");
            return replies;
        }

        // immediate dups in a question will be ignored. Eg. HELP or score or the same question answers in a row
        if (context.containsKey("question")) {
            try {
                JSONObject question = (JSONObject) context.get("question");
                String last_a = question.getString("id") + " | " + human;
                if (last_a.equalsIgnoreCase((String) context.get("last_a"))) {
                    // Dup answer in the middle of a session. Ignore it.
                    return null;
                } else {
                    context.put("last_a", last_a); // continue
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (human.toUpperCase().contains("HELP")) {
            return "This bot helps you practice and pass the US Citizenship Civics Test. Reply STOP to stop a quiz and see your score. Text GO to start a new quiz session after stopping. You can answer questions with text or your voice.";
        }

        if ("score".equalsIgnoreCase(human)) {
            try {
                int corrects = ((List <JSONObject>) context.get("corrects")).size();
                int errors = ((List <JSONObject>) context.get("errors")).size();
                // return "So far, you answered " + corrects + " questions correctly out of " + (errors + corrects) + " total questions. Your score is " + (int) (100. * corrects / (errors + corrects)) + "%. At least 60% is needed to pass the exam. You can reply STOP at any time to end this quiz session.";

                return createQuickReplies(
                        "So far, you answered " + corrects + " questions correctly out of " + (errors + corrects) + " total questions. Your score is " + (int) (100. * corrects / (errors + corrects)) + "%. At least 60% is needed to pass the exam. You can reply STOP at any time to end this quiz session.",
                        new HashMap<String, String>(){{
                            put("Next question", "next_q");
                            put("Stop", "stop");
                        }}
                );
                /*
                JSONObject message = new JSONObject();
                message.put("text", "So far, you answered " + corrects + " questions correctly out of " + (errors + corrects) + " total questions. Your score is " + (int) (100. * corrects / (errors + corrects)) + "%. At least 60% is needed to pass the exam. You can reply STOP at any time to end this quiz session.");
                message.put("quick_replies", createQuickReplies(
                        new String [] {"Next question", "Stop"},
                        new String [] {"next_q", "stop"}
                ));
                return message;
                */
            } catch (Exception e) {
                e.printStackTrace();
                return "Sorry, you do not have a score yet.";
            }
        }

        if ("next_q".equalsIgnoreCase(human)) {
            try {
                if (!((Boolean) context.get("done")).booleanValue()) {
                    // The current question has not been answered
                    JSONObject question = (JSONObject) context.get("question");
                    return "Hey, please do not skip questions. Reply with an answer to the current question: " + question.getString("question");
                } else {
                    return nextQuestion(context);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (human.startsWith("SHOW-ANSWER-")) {
            try {
                int qid = Integer.parseInt(human.substring(12));
                JSONObject question = (JSONObject) context.get("question");
                List<JSONObject> questions = (List<JSONObject>) context.get("questions");
                List<JSONObject> corrects = (List<JSONObject>) context.get("corrects");
                List<JSONObject> errors = (List<JSONObject>) context.get("errors");

                JSONObject q = null;
                if (q == null) {
                    for (int i = 0; i < questions.size(); i++) {
                        if (qid == questions.get(i).getInt("id")) {
                            q = questions.get(i);
                            break;
                        }
                    }
                }
                if (q == null) {
                    for (int i = 0; i < errors.size(); i++) {
                        if (qid == errors.get(i).getInt("id")) {
                            q = errors.get(i);
                            break;
                        }
                    }
                }
                if (q == null) {
                    for (int i = 0; i < corrects.size(); i++) {
                        if (qid == corrects.get(i).getInt("id")) {
                            q = corrects.get(i);
                            break;
                        }
                    }
                }
                if (q == null) {
                    return "Sorry, I cannot find the selected question.";
                }

                if (question.getInt("id") == q.getInt("id")) {
                    // The current question now has an answer!
                    context.put("done", true);
                }

                JSONObject message = createQuickReplies(
                        q.getString("answer"),
                        new HashMap<String, String>(){{
                            put("My score", "score");
                            put("Next question", "next_q");
                        }}
                );
                /*
                JSONObject message = new JSONObject();
                message.put("text", q.getString("answer"));
                message.put("quick_replies", createQuickReplies(
                        new String [] {"My score", "Next question"},
                        new String [] {"score", "next_q"}
                ));
                */
                /*
                JSONObject payload = new JSONObject();
                payload.put("template_type", "button");
                payload.put("text", q.getString("answer"));
                String[] button_titles = {"Hear it", "My score", "Next question"};
                String[] button_payloads = {"https://www.uscis.gov/sites/default/files/files/nativedocuments/Track%20" + q.getInt("id") + ".mp3", "score", "next_q"};
                payload.put("buttons", createButtons(button_titles, button_payloads));
                */

                List replies = new ArrayList();
                replies.add("Okay!");
                replies.add (q.getString("question"));
                replies.add(message);
                return replies;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (context.get("question") != null) {
                if (((Boolean) context.get("done")).booleanValue()) {
                    // The current question is done. Any non-special input results in the next question
                    return nextQuestion(context);
                }

                if ("try_again".equalsIgnoreCase(human)) {
                    return "Please reply with your answer.";
                }

                JSONObject q = (JSONObject) context.get("question");
                int qid = q.getInt("id");
                String pqid = null;
                if (qid < 10) {
                    pqid = "0" + qid;
                } else {
                    pqid = "" + qid;
                }

                Pattern ptn = Pattern.compile(q.getString("score"), Pattern.CASE_INSENSITIVE);
                Matcher match = ptn.matcher(human);

                boolean matched = true;
                for (int i = 0; i < q.getInt("repeat"); i++) {
                    if (!match.find()) {
                        matched = false;
                    }
                }

                if (matched) {
                    if (((Boolean) context.get("newq")).booleanValue()) {
                        ((List <JSONObject>) context.get("corrects")).add(q);
                    }
                    context.put("newq", false);
                    context.put("done", true);

                    return createQuickReplies(
                            "Correct! The answer is " + q.getString("answer"),
                            new HashMap<String, String>(){{
                                put("My score", "score");
                                put("Next question", "next_q");
                            }}
                    );
                    /*
                    JSONObject message = new JSONObject();
                    message.put("text", "Correct! The answer is " + q.getString("answer"));
                    message.put("quick_replies", createQuickReplies(
                            new String [] {"My score", "Next question"},
                            new String [] {"score", "next_q"}
                    ));
                    return message;
                    */
                    /*
                    JSONObject payload = new JSONObject();
                    payload.put("template_type", "button");
                    payload.put("text", "Correct! The answer is " + q.getString("answer"));
                    String[] button_titles = {"Hear it", "My score", "Next question"};
                    String[] button_payloads = {"https://www.uscis.gov/sites/default/files/files/nativedocuments/Track%20" + pqid + ".mp3", "score", "next_q"};
                    payload.put("buttons", createButtons(button_titles, button_payloads));
                    return payload;
                    */
                    // List replies = new ArrayList();
                    // replies.add("Correct!");
                    // replies.add(nextQuestion(context));
                    // return replies;

                } else {
                    if (((Boolean) context.get("newq")).booleanValue()) {
                        ((List <JSONObject>) context.get("errors")).add(q);
                    }
                    context.put("newq", false);

                    Pattern ptn2 = Pattern.compile("n.t.+((know)|(remember)|(recall)|(sure)|(positive))", Pattern.CASE_INSENSITIVE);
                    Matcher match2 = ptn2.matcher(human);

                    HashMap<String, String> qrbuttons = new HashMap<String, String> ();
                    qrbuttons.put("See answer", "SHOW-ANSWER-" + qid);
                    qrbuttons.put("Try again", "try_again");

                    if (match2.find()) {
                        return createQuickReplies(
                                "Okay. Tap the button below to see the answer!",
                                qrbuttons
                        );
                    } else {
                        return createQuickReplies(
                                "Hmm, this is not correct. Do you want to see the answer?",
                                qrbuttons
                        );
                    }
                    /*
                    JSONObject message = new JSONObject();
                    if (match2.find()) {
                        message.put("text", "Okay. Tap the button below to see the answer!");
                    } else {
                        message.put("text", "Hmm, this is not correct. Do you want to see the answer?");
                    }
                    message.put("quick_replies", createQuickReplies(
                            new String [] {"See answer", "Try again"},
                            new String [] {"SHOW-ANSWER-" + qid, "try_again"}
                    ));
                    return message;
                    */
                    /*
                    JSONObject payload = new JSONObject();
                    payload.put("template_type", "button");
                    if (match2.find()) {
                        payload.put("text", "Okay. Tap the button below to see the answer!");
                    } else {
                        payload.put("text", "Hmm, this is not correct. Do you want to see the answer? Or, you can try answering again!");
                    }

                    String[] button_titles = {"See the answer"};
                    String[] button_payloads = {"SHOW-ANSWER-" + qid};

                    payload.put("buttons", createButtons(button_titles, button_payloads));
                    return payload;
                    */
                }

            } else {
                ServletContext sctx = getServletContext();
                InputStream is = sctx.getResourceAsStream("/WEB-INF/civics_questions.json");
                if (is != null) {
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr);

                    String json = "", line;
                    while ((line = reader.readLine()) != null) {
                        json = json + line;
                    }
                    System.out.println("civics_questions.json: " + json);

                    JSONArray qs = new JSONArray (json);
                    List <JSONObject> questions = new ArrayList <JSONObject> ();
                    for (int i = 0; i < qs.length(); i++) {
                        questions.add(qs.getJSONObject(i));
                    }
                    context.put("questions", questions);
                    context.put("corrects", new ArrayList<JSONObject> ());
                    context.put("errors", new ArrayList<JSONObject> ());
                } else {
                    System.out.print("The InputStream is NULL!");
                }

                return nextQuestion(context);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Object nextQuestion (ConcurrentHashMap<String, Object> context) throws Exception {
        List <JSONObject> questions = (List <JSONObject>) context.get("questions");
        if (questions.isEmpty()) {
            context.remove("question");

            return createQuickReplies(
                    "You have gone through all the questions! We will start over next. Do you want to see your score?",
                    new HashMap<String, String>(){{
                        put("Yes", "stop");
                        put("No", "go");
                    }}
            );
            /*
            JSONObject message = new JSONObject();
            message.put("text", "You have gone through all the questions! We will start over next. Do you want to see your score?");
            message.put("quick_replies", createQuickReplies(
                    new String [] {"Yes", "No"},
                    new String [] {"stop", "go"}
            ));
            return message;
            */
            /*
            JSONObject payload = new JSONObject();
            payload.put("template_type", "button");
            payload.put("text", "You have gone through all the questions! We will start over next. Do you want to see your score?");

            String[] button_titles = {"Yes"};
            String[] button_payloads = {"stop"};

            payload.put("buttons", createButtons(button_titles, button_payloads));
            return payload;
            */
        }

        int r = rand.nextInt(questions.size());
        JSONObject q = questions.get(r);
        questions.remove(q);

        context.put("questions", questions);
        context.put("question", q);
        context.put("newq", true);
        context.put("done", false);

        // return wrapQuestion(q);
        return q.getString("question");
    }

    public JSONObject wrapQuestion (JSONObject q) {
        JSONObject payload = null;
        try {
            payload = new JSONObject ();

            payload.put("template_type", "button");
            payload.put("text",  q.getString("question") + " [Reply with text or your voice] ");

            String[] button_titles = {"Hear it!"};
            // String[] button_payloads = {"http://api.voicerss.org/?key=fc3e36d3b4b6418cb2c245d7b02be70a&hl=en-us&src=" + URLEncoder.encode(q.getString("question"))};
            String[] button_payloads = {"https://chatbotbook.ringfulhealth.com/java/watson_speech?t=" + URLEncoder.encode(q.getString("question"))};
            // String[] button_payloads = {"https://www.uscis.gov/sites/default/files/files/nativedocuments/Track%20" + q.getInt("id") + ".mp3"};

            payload.put("buttons", createButtons(button_titles, button_payloads));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return payload;
    }
}