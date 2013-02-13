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
    <#if 1 < numberOfObjects>
      <p>Your ${numberOfObjects} items were successfully ingested into the repository.</p>
    <#else>
      <p>Your item was successfully ingested into the repository.</p>
    </#if>
  </title>
</head>
<body>
	<img style="float: right;" alt="UNC Libraries logo" src="${irBaseUrl}static/images/email_logo.png"/>
  <h3>Carolina Digital Repository</h3>
  <#if 1 < numberOfObjects>
  <p>${numberOfObjects} items were successfully ingested into the repository in <#if 1 < tops?size>these locations<#else>this location</#if>:</p>
  <#else>
  <p>Your item was successfully ingested into the repository in this location:</p>
  </#if>
  <ul>
  <#list tops as obj>
    <li><a href="${irBaseUrl}record?id=${obj.pid}">${obj.label!"No label set!"}</a><br/>PID ${obj.pid}</li>
  </#list>
  </ul>
  <p>Thank you for contributing to the <a href="${irBaseUrl}">Carolina Digital Repository</a>, a service of the <a href="http://www.lib.unc.edu/">University of North Carolina at Chapel Hill Libraries</a>.</p>
</body>
</html>