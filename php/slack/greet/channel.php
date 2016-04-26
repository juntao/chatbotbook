<?php
  require_once ('../framework/channel.php');

  function converse ($human, &$context) {
    // Initial response
    if (is_null($context['status'])) {
      $context['status'] = "wait_for_name";
      return "Hello! What is your name? Please enter _firstname lastname_";
    }

    // The user responded to the bot's question about name
    if ($context['status'] == "wait_for_name") {
      $context['name'] = $human;
      $context['status'] = "wait_for_gender";
      return "It is great meeting you! What is your gender? Please reply Female, Male, or Other";
    }

    // The user responded to the bot's question about gender. We generate a response and set the status to NULL again so that we can start over.
    if ($context['status'] == "wait_for_gender") {
      $salutation = '';
      if (0 === strpos(strtoupper($human), 'F')) {
        $salutation = 'Ms. ';
      } elseif (0 === strpos(strtoupper($human), 'M')) {
        $salutation = 'Mr. ';
      } elseif (0 === strpos(strtoupper($human), 'O')) {
        $salutation = '';
      } else {
        $context['status'] = "wait_for_gender";
        return "Sorry, I did not get it. Please reply Female, Male, or Other";
      }

      $context['status'] = NULL;
      return "Hello, " . $salutation . $context['name'];
    }
  }
?>
