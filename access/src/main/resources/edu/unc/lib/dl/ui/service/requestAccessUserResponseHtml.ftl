<#--

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<html>
<head>
  <title>
      <p>Request for access confirmation.</p>
  </title>
</head>
<body>
  <h3>Carolina Digital Repository</h3>
  <p>Thank you for your interest, ${form.personalName!}.  This email is to confirm that your request for access has been sent.  
  You will receive an update on the status of your request within two business days.</p>
  <p>The following information was sent:</p>  
  <table>
  	<#if form.requestedId?has_content>
  	<tr>
  		<td>
  			Requesting
  		</td>
  		<td>
  			<a href="https://${serverName!}/record?id=${form.requestedId}">${form.requestedTitle!}</a>
  		</td>
  	</tr>
  	</#if>
  	<tr>
  		<td>
  			Name
  		</td>
  		<td>
  			${form.personalName!}
  		</td>
  	</tr>
  	<tr>
  		<td>
  			Username
  		</td>
  		<td>
  			${form.username!}
  		</td>
  	</tr>
  	<tr>
  		<td>
  			Email
  		</td>
  		<td>
  			${form.emailAddress!}
  		</td>
  	</tr>
  	<tr>
  		<td>
  			Phone
  		</td>
  		<td>
  			${form.phoneNumber!}
  		</td>
  	</tr>
  	<tr>
  		<td>
  			Affiliation
  		</td>
  		<td>
  			${form.affiliation!}
  		</td>
  	</tr>
  	<tr>
  		<td>
  			Comments
  		</td>
  		<td>
  			${form.comments!}
  		</td>
  	</tr>
  </table>
</body>
</html>