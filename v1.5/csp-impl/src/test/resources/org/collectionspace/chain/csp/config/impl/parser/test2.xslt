<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="no"/>

<!-- identity template -->
<xsl:template match="@*|node()">
    <xsl:copy>
        <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
</xsl:template>

<xsl:template match="tag2/tag3">
	<tag5>
		<xslt src="test3.xslt">
        	<xsl:apply-templates select="@*|node()"/>
        </xslt>
	</tag5>
</xsl:template>

</xsl:stylesheet>
