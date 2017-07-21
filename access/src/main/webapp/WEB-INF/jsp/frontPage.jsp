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

<div id="peek">
	<div id="front-headline">
		<h1>Preserve, share, and promote your scholarly&nbsp;and&nbsp;creative&nbsp;work.</h1>
	</div>
	<div id="peek-enter">
		<div class="button">
			<a href="#p">A peek inside the repository</a>
		</div>
	</div>
	<div id="peek-exit">
		<div class="button">
			<a href="#">Back to the homepage</a>
		</div>
	</div>
</div>

<script src="/static/js/lib/jquery.min.js"></script>
<script src="/static/js/lib/underscore.js"></script>
<script src="/static/front/peek.js"></script>

<div id="deposit-banner">
	<div id="deposit-banner-title">
	 <h2>Share Your Work</h2>
	</div>
	<nav class="how-to-deposit">
		<ul class="deposit-options-list">
				<li class="deposit-option">
				<a href="/forms/open-access-carolina">
					<div class="deposit-icons"><i class="fa fa-file-text-o"></i></div>
					<h2>Open Access Articles</h2>
				</a>
			</li>
			<li class="deposit-option">
				<a href="/studentPapers">
					<div class="deposit-icons"><i class="fa fa-graduation-cap"></i></div>
					<h2>Student<br>Papers</h2>
			</a>
			</li>
			<li class="deposit-option">
				<a href="/forms/posters">
				<div class="deposit-icons"><i class="fa fa-line-chart"></i></div>
				<h2>Posters &amp; Presentations</h2>
				</a>
			</li>
			<li class="deposit-option">
				<a href="/forms/dataset">
				<div class="deposit-icons"><i class="fa fa-table"></i></div>
				<h2>Open Access Datasets</h2>
				</a>
			</li>
			</ul>
	</nav>
	</div>
	
<div id="front">

	<div class="row">
	<div id="front-points">
		<h2>Long-term Preservation</h2>
			<p>We provide long-term access and safekeeping for scholarly works, datasets, research materials, records, and audiovisual materials produced by the UNC-Chapel Hill community.</p>
			<h2>Accessible &amp; Searchable</h2>
			<p>We ensure your work is accessible and searchable on our website and indexed in search engines.</p>
			<h2>You Decide Who Gets Access</h2>
			<p>We offer a range of access controls including embargoes and granting access to specific groups on campus.</p>
			<h2>Support for Larger Collections</h2>
			<p>We offer deposit and preservation support for larger UNC digital collections. <a href="http://blogs.lib.unc.edu/cdr/index.php/contact-us/">Contact us</a> to deposit your collection.</p>
				</div>
		
		
	</div>

	<hr>

	<div class="row">
		<div class="half">
			<h2>What’s in the repository?</h2>

			<p>
				<a href="search?types=Collection"><c:out value="${collectionsCount}"/> collections</a>, including
				<a href="search?format=text"><c:out value="${formatCounts.text}"/> texts</a>,
				<a href="search?format=image"><c:out value="${formatCounts.image}"/> images</a>,
				<a href="search?format=dataset"><c:out value="${formatCounts.dataset}"/> datasets</a>,
				<a href="search?format=audio"><c:out value="${formatCounts.audio}"/> audio files</a>, and
				<a href="search?format=video"><c:out value="${formatCounts.video}"/> videos</a>.
			</p>
			
			<p>
				Material from <a href="browse/dept/"><c:out value="${departmentsCount}"/> UNC-Chapel Hill departments</a>,
				from <a href="search?dept=department+of+anthropology">Anthropology</a>
				to <a href="search?dept=curriculum+in+toxicology">Toxicology</a>.
			</p>
		</div>

		<div class="half" id="front-collections">
			<h2>A Few of Our Collections</h2>

			<table>
				<tr>
				<td><a href="record/uuid:bded4944-f033-4015-af0f-3d39595f4d30"><img src="/static/front/aapaa.jpg"></a></td>
				<td class="title"><a href="record/uuid:bded4944-f033-4015-af0f-3d39595f4d30">African American Performance Art Archive</a></td>
		
				<td><a href="record/uuid:1add9fbc-f5c4-49a8-848e-96a52e3ade9c"><img src="/static/front/azoria.jpg"></a></td>
				<td class="title"><a href="record/uuid:1add9fbc-f5c4-49a8-848e-96a52e3ade9c">Azoria Project Archive</a></td>
				</tr>
				<tr>
				<td><a href="record/uuid:dfebbdf7-3361-4097-9fa4-7001ab6fcc11"><img src="/static/front/bmc.png"></a></td>
				<td class="title"><a href="record/uuid:dfebbdf7-3361-4097-9fa4-7001ab6fcc11">BioMed Central</a></td>
		
				<td><a href="record/uuid:cbbc2cc1-c538-4e28-b567-55db61b7942e"><img src="/static/front/cpj.png"></a></td>
				<td class="title"><a href="record/uuid:cbbc2cc1-c538-4e28-b567-55db61b7942e">Carolina Planning Journal</a></td>
				</tr>
				<tr>
				<td><a href="record/uuid:a012aad2-1ab1-43b2-b5ab-0e14740e5e07"><img src="/static/front/etds.png"></a></td>
				<td class="title"><a href="record/uuid:a012aad2-1ab1-43b2-b5ab-0e14740e5e07">Electronic Theses and Dissertations</a></td>
		
				<td><a href="record/uuid:9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d9"><img src="/static/front/popl.png"></a></td>
				<td class="title"><a href="record/uuid:9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d9">Program on Public Life</a></td>
				</tr>
				<tr>
				<td><a href="record/uuid:8ae56bbc-400e-496d-af4b-3c585e20dba1"><img src="/static/front/rla.png"></a></td>
				<td class="title"><a href="record/uuid:8ae56bbc-400e-496d-af4b-3c585e20dba1">Research Laboratories of Archaeology</a></td>
		
				<td><a href="record/uuid:5e4b2719-bb71-45ec-be63-5d018b6f5aab"><img src="/static/front/sfc.png"></a></td>
				<td class="title"><a href="record/uuid:5e4b2719-bb71-45ec-be63-5d018b6f5aab">Southern Folklife Collection Digital Files</a></td>
				</tr>
			</table>
		</div>
	</div>

	<hr>

	<div id="front-case-study" class="row">		
		<div class="case-study">
			<h2><i>Case Study:</i> ${ wpRssItem.title }</h2>
			${ wpRssItem.encoded }
		</div>
	</div>

</div>
