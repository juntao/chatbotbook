<?php
 if ($_REQUEST["user_name"] == "slackbot") {
   exit;
 }
?>
{
  "text":"<?= $_REQUEST['text'] ?>",
  "mrkdwn":true
}
