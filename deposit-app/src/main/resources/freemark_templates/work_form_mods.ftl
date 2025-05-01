<?xml version="1.0" encoding="UTF-8"?>
<mods xmlns="http://www.loc.gov/mods/v3">
    <#if data.title?has_content>
        <titleInfo>
            <title>${data.title}</title>
        </titleInfo>
    </#if>
    <#if data.alternateTitle?has_content>
        <titleInfo type="alternative">
            <title>${data.alternateTitle}</title>
        </titleInfo>
    </#if>
    <#if data.precedingTitle?has_content>
        <relatedItem type="preceding">
            <titleInfo>
                <title>${data.precedingTitle}</title>
            </titleInfo>
        </relatedItem>
    </#if>
    <#if data.succeedingTitle?has_content>
        <relatedItem type="succeeding">
            <titleInfo>
                <title>${data.succeedingTitle}</title>
            </titleInfo>
        </relatedItem>
    </#if>
    <#if data.relatedUrl?has_content>
        <relatedItem displayLabel="Related resource">
            <location>
                <url>${data.relatedUrl}</url>
            </location>
        </relatedItem>
    </#if>

    <#if data.creatorInfo?has_content>
        <#list data.creatorInfo as creator>
            <#if creator.fname?has_content || creator.lname?has_content>
                <name type="personal">
                    <#if creator.fname?has_content>
                        <namePart type="given">${creator.fname}</namePart>
                    </#if>
                    <#if creator.lname?has_content>
                        <namePart type="family">${creator.lname}</namePart>
                    </#if>
                    <#if creator.dates?has_content>
                        <namePart type="date">${creator.dates}</namePart>
                    </#if>
                    <#if creator.termsAddress?has_content>
                            <namePart type="termsOfAddress">${creator.termsAddress}</namePart>
                    </#if>
                    <role>
                        <roleTerm type="text" authority="marcrelator">Creator</roleTerm>
                    </role>
                </name>
            </#if>
        </#list>
    </#if>
    <#if data.corporateCreator?has_content>
        <#list data.corporateCreator as creator>
            <#if creator.name?has_content>
                <name type="corporate">
                    <namePart>${creator.name}</namePart>
                    <role>
                        <roleTerm type="text" authority="marcrelator">Creator</roleTerm>
                    </role>
                </name>
            </#if>
        </#list>
    </#if>

    <#if data.dateCreated?has_content>
        <originInfo>
            <dateCreated encoding="w3cdtf">${data.dateCreated}</dateCreated>
        </originInfo>
    </#if>
    <#if data.dateOfIssue?has_content || data.volume?has_content || data.number?has_content>
        <part>
            <#if data.dateOfIssue?has_content>
                <date encoding="iso8601">${data.dateOfIssue}</date>
            </#if>
            <#if data.volume?has_content>
                <detail type="volume">
                    <number>${data.volume}</number>
                </detail>
            </#if>
            <#if data.number?has_content>
                <detail type="number">
                    <number>${data.number}</number>
                </detail>
            </#if>
        </part>
    </#if>
    
    <#if data.fname?has_content || data.lname?has_content>
        <name type="personal">
            <#if data.fname?has_content><namePart type="given">${data.fname}</namePart></#if>
            <#if data.lname?has_content><namePart type="family">${data.lname}</namePart></#if>
        </name>
    </#if>

    <#if data.language?has_content>
        <language>
            <languageTerm authority="iso639-2b" type="code">${data.language}</languageTerm>
        </language>
    </#if>
    <#if data.resourceType?has_content>
        <typeOfResource>${data.resourceType}</typeOfResource>
    </#if>
    <#if data.genre?has_content>
        <genre authority="lcgft">${data.genre}</genre>
    </#if>

    <#if data.placeOfPublication?has_content>
        <originInfo>
            <place>
                <placeTerm type="text">${data.placeOfPublication}</placeTerm>
            </place>
            <publisher>${data.publisher}</publisher>
            <issuance>${data.issuance}</issuance>
            <frequency authority="marcfrequency">${data.frequency}</frequency>
        </originInfo>
    </#if>

    <#-- Subject handling -->
    <#if data.subjectTopical?has_content>
        <#list data.subjectTopical as subject>
            <#if subject.subjectTopical?has_content>
                <subject authority="lcsh">
                    <topic>${subject.subjectTopical}</topic>
                </subject>
            </#if>
        </#list>
    </#if>
    <#if data.subjectPersonal?has_content>
        <#list data.subjectPersonal as subject>
            <#if subject.subjectPersonal?has_content>
                <subject authority="lcsh">
                    <name type="personal">
                        <namePart>${subject.subjectPersonal}</namePart>
                    </name>
                </subject>
            </#if>
        </#list>
    </#if>
    <#if data.subjectCorporate?has_content>
        <#list data.subjectCorporate as subject>
            <#if subject.subjectCorporate?has_content>
                <subject authority="lcsh">
                    <name type="corporate">
                        <namePart>${subject.subjectCorporate}</namePart>
                    </name>
                </subject>
            </#if>
        </#list>
    </#if>
    <#if data.subjectGeographic?has_content>
        <#list data.subjectGeographic as subject>
            <#if subject.subjectGeographic?has_content>
                <subject authority="lcsh">
                    <geographic>${subject.subjectGeographic}</geographic>
                </subject>
            </#if>
        </#list>
    </#if>

    <#if data.keywords?has_content>
        <note displayLabel="Keywords">${data.keywords?map(keywords -> keywords.keyword)?join("; ")}</note>
    </#if>
</mods>