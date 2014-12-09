<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="yes" indent="no"/>
    
    <xsl:template name="getScriptName">
        <xsl:param name="scriptNodes"/>
            <xsl:choose>
                <xsl:when test="boolean($scriptNodes[@type='text'])">
                    <xsl:for-each select="$scriptNodes[@type='text']">
                        <xsl:if test="position() != 1">
                            <xsl:text>; </xsl:text>
                        </xsl:if>
                        <xsl:value-of select="text()"/>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test="boolean($scriptNodes[@type = 'code' and @authority='iso15924'])">
                    <xsl:call-template name="getISO15924Name">
                        <xsl:with-param name="scriptCode" select="$scriptNodes[@type = 'code' and @authority='iso15924']/text()"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="getISO15924Name">
                        <xsl:with-param name="scriptCode" select="$scriptNodes/text()"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
	</xsl:template>

	<xsl:template name="getISO15924Name">
		<xsl:param name="scriptCode"/>
		<xsl:choose>
		    <xsl:when test="$scriptCode = 'Adlm'">Adlam</xsl:when>
		    <xsl:when test="$scriptCode = 'Afak'">Afaka</xsl:when>
		    <xsl:when test="$scriptCode = 'Aghb'">Caucasian Albanian</xsl:when>
		    <xsl:when test="$scriptCode = 'Ahom'">Ahom, Tai Ahom</xsl:when>
		    <xsl:when test="$scriptCode = 'Arab'">Arabic</xsl:when>
		    <xsl:when test="$scriptCode = 'Aran'">Arabic (Nastaliq variant)</xsl:when>
		    <xsl:when test="$scriptCode = 'Armi'">Imperial Aramaic</xsl:when>
		    <xsl:when test="$scriptCode = 'Armn'">Armenian</xsl:when>
		    <xsl:when test="$scriptCode = 'Avst'">Avestan</xsl:when>
		    <xsl:when test="$scriptCode = 'Bali'">Balinese</xsl:when>
		    <xsl:when test="$scriptCode = 'Bamu'">Bamum</xsl:when>
		    <xsl:when test="$scriptCode = 'Bass'">Bassa Vah</xsl:when>
		    <xsl:when test="$scriptCode = 'Batk'">Batak</xsl:when>
		    <xsl:when test="$scriptCode = 'Beng'">Bengali</xsl:when>
		    <xsl:when test="$scriptCode = 'Blis'">Blissymbols</xsl:when>
		    <xsl:when test="$scriptCode = 'Bopo'">Bopomofo</xsl:when>
		    <xsl:when test="$scriptCode = 'Brah'">Brahmi</xsl:when>
		    <xsl:when test="$scriptCode = 'Brai'">Braille</xsl:when>
		    <xsl:when test="$scriptCode = 'Bugi'">Buginese</xsl:when>
		    <xsl:when test="$scriptCode = 'Buhd'">Buhid</xsl:when>
		    <xsl:when test="$scriptCode = 'Cakm'">Chakma</xsl:when>
		    <xsl:when test="$scriptCode = 'Cans'">Unified Canadian Aboriginal Syllabics</xsl:when>
		    <xsl:when test="$scriptCode = 'Cari'">Carian</xsl:when>
		    <xsl:when test="$scriptCode = 'Cham'">Cham</xsl:when>
		    <xsl:when test="$scriptCode = 'Cher'">Cherokee</xsl:when>
		    <xsl:when test="$scriptCode = 'Cirt'">Cirth</xsl:when>
		    <xsl:when test="$scriptCode = 'Copt'">Coptic</xsl:when>
		    <xsl:when test="$scriptCode = 'Cprt'">Cypriot</xsl:when>
		    <xsl:when test="$scriptCode = 'Cyrl'">Cyrillic</xsl:when>
		    <xsl:when test="$scriptCode = 'Cyrs'">Cyrillic (Old Church Slavonic variant)</xsl:when>
		    <xsl:when test="$scriptCode = 'Deva'">Devanagari (Nagari)</xsl:when>
		    <xsl:when test="$scriptCode = 'Dsrt'">Deseret (Mormon)</xsl:when>
		    <xsl:when test="$scriptCode = 'Dupl'">Duployan shorthand, Duployan stenography</xsl:when>
		    <xsl:when test="$scriptCode = 'Egyd'">Egyptian demotic</xsl:when>
		    <xsl:when test="$scriptCode = 'Egyh'">Egyptian hieratic</xsl:when>
		    <xsl:when test="$scriptCode = 'Egyp'">Egyptian hieroglyphs</xsl:when>
		    <xsl:when test="$scriptCode = 'Elba'">Elbasan</xsl:when>
		    <xsl:when test="$scriptCode = 'Ethi'">Ethiopic (Ge?ez)</xsl:when>
		    <xsl:when test="$scriptCode = 'Geok'">Khutsuri (Asomtavruli and Nuskhuri)</xsl:when>
		    <xsl:when test="$scriptCode = 'Geor'">Georgian (Mkhedruli)</xsl:when>
		    <xsl:when test="$scriptCode = 'Glag'">Glagolitic</xsl:when>
		    <xsl:when test="$scriptCode = 'Goth'">Gothic</xsl:when>
		    <xsl:when test="$scriptCode = 'Gran'">Grantha</xsl:when>
		    <xsl:when test="$scriptCode = 'Grek'">Greek</xsl:when>
		    <xsl:when test="$scriptCode = 'Gujr'">Gujarati</xsl:when>
		    <xsl:when test="$scriptCode = 'Guru'">Gurmukhi</xsl:when>
		    <xsl:when test="$scriptCode = 'Hang'">Hangul (Hang?l, Hangeul)</xsl:when>
		    <xsl:when test="$scriptCode = 'Hani'">Han (Hanzi, Kanji, Hanja)</xsl:when>
		    <xsl:when test="$scriptCode = 'Hano'">Hanunoo (Hanunóo)</xsl:when>
		    <xsl:when test="$scriptCode = 'Hans'">Han (Simplified variant)</xsl:when>
		    <xsl:when test="$scriptCode = 'Hant'">Han (Traditional variant)</xsl:when>
		    <xsl:when test="$scriptCode = 'Hatr'">Hatran</xsl:when>
		    <xsl:when test="$scriptCode = 'Hebr'">Hebrew</xsl:when>
		    <xsl:when test="$scriptCode = 'Hira'">Hiragana</xsl:when>
		    <xsl:when test="$scriptCode = 'Hluw'">Anatolian Hieroglyphs (Luwian Hieroglyphs, Hittite Hieroglyphs)</xsl:when>
		    <xsl:when test="$scriptCode = 'Hmng'">Pahawh Hmong</xsl:when>
		    <xsl:when test="$scriptCode = 'Hrkt'">Japanese syllabaries (alias for Hiragana + Katakana)</xsl:when>
		    <xsl:when test="$scriptCode = 'Hung'">Old Hungarian (Hungarian Runic)</xsl:when>
		    <xsl:when test="$scriptCode = 'Inds'">Indus (Harappan)</xsl:when>
		    <xsl:when test="$scriptCode = 'Ital'">Old Italic (Etruscan, Oscan, etc.)</xsl:when>
		    <xsl:when test="$scriptCode = 'Java'">Javanese</xsl:when>
		    <xsl:when test="$scriptCode = 'Jpan'">Japanese (alias for Han + Hiragana + Katakana)</xsl:when>
		    <xsl:when test="$scriptCode = 'Jurc'">Jurchen</xsl:when>
		    <xsl:when test="$scriptCode = 'Kali'">Kayah Li</xsl:when>
		    <xsl:when test="$scriptCode = 'Kana'">Katakana</xsl:when>
		    <xsl:when test="$scriptCode = 'Khar'">Kharoshthi</xsl:when>
		    <xsl:when test="$scriptCode = 'Khmr'">Khmer</xsl:when>
		    <xsl:when test="$scriptCode = 'Khoj'">Khojki</xsl:when>
		    <xsl:when test="$scriptCode = 'Kitl'">Khitan large script</xsl:when>
		    <xsl:when test="$scriptCode = 'Kits'">Khitan small script</xsl:when>
		    <xsl:when test="$scriptCode = 'Knda'">Kannada</xsl:when>
		    <xsl:when test="$scriptCode = 'Kore'">Korean (alias for Hangul + Han)</xsl:when>
		    <xsl:when test="$scriptCode = 'Kpel'">Kpelle</xsl:when>
		    <xsl:when test="$scriptCode = 'Kthi'">Kaithi</xsl:when>
		    <xsl:when test="$scriptCode = 'Lana'">Tai Tham (Lanna)</xsl:when>
		    <xsl:when test="$scriptCode = 'Laoo'">Lao</xsl:when>
		    <xsl:when test="$scriptCode = 'Latf'">Latin (Fraktur variant)</xsl:when>
		    <xsl:when test="$scriptCode = 'Latg'">Latin (Gaelic variant)</xsl:when>
		    <xsl:when test="$scriptCode = 'Latn'">Latin</xsl:when>
		    <xsl:when test="$scriptCode = 'Lepc'">Lepcha (Róng)</xsl:when>
		    <xsl:when test="$scriptCode = 'Limb'">Limbu</xsl:when>
		    <xsl:when test="$scriptCode = 'Lina'">Linear A</xsl:when>
		    <xsl:when test="$scriptCode = 'Linb'">Linear B</xsl:when>
		    <xsl:when test="$scriptCode = 'Lisu'">Lisu (Fraser)</xsl:when>
		    <xsl:when test="$scriptCode = 'Loma'">Loma</xsl:when>
		    <xsl:when test="$scriptCode = 'Lyci'">Lycian</xsl:when>
		    <xsl:when test="$scriptCode = 'Lydi'">Lydian</xsl:when>
		    <xsl:when test="$scriptCode = 'Mahj'">Mahajani</xsl:when>
		    <xsl:when test="$scriptCode = 'Mand'">Mandaic, Mandaean</xsl:when>
		    <xsl:when test="$scriptCode = 'Mani'">Manichaean</xsl:when>
		    <xsl:when test="$scriptCode = 'Marc'">Marchen</xsl:when>
		    <xsl:when test="$scriptCode = 'Maya'">Mayan hieroglyphs</xsl:when>
		    <xsl:when test="$scriptCode = 'Mend'">Mende Kikakui</xsl:when>
		    <xsl:when test="$scriptCode = 'Merc'">Meroitic Cursive</xsl:when>
		    <xsl:when test="$scriptCode = 'Mero'">Meroitic Hieroglyphs</xsl:when>
		    <xsl:when test="$scriptCode = 'Mlym'">Malayalam</xsl:when>
		    <xsl:when test="$scriptCode = 'Modi'">Modi, Mo??</xsl:when>
		    <xsl:when test="$scriptCode = 'Mong'">Mongolian</xsl:when>
		    <xsl:when test="$scriptCode = 'Moon'">Moon (Moon code, Moon script, Moon type)</xsl:when>
		    <xsl:when test="$scriptCode = 'Mroo'">Mro, Mru</xsl:when>
		    <xsl:when test="$scriptCode = 'Mtei'">Meitei Mayek (Meithei, Meetei)</xsl:when>
		    <xsl:when test="$scriptCode = 'Mult'">Multani</xsl:when>
		    <xsl:when test="$scriptCode = 'Mymr'">Myanmar (Burmese)</xsl:when>
		    <xsl:when test="$scriptCode = 'Narb'">Old North Arabian (Ancient North Arabian)</xsl:when>
		    <xsl:when test="$scriptCode = 'Nbat'">Nabataean</xsl:when>
		    <xsl:when test="$scriptCode = 'Nkgb'">Nakhi Geba ('Na-'Khi ²Gg?-¹baw, Naxi Geba)</xsl:when>
		    <xsl:when test="$scriptCode = 'Nkoo'">N’Ko</xsl:when>
		    <xsl:when test="$scriptCode = 'Nshu'">Nüshu</xsl:when>
		    <xsl:when test="$scriptCode = 'Ogam'">Ogham</xsl:when>
		    <xsl:when test="$scriptCode = 'Olck'">Ol Chiki (Ol Cemet’, Ol, Santali)</xsl:when>
		    <xsl:when test="$scriptCode = 'Orkh'">Old Turkic, Orkhon Runic</xsl:when>
		    <xsl:when test="$scriptCode = 'Orya'">Oriya</xsl:when>
		    <xsl:when test="$scriptCode = 'Osge'">Osage</xsl:when>
		    <xsl:when test="$scriptCode = 'Osma'">Osmanya</xsl:when>
		    <xsl:when test="$scriptCode = 'Palm'">Palmyrene</xsl:when>
		    <xsl:when test="$scriptCode = 'Pauc'">Pau Cin Hau</xsl:when>
		    <xsl:when test="$scriptCode = 'Perm'">Old Permic</xsl:when>
		    <xsl:when test="$scriptCode = 'Phag'">Phags-pa</xsl:when>
		    <xsl:when test="$scriptCode = 'Phli'">Inscriptional Pahlavi</xsl:when>
		    <xsl:when test="$scriptCode = 'Phlp'">Psalter Pahlavi</xsl:when>
		    <xsl:when test="$scriptCode = 'Phlv'">Book Pahlavi</xsl:when>
		    <xsl:when test="$scriptCode = 'Phnx'">Phoenician</xsl:when>
		    <xsl:when test="$scriptCode = 'Plrd'">Miao (Pollard)</xsl:when>
		    <xsl:when test="$scriptCode = 'Prti'">Inscriptional Parthian</xsl:when>
		    <xsl:when test="$scriptCode = 'Qaaa'">Reserved for private use (start)</xsl:when>
		    <xsl:when test="$scriptCode = 'Qabx'">Reserved for private use (end)</xsl:when>
		    <xsl:when test="$scriptCode = 'Rjng'">Rejang (Redjang, Kaganga)</xsl:when>
		    <xsl:when test="$scriptCode = 'Roro'">Rongorongo</xsl:when>
		    <xsl:when test="$scriptCode = 'Runr'">Runic</xsl:when>
		    <xsl:when test="$scriptCode = 'Samr'">Samaritan</xsl:when>
		    <xsl:when test="$scriptCode = 'Sara'">Sarati</xsl:when>
		    <xsl:when test="$scriptCode = 'Sarb'">Old South Arabian</xsl:when>
		    <xsl:when test="$scriptCode = 'Saur'">Saurashtra</xsl:when>
		    <xsl:when test="$scriptCode = 'Sgnw'">SignWriting</xsl:when>
		    <xsl:when test="$scriptCode = 'Shaw'">Shavian (Shaw)</xsl:when>
		    <xsl:when test="$scriptCode = 'Shrd'">Sharada, ??rad?</xsl:when>
		    <xsl:when test="$scriptCode = 'Sidd'">Siddham, Siddha?, Siddham?t?k?</xsl:when>
		    <xsl:when test="$scriptCode = 'Sind'">Khudawadi, Sindhi</xsl:when>
		    <xsl:when test="$scriptCode = 'Sinh'">Sinhala</xsl:when>
		    <xsl:when test="$scriptCode = 'Sora'">Sora Sompeng</xsl:when>
		    <xsl:when test="$scriptCode = 'Sund'">Sundanese</xsl:when>
		    <xsl:when test="$scriptCode = 'Sylo'">Syloti Nagri</xsl:when>
		    <xsl:when test="$scriptCode = 'Syrc'">Syriac</xsl:when>
		    <xsl:when test="$scriptCode = 'Syre'">Syriac (Estrangelo variant)</xsl:when>
		    <xsl:when test="$scriptCode = 'Syrj'">Syriac (Western variant)</xsl:when>
		    <xsl:when test="$scriptCode = 'Syrn'">Syriac (Eastern variant)</xsl:when>
		    <xsl:when test="$scriptCode = 'Tagb'">Tagbanwa</xsl:when>
		    <xsl:when test="$scriptCode = 'Takr'">Takri, ??kr?, ???kr?</xsl:when>
		    <xsl:when test="$scriptCode = 'Tale'">Tai Le</xsl:when>
		    <xsl:when test="$scriptCode = 'Talu'">New Tai Lue</xsl:when>
		    <xsl:when test="$scriptCode = 'Taml'">Tamil</xsl:when>
		    <xsl:when test="$scriptCode = 'Tang'">Tangut</xsl:when>
		    <xsl:when test="$scriptCode = 'Tavt'">Tai Viet</xsl:when>
		    <xsl:when test="$scriptCode = 'Telu'">Telugu</xsl:when>
		    <xsl:when test="$scriptCode = 'Teng'">Tengwar</xsl:when>
		    <xsl:when test="$scriptCode = 'Tfng'">Tifinagh (Berber)</xsl:when>
		    <xsl:when test="$scriptCode = 'Tglg'">Tagalog (Baybayin, Alibata)</xsl:when>
		    <xsl:when test="$scriptCode = 'Thaa'">Thaana</xsl:when>
		    <xsl:when test="$scriptCode = 'Thai'">Thai</xsl:when>
		    <xsl:when test="$scriptCode = 'Tibt'">Tibetan</xsl:when>
		    <xsl:when test="$scriptCode = 'Tirh'">Tirhuta</xsl:when>
		    <xsl:when test="$scriptCode = 'Ugar'">Ugaritic</xsl:when>
		    <xsl:when test="$scriptCode = 'Vaii'">Vai</xsl:when>
		    <xsl:when test="$scriptCode = 'Visp'">Visible Speech</xsl:when>
		    <xsl:when test="$scriptCode = 'Wara'">Warang Citi (Varang Kshiti)</xsl:when>
		    <xsl:when test="$scriptCode = 'Wole'">Woleai</xsl:when>
		    <xsl:when test="$scriptCode = 'Xpeo'">Old Persian</xsl:when>
		    <xsl:when test="$scriptCode = 'Xsux'">Cuneiform, Sumero-Akkadian</xsl:when>
		    <xsl:when test="$scriptCode = 'Yiii'">Yi</xsl:when>
		    <xsl:when test="$scriptCode = 'Zinh'">Code for inherited script</xsl:when>
		    <xsl:when test="$scriptCode = 'Zmth'">Mathematical notation</xsl:when>
		    <xsl:when test="$scriptCode = 'Zsym'">Symbols</xsl:when>
		    <xsl:when test="$scriptCode = 'Zxxx'">Code for unwritten documents</xsl:when>
		    <xsl:when test="$scriptCode = 'Zyyy'">Code for undetermined script</xsl:when>
		    <xsl:when test="$scriptCode = 'Zzzz'">Code for uncoded script</xsl:when>
			<xsl:otherwise><xsl:value-of select="$scriptCode"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
