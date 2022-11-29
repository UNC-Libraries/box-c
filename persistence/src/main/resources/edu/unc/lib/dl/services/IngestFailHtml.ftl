<#ftl ns_prefixes={"svrl":"http://purl.oclc.org/dsdl/svrl"}>
<html>
<head>
  <title>Your repository ingest failed to complete.</title>
</head>
<body>
	<img style="float: right;" alt="UNC Libraries logo" src="${irBaseUrl}images/email_logo.png"/>
  <h3>Digital Collections Repository</h3>
  <p>Your submission was not ingested into the repository.</p>
  
  <#if message??>
  <p>Error: ${message}</p>
  </#if>
  
	<#if METSParseException??>
	  <#if (METSParseException["fatalErrors"]?size > 0)>
	  Fatal Errors:<br />
	  <ul>
	      <#list METSParseException["fatalErrors"] as sax>
	  	  <li>${sax.message}</li>
	      </#list>
	    </ul>
	  </#if>
	  <#if (METSParseException["errors"]?size > 0)>
	  Errors:<br />
	  <ul>
	      <#list METSParseException["errors"] as sax>
	  	  <li>${sax.message}</li>
	      </#list>	
	    </ul>
	  </#if>
	  <#if (METSParseException["warnings"]?size > 0)>
	  Warnings:<br />
	  <ul>
	      <#list METSParseException["warnings"] as sax>
	  	  <li>${sax.message}</li>
	      </#list>
	    </ul>
	  </#if>
	<#elseif svrl??>
	  ${svrl["svrl:schematron-output/@title"]}<br />
		Your METS document did not conform to a METS profile.  Here is the list of problems identified:
		<ul>
		<#list svrl["svrl:schematron-output/svrl:failed-assert"] as fail>
		<li>${fail["svrl:text"]}<br />
		location: ${fail["@location"]}<br />
		failed test: ${fail["@test"]}
		</li>
		</#list>
		</ul>
	<#elseif FilesDoNotMatchManifestException??>
	  <#if (FilesDoNotMatchManifestException["missingFiles"]?size > 0)>
	    Missing Files:<br />
	    <ul>
	      <#list FilesDoNotMatchManifestException["missingFiles"] as file>
	  	  <li>${file}</li>
	      </#list>	
	    </ul>
	  </#if>	  
	  <#if (FilesDoNotMatchManifestException["extraFiles"]?size > 0)>
	    Extra Files:<br />
	    <ul>
	      <#list FilesDoNotMatchManifestException["extraFiles"] as file>
	  	  <li>${file}</li>
	      </#list>	
	    </ul>
	  </#if>	  
	  <#if (FilesDoNotMatchManifestException["badChecksumFiles"]?size > 0)>
	    Files not matching checksum:<br />
	    <ul>
	      <#list FilesDoNotMatchManifestException["badChecksumFiles"] as file>
	  	  <li>${file}</li>
	      </#list>	
	    </ul>
	  </#if>
	</#if>
  
  <p>Thank you for contributing to the <a href="${irBaseUrl}">Digital Collections Repository</a>, a service of the <a href="http://www.lib.unc.edu/">University of North Carolina at Chapel Hill Libraries</a>.</p>
</body>
</html>