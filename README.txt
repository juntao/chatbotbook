PHP
===

Copy the entire content inside the php directory into a web directory managed by your Apache web server.

Edit the platform/app.php file to add platform-specific access tokens or credentials. For example,

  * facebook/echo.php for the Echo application's Facebook page access token.
  * skype/greet.php for the Greet application's Skype AppID.

Then, in your messaging platform's chatbot configuration screen, point the webhook to the related PHP URL (e.g., full URL to skype/greet.php in the Skype bot configuration).

Java
===

In the java/webhooks/src/main/java/com/ringfulhealth/chatbotbook directory, edit the platform/app.java file to add platform-specific access tokens or credentials. For example,

  * facebook/EchoServlet.java for the Echo application's Facebook page access token.
  * skype/GreetServlet.java for the Greet application's Skype AppID.

Build the Java application with Maven:
> mvn clean package

Deploy the resultant WAR file into a Tomcat application server.

Then, in your messaging platform's chatbot configuration screen, point the webhook to TOMCAT_SERVER/java/app (e.g., TOMCAT_SERVER/java/skype/greet in the Skype bot configuration).



