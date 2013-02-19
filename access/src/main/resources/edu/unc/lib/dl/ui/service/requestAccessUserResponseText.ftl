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
Carolina Digital Repository

Thank you for your interest, ${form.personalName!}.  This email is to confirm that your request for access has been sent.
You will receive an update on the status of your request within two business days.

The following information was sent:  
<#if form.requestedId?has_content>
Requesting:
${form.requestedTitle!}
https://${serverName!}/record?id=${form.requestedId}

</#if>
Name:
${form.personalName!}

Username:
${form.username!}

Email:
${form.emailAddress!}

Phone:
${form.phoneNumber!}

Affiliation:
${form.affiliation!}

Comments:
${form.comments!}