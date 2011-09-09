<#ftl ns_prefixes={"svrl":"http://purl.oclc.org/dsdl/svrl"}>
Your submission was not ingested into the repository.

<#if message??>
Error: ${message}
</#if>

<#if METSParseException??>
  <#if (METSParseException["fatalErrors"]?size > 0)>
  Fatal Errors:
    <#list METSParseException["fatalErrors"] as sax>
    * ${sax.message}
    </#list>
  </#if>
  <#if (METSParseException["errors"]?size > 0)>
  Errors:
    <#list METSParseException["errors"] as sax>
  	* ${sax.message}
    </#list>
  </#if>
  <#if (METSParseException["warnings"]?size > 0)>
  Warnings:
    <#list METSParseException["warnings"] as sax>
  	* ${sax.message}
    </#list>
  </#if>
<#elseif svrl??>
  ${svrl["svrl:schematron-output/@title"]}
	Your METS document did not conform to a METS profile.  Here is the list of problems identified:
	<#list svrl["svrl:schematron-output/svrl:failed-assert"] as fail>
	* ${fail["svrl:text"]}
	  location: ${fail["@location"]}
	  failed test: ${fail["@test"]}
	</#list>
<#elseif FilesDoNotMatchManifestException??>
  <#if (FilesDoNotMatchManifestException["missingFiles"]?size > 0)>
  Missing Files:
    <#list FilesDoNotMatchManifestException["missingFiles"] as file>
  	* ${file}
    </#list>
  </#if>	  
  <#if (FilesDoNotMatchManifestException["extraFiles"]?size > 0)>
  Extra Files:
    <#list FilesDoNotMatchManifestException["extraFiles"] as file>
  	* ${file}
    </#list>
  </#if>	  
  <#if (FilesDoNotMatchManifestException["badChecksumFiles"]?size > 0)>
  Files not matching checksum:
    <#list FilesDoNotMatchManifestException["badChecksumFiles"] as file>
    * ${file}
    </#list>
  </#if>
</#if>

Thank you for contributing to the Carolina Digital Repository, a service of the University of North Carolina at Chapel Hill Libraries.

    Carolina Digital Repository:    ${irBaseUrl}
      UNC Chapel Hill Libraries:    http://www.lib.unc.edu/