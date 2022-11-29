<#if 1 < numberOfObjects>
${numberOfObjects} items were successfully ingested into the repository in <#if 1 < tops?size>these locations<#else>this location</#if>:
<#else>
Your item was successfully ingested into the repository in this location:
</#if>
<#list tops as obj>
  ${obj.label!"No label set!"} - ${irBaseUrl}record?id=${obj.pid}
</#list>

Thank you for contributing to the Digital Collections Repository, a service of the University of North Carolina at Chapel Hill Libraries.

    Digital Collections Repository:    ${irBaseUrl}
      UNC Chapel Hill Libraries:    http://www.lib.unc.edu/