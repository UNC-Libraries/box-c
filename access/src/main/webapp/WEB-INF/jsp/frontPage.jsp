<%--

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

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>
<jsp:useBean id="externalContent" class="edu.unc.lib.dl.ui.util.ExternalContentSettings" scope="page"/>

<div class="content-wrap">
<div class="contentarea">
	<h2>About</h2>
	<p>
		The Carolina Digital Repository (CDR) safeguards and provides access to the scholarly work and research files produced or 
		collected by faculty, students and staff at the University of North Carolina at Chapel Hill. 
		<a href="external?page=about.about">Read More</a>
	</p>
	<p>
		<a href="search?types=Collection"><c:out value="${collectionsCount}"/> collections</a>, including
			<a href="search?facets=format%3A%5Etext"><c:out value="${formatCounts.text}"/> texts</a>,
			<a href="search?facets=format%3A%5Eimage"><c:out value="${formatCounts.image}"/> images</a>,
			<a href="search?facets=format%3A%5Edataset"><c:out value="${formatCounts.dataset}"/> datasets</a>,
			<a href="search?facets=format%3A%5Eaudio"><c:out value="${formatCounts.audio}"/> audio files</a>, and
			<a href="search?facets=format%3A%5Evideo"><c:out value="${formatCounts.video}"/> videos</a>.
	</p>
	<p>
		Material from <a href="browseDepartments"><c:out value="${departmentsCount}"/> UNC Chapel Hill departments</a>,
		from <a href="search?action=setFacet%3adept%2c%22anthropology%22">Anthropology</a>
		to <a href="search?action=setFacet%3adept%2c%22toxicology%22">Toxicology</a>.
	</p>
</div>
<div class="lightest">
<div class="threecol lightest shadowtop">
	<c:import url="common/searchBox.jsp">
		<c:param name="title">Search the Carolina Digital Repository</c:param>
		<c:param name="showSearchWithin">false</c:param>
		<c:param name="showBrowse">true</c:param>
	</c:import>
</div>
<div class="fourcol light shadowtop">
	<div class="contentarea">
		<h2>News</h2>
		<ul class="fpfeed_list">
			<c:forEach items="${newsRssFeed.items}" var="newsItem">
				<li><a href="${newsItem.link}"><c:out value="${cdr:truncateText(newsItem.title, 65)}"/></a></li>
			</c:forEach>
		</ul>
		<p class="smaller"><a href="http://www.lib.unc.edu/blogs/cdr/">Read More</a></p>
	</div>
</div>
</div>
<div id="fpnewly_added" class="fourcol">
	<div class="contentarea">
		<h2>Newly Added</h2>
		<ul class="fpfeed_list">
			<c:forEach items="${newlyAddedList}" var="entry">
				<c:url var="entryUrl" value="record">
					<c:param name="${searchSettings.searchStateParams['ID']}" value="${entry.id}" />
				</c:url>
				<li><a href="<c:out value='${entryUrl}'/>"><c:out value="${cdr:truncateText(entry.title, 65)}"/></a></li>
			</c:forEach>
		</ul>
	</div>
</div>
<div class="gray">
	<script type="text/javascript" src="/static/js/featuredContent.js"></script>
	<script type="text/javascript">
		$(document).ready(function(){	
			$("#slideshow").easySlider({
				prevId: 'prev_button',
				prevText: "<div></div><img src='/static/images/left_slideshow_arrow.png'/>",
				nextId: 'next_button',
				nextText: "<div></div><img src='/static/images/right_slideshow_arrow.png'/>",
				controlsShow: true,
				fade: true,
				speed: 500
			});
		});	
	</script>
	<div id="slideshow_container">
		<div id="slideshow">
			<ul id="slideshow_ul">
				<c:forEach items="${featuredContentFeed.items}" var="entry">
					<li>
						<div class="twocol">
							<div class="slide_image_panel">
								<a href="<c:out value='${entry.link}'/>">
									<img src="${entry.enclosure.url}"/>
								</a>
							</div>
						</div>
						<div class="fourcol gray">
							<div class="contentarea">
								<h2><a href="<c:out value='${entry.link}'/>"><c:out value="${entry.title}"/></a></h2>
								<p><c:out value="${entry.description}"/></p>
							</div>
						</div>
					</li>
				</c:forEach>
			</ul>
		</div>
	</div>
</div>
</div>
