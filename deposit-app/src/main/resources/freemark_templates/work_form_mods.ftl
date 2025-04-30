<?xml version="1.0" encoding="UTF-8"?>
<mods xmlns="http://www.loc.gov/mods/v3">
    <#if data.title?has_content>
        <titleInfo>
            <title>${data.title}</title>
        </titleInfo>
    </#if>

    <#if data.fname?has_content || data.lname?has_content>
        <name type="personal">
            <#if data.fname?has_content><namePart type="given">${data.fname}</namePart></#if>
            <#if data.lname?has_content><namePart type="family">${data.lname}</namePart></#if>
        </name>
    </#if>

    <#if data.language?has_content>
        <language>
            <languageTerm type="code" authority="iso639-2b">${data.language}</languageTerm>
        </language>
    </#if>

    <#-- Subject handling -->
    <#if data.subjectInfoTopical?has_content>
        <#list data.subjectInfoTopical as subject>
            <#if subject.subjectTopical?has_content>
                <subject>
                    <topic>${subject.subjectTopical}</topic>
                </subject>
            </#if>
        </#list>
    </#if>

    <#-- More MODS elements -->
</mods>