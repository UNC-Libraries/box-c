<?xml version="1.0" encoding="UTF-8"?>

<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
	queryBinding="xslt2">
	<sch:ns uri="http://www.loc.gov/mods/v3" prefix="mods"/>
	<sch:ns uri="http://www.w3.org/1999/xlink" prefix="xlink"/>

	<sch:title>Object-Level MODS Descriptive Metadata Requirements</sch:title>

	<sch:let name="path" value="/edu/unc/lib/dl"/>
	<!--<sch:let name="path" value="/home/count0/workspace/cdla-common/src/main/resources/edu/unc/lib/dl"/>-->
	
	<sch:let name="langISO"
		value="document('/edu/unc/lib/dl/schematron/ISO-639-2_utf-8.xml')//value"/>
	
	<!-- commenting out genres -->
	<!--<sch:let name="genreTGM" value="document('/edu/unc/lib/dl/schematron/tgm2.xml')//DESCRIPTOR"/>
	<sch:let name="genreEPRINTS"
		value="document('/edu/unc/lib/dl/schematron/genres_eprints_swap.xml')//value"/>
	<sch:let name="genreEPRINTSURI"
		value="document('/edu/unc/lib/dl/schematron/genres_eprints_swap.xml')//valueURI"/>
	-->
	
	<sch:pattern>
		<sch:title>Prerequisites</sch:title>
		<sch:rule context="mods:mods">
			<sch:assert test="function-available('document')">The document() function is
			required.</sch:assert>
		</sch:rule>
	</sch:pattern>

	<sch:pattern>
		<sch:title>Key Date</sch:title>
		<sch:rule context="mods:originInfo/mods:*[@keyDate='yes']">
			<sch:assert test="@encoding = 'iso8601'">Dates must be explicitly encoded according to
				the ISO 8601 standard. (The encoding attribute must be 'iso8601'.)</sch:assert>
		</sch:rule>
	</sch:pattern>
	
	<sch:pattern>
		<sch:title>Language Authority</sch:title>
		<sch:rule context="mods:language/mods:languageTerm[@type='code']">
			<sch:assert test="$langISO[text() = current()/text()]">The language code "<sch:value-of select="current()/text()"/>" under the authority "<sch:value-of select="@authority"/>" is not in the ISO 639.2 bibliographic code list. (see http://www.loc.gov/standards/iso639-2/)</sch:assert>
		</sch:rule>
	</sch:pattern>

	<sch:pattern>
		<sch:title>Language Authority</sch:title>
		<sch:rule context="mods:language/mods:languageTerm[@type='code']">
			<sch:assert test="@authority = 'iso639-2b'">Language authority must be specified as the
				ISO 639-2b standard, 'iso639-2b'.</sch:assert>
		</sch:rule>
	</sch:pattern>

	<!--<sch:pattern>
		<sch:title>Abstract</sch:title>
		<sch:rule context="mods:abstract">
			<sch:assert test="count(*[not(text())]) = 0">Only text is allowed within
			abstracts.</sch:assert>
		</sch:rule>
	</sch:pattern>
	-->
	
	<!-- 1/27/2010 added authority term for EPrints Type, made local authority a free text -GJ -->
	<!-- TODO make SKOS vocabulary files, instead of custom schema -->
	<!-- TODO later, perhaps require typeOfResource of 'text' when they use an EPrints type -->
	
	<!--
	<sch:pattern>
		<sch:title>Valid Genre Authorities</sch:title>
		<sch:rule context="mods:genre">
			<sch:assert test="@authority = 'lctgm' or @authority = 'local' or @authority = 'eprints' or @authority = 'marcgt' or exists(@authorityURI)">Valid genre authorities are 'lctgm', 'eprint', 'marcgt', or 'local'.</sch:assert>
		</sch:rule>
	</sch:pattern>
	
	<sch:pattern>
		<sch:title>Valid TGM II Genre Terms</sch:title>
		<sch:rule context="mods:genre">
			<sch:assert test="not(@authority = 'lctgm') or $genreTGM[text() = current()/text()]">The genre "<sch:value-of select="current()/text()"/>" under the authority "<sch:value-of select="@authority"/>" is not in the LOC Thesaurus for Graphic Materials II (TGM II)</sch:assert>
		</sch:rule>
	</sch:pattern>
	
	<sch:pattern>
		<sch:title>Valid Local Genre Terms (EPrints Types)</sch:title>
		<sch:rule context="mods:genre">
			<sch:assert test="not(@authority eq 'eprints') or exists($genreEPRINTS[text() = current()/text()])">The genre "<sch:value-of select="current()/text()"/>" under the authority "<sch:value-of select="@authority"/>" is not in the EPrints Type vocabulary.</sch:assert>
			<sch:assert test="not(@authority eq 'eprints' and exists(@valueURI)) or exists($genreEPRINTSURI[text() = current()/@valueURI])">The genre valueURI attribute "<sch:value-of select="current()/@valueURI"/>" under the authority "<sch:value-of select="@authority"/>" is not in the EPrints Type vocabulary.</sch:assert>
			<sch:assert test="not(@authorityURI eq 'http://purl.org/eprint/type/') or exists($genreEPRINTS[text() = current()/text()])">The genre "<sch:value-of select="current()/text()"/>" under the authority "<sch:value-of select="@authority"/>" is not in the EPrints Type vocabulary.</sch:assert>
			<sch:assert test="not(@authorityURI eq 'http://purl.org/eprint/type/' and exists(@valueURI)) or exists($genreEPRINTSURI[text() = current()/@valueURI])">The genre valueURI attribute "<sch:value-of select="current()/@valueURI"/>" under the authority "<sch:value-of select="@authority"/>" is not in the EPrints Type vocabulary.</sch:assert>
		</sch:rule>
	</sch:pattern>
	-->

</sch:schema>
