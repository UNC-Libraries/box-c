<?xml version="1.0" encoding="UTF-8"?>
<mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
    <#if data.title?has_content>
        <mods:titleInfo>
            <mods:title>${data.title}</mods:title>
        </mods:titleInfo>
    </#if>
    <#if data.alternateTitle?has_content>
        <mods:titleInfo type="alternative">
            <mods:title>${data.alternateTitle}</mods:title>
        </mods:titleInfo>
    </#if>
    <#if data.precedingTitle?has_content>
        <mods:relatedItem type="preceding">
            <mods:titleInfo>
                <mods:title>${data.precedingTitle}</mods:title>
            </mods:titleInfo>
        </mods:relatedItem>
    </#if>
    <#if data.succeedingTitle?has_content>
        <mods:relatedItem type="succeeding">
            <mods:titleInfo>
                <mods:title>${data.succeedingTitle}</mods:title>
            </mods:titleInfo>
        </mods:relatedItem>
    </#if>
    <#if data.relatedUrl?has_content>
        <mods:relatedItem displayLabel="Related resource">
            <mods:location>
                <mods:url>${data.relatedUrl}</mods:url>
            </mods:location>
        </mods:relatedItem>
    </#if>

    <#if data.description?has_content>
        <mods:note displayLabel="Description">${data.description}</mods:note>
    </#if>

    <#if data.creatorInfo?has_content>
        <#list data.creatorInfo as creator>
            <#if creator.fname?has_content || creator.lname?has_content>
                <mods:name type="personal">
                    <#if creator.fname?has_content>
                        <mods:namePart type="given">${creator.fname}</mods:namePart>
                    </#if>
                    <#if creator.lname?has_content>
                        <mods:namePart type="family">${creator.lname}</mods:namePart>
                    </#if>
                    <#if creator.dates?has_content>
                        <mods:namePart type="date">${creator.dates}</mods:namePart>
                    </#if>
                    <#if creator.termsAddress?has_content>
                            <mods:namePart type="termsOfAddress">${creator.termsAddress}</mods:namePart>
                    </#if>
                    <mods:role>
                        <mods:roleTerm type="text" authority="marcrelator">Creator</mods:roleTerm>
                    </mods:role>
                </mods:name>
            </#if>
        </#list>
    </#if>
    <#if data.corporateCreator?has_content>
        <#list data.corporateCreator as creator>
            <#if creator.name?has_content>
                <mods:name type="corporate">
                    <mods:namePart>${creator.name}</mods:namePart>
                    <mods:role>
                        <mods:roleTerm type="text" authority="marcrelator">Creator</mods:roleTerm>
                    </mods:role>
                </mods:name>
            </#if>
        </#list>
    </#if>

    <#if data.dateCreated?has_content>
        <mods:originInfo>
            <mods:dateCreated encoding="w3cdtf">${data.dateCreated}</mods:dateCreated>
        </mods:originInfo>
    </#if>
    <#if data.dateOfIssue?has_content || data.volume?has_content || data.number?has_content>
        <mods:part>
            <#if data.dateOfIssue?has_content>
                <mods:date encoding="iso8601">${data.dateOfIssue}</mods:date>
            </#if>
            <#if data.volume?has_content>
                <mods:detail type="volume">
                    <mods:number>${data.volume}</mods:number>
                </mods:detail>
            </#if>
            <#if data.number?has_content>
                <mods:detail type="number">
                    <mods:number>${data.number}</mods:number>
                </mods:detail>
            </#if>
        </mods:part>
    </#if>
    
    <#if data.fname?has_content || data.lname?has_content>
        <mods:name type="personal">
            <#if data.fname?has_content><mods:namePart type="given">${data.fname}</mods:namePart></#if>
            <#if data.lname?has_content><mods:namePart type="family">${data.lname}</mods:namePart></#if>
        </mods:name>
    </#if>

    <#if data.language?has_content>
        <mods:language>
            <mods:languageTerm authority="iso639-2b" type="code">${data.language}</mods:languageTerm>
        </mods:language>
    </#if>
    <#if data.resourceType?has_content>
        <mods:typeOfResource>${data.resourceType}</mods:typeOfResource>
    </#if>
    <#if data.genre?has_content>
        <mods:genre authority="lcgft">${data.genre}</mods:genre>
    </#if>

    <#if data.placeOfPublication?has_content>
        <mods:originInfo>
            <mods:place>
                <mods:placeTerm type="text">${data.placeOfPublication}</mods:placeTerm>
            </mods:place>
            <mods:publisher>${data.publisher}</mods:publisher>
            <mods:issuance>${data.issuance}</mods:issuance>
            <mods:frequency authority="marcfrequency">${data.frequency}</mods:frequency>
        </mods:originInfo>
    </#if>

    <#-- Subject handling -->
    <#if data.subjectTopical?has_content>
        <#list data.subjectTopical as subject>
            <#if subject.subjectTopical?has_content>
                <mods:subject authority="lcsh">
                    <mods:topic>${subject.subjectTopical}</mods:topic>
                </mods:subject>
            </#if>
        </#list>
    </#if>
    <#if data.subjectPersonal?has_content>
        <#list data.subjectPersonal as subject>
            <#if subject.subjectPersonal?has_content>
                <mods:subject authority="lcsh">
                    <mods:name type="personal">
                        <mods:namePart>${subject.subjectPersonal}</mods:namePart>
                    </mods:name>
                </mods:subject>
            </#if>
        </#list>
    </#if>
    <#if data.subjectCorporate?has_content>
        <#list data.subjectCorporate as subject>
            <#if subject.subjectCorporate?has_content>
                <mods:subject authority="lcsh">
                    <mods:name type="corporate">
                        <mods:namePart>${subject.subjectCorporate}</mods:namePart>
                    </mods:name>
                </mods:subject>
            </#if>
        </#list>
    </#if>
    <#if data.subjectGeographic?has_content>
        <#list data.subjectGeographic as subject>
            <#if subject.subjectGeographic?has_content>
                <mods:subject authority="lcsh">
                    <mods:geographic>${subject.subjectGeographic}</mods:geographic>
                </mods:subject>
            </#if>
        </#list>
    </#if>

    <#if data.keywords?has_content>
        <mods:note displayLabel="Keywords">${data.keywords?map(keywords -> keywords.keyword)?join("; ")}</mods:note>
    </#if>
</mods:mods>