<?php
/*
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
Embeddable form for submitting feedback information about the CDR public UI.
Authors: sdball, bbpennel
$Id: contactus.php 2800 2011-08-30 19:25:38Z bbpennel $
$URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/access/src/main/external/forms/contactus.php $
*/

if ($_SERVER['REQUEST_METHOD'] == "POST" && $_POST["submit"] != "") {
	$errors = validate($_POST);
	if (empty($errors)) {
		require('/cgi/includes/webdata/util/email_class/email_class.inc');
		$refer = isset($_POST['refer']) ? $_POST['refer'] : '(refer missing)';
		$to = 'cdr@unc.edu';
		$from = 'cdr_feedback';
		$subject = 'CDR Feedback';
		$message = "The following feedback was sent from the CDR URI:\n" . $refer . "\n\n";
		$message .= 'Name: ' . trim($_POST['name']) . "\n";
		$message .= 'Email: ' . trim($_POST['email']) . "\n";
		$message .= 'Department: ';
		$message .= empty($_POST['dept']) ? '-' : trim($_POST['dept']);
		$message .= "\n\nComments:\n" . trim($_POST['comments']) . "\n";
		$replyto = trim($_POST['email']);
		$email = new Email($to, $from, $subject, $message, null, $replyto);
		$email->send_email();
		?>
		<div class="contentarea">
			<h2>Contact Us</h2>
	
			<p>Thank you for taking the time to contact us with your feedback.  We value your input.</p>
			
			<p><a href="<?php echo $refer; ?>">Back</a></p>
		</div>
		<?php
		return;
	}
}

function validate($formdata){
	$errors = array();
	$required = array(
		'name' => 'Please fill in your name.',
		'email' => 'Please fill in your email address.',
		'comments' => 'Please fill in your comments.'
	);
	foreach ($required as $field => $message) {
		if (!strlen(trim($formdata[$field]))) {
			$errors[$field] = $message;
		}
	}
	
	if (!preg_match('/^.+@.+\..+/', $formdata['email'])) {
		$errors['email'] = 'Please fill in a valid email address.';
	}
	
	return $errors;
}

function render_error($errors, $key) {
	if (isset($errors[$key])) {
		?>
		<strong><?php echo $errors[$key]; ?></strong>
		<?php
	}
}

$name = strip_tags(isset($_POST['name']) ? $_POST['name'] : '');
$email = strip_tags(isset($_POST['email']) ? $_POST['email'] : '');
$dept = strip_tags(isset($_POST['dept']) ? $_POST['dept'] : '');
$comments = strip_tags(isset($_POST['comments']) ? $_POST['comments'] : '');
$refer = strip_tags(isset($_REQUEST['refer']) ? $_REQUEST['refer'] : '(refer missing)');

?>

<div class="contentarea">
	<h2>Contact Us</h2>
	
	<p>Please describe any problem(s) you are having with website and/or suggestions for improvement.</p>
	<br/>
	<form class="user_form" action="" method="post" accept-charset="utf-8">
		<div class="form_section">
			<label for="name">Name<?php render_error($errors, 'name'); ?></label>
			<input type="text" id="name" name="name" value="<?php echo $name; ?>"
				<?php echo (isset($errors['name'])) ? 'class="error"' : ''; ?>
			/>
		</div>
		
		<div class="form_section">
			<label for="email">E-mail Address<?php render_error($errors, 'email'); ?></label>
			<input type="text" id="email" name="email" value="<?php echo $email; ?>"
				<?php echo (isset($errors['email'])) ? 'class="error"' : ''; ?>
			/>
		</div>
		
		<div class="form_section">
			<label for="dept">Department<?php render_error($errors, 'dept'); ?></label>
			<input type="text" id="dept" name="dept" value="<?php echo $dept; ?>"
				<?php echo (isset($errors['dept'])) ? 'class="error"' : ''; ?>
			/>
		</div>
		
		<div class="form_section">
			<label for="text">Comments<?php render_error($errors, 'comments'); ?></label>
			<textarea id="text" rows="8" cols="50" name="comments"
				<?php echo (isset($errors['comments'])) ? 'class="error"' : ''; ?>
			><?php echo $comments; ?></textarea>
		</div>
		
		<div class="form_section">
			<input type="hidden" name="refer" value="<?php echo $refer; ?>" />
			<input class="submitbutton" type="submit" value="Submit" name="submit"/>
		</div>
		
		<input type="hidden" name="page" value="contact"/>
	</form>
</div>
