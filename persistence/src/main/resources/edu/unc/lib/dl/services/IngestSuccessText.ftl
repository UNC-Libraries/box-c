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
<#if 1 < numberOfObjects>
${numberOfObjects} items were successfully ingested into the repository in <#if 1 < tops?size>these locations<#else>this location</#if>:
<#else>
Your item was successfully ingested into the repository in this location:
</#if>
<#list tops as obj>
  ${obj.label!"No label set!"} - ${irBaseUrl}record?id=${obj.pid}
</#list>

Thank you for contributing to the Carolina Digital Repository, a service of the University of North Carolina at Chapel Hill Libraries.

    Carolina Digital Repository:    ${irBaseUrl}
      UNC Chapel Hill Libraries:    http://www.lib.unc.edu/