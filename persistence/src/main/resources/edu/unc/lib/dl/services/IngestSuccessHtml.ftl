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
  <h3>Digital Collections Repository</h3>
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
  <p>Thank you for contributing to the <a href="${irBaseUrl}">Digital Collections Repository</a>, a service of the <a href="http://www.lib.unc.edu/">University of North Carolina at Chapel Hill Libraries</a>.</p>
</body>
</html>