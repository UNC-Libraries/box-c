<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:fn="http://www.w3.org/2005/xpath-functions">
	<xsl:output  method="xml" omit-xml-declaration="yes" indent="no"/>
	
	<!-- Returns the language names for mods:language nodes -->
	<xsl:template name="getLanguageName">
		<xsl:param name="languageNodes"/>
		
		<xsl:choose>
			<xsl:when test="boolean($languageNodes[@type='text'])">
				<xsl:for-each select="$languageNodes[@type='text']">
					<xsl:if test="position() != 1">
							<xsl:text>; </xsl:text>
					</xsl:if>
					<xsl:value-of select="text()" />
				</xsl:for-each>
			</xsl:when>
			<xsl:when test="boolean($languageNodes[@type = 'code' and @authority='iso639-2b'])">
				<xsl:call-template name="ISO639-2btoName">
					<xsl:with-param name="langCode" select="$languageNodes[@type = 'code' and @authority='iso639-2b']/text()" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$languageNodes/text()"/>
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:template>
	
	<xsl:template name="RFC3066toISO639-2b">
		<xsl:param name="langCode" />
		<!-- Strip off the region if it is set -->
		<xsl:variable name="firstTwo">
			<xsl:choose>
				<xsl:when test="fn:contains($langCode, '-')"><xsl:value-of select="fn:substring-before($langCode, '-')"/></xsl:when>
				<xsl:otherwise><xsl:value-of select="$langCode"/></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
 		<xsl:call-template name="ISO639-1toISO639-2b">
 			<xsl:with-param name="langCode" select="$firstTwo"/>
 		</xsl:call-template>
	</xsl:template>
	
	<xsl:template name="ISO639-2btoName">
		<xsl:param name="langCode" />
		<xsl:choose>
			<xsl:when test="$langCode = '(none)'">Serbo-Croatian</xsl:when>
			<xsl:when test="$langCode = 'aar'">Afar</xsl:when>
			<xsl:when test="$langCode = 'abk'">Abkhazian</xsl:when>
			<xsl:when test="$langCode = 'ace'">Achinese</xsl:when>
			<xsl:when test="$langCode = 'ach'">Acoli</xsl:when>
			<xsl:when test="$langCode = 'ada'">Adangme</xsl:when>
			<xsl:when test="$langCode = 'ady'">Adyghe; Adygei</xsl:when>
			<xsl:when test="$langCode = 'afa'">Afro-Asiatic languages</xsl:when>
			<xsl:when test="$langCode = 'afh'">Afrihili</xsl:when>
			<xsl:when test="$langCode = 'afr'">Afrikaans</xsl:when>
			<xsl:when test="$langCode = 'ain'">Ainu</xsl:when>
			<xsl:when test="$langCode = 'aka'">Akan</xsl:when>
			<xsl:when test="$langCode = 'akk'">Akkadian</xsl:when>
			<xsl:when test="$langCode = 'alb'">Albanian</xsl:when>
			<xsl:when test="$langCode = 'ale'">Aleut</xsl:when>
			<xsl:when test="$langCode = 'alg'">Algonquian languages</xsl:when>
			<xsl:when test="$langCode = 'alt'">Southern Altai</xsl:when>
			<xsl:when test="$langCode = 'amh'">Amharic</xsl:when>
			<xsl:when test="$langCode = 'ang'">English, Old (ca.450-1100)</xsl:when>
			<xsl:when test="$langCode = 'anp'">Angika</xsl:when>
			<xsl:when test="$langCode = 'apa'">Apache languages</xsl:when>
			<xsl:when test="$langCode = 'ara'">Arabic</xsl:when>
			<xsl:when test="$langCode = 'arc'">Official Aramaic (700-300 BCE); Imperial Aramaic (700-300 BCE)</xsl:when>
			<xsl:when test="$langCode = 'arg'">Aragonese</xsl:when>
			<xsl:when test="$langCode = 'arm'">Armenian</xsl:when>
			<xsl:when test="$langCode = 'arn'">Mapudungun; Mapuche</xsl:when>
			<xsl:when test="$langCode = 'arp'">Arapaho</xsl:when>
			<xsl:when test="$langCode = 'art'">Artificial languages</xsl:when>
			<xsl:when test="$langCode = 'arw'">Arawak</xsl:when>
			<xsl:when test="$langCode = 'asm'">Assamese</xsl:when>
			<xsl:when test="$langCode = 'ast'">Asturian; Bable; Leonese; Asturleonese</xsl:when>
			<xsl:when test="$langCode = 'ath'">Athapascan languages</xsl:when>
			<xsl:when test="$langCode = 'aus'">Australian languages</xsl:when>
			<xsl:when test="$langCode = 'ava'">Avaric</xsl:when>
			<xsl:when test="$langCode = 'ave'">Avestan</xsl:when>
			<xsl:when test="$langCode = 'awa'">Awadhi</xsl:when>
			<xsl:when test="$langCode = 'aym'">Aymara</xsl:when>
			<xsl:when test="$langCode = 'aze'">Azerbaijani</xsl:when>
			<xsl:when test="$langCode = 'bad'">Banda languages</xsl:when>
			<xsl:when test="$langCode = 'bai'">Bamileke languages</xsl:when>
			<xsl:when test="$langCode = 'bak'">Bashkir</xsl:when>
			<xsl:when test="$langCode = 'bal'">Baluchi</xsl:when>
			<xsl:when test="$langCode = 'bam'">Bambara</xsl:when>
			<xsl:when test="$langCode = 'ban'">Balinese</xsl:when>
			<xsl:when test="$langCode = 'baq'">Basque</xsl:when>
			<xsl:when test="$langCode = 'bas'">Basa</xsl:when>
			<xsl:when test="$langCode = 'bat'">Baltic languages</xsl:when>
			<xsl:when test="$langCode = 'bej'">Beja; Bedawiyet</xsl:when>
			<xsl:when test="$langCode = 'bel'">Belarusian</xsl:when>
			<xsl:when test="$langCode = 'bem'">Bemba</xsl:when>
			<xsl:when test="$langCode = 'ben'">Bengali</xsl:when>
			<xsl:when test="$langCode = 'ber'">Berber languages)</xsl:when>
			<xsl:when test="$langCode = 'bho'">Bhojpuri</xsl:when>
			<xsl:when test="$langCode = 'bih'">Bihari languages</xsl:when>
			<xsl:when test="$langCode = 'bik'">Bikol</xsl:when>
			<xsl:when test="$langCode = 'bin'">Bini; Edo</xsl:when>
			<xsl:when test="$langCode = 'bis'">Bislama</xsl:when>
			<xsl:when test="$langCode = 'bla'">Siksika</xsl:when>
			<xsl:when test="$langCode = 'bnt'">Bantu languages</xsl:when>
			<xsl:when test="$langCode = 'bos'">Bosnian</xsl:when>
			<xsl:when test="$langCode = 'bra'">Braj</xsl:when>
			<xsl:when test="$langCode = 'bre'">Breton</xsl:when>
			<xsl:when test="$langCode = 'btk'">Batak languages</xsl:when>
			<xsl:when test="$langCode = 'bua'">Buriat</xsl:when>
			<xsl:when test="$langCode = 'bug'">Buginese</xsl:when>
			<xsl:when test="$langCode = 'bul'">Bulgarian</xsl:when>
			<xsl:when test="$langCode = 'bur'">Burmese</xsl:when>
			<xsl:when test="$langCode = 'byn'">Blin; Bilin</xsl:when>
			<xsl:when test="$langCode = 'cad'">Caddo</xsl:when>
			<xsl:when test="$langCode = 'cai'">Central American Indian languages</xsl:when>
			<xsl:when test="$langCode = 'car'">Galibi Carib</xsl:when>
			<xsl:when test="$langCode = 'cat'">Catalan; Valencian</xsl:when>
			<xsl:when test="$langCode = 'cau'">Caucasian languages</xsl:when>
			<xsl:when test="$langCode = 'ceb'">Cebuano</xsl:when>
			<xsl:when test="$langCode = 'cel'">Celtic languages</xsl:when>
			<xsl:when test="$langCode = 'cha'">Chamorro</xsl:when>
			<xsl:when test="$langCode = 'chb'">Chibcha</xsl:when>
			<xsl:when test="$langCode = 'che'">Chechen</xsl:when>
			<xsl:when test="$langCode = 'chg'">Chagatai</xsl:when>
			<xsl:when test="$langCode = 'chi'">Chinese</xsl:when>
			<xsl:when test="$langCode = 'chk'">Chuukese</xsl:when>
			<xsl:when test="$langCode = 'chm'">Mari</xsl:when>
			<xsl:when test="$langCode = 'chn'">Chinook jargon</xsl:when>
			<xsl:when test="$langCode = 'cho'">Choctaw</xsl:when>
			<xsl:when test="$langCode = 'chp'">Chipewyan; Dene Suline</xsl:when>
			<xsl:when test="$langCode = 'chr'">Cherokee</xsl:when>
			<xsl:when test="$langCode = 'chu'">Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic</xsl:when>
			<xsl:when test="$langCode = 'chv'">Chuvash</xsl:when>
			<xsl:when test="$langCode = 'chy'">Cheyenne</xsl:when>
			<xsl:when test="$langCode = 'cmc'">Chamic languages</xsl:when>
			<xsl:when test="$langCode = 'cop'">Coptic</xsl:when>
			<xsl:when test="$langCode = 'cor'">Cornish</xsl:when>
			<xsl:when test="$langCode = 'cos'">Corsican</xsl:when>
			<xsl:when test="$langCode = 'cpe'">Creoles and pidgins, English based</xsl:when>
			<xsl:when test="$langCode = 'cpf'">Creoles and pidgins, French-based</xsl:when>
			<xsl:when test="$langCode = 'cpp'">Creoles and pidgins, Portuguese-based</xsl:when>
			<xsl:when test="$langCode = 'cre'">Cree</xsl:when>
			<xsl:when test="$langCode = 'crh'">Crimean Tatar; Crimean Turkish</xsl:when>
			<xsl:when test="$langCode = 'crp'">Creoles and pidgins</xsl:when>
			<xsl:when test="$langCode = 'csb'">Kashubian</xsl:when>
			<xsl:when test="$langCode = 'cus'">Cushitic languages</xsl:when>
			<xsl:when test="$langCode = 'cze'">Czech</xsl:when>
			<xsl:when test="$langCode = 'dak'">Dakota</xsl:when>
			<xsl:when test="$langCode = 'dan'">Danish</xsl:when>
			<xsl:when test="$langCode = 'dar'">Dargwa</xsl:when>
			<xsl:when test="$langCode = 'day'">Land Dayak languages</xsl:when>
			<xsl:when test="$langCode = 'del'">Delaware</xsl:when>
			<xsl:when test="$langCode = 'den'">Slave (Athapascan)</xsl:when>
			<xsl:when test="$langCode = 'dgr'">Dogrib</xsl:when>
			<xsl:when test="$langCode = 'din'">Dinka</xsl:when>
			<xsl:when test="$langCode = 'div'">Divehi; Dhivehi; Maldivian</xsl:when>
			<xsl:when test="$langCode = 'doi'">Dogri</xsl:when>
			<xsl:when test="$langCode = 'dra'">Dravidian languages</xsl:when>
			<xsl:when test="$langCode = 'dsb'">Lower Sorbian</xsl:when>
			<xsl:when test="$langCode = 'dua'">Duala</xsl:when>
			<xsl:when test="$langCode = 'dum'">Dutch, Middle (ca.1050-1350)</xsl:when>
			<xsl:when test="$langCode = 'dut'">Dutch; Flemish</xsl:when>
			<xsl:when test="$langCode = 'dyu'">Dyula</xsl:when>
			<xsl:when test="$langCode = 'dzo'">Dzongkha</xsl:when>
			<xsl:when test="$langCode = 'efi'">Efik</xsl:when>
			<xsl:when test="$langCode = 'egy'">Egyptian (Ancient)</xsl:when>
			<xsl:when test="$langCode = 'eka'">Ekajuk</xsl:when>
			<xsl:when test="$langCode = 'elx'">Elamite</xsl:when>
			<xsl:when test="$langCode = 'eng'">English</xsl:when>
			<xsl:when test="$langCode = 'enm'">English, Middle (1100-1500)</xsl:when>
			<xsl:when test="$langCode = 'epo'">Esperanto</xsl:when>
			<xsl:when test="$langCode = 'est'">Estonian</xsl:when>
			<xsl:when test="$langCode = 'ewe'">Ewe</xsl:when>
			<xsl:when test="$langCode = 'ewo'">Ewondo</xsl:when>
			<xsl:when test="$langCode = 'fan'">Fang</xsl:when>
			<xsl:when test="$langCode = 'fao'">Faroese</xsl:when>
			<xsl:when test="$langCode = 'fat'">Fanti</xsl:when>
			<xsl:when test="$langCode = 'fij'">Fijian</xsl:when>
			<xsl:when test="$langCode = 'fil'">Filipino; Pilipino</xsl:when>
			<xsl:when test="$langCode = 'fin'">Finnish</xsl:when>
			<xsl:when test="$langCode = 'fiu'">Finno-Ugrian languages)</xsl:when>
			<xsl:when test="$langCode = 'fon'">Fon</xsl:when>
			<xsl:when test="$langCode = 'fre'">French</xsl:when>
			<xsl:when test="$langCode = 'frm'">French, Middle (ca.1400-1600)</xsl:when>
			<xsl:when test="$langCode = 'fro'">French, Old (842-ca.1400)</xsl:when>
			<xsl:when test="$langCode = 'frr'">Northern Frisian</xsl:when>
			<xsl:when test="$langCode = 'frs'">Eastern Frisian</xsl:when>
			<xsl:when test="$langCode = 'fry'">Western Frisian</xsl:when>
			<xsl:when test="$langCode = 'ful'">Fulah</xsl:when>
			<xsl:when test="$langCode = 'fur'">Friulian</xsl:when>
			<xsl:when test="$langCode = 'gaa'">Ga</xsl:when>
			<xsl:when test="$langCode = 'gay'">Gayo</xsl:when>
			<xsl:when test="$langCode = 'gba'">Gbaya</xsl:when>
			<xsl:when test="$langCode = 'gem'">Germanic languages</xsl:when>
			<xsl:when test="$langCode = 'geo'">Georgian</xsl:when>
			<xsl:when test="$langCode = 'ger'">German</xsl:when>
			<xsl:when test="$langCode = 'gez'">Geez</xsl:when>
			<xsl:when test="$langCode = 'gil'">Gilbertese</xsl:when>
			<xsl:when test="$langCode = 'gla'">Gaelic; Scottish Gaelic</xsl:when>
			<xsl:when test="$langCode = 'gle'">Irish</xsl:when>
			<xsl:when test="$langCode = 'glg'">Galician</xsl:when>
			<xsl:when test="$langCode = 'glv'">Manx</xsl:when>
			<xsl:when test="$langCode = 'gmh'">German, Middle High (ca.1050-1500)</xsl:when>
			<xsl:when test="$langCode = 'goh'">German, Old High (ca.750-1050)</xsl:when>
			<xsl:when test="$langCode = 'gon'">Gondi</xsl:when>
			<xsl:when test="$langCode = 'gor'">Gorontalo</xsl:when>
			<xsl:when test="$langCode = 'got'">Gothic</xsl:when>
			<xsl:when test="$langCode = 'grb'">Grebo</xsl:when>
			<xsl:when test="$langCode = 'grc'">Greek, Ancient (to 1453)</xsl:when>
			<xsl:when test="$langCode = 'gre'">Greek, Modern (1453-)</xsl:when>
			<xsl:when test="$langCode = 'grn'">Guarani</xsl:when>
			<xsl:when test="$langCode = 'gsw'">Swiss German; Alemannic; Alsatian</xsl:when>
			<xsl:when test="$langCode = 'guj'">Gujarati</xsl:when>
			<xsl:when test="$langCode = 'gwi'">Gwich'in</xsl:when>
			<xsl:when test="$langCode = 'hai'">Haida</xsl:when>
			<xsl:when test="$langCode = 'hat'">Haitian; Haitian Creole</xsl:when>
			<xsl:when test="$langCode = 'hau'">Hausa</xsl:when>
			<xsl:when test="$langCode = 'haw'">Hawaiian</xsl:when>
			<xsl:when test="$langCode = 'heb'">Hebrew</xsl:when>
			<xsl:when test="$langCode = 'her'">Herero</xsl:when>
			<xsl:when test="$langCode = 'hil'">Hiligaynon</xsl:when>
			<xsl:when test="$langCode = 'him'">Himachali languages; Western Pahari languages</xsl:when>
			<xsl:when test="$langCode = 'hin'">Hindi</xsl:when>
			<xsl:when test="$langCode = 'hit'">Hittite</xsl:when>
			<xsl:when test="$langCode = 'hmn'">Hmong; Mong</xsl:when>
			<xsl:when test="$langCode = 'hmo'">Hiri Motu</xsl:when>
			<xsl:when test="$langCode = 'hrv'">Croatian</xsl:when>
			<xsl:when test="$langCode = 'hsb'">Upper Sorbian</xsl:when>
			<xsl:when test="$langCode = 'hun'">Hungarian</xsl:when>
			<xsl:when test="$langCode = 'hup'">Hupa</xsl:when>
			<xsl:when test="$langCode = 'iba'">Iban</xsl:when>
			<xsl:when test="$langCode = 'ibo'">Igbo</xsl:when>
			<xsl:when test="$langCode = 'ice'">Icelandic</xsl:when>
			<xsl:when test="$langCode = 'ido'">Ido</xsl:when>
			<xsl:when test="$langCode = 'iii'">Sichuan Yi; Nuosu</xsl:when>
			<xsl:when test="$langCode = 'ijo'">Ijo languages</xsl:when>
			<xsl:when test="$langCode = 'iku'">Inuktitut</xsl:when>
			<xsl:when test="$langCode = 'ile'">Interlingue; Occidental</xsl:when>
			<xsl:when test="$langCode = 'ilo'">Iloko</xsl:when>
			<xsl:when test="$langCode = 'ina'">Interlingua (International Auxiliary Language Association)</xsl:when>
			<xsl:when test="$langCode = 'inc'">Indic languages</xsl:when>
			<xsl:when test="$langCode = 'ind'">Indonesian</xsl:when>
			<xsl:when test="$langCode = 'ine'">Indo-European languages</xsl:when>
			<xsl:when test="$langCode = 'inh'">Ingush</xsl:when>
			<xsl:when test="$langCode = 'ipk'">Inupiaq</xsl:when>
			<xsl:when test="$langCode = 'ira'">Iranian languages</xsl:when>
			<xsl:when test="$langCode = 'iro'">Iroquoian languages</xsl:when>
			<xsl:when test="$langCode = 'ita'">Italian</xsl:when>
			<xsl:when test="$langCode = 'jav'">Javanese</xsl:when>
			<xsl:when test="$langCode = 'jbo'">Lojban</xsl:when>
			<xsl:when test="$langCode = 'jpn'">Japanese</xsl:when>
			<xsl:when test="$langCode = 'jpr'">Judeo-Persian</xsl:when>
			<xsl:when test="$langCode = 'jrb'">Judeo-Arabic</xsl:when>
			<xsl:when test="$langCode = 'kaa'">Kara-Kalpak</xsl:when>
			<xsl:when test="$langCode = 'kab'">Kabyle</xsl:when>
			<xsl:when test="$langCode = 'kac'">Kachin; Jingpho</xsl:when>
			<xsl:when test="$langCode = 'kal'">Kalaallisut; Greenlandic</xsl:when>
			<xsl:when test="$langCode = 'kam'">Kamba</xsl:when>
			<xsl:when test="$langCode = 'kan'">Kannada</xsl:when>
			<xsl:when test="$langCode = 'kar'">Karen languages</xsl:when>
			<xsl:when test="$langCode = 'kas'">Kashmiri</xsl:when>
			<xsl:when test="$langCode = 'kau'">Kanuri</xsl:when>
			<xsl:when test="$langCode = 'kaw'">Kawi</xsl:when>
			<xsl:when test="$langCode = 'kaz'">Kazakh</xsl:when>
			<xsl:when test="$langCode = 'kbd'">Kabardian</xsl:when>
			<xsl:when test="$langCode = 'kha'">Khasi</xsl:when>
			<xsl:when test="$langCode = 'khi'">Khoisan languages</xsl:when>
			<xsl:when test="$langCode = 'khm'">Central Khmer</xsl:when>
			<xsl:when test="$langCode = 'kho'">Khotanese; Sakan</xsl:when>
			<xsl:when test="$langCode = 'kik'">Kikuyu; Gikuyu</xsl:when>
			<xsl:when test="$langCode = 'kin'">Kinyarwanda</xsl:when>
			<xsl:when test="$langCode = 'kir'">Kirghiz; Kyrgyz</xsl:when>
			<xsl:when test="$langCode = 'kmb'">Kimbundu</xsl:when>
			<xsl:when test="$langCode = 'kok'">Konkani</xsl:when>
			<xsl:when test="$langCode = 'kom'">Komi</xsl:when>
			<xsl:when test="$langCode = 'kon'">Kongo</xsl:when>
			<xsl:when test="$langCode = 'kor'">Korean</xsl:when>
			<xsl:when test="$langCode = 'kos'">Kosraean</xsl:when>
			<xsl:when test="$langCode = 'kpe'">Kpelle</xsl:when>
			<xsl:when test="$langCode = 'krc'">Karachay-Balkar</xsl:when>
			<xsl:when test="$langCode = 'krl'">Karelian</xsl:when>
			<xsl:when test="$langCode = 'kro'">Kru languages</xsl:when>
			<xsl:when test="$langCode = 'kru'">Kurukh</xsl:when>
			<xsl:when test="$langCode = 'kua'">Kuanyama; Kwanyama</xsl:when>
			<xsl:when test="$langCode = 'kum'">Kumyk</xsl:when>
			<xsl:when test="$langCode = 'kur'">Kurdish</xsl:when>
			<xsl:when test="$langCode = 'kut'">Kutenai</xsl:when>
			<xsl:when test="$langCode = 'lad'">Ladino</xsl:when>
			<xsl:when test="$langCode = 'lah'">Lahnda</xsl:when>
			<xsl:when test="$langCode = 'lam'">Lamba</xsl:when>
			<xsl:when test="$langCode = 'lao'">Lao</xsl:when>
			<xsl:when test="$langCode = 'lat'">Latin</xsl:when>
			<xsl:when test="$langCode = 'lav'">Latvian</xsl:when>
			<xsl:when test="$langCode = 'lez'">Lezghian</xsl:when>
			<xsl:when test="$langCode = 'lim'">Limburgan; Limburger; Limburgish</xsl:when>
			<xsl:when test="$langCode = 'lin'">Lingala</xsl:when>
			<xsl:when test="$langCode = 'lit'">Lithuanian</xsl:when>
			<xsl:when test="$langCode = 'lol'">Mongo</xsl:when>
			<xsl:when test="$langCode = 'loz'">Lozi</xsl:when>
			<xsl:when test="$langCode = 'ltz'">Luxembourgish; Letzeburgesch</xsl:when>
			<xsl:when test="$langCode = 'lua'">Luba-Lulua</xsl:when>
			<xsl:when test="$langCode = 'lub'">Luba-Katanga</xsl:when>
			<xsl:when test="$langCode = 'lug'">Ganda</xsl:when>
			<xsl:when test="$langCode = 'lui'">Luiseno</xsl:when>
			<xsl:when test="$langCode = 'lun'">Lunda</xsl:when>
			<xsl:when test="$langCode = 'luo'">Luo (Kenya and Tanzania)</xsl:when>
			<xsl:when test="$langCode = 'lus'">Lushai</xsl:when>
			<xsl:when test="$langCode = 'mac'">Macedonian</xsl:when>
			<xsl:when test="$langCode = 'mad'">Madurese</xsl:when>
			<xsl:when test="$langCode = 'mag'">Magahi</xsl:when>
			<xsl:when test="$langCode = 'mah'">Marshallese</xsl:when>
			<xsl:when test="$langCode = 'mai'">Maithili</xsl:when>
			<xsl:when test="$langCode = 'mak'">Makasar</xsl:when>
			<xsl:when test="$langCode = 'mal'">Malayalam</xsl:when>
			<xsl:when test="$langCode = 'man'">Mandingo</xsl:when>
			<xsl:when test="$langCode = 'mao'">Maori</xsl:when>
			<xsl:when test="$langCode = 'map'">Austronesian languages</xsl:when>
			<xsl:when test="$langCode = 'mar'">Marathi</xsl:when>
			<xsl:when test="$langCode = 'mas'">Masai</xsl:when>
			<xsl:when test="$langCode = 'may'">Malay</xsl:when>
			<xsl:when test="$langCode = 'mdf'">Moksha</xsl:when>
			<xsl:when test="$langCode = 'mdr'">Mandar</xsl:when>
			<xsl:when test="$langCode = 'men'">Mende</xsl:when>
			<xsl:when test="$langCode = 'mga'">Irish, Middle (900-1200)</xsl:when>
			<xsl:when test="$langCode = 'mic'">Mi'kmaq; Micmac</xsl:when>
			<xsl:when test="$langCode = 'min'">Minangkabau</xsl:when>
			<xsl:when test="$langCode = 'mis'">Uncoded languages</xsl:when>
			<xsl:when test="$langCode = 'mkh'">Mon-Khmer languages</xsl:when>
			<xsl:when test="$langCode = 'mlg'">Malagasy</xsl:when>
			<xsl:when test="$langCode = 'mlt'">Maltese</xsl:when>
			<xsl:when test="$langCode = 'mnc'">Manchu</xsl:when>
			<xsl:when test="$langCode = 'mni'">Manipuri</xsl:when>
			<xsl:when test="$langCode = 'mno'">Manobo languages</xsl:when>
			<xsl:when test="$langCode = 'moh'">Mohawk</xsl:when>
			<xsl:when test="$langCode = 'mon'">Mongolian</xsl:when>
			<xsl:when test="$langCode = 'mos'">Mossi</xsl:when>
			<xsl:when test="$langCode = 'mul'">Multiple languages</xsl:when>
			<xsl:when test="$langCode = 'mun'">Munda languages</xsl:when>
			<xsl:when test="$langCode = 'mus'">Creek</xsl:when>
			<xsl:when test="$langCode = 'mwl'">Mirandese</xsl:when>
			<xsl:when test="$langCode = 'mwr'">Marwari</xsl:when>
			<xsl:when test="$langCode = 'myn'">Mayan languages</xsl:when>
			<xsl:when test="$langCode = 'myv'">Erzya</xsl:when>
			<xsl:when test="$langCode = 'nah'">Nahuatl languages</xsl:when>
			<xsl:when test="$langCode = 'nai'">North American Indian languages</xsl:when>
			<xsl:when test="$langCode = 'nap'">Neapolitan</xsl:when>
			<xsl:when test="$langCode = 'nau'">Nauru</xsl:when>
			<xsl:when test="$langCode = 'nav'">Navajo; Navaho</xsl:when>
			<xsl:when test="$langCode = 'nbl'">Ndebele, South; South Ndebele</xsl:when>
			<xsl:when test="$langCode = 'nde'">Ndebele, North; North Ndebele</xsl:when>
			<xsl:when test="$langCode = 'ndo'">Ndonga</xsl:when>
			<xsl:when test="$langCode = 'nds'">Low German; Low Saxon; German, Low; Saxon, Low</xsl:when>
			<xsl:when test="$langCode = 'nep'">Nepali</xsl:when>
			<xsl:when test="$langCode = 'new'">Nepal Bhasa; Newari</xsl:when>
			<xsl:when test="$langCode = 'nia'">Nias</xsl:when>
			<xsl:when test="$langCode = 'nic'">Niger-Kordofanian languages</xsl:when>
			<xsl:when test="$langCode = 'niu'">Niuean</xsl:when>
			<xsl:when test="$langCode = 'nno'">Norwegian Nynorsk; Nynorsk, Norwegian</xsl:when>
			<xsl:when test="$langCode = 'nob'">Bokm√Çl, Norwegian; Norwegian Bokm√Çl</xsl:when>
			<xsl:when test="$langCode = 'nog'">Nogai</xsl:when>
			<xsl:when test="$langCode = 'non'">Norse, Old</xsl:when>
			<xsl:when test="$langCode = 'nor'">Norwegian</xsl:when>
			<xsl:when test="$langCode = 'nqo'">N'Ko</xsl:when>
			<xsl:when test="$langCode = 'nso'">Pedi; Sepedi; Northern Sotho</xsl:when>
			<xsl:when test="$langCode = 'nub'">Nubian languages</xsl:when>
			<xsl:when test="$langCode = 'nwc'">Classical Newari; Old Newari; Classical Nepal Bhasa</xsl:when>
			<xsl:when test="$langCode = 'nya'">Chichewa; Chewa; Nyanja</xsl:when>
			<xsl:when test="$langCode = 'nym'">Nyamwezi</xsl:when>
			<xsl:when test="$langCode = 'nyn'">Nyankole</xsl:when>
			<xsl:when test="$langCode = 'nyo'">Nyoro</xsl:when>
			<xsl:when test="$langCode = 'nzi'">Nzima</xsl:when>
			<xsl:when test="$langCode = 'oci'">Occitan (post 1500)</xsl:when>
			<xsl:when test="$langCode = 'oji'">Ojibwa</xsl:when>
			<xsl:when test="$langCode = 'ori'">Oriya</xsl:when>
			<xsl:when test="$langCode = 'orm'">Oromo</xsl:when>
			<xsl:when test="$langCode = 'osa'">Osage</xsl:when>
			<xsl:when test="$langCode = 'oss'">Ossetian; Ossetic</xsl:when>
			<xsl:when test="$langCode = 'ota'">Turkish, Ottoman (1500-1928)</xsl:when>
			<xsl:when test="$langCode = 'oto'">Otomian languages</xsl:when>
			<xsl:when test="$langCode = 'paa'">Papuan languages</xsl:when>
			<xsl:when test="$langCode = 'pag'">Pangasinan</xsl:when>
			<xsl:when test="$langCode = 'pal'">Pahlavi</xsl:when>
			<xsl:when test="$langCode = 'pam'">Pampanga; Kapampangan</xsl:when>
			<xsl:when test="$langCode = 'pan'">Panjabi; Punjabi</xsl:when>
			<xsl:when test="$langCode = 'pap'">Papiamento</xsl:when>
			<xsl:when test="$langCode = 'pau'">Palauan</xsl:when>
			<xsl:when test="$langCode = 'peo'">Persian, Old (ca.600-400 B.C.)</xsl:when>
			<xsl:when test="$langCode = 'per'">Persian</xsl:when>
			<xsl:when test="$langCode = 'phi'">Philippine languages)</xsl:when>
			<xsl:when test="$langCode = 'phn'">Phoenician</xsl:when>
			<xsl:when test="$langCode = 'pli'">Pali</xsl:when>
			<xsl:when test="$langCode = 'pol'">Polish</xsl:when>
			<xsl:when test="$langCode = 'pon'">Pohnpeian</xsl:when>
			<xsl:when test="$langCode = 'por'">Portuguese</xsl:when>
			<xsl:when test="$langCode = 'pra'">Prakrit languages</xsl:when>
			<xsl:when test="$langCode = 'pro'">Proven√Åal, Old (to 1500);Occitan, Old (to 1500)</xsl:when>
			<xsl:when test="$langCode = 'pus'">Pushto; Pashto</xsl:when>
			<xsl:when test="$langCode = 'qaa-qtz'">Reserved for local use</xsl:when>
			<xsl:when test="$langCode = 'que'">Quechua</xsl:when>
			<xsl:when test="$langCode = 'raj'">Rajasthani</xsl:when>
			<xsl:when test="$langCode = 'rap'">Rapanui</xsl:when>
			<xsl:when test="$langCode = 'rar'">Rarotongan; Cook Islands Maori</xsl:when>
			<xsl:when test="$langCode = 'roa'">Romance languages</xsl:when>
			<xsl:when test="$langCode = 'roh'">Romansh</xsl:when>
			<xsl:when test="$langCode = 'rom'">Romany</xsl:when>
			<xsl:when test="$langCode = 'rum'">Romanian; Moldavian; Moldovan</xsl:when>
			<xsl:when test="$langCode = 'run'">Rundi</xsl:when>
			<xsl:when test="$langCode = 'rup'">Aromanian; Arumanian; Macedo-Romanian</xsl:when>
			<xsl:when test="$langCode = 'rus'">Russian</xsl:when>
			<xsl:when test="$langCode = 'sad'">Sandawe</xsl:when>
			<xsl:when test="$langCode = 'sag'">Sango</xsl:when>
			<xsl:when test="$langCode = 'sah'">Yakut</xsl:when>
			<xsl:when test="$langCode = 'sai'">South American Indian languages</xsl:when>
			<xsl:when test="$langCode = 'sal'">Salishan languages</xsl:when>
			<xsl:when test="$langCode = 'sam'">Samaritan Aramaic</xsl:when>
			<xsl:when test="$langCode = 'san'">Sanskrit</xsl:when>
			<xsl:when test="$langCode = 'sas'">Sasak</xsl:when>
			<xsl:when test="$langCode = 'sat'">Santali</xsl:when>
			<xsl:when test="$langCode = 'scn'">Sicilian</xsl:when>
			<xsl:when test="$langCode = 'sco'">Scots</xsl:when>
			<xsl:when test="$langCode = 'sel'">Selkup</xsl:when>
			<xsl:when test="$langCode = 'sem'">Semitic languages</xsl:when>
			<xsl:when test="$langCode = 'sga'">Irish, Old (to 900)</xsl:when>
			<xsl:when test="$langCode = 'sgn'">Sign Languages</xsl:when>
			<xsl:when test="$langCode = 'shn'">Shan</xsl:when>
			<xsl:when test="$langCode = 'sid'">Sidamo</xsl:when>
			<xsl:when test="$langCode = 'sin'">Sinhala; Sinhalese</xsl:when>
			<xsl:when test="$langCode = 'sio'">Siouan languages</xsl:when>
			<xsl:when test="$langCode = 'sit'">Sino-Tibetan languages</xsl:when>
			<xsl:when test="$langCode = 'sla'">Slavic languages</xsl:when>
			<xsl:when test="$langCode = 'slo'">Slovak</xsl:when>
			<xsl:when test="$langCode = 'slv'">Slovenian</xsl:when>
			<xsl:when test="$langCode = 'sma'">Southern Sami</xsl:when>
			<xsl:when test="$langCode = 'sme'">Northern Sami</xsl:when>
			<xsl:when test="$langCode = 'smi'">Sami languages</xsl:when>
			<xsl:when test="$langCode = 'smj'">Lule Sami</xsl:when>
			<xsl:when test="$langCode = 'smn'">Inari Sami</xsl:when>
			<xsl:when test="$langCode = 'smo'">Samoan</xsl:when>
			<xsl:when test="$langCode = 'sms'">Skolt Sami</xsl:when>
			<xsl:when test="$langCode = 'sna'">Shona</xsl:when>
			<xsl:when test="$langCode = 'snd'">Sindhi</xsl:when>
			<xsl:when test="$langCode = 'snk'">Soninke</xsl:when>
			<xsl:when test="$langCode = 'sog'">Sogdian</xsl:when>
			<xsl:when test="$langCode = 'som'">Somali</xsl:when>
			<xsl:when test="$langCode = 'son'">Songhai languages</xsl:when>
			<xsl:when test="$langCode = 'sot'">Sotho, Southern</xsl:when>
			<xsl:when test="$langCode = 'spa'">Spanish; Castilian</xsl:when>
			<xsl:when test="$langCode = 'srd'">Sardinian</xsl:when>
			<xsl:when test="$langCode = 'srn'">Sranan Tongo</xsl:when>
			<xsl:when test="$langCode = 'srp'">Serbian</xsl:when>
			<xsl:when test="$langCode = 'srr'">Serer</xsl:when>
			<xsl:when test="$langCode = 'ssa'">Nilo-Saharan languages</xsl:when>
			<xsl:when test="$langCode = 'ssw'">Swati</xsl:when>
			<xsl:when test="$langCode = 'suk'">Sukuma</xsl:when>
			<xsl:when test="$langCode = 'sun'">Sundanese</xsl:when>
			<xsl:when test="$langCode = 'sus'">Susu</xsl:when>
			<xsl:when test="$langCode = 'sux'">Sumerian</xsl:when>
			<xsl:when test="$langCode = 'swa'">Swahili</xsl:when>
			<xsl:when test="$langCode = 'swe'">Swedish</xsl:when>
			<xsl:when test="$langCode = 'syc'">Classical Syriac</xsl:when>
			<xsl:when test="$langCode = 'syr'">Syriac</xsl:when>
			<xsl:when test="$langCode = 'tah'">Tahitian</xsl:when>
			<xsl:when test="$langCode = 'tai'">Tai languages</xsl:when>
			<xsl:when test="$langCode = 'tam'">Tamil</xsl:when>
			<xsl:when test="$langCode = 'tat'">Tatar</xsl:when>
			<xsl:when test="$langCode = 'tel'">Telugu</xsl:when>
			<xsl:when test="$langCode = 'tem'">Timne</xsl:when>
			<xsl:when test="$langCode = 'ter'">Tereno</xsl:when>
			<xsl:when test="$langCode = 'tet'">Tetum</xsl:when>
			<xsl:when test="$langCode = 'tgk'">Tajik</xsl:when>
			<xsl:when test="$langCode = 'tgl'">Tagalog</xsl:when>
			<xsl:when test="$langCode = 'tha'">Thai</xsl:when>
			<xsl:when test="$langCode = 'tib'">Tibetan</xsl:when>
			<xsl:when test="$langCode = 'tig'">Tigre</xsl:when>
			<xsl:when test="$langCode = 'tir'">Tigrinya</xsl:when>
			<xsl:when test="$langCode = 'tiv'">Tiv</xsl:when>
			<xsl:when test="$langCode = 'tkl'">Tokelau</xsl:when>
			<xsl:when test="$langCode = 'tlh'">Klingon; tlhIngan-Hol</xsl:when>
			<xsl:when test="$langCode = 'tli'">Tlingit</xsl:when>
			<xsl:when test="$langCode = 'tmh'">Tamashek</xsl:when>
			<xsl:when test="$langCode = 'tog'">Tonga (Nyasa)</xsl:when>
			<xsl:when test="$langCode = 'ton'">Tonga (Tonga Islands)</xsl:when>
			<xsl:when test="$langCode = 'tpi'">Tok Pisin</xsl:when>
			<xsl:when test="$langCode = 'tsi'">Tsimshian</xsl:when>
			<xsl:when test="$langCode = 'tsn'">Tswana</xsl:when>
			<xsl:when test="$langCode = 'tso'">Tsonga</xsl:when>
			<xsl:when test="$langCode = 'tuk'">Turkmen</xsl:when>
			<xsl:when test="$langCode = 'tum'">Tumbuka</xsl:when>
			<xsl:when test="$langCode = 'tup'">Tupi languages</xsl:when>
			<xsl:when test="$langCode = 'tur'">Turkish</xsl:when>
			<xsl:when test="$langCode = 'tut'">Altaic languages</xsl:when>
			<xsl:when test="$langCode = 'tvl'">Tuvalu</xsl:when>
			<xsl:when test="$langCode = 'twi'">Twi</xsl:when>
			<xsl:when test="$langCode = 'tyv'">Tuvinian</xsl:when>
			<xsl:when test="$langCode = 'udm'">Udmurt</xsl:when>
			<xsl:when test="$langCode = 'uga'">Ugaritic</xsl:when>
			<xsl:when test="$langCode = 'uig'">Uighur; Uyghur</xsl:when>
			<xsl:when test="$langCode = 'ukr'">Ukrainian</xsl:when>
			<xsl:when test="$langCode = 'umb'">Umbundu</xsl:when>
			<xsl:when test="$langCode = 'und'">Undetermined</xsl:when>
			<xsl:when test="$langCode = 'urd'">Urdu</xsl:when>
			<xsl:when test="$langCode = 'uzb'">Uzbek</xsl:when>
			<xsl:when test="$langCode = 'vai'">Vai</xsl:when>
			<xsl:when test="$langCode = 'ven'">Venda</xsl:when>
			<xsl:when test="$langCode = 'vie'">Vietnamese</xsl:when>
			<xsl:when test="$langCode = 'vol'">Volap¬∏k</xsl:when>
			<xsl:when test="$langCode = 'vot'">Votic</xsl:when>
			<xsl:when test="$langCode = 'wak'">Wakashan languages</xsl:when>
			<xsl:when test="$langCode = 'wal'">Wolaitta; Wolaytta</xsl:when>
			<xsl:when test="$langCode = 'war'">Waray</xsl:when>
			<xsl:when test="$langCode = 'was'">Washo</xsl:when>
			<xsl:when test="$langCode = 'wel'">Welsh</xsl:when>
			<xsl:when test="$langCode = 'wen'">Sorbian languages</xsl:when>
			<xsl:when test="$langCode = 'wln'">Walloon</xsl:when>
			<xsl:when test="$langCode = 'wol'">Wolof</xsl:when>
			<xsl:when test="$langCode = 'xal'">Kalmyk; Oirat</xsl:when>
			<xsl:when test="$langCode = 'xho'">Xhosa</xsl:when>
			<xsl:when test="$langCode = 'yao'">Yao</xsl:when>
			<xsl:when test="$langCode = 'yap'">Yapese</xsl:when>
			<xsl:when test="$langCode = 'yid'">Yiddish</xsl:when>
			<xsl:when test="$langCode = 'yor'">Yoruba</xsl:when>
			<xsl:when test="$langCode = 'ypk'">Yupik languages</xsl:when>
			<xsl:when test="$langCode = 'zap'">Zapotec</xsl:when>
			<xsl:when test="$langCode = 'zbl'">Blissymbols; Blissymbolics; Bliss</xsl:when>
			<xsl:when test="$langCode = 'zen'">Zenaga</xsl:when>
			<xsl:when test="$langCode = 'zha'">Zhuang; Chuang</xsl:when>
			<xsl:when test="$langCode = 'znd'">Zande languages</xsl:when>
			<xsl:when test="$langCode = 'zul'">Zulu</xsl:when>
			<xsl:when test="$langCode = 'zun'">Zuni</xsl:when>
			<xsl:when test="$langCode = 'zxx'">No linguistic content; Not applicable</xsl:when>
			<xsl:when test="$langCode = 'zza'">Zaza; Dimili; Dimli; Kirdki; Kirmanjki; Zazaki</xsl:when>
			<xsl:otherwise><xsl:value-of select="$langCode"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="ISO639-1toISO639-2b">
		<xsl:param name="langCode" />
		<xsl:choose>
			<xsl:when test="$langCode = 'aa'">aar</xsl:when>
			<xsl:when test="$langCode = 'ab'">abk</xsl:when>
			<xsl:when test="$langCode = 'ae'">ave</xsl:when>
			<xsl:when test="$langCode = 'af'">afr</xsl:when>
			<xsl:when test="$langCode = 'ak'">aka</xsl:when>
			<xsl:when test="$langCode = 'am'">amh</xsl:when>
			<xsl:when test="$langCode = 'an'">arg</xsl:when>
			<xsl:when test="$langCode = 'ar'">ara</xsl:when>
			<xsl:when test="$langCode = 'as'">asm</xsl:when>
			<xsl:when test="$langCode = 'av'">ava</xsl:when>
			<xsl:when test="$langCode = 'ay'">aym</xsl:when>
			<xsl:when test="$langCode = 'az'">aze</xsl:when>
			<xsl:when test="$langCode = 'ba'">bak</xsl:when>
			<xsl:when test="$langCode = 'be'">bel</xsl:when>
			<xsl:when test="$langCode = 'bg'">bul</xsl:when>
			<xsl:when test="$langCode = 'bh'">bih</xsl:when>
			<xsl:when test="$langCode = 'bi'">bis</xsl:when>
			<xsl:when test="$langCode = 'bm'">bam</xsl:when>
			<xsl:when test="$langCode = 'bn'">ben</xsl:when>
			<xsl:when test="$langCode = 'bo'">tib</xsl:when>
			<xsl:when test="$langCode = 'br'">bre</xsl:when>
			<xsl:when test="$langCode = 'bs'">bos</xsl:when>
			<xsl:when test="$langCode = 'ca'">cat</xsl:when>
			<xsl:when test="$langCode = 'ce'">che</xsl:when>
			<xsl:when test="$langCode = 'ch'">cha</xsl:when>
			<xsl:when test="$langCode = 'co'">cos</xsl:when>
			<xsl:when test="$langCode = 'cr'">cre</xsl:when>
			<xsl:when test="$langCode = 'cs'">cze</xsl:when>
			<xsl:when test="$langCode = 'cu'">chu</xsl:when>
			<xsl:when test="$langCode = 'cv'">chv</xsl:when>
			<xsl:when test="$langCode = 'cy'">wel</xsl:when>
			<xsl:when test="$langCode = 'da'">dan</xsl:when>
			<xsl:when test="$langCode = 'de'">ger</xsl:when>
			<xsl:when test="$langCode = 'dv'">div</xsl:when>
			<xsl:when test="$langCode = 'dz'">dzo</xsl:when>
			<xsl:when test="$langCode = 'ee'">ewe</xsl:when>
			<xsl:when test="$langCode = 'el'">gre</xsl:when>
			<xsl:when test="$langCode = 'en'">eng</xsl:when>
			<xsl:when test="$langCode = 'eo'">epo</xsl:when>
			<xsl:when test="$langCode = 'es'">spa</xsl:when>
			<xsl:when test="$langCode = 'et'">est</xsl:when>
			<xsl:when test="$langCode = 'eu'">baq</xsl:when>
			<xsl:when test="$langCode = 'fa'">per</xsl:when>
			<xsl:when test="$langCode = 'ff'">ful</xsl:when>
			<xsl:when test="$langCode = 'fi'">fin</xsl:when>
			<xsl:when test="$langCode = 'fj'">fij</xsl:when>
			<xsl:when test="$langCode = 'fo'">fao</xsl:when>
			<xsl:when test="$langCode = 'fr'">fre</xsl:when>
			<xsl:when test="$langCode = 'fy'">fry</xsl:when>
			<xsl:when test="$langCode = 'ga'">gle</xsl:when>
			<xsl:when test="$langCode = 'gd'">gla</xsl:when>
			<xsl:when test="$langCode = 'gl'">glg</xsl:when>
			<xsl:when test="$langCode = 'gn'">grn</xsl:when>
			<xsl:when test="$langCode = 'gu'">guj</xsl:when>
			<xsl:when test="$langCode = 'gv'">glv</xsl:when>
			<xsl:when test="$langCode = 'ha'">hau</xsl:when>
			<xsl:when test="$langCode = 'he'">heb</xsl:when>
			<xsl:when test="$langCode = 'hi'">hin</xsl:when>
			<xsl:when test="$langCode = 'ho'">hmo</xsl:when>
			<xsl:when test="$langCode = 'hr'">hrv</xsl:when>
			<xsl:when test="$langCode = 'ht'">hat</xsl:when>
			<xsl:when test="$langCode = 'hu'">hun</xsl:when>
			<xsl:when test="$langCode = 'hy'">arm</xsl:when>
			<xsl:when test="$langCode = 'hz'">her</xsl:when>
			<xsl:when test="$langCode = 'ia'">ina</xsl:when>
			<xsl:when test="$langCode = 'id'">ind</xsl:when>
			<xsl:when test="$langCode = 'ie'">ile</xsl:when>
			<xsl:when test="$langCode = 'ig'">ibo</xsl:when>
			<xsl:when test="$langCode = 'ii'">iii</xsl:when>
			<xsl:when test="$langCode = 'ik'">ipk</xsl:when>
			<xsl:when test="$langCode = 'io'">ido</xsl:when>
			<xsl:when test="$langCode = 'is'">ice</xsl:when>
			<xsl:when test="$langCode = 'it'">ita</xsl:when>
			<xsl:when test="$langCode = 'iu'">iku</xsl:when>
			<xsl:when test="$langCode = 'ja'">jpn</xsl:when>
			<xsl:when test="$langCode = 'jv'">jav</xsl:when>
			<xsl:when test="$langCode = 'ka'">geo</xsl:when>
			<xsl:when test="$langCode = 'kg'">kon</xsl:when>
			<xsl:when test="$langCode = 'ki'">kik</xsl:when>
			<xsl:when test="$langCode = 'kj'">kua</xsl:when>
			<xsl:when test="$langCode = 'kk'">kaz</xsl:when>
			<xsl:when test="$langCode = 'kl'">kal</xsl:when>
			<xsl:when test="$langCode = 'km'">khm</xsl:when>
			<xsl:when test="$langCode = 'kn'">kan</xsl:when>
			<xsl:when test="$langCode = 'ko'">kor</xsl:when>
			<xsl:when test="$langCode = 'kr'">kau</xsl:when>
			<xsl:when test="$langCode = 'ks'">kas</xsl:when>
			<xsl:when test="$langCode = 'ku'">kur</xsl:when>
			<xsl:when test="$langCode = 'kv'">kom</xsl:when>
			<xsl:when test="$langCode = 'kw'">cor</xsl:when>
			<xsl:when test="$langCode = 'ky'">kir</xsl:when>
			<xsl:when test="$langCode = 'la'">lat</xsl:when>
			<xsl:when test="$langCode = 'lb'">ltz</xsl:when>
			<xsl:when test="$langCode = 'lg'">lug</xsl:when>
			<xsl:when test="$langCode = 'li'">lim</xsl:when>
			<xsl:when test="$langCode = 'ln'">lin</xsl:when>
			<xsl:when test="$langCode = 'lo'">lao</xsl:when>
			<xsl:when test="$langCode = 'lt'">lit</xsl:when>
			<xsl:when test="$langCode = 'lu'">lub</xsl:when>
			<xsl:when test="$langCode = 'lv'">lav</xsl:when>
			<xsl:when test="$langCode = 'mg'">mlg</xsl:when>
			<xsl:when test="$langCode = 'mh'">mah</xsl:when>
			<xsl:when test="$langCode = 'mi'">mao</xsl:when>
			<xsl:when test="$langCode = 'mk'">mac</xsl:when>
			<xsl:when test="$langCode = 'ml'">mal</xsl:when>
			<xsl:when test="$langCode = 'mn'">mon</xsl:when>
			<xsl:when test="$langCode = 'mr'">mar</xsl:when>
			<xsl:when test="$langCode = 'ms'">may</xsl:when>
			<xsl:when test="$langCode = 'mt'">mlt</xsl:when>
			<xsl:when test="$langCode = 'my'">bur</xsl:when>
			<xsl:when test="$langCode = 'na'">nau</xsl:when>
			<xsl:when test="$langCode = 'nb'">nob</xsl:when>
			<xsl:when test="$langCode = 'nd'">nde</xsl:when>
			<xsl:when test="$langCode = 'ne'">nep</xsl:when>
			<xsl:when test="$langCode = 'ng'">ndo</xsl:when>
			<xsl:when test="$langCode = 'nl'">dut</xsl:when>
			<xsl:when test="$langCode = 'nn'">nno</xsl:when>
			<xsl:when test="$langCode = 'no'">nor</xsl:when>
			<xsl:when test="$langCode = 'nr'">nbl</xsl:when>
			<xsl:when test="$langCode = 'nv'">nav</xsl:when>
			<xsl:when test="$langCode = 'ny'">nya</xsl:when>
			<xsl:when test="$langCode = 'oc'">oci</xsl:when>
			<xsl:when test="$langCode = 'oj'">oji</xsl:when>
			<xsl:when test="$langCode = 'om'">orm</xsl:when>
			<xsl:when test="$langCode = 'or'">ori</xsl:when>
			<xsl:when test="$langCode = 'os'">oss</xsl:when>
			<xsl:when test="$langCode = 'pa'">pan</xsl:when>
			<xsl:when test="$langCode = 'pi'">pli</xsl:when>
			<xsl:when test="$langCode = 'pl'">pol</xsl:when>
			<xsl:when test="$langCode = 'ps'">pus</xsl:when>
			<xsl:when test="$langCode = 'pt'">por</xsl:when>
			<xsl:when test="$langCode = 'qu'">que</xsl:when>
			<xsl:when test="$langCode = 'rm'">roh</xsl:when>
			<xsl:when test="$langCode = 'rn'">run</xsl:when>
			<xsl:when test="$langCode = 'ro'">ron</xsl:when>
			<xsl:when test="$langCode = 'ru'">rus</xsl:when>
			<xsl:when test="$langCode = 'rw'">kin</xsl:when>
			<xsl:when test="$langCode = 'sa'">san</xsl:when>
			<xsl:when test="$langCode = 'sc'">srd</xsl:when>
			<xsl:when test="$langCode = 'sd'">snd</xsl:when>
			<xsl:when test="$langCode = 'se'">sme</xsl:when>
			<xsl:when test="$langCode = 'sg'">sag</xsl:when>
			<xsl:when test="$langCode = 'si'">sin</xsl:when>
			<xsl:when test="$langCode = 'sk'">slk</xsl:when>
			<xsl:when test="$langCode = 'sl'">slv</xsl:when>
			<xsl:when test="$langCode = 'sm'">smo</xsl:when>
			<xsl:when test="$langCode = 'sn'">sna</xsl:when>
			<xsl:when test="$langCode = 'so'">som</xsl:when>
			<xsl:when test="$langCode = 'sq'">alb</xsl:when>
			<xsl:when test="$langCode = 'sr'">srp</xsl:when>
			<xsl:when test="$langCode = 'ss'">ssw</xsl:when>
			<xsl:when test="$langCode = 'st'">sot</xsl:when>
			<xsl:when test="$langCode = 'su'">sun</xsl:when>
			<xsl:when test="$langCode = 'sv'">swe</xsl:when>
			<xsl:when test="$langCode = 'sw'">swa</xsl:when>
			<xsl:when test="$langCode = 'ta'">tam</xsl:when>
			<xsl:when test="$langCode = 'te'">tel</xsl:when>
			<xsl:when test="$langCode = 'tg'">tgk</xsl:when>
			<xsl:when test="$langCode = 'th'">tha</xsl:when>
			<xsl:when test="$langCode = 'ti'">tir</xsl:when>
			<xsl:when test="$langCode = 'tk'">tuk</xsl:when>
			<xsl:when test="$langCode = 'tl'">tgl</xsl:when>
			<xsl:when test="$langCode = 'tn'">tsn</xsl:when>
			<xsl:when test="$langCode = 'to'">ton</xsl:when>
			<xsl:when test="$langCode = 'tr'">tur</xsl:when>
			<xsl:when test="$langCode = 'ts'">tso</xsl:when>
			<xsl:when test="$langCode = 'tt'">tat</xsl:when>
			<xsl:when test="$langCode = 'tw'">twi</xsl:when>
			<xsl:when test="$langCode = 'ty'">tah</xsl:when>
			<xsl:when test="$langCode = 'ug'">uig</xsl:when>
			<xsl:when test="$langCode = 'uk'">ukr</xsl:when>
			<xsl:when test="$langCode = 'ur'">urd</xsl:when>
			<xsl:when test="$langCode = 'uz'">uzb</xsl:when>
			<xsl:when test="$langCode = 've'">ven</xsl:when>
			<xsl:when test="$langCode = 'vi'">vie</xsl:when>
			<xsl:when test="$langCode = 'vo'">vol</xsl:when>
			<xsl:when test="$langCode = 'wa'">wln</xsl:when>
			<xsl:when test="$langCode = 'wo'">wol</xsl:when>
			<xsl:when test="$langCode = 'xh'">xho</xsl:when>
			<xsl:when test="$langCode = 'yi'">yid</xsl:when>
			<xsl:when test="$langCode = 'yo'">yor</xsl:when>
			<xsl:when test="$langCode = 'za'">zha</xsl:when>
			<xsl:when test="$langCode = 'zh'">chi</xsl:when>
			<xsl:when test="$langCode = 'zu'">zul</xsl:when>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>