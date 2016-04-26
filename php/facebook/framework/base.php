<?php
  // This block will be skipped if the hub.verify_token is null or empty
  if (strlen($_GET['hub_verify_token']) > 0) {
    echo $_GET['hub_challenge'];
    return;
  }

  require_once (dirname(__FILE__).'/phpFastCache/phpFastCache.php');
  use phpFastCache\CacheManager;

  $config = array(
    "storage"   =>  "Files",
    "path" => sys_get_temp_dir()
  );
  CacheManager::setup($config);

  $body = file_get_contents('php://input');
  error_log ($body);
  $json = json_decode($body, true);
  for($i=0; $i<count($json['entry']); $i++) {
    $messagings = $json['entry'][$i]['messaging'];
    for($j=0; $j<count($messagings); $j++) {
      $messaging = $messagings[$j];
      $sender_id = $messaging['sender']['id'];
      if (!empty($messaging['message'])) {
        $message = $messaging['message']['text'];
      } elseif (!empty($messaging['postback'])) {
        $message = $messaging['postback']['payload'];
      }

      $chat_context = CacheManager::get($sender_id . "_context");
      if (is_null($chat_context)) {
        $chat_context = array ();
      }

      $bot_says = converse ($message, $chat_context);
      CacheManager::set($sender_id . "_context", $chat_context, 600); // cache for 600 seconds

      if (is_string($bot_says)) {
        $arr = array(
          "recipient" => array("id" => $sender_id),
          "message" => array("text" => $bot_says)
        );
      } elseif (is_array($bot_says) && (!empty($bot_says['template_type']))) {
        $arr = array(
          "recipient" => array("id" => $sender_id),
          "message" => array(
            "attachment" => array (
              "type" => "template",
              "payload" => $bot_says
            )
          )
        );
      } elseif (is_array($bot_says) && (!empty($bot_says['url']))) {
        $arr = array(
          "recipient" => array("id" => $sender_id),
          "message" => array(
            "attachment" => array (
              "type" => "image",
              "payload" => $bot_says
            )
          )
        );
      }

      $url = "https://graph.facebook.com/v2.6/me/messages?access_token=" . $page_access_token;
      $content = json_encode($arr);
      error_log($url);
      error_log($content);

      $ch = curl_init($url);
      curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
      curl_setopt($ch, CURLOPT_HTTPHEADER,
        array("Content-type:application/json")
      );
      curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "POST");
      curl_setopt($ch, CURLOPT_POSTFIELDS, $content);
      curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
      curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);

      $resp = curl_exec($ch);
      $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
      curl_close($ch);

      error_log($status . " " . $resp);
    }
  }
  
  // INPUT PARAMS
  //   $human is the human message to this bot
  //   $context is an array that contains any data related to this chat session. You can put any data here and access it later.

  // RETURNS the bot's next message to the human

  // function converse ($human, &$context) {
    // IMPLEMENT ME!
  // }
?>
