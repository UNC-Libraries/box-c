<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
		xmlns:cdr-fn="http://cdr.lib.unc.edu/" 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output  method="xml" omit-xml-declaration="yes" indent="no"/>
	
	<xsl:variable name="imageType">image,Image</xsl:variable>
	<xsl:variable name="collectionType">collection,Collection</xsl:variable>
	<xsl:variable name="datasetType">dataset,Dataset</xsl:variable>
	<xsl:variable name="interactiveType">interactive,Interactive Resource</xsl:variable>
	<xsl:variable name="videoType">video,Video</xsl:variable>
	<xsl:variable name="softwareType">software,Software</xsl:variable>
	<xsl:variable name="soundType">audio,Audio</xsl:variable>
	<xsl:variable name="archiveType">archive,Archive File</xsl:variable>
	<xsl:variable name="textType">text,Text</xsl:variable>
	<xsl:variable name="unknownType">unknown,Unknown</xsl:variable>
	
	<!-- 
	* Collection
    * Dataset
    * Image
    * Interactive resource
    * Video
    * Software
    * Sound
    * Archive file (.zip, .tar, .gz, etc)
    * Text  -->
	<xsl:function name="cdr-fn:getContentType">
		<xsl:param name="mimeType" />
		<xsl:param name="fileExtension" />
		<xsl:choose>
			<!-- Image types -->
			<xsl:when test="$mimeType = 'application/jpg' or $fileExtension = 'jpg'"><xsl:value-of select="$imageType"/></xsl:when>
			<xsl:when test="substring-before($mimeType,'/') = 'image'"><xsl:value-of select="$imageType"/></xsl:when>
			<xsl:when test="$fileExtension = 'psd' or $fileExtension = 'psf'"><xsl:value-of select="$imageType"/></xsl:when>
			<!-- Video types -->
			<xsl:when test="substring-before($mimeType,'/') = 'video'"><xsl:value-of select="$videoType"/></xsl:when>
			<xsl:when test="$fileExtension = 'mp4'"><xsl:value-of select="$videoType"/></xsl:when>
			<!-- Software types -->
			<xsl:when test="$fileExtension = 'php'"><xsl:value-of select="$softwareType"/></xsl:when>
			<xsl:when test="$fileExtension = 'js'"><xsl:value-of select="$softwareType"/></xsl:when>
			<xsl:when test="$fileExtension = 'css'"><xsl:value-of select="$softwareType"/></xsl:when>
			<!-- Audio types -->
			<xsl:when test="substring-before($mimeType,'/') = 'audio'"><xsl:value-of select="$soundType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/ogg'"><xsl:value-of select="$soundType"/></xsl:when>
			<xsl:when test="$fileExtension = 'mp3'"><xsl:value-of select="$soundType"/></xsl:when>
			<xsl:when test="$fileExtension = 'wav'"><xsl:value-of select="$soundType"/></xsl:when>
			<xsl:when test="$fileExtension = 'rm'"><xsl:value-of select="$soundType"/></xsl:when>
			<!-- Text types -->
			<xsl:when test="$fileExtension = 'txt'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$mimeType = 'text/plain'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/msword'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/rtf'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/pdf' or $fileExtension = 'pdf'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/postscript'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$mimeType = 'text/richtext'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/powerpoint'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.ms-powerpoint'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$fileExtension = 'doc' or $fileExtension = 'docx' or $fileExtension = 'dotx'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$fileExtension = 'ppt' or $fileExtension = 'pptx'"><xsl:value-of select="$textType"/></xsl:when>
			<xsl:when test="$fileExtension = 'wpd'"><xsl:value-of select="$textType"/></xsl:when>
			<!-- Archive types -->
			<xsl:when test="$mimeType = 'application/x-gtar'"><xsl:value-of select="$archiveType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/x-gzip'"><xsl:value-of select="$archiveType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/x-compress'"><xsl:value-of select="$archiveType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/x-compressed'"><xsl:value-of select="$archiveType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/zip'"><xsl:value-of select="$archiveType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/x-stuffit'"><xsl:value-of select="$archiveType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/x-tar'"><xsl:value-of select="$archiveType"/></xsl:when>
			<!-- Interactive Resource Types -->
			<xsl:when test="$fileExtension = 'swf'"><xsl:value-of select="$interactiveType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/x-shockwave-flash'"><xsl:value-of select="$interactiveType"/></xsl:when>
			<xsl:when test="$mimeType = 'application/x-silverlight-app'"><xsl:value-of select="$interactiveType"/></xsl:when>			
			<xsl:when test="$mimeType = 'text/html'"><xsl:value-of select="$interactiveType"/></xsl:when>
			<xsl:when test="$fileExtension = 'html' or $fileExtension = 'htm'"><xsl:value-of select="$interactiveType"/></xsl:when>
			<!-- Dataset types -->
			<xsl:when test="$mimeType = 'application/vnd.ms-excel'"><xsl:value-of select="$datasetType"/></xsl:when>
			<xsl:when test="$fileExtension = 'xls' or $fileExtension = 'xlsx'"><xsl:value-of select="$datasetType"/></xsl:when>
			<xsl:when test="$mimeType = 'text/xml'"><xsl:value-of select="$datasetType"/></xsl:when>
			<xsl:when test="$mimeType = 'text/tab-separated-values'"><xsl:value-of select="$datasetType"/></xsl:when>
			<xsl:when test="$fileExtension = 'log'"><xsl:value-of select="$datasetType"/></xsl:when>
			<xsl:when test="$fileExtension = 'db'"><xsl:value-of select="$datasetType"/></xsl:when>
			<xsl:when test="$fileExtension = 'accdb'"><xsl:value-of select="$datasetType"/></xsl:when>
			<!-- Unknown -->
			<xsl:when test="$mimeType = 'application/octet-stream'"><xsl:value-of select="$unknownType"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="$unknownType"/></xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	
	<xsl:function name="cdr-fn:substringAfterLast">
		<xsl:param name="text"/>
		<xsl:param name="delimiter"/>
		<xsl:variable name="textAfter" select="substring-after($text, $delimiter)"/>
		<xsl:choose>
			<xsl:when test="contains($textAfter, $delimiter)">
				<xsl:value-of select="cdr-fn:substringAfterLast($textAfter, $delimiter)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$textAfter"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	
	<xsl:function name="cdr-fn:getFileExtension">
		<xsl:param name="mimeType" />
		<xsl:param name="fileName" />
		
		<xsl:variable name="fileExtension" select="lower-case(cdr-fn:substringAfterLast($fileName, '.'))"/>
		
		<xsl:choose>
			<xsl:when test="boolean($fileExtension) and $fileExtension != $fileName and string-length($fileExtension) &lt; 10"><xsl:value-of select="$fileExtension"/></xsl:when>
			<!-- Text types -->
			<xsl:when test="$mimeType = 'text/richtext'">rtf</xsl:when>
			<xsl:when test="$mimeType = 'application/powerpoint'">ppt</xsl:when>
			
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'">docx</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.template'">dotx</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.presentationml.presentation'">pptx</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.presentationml.slideshow'">ppsx</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.presentationml.template'">potx</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'">xlsx</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.template'">xltx</xsl:when>
			<!-- Interactive Resource Types -->
			<xsl:when test="$mimeType = 'application/x-silverlight-app'">xap</xsl:when>
			<!-- Unknown -->
			<xsl:when test="$mimeType = 'application/octet-stream'"></xsl:when>
			<xsl:when test="$mimeType = 'application/envoy'">evy</xsl:when>
			<xsl:when test="$mimeType = 'application/fractals'">fif</xsl:when>
			<xsl:when test="$mimeType = 'application/futuresplash'">spl</xsl:when>
			<xsl:when test="$mimeType = 'application/hta'">hta</xsl:when>
			<xsl:when test="$mimeType = 'application/internet-property-stream'">acx</xsl:when>
			<xsl:when test="$mimeType = 'application/mac-binhex40'">hqx</xsl:when>
			<xsl:when test="$mimeType = 'application/msword'">doc</xsl:when>
			<xsl:when test="$mimeType = 'application/octet-stream'"></xsl:when>
			<xsl:when test="$mimeType = 'application/oda'">oda</xsl:when>
			<xsl:when test="$mimeType = 'application/olescript'">axs</xsl:when>
			<xsl:when test="$mimeType = 'application/pdf'">pdf</xsl:when>
			<xsl:when test="$mimeType = 'application/pics-rules'">prf</xsl:when>
			<xsl:when test="$mimeType = 'application/pkcs10'">p10</xsl:when>
			<xsl:when test="$mimeType = 'application/pkix-crl'">crl</xsl:when>
			<xsl:when test="$mimeType = 'application/postscript'">ps</xsl:when>
			<xsl:when test="$mimeType = 'application/rtf'">rtf</xsl:when>
			<xsl:when test="$mimeType = 'application/set-payment-initiation'">setpay</xsl:when>
			<xsl:when test="$mimeType = 'application/set-registration-initiation'">setreg</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.ms-excel'">xls</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.ms-outlook'">msg</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.ms-pkicertstore'">sst</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.ms-pkiseccat'">cat</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.ms-pkistl'">stl</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.ms-powerpoint'">ppt</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.ms-project'">mpp</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.ms-works'">wcm</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.presentationml.presentation'">pptx</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'">xlsx</xsl:when>
			<xsl:when test="$mimeType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'">docx</xsl:when>
			<xsl:when test="$mimeType = 'application/winhlp'">hlp</xsl:when>
			<xsl:when test="$mimeType = 'application/x-bcpio'">bcpio</xsl:when>
			<xsl:when test="$mimeType = 'application/x-cdf'">cdf</xsl:when>
			<xsl:when test="$mimeType = 'application/x-compress'">z</xsl:when>
			<xsl:when test="$mimeType = 'application/x-compressed'">tgz</xsl:when>
			<xsl:when test="$mimeType = 'application/x-cpio'">cpio</xsl:when>
			<xsl:when test="$mimeType = 'application/x-csh'">csh</xsl:when>
			<xsl:when test="$mimeType = 'application/x-director'">dcr</xsl:when>
			<xsl:when test="$mimeType = 'application/x-dvi'">dvi</xsl:when>
			<xsl:when test="$mimeType = 'application/x-gtar'">gtar</xsl:when>
			<xsl:when test="$mimeType = 'application/x-gzip'">gz</xsl:when>
			<xsl:when test="$mimeType = 'application/x-hdf'">hdf</xsl:when>
			<xsl:when test="$mimeType = 'application/x-internet-signup'">ins</xsl:when>
			<xsl:when test="$mimeType = 'application/x-iphone'">iii</xsl:when>
			<xsl:when test="$mimeType = 'application/x-javascript'">js</xsl:when>
			<xsl:when test="$mimeType = 'application/x-latex'">latex</xsl:when>
			<xsl:when test="$mimeType = 'application/x-msaccess'">mdb</xsl:when>
			<xsl:when test="$mimeType = 'application/x-mscardfile'">crd</xsl:when>
			<xsl:when test="$mimeType = 'application/x-msclip'">clp</xsl:when>
			<xsl:when test="$mimeType = 'application/x-msdownload'">dll</xsl:when>
			<xsl:when test="$mimeType = 'application/x-msmediaview'">mvb</xsl:when>
			<xsl:when test="$mimeType = 'application/x-msmetafile'">wmf</xsl:when>
			<xsl:when test="$mimeType = 'application/x-msmoney'">mny</xsl:when>
			<xsl:when test="$mimeType = 'application/x-mspublisher'">pub</xsl:when>
			<xsl:when test="$mimeType = 'application/x-msschedule'">scd</xsl:when>
			<xsl:when test="$mimeType = 'application/x-msterminal'">trm</xsl:when>
			<xsl:when test="$mimeType = 'application/x-mswrite'">wri</xsl:when>
			<xsl:when test="$mimeType = 'application/x-netcdf'">cdf</xsl:when>
			<xsl:when test="$mimeType = 'application/x-perfmon'">pma</xsl:when>
			<xsl:when test="$mimeType = 'application/x-pkcs12'">pfx</xsl:when>
			<xsl:when test="$mimeType = 'application/x-pkcs7-certificates'">p7b</xsl:when>
			<xsl:when test="$mimeType = 'application/x-pkcs7-certificates'">spc</xsl:when>
			<xsl:when test="$mimeType = 'application/x-pkcs7-certreqresp'">p7r</xsl:when>
			<xsl:when test="$mimeType = 'application/x-pkcs7-mime'">p7m</xsl:when>
			<xsl:when test="$mimeType = 'application/x-pkcs7-signature'">p7s</xsl:when>
			<xsl:when test="$mimeType = 'application/x-sh'">sh</xsl:when>
			<xsl:when test="$mimeType = 'application/x-shar'">shar</xsl:when>
			<xsl:when test="$mimeType = 'application/x-shockwave-flash'">swf</xsl:when>
			<xsl:when test="$mimeType = 'application/x-stuffit'">sit</xsl:when>
			<xsl:when test="$mimeType = 'application/x-sv4cpio'">sv4cpio</xsl:when>
			<xsl:when test="$mimeType = 'application/x-sv4crc'">sv4crc</xsl:when>
			<xsl:when test="$mimeType = 'application/x-tar'">tar</xsl:when>
			<xsl:when test="$mimeType = 'application/x-tcl'">tcl</xsl:when>
			<xsl:when test="$mimeType = 'application/x-tex'">tex</xsl:when>
			<xsl:when test="$mimeType = 'application/x-texinfo'">texinfo</xsl:when>
			<xsl:when test="$mimeType = 'application/x-troff'">roff</xsl:when>
			<xsl:when test="$mimeType = 'application/x-troff-man'">man</xsl:when>
			<xsl:when test="$mimeType = 'application/x-troff-me'">me</xsl:when>
			<xsl:when test="$mimeType = 'application/x-troff-ms'">ms</xsl:when>
			<xsl:when test="$mimeType = 'application/x-ustar'">ustar</xsl:when>
			<xsl:when test="$mimeType = 'application/x-wais-source'">src</xsl:when>
			<xsl:when test="$mimeType = 'application/x-x509-ca-cert'">crt</xsl:when>
			<xsl:when test="$mimeType = 'application/ynd.ms-pkipko'">pko</xsl:when>
			<xsl:when test="$mimeType = 'application/zip'">zip</xsl:when>
			<xsl:when test="$mimeType = 'audio/basic'">au</xsl:when>
			<xsl:when test="$mimeType = 'audio/basic'">snd</xsl:when>
			<xsl:when test="$mimeType = 'audio/mid'">mid</xsl:when>
			<xsl:when test="$mimeType = 'audio/mid'">rmi</xsl:when>
			<xsl:when test="$mimeType = 'audio/mpeg'">mp3</xsl:when>
			<xsl:when test="$mimeType = 'audio/x-aiff'">aif</xsl:when>
			<xsl:when test="$mimeType = 'audio/x-mpegurl'">m</xsl:when>
			<xsl:when test="$mimeType = 'audio/x-pn-realaudio'">ram</xsl:when>
			<xsl:when test="$mimeType = 'audio/x-wav'">wav</xsl:when>
			<xsl:when test="$mimeType = 'image/bmp'">bmp</xsl:when>
			<xsl:when test="$mimeType = 'image/cis-cod'">cod</xsl:when>
			<xsl:when test="$mimeType = 'image/gif'">gif</xsl:when>
			<xsl:when test="$mimeType = 'image/ief'">ief</xsl:when>
			<xsl:when test="$mimeType = 'image/jpeg'">jpg</xsl:when>
			<xsl:when test="$mimeType = 'image/pipeg'">jfif</xsl:when>
			<xsl:when test="$mimeType = 'image/png'">png</xsl:when>
			<xsl:when test="$mimeType = 'image/svg+xml'">svg</xsl:when>
			<xsl:when test="$mimeType = 'image/tiff'">tif</xsl:when>
			<xsl:when test="$mimeType = 'image/x-cmu-raster'">ras</xsl:when>
			<xsl:when test="$mimeType = 'image/x-cmx'">cmx</xsl:when>
			<xsl:when test="$mimeType = 'image/x-icon'">ico</xsl:when>
			<xsl:when test="$mimeType = 'image/x-portable-anymap'">pnm</xsl:when>
			<xsl:when test="$mimeType = 'image/x-portable-bitmap'">pbm</xsl:when>
			<xsl:when test="$mimeType = 'image/x-portable-graymap'">pgm</xsl:when>
			<xsl:when test="$mimeType = 'image/x-portable-pixmap'">ppm</xsl:when>
			<xsl:when test="$mimeType = 'image/x-rgb'">rgb</xsl:when>
			<xsl:when test="$mimeType = 'image/x-xbitmap'">xbm</xsl:when>
			<xsl:when test="$mimeType = 'image/x-xpixmap'">xpm</xsl:when>
			<xsl:when test="$mimeType = 'image/x-xwindowdump'">xwd</xsl:when>
			<xsl:when test="$mimeType = 'message/rfc822'">mhtml</xsl:when>
			<xsl:when test="$mimeType = 'text/css'">css</xsl:when>
			<xsl:when test="$mimeType = 'text/html'">html</xsl:when>
			<xsl:when test="$mimeType = 'text/iuls'">uls</xsl:when>
			<xsl:when test="$mimeType = 'text/plain'">txt</xsl:when>
			<xsl:when test="$mimeType = 'text/richtext'">rtx</xsl:when>
			<xsl:when test="$mimeType = 'text/scriptlet'">sct</xsl:when>
			<xsl:when test="$mimeType = 'text/tab-separated-values'">tsv</xsl:when>
			<xsl:when test="$mimeType = 'text/webviewhtml'">htt</xsl:when>
			<xsl:when test="$mimeType = 'text/x-component'">htc</xsl:when>
			<xsl:when test="$mimeType = 'text/x-setext'">etx</xsl:when>
			<xsl:when test="$mimeType = 'text/x-vcard'">vcf</xsl:when>
			<xsl:when test="$mimeType = 'video/mpeg'">mpeg</xsl:when>
			<xsl:when test="$mimeType = 'video/quicktime'">mov</xsl:when>
			<xsl:when test="$mimeType = 'video/x-la-asf'">lsf</xsl:when>
			<xsl:when test="$mimeType = 'video/x-la-asf'">lsx</xsl:when>
			<xsl:when test="$mimeType = 'video/x-ms-asf'">asf</xsl:when>
			<xsl:when test="$mimeType = 'video/x-msvideo'">avi</xsl:when>
			<xsl:when test="$mimeType = 'video/x-sgi-movie'">movie</xsl:when>
			<xsl:when test="$mimeType = 'x-world/x-vrml'">vrml</xsl:when>

			<xsl:otherwise><xsl:value-of select="lower-case(cdr-fn:substringAfterLast($mimeType, '/'))"/></xsl:otherwise>
		</xsl:choose>
	</xsl:function>
</xsl:stylesheet>