<?php
  // This block will be skipped if the hub.verify_token is null or empty
  if (strlen($_GET['hub_verify_token']) > 0) {
    echo $_GET['hub_challenge'];
    return;
  }

  $page_access_token = "YOUR-FB-PAGE-ACCESS-TOKEN";

  $body = file_get_contents('php://input');
  $json = json_decode($body, true);
  for($i=0; $i<count($json['entry']); $i++) {
    $messagings = $json['entry'][$i]['messaging'];
    for($j=0; $j<count($messagings); $j++) {
      $messaging = $messagings[$j];
      $sender_id = $messaging['sender']['id'];
      $message = $messaging['message']['text'];

      $arr = array(
        "recipient" => array("id" => $sender_id),
        "message" => array("text" => $message)
      );
      $content = json_encode($arr);

      $url = "https://graph.facebook.com/v2.6/me/messages?access_token=" . $page_access_token;
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
    }
  }
?>
