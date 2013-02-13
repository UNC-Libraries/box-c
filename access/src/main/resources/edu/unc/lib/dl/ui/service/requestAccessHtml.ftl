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
      <p>Request for access received.</p>
  </title>
</head>
<body>
  <h3>Carolina Digital Repository</h3>
  <p>A request for access was sent by ${form.personalName!}</p>
  <table>
  	<#if form.requestedId?has_content>
  	<tr>
  		<td>
  			Requesting
  		</td>
  		<td>
  			${form.requestedId}<br/>
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