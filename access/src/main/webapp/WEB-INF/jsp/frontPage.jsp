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

<script src="/static/js/underscore.js"></script>
<script src="/static/peek/javascripts/peek.js"></script>
<script src="/static/peek/javascripts/column.js"></script>
<script src="/static/front/peek.js"></script>

<div id="front">
	
	<div id="front-headline">
		<h1>Preserve, share, and promote your scholarly&nbsp;and&nbsp;creative&nbsp;work.</h1>
	</div>

	<div class="row">
		<div class="half" id="front-points">
			<table>
				<tr>
					<td class="illustration"><img src="/static/front/tube.png"></td>
					<td>We provide long-term access and safekeeping for scholarly works, datasets, research materials, records, and audiovisual materials produced by the UNC-Chapel Hill community.</td>
				</tr>
				<tr>
					<td class="illustration"><img src="/static/front/frame.png"></td>
					<td>We ensure your work is accessible and searchable on our website and indexed in search engines.</td>
				</tr>
				<tr>
					<td class="illustration"><img src="/static/front/lock.png"></td>
					<td>You decide who gets access: we offer a range of access controls including embargoes and granting access to specific groups on campus.</td>
				</tr>
			</table>
		</div>
		
		<div class="half" id="front-how">
			<h2>How to start preserving your work</h2>
		
			<p>To get started, just tell us about the work you’d like to preserve. We’ll meet with you to discuss how to transfer it, describe it, and make it available on the web. After you deposit your work, we take care of the rest, ensuring it’s kept safe and accessible for the future.</p>
		
			<p class="button"><a href="external?refer=http%3a%2f%2flocalhost%2f&page=contact">Contact us to get started</a></p>
		</div>
	</div>

	<hr>

	<div class="row">
		<div class="half">
			<h2>What’s in the repository?</h2>

			<p><a href="#">17 collections</a>, including <a href="#">9959 texts</a>, <a href="#">3074 images</a>, <a href="#">182 datasets</a>, <a href="#">67 audio files</a>, and <a href="#">27 videos</a>.</p>
			<p>Material from <a href="#">253 UNC Chapel Hill departments</a>, from <a href="#">Anthropology</a> to <a href="#">Toxicology</a>.</p>
		</div>

		<div class="half">
			<h2>A Few of Our Collections</h2>

			<p>
				<a href="record?id=uuid%3abded4944-f033-4015-af0f-3d39595f4d30" title="African American Performance Art Archive"><img src="/static/front/aapaa.jpg"></a>
				<a href="record?id=uuid%3a1add9fbc-f5c4-49a8-848e-96a52e3ade9c" title="Azoria Project Archive"><img src="/static/front/azoria.jpg"></a>
				<a href="record?id=uuid%3adfebbdf7-3361-4097-9fa4-7001ab6fcc11" title="BioMed Central"><img src="/static/front/bmc.png"></a>
				<a href="record?id=uuid%3acbbc2cc1-c538-4e28-b567-55db61b7942e" title="Carolina Planning Journal"><img src="/static/front/cpj.png"></a>
				<a href="record?id=uuid%3aa012aad2-1ab1-43b2-b5ab-0e14740e5e07" title="Electronic Theses and Dissertations"><img src="/static/front/etds.png"></a>
				<a href="record?id=uuid%3a9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d9" title="Program on Public Life"><img src="/static/front/popl.png"></a>
				<a href="record?id=uuid%3a8ae56bbc-400e-496d-af4b-3c585e20dba1" title="Research Laboratories of Archaeology"><img src="/static/front/rla.png"></a>
				<a href="record?id=uuid%3a5e4b2719-bb71-45ec-be63-5d018b6f5aab" title="Southern Folklife Collection Digital Files"><img src="/static/front/sfc.png"></a>
			</p>
		</div>
	</div>

	<hr>

	<div id="front-case-study" class="row">
		<div class="half">
	
			<figure>
				<a href="https://cdr.lib.unc.edu/record?id=uuid:c394b981-3c10-4faf-a9b2-c506f795840b">
					<img src="/static/front/rla-deer.jpg">
					<figcaption>
						<b>Ceramic Animal Effigy (Deer?)</b>
						Warren Wilson Site (1995)
					</figcaption>
				</a>
			</figure>
	
			<figure>
				<a href="https://cdr.lib.unc.edu/record?id=uuid:9ffd4af0-4f9d-4eae-a84a-0812997303dc">
					<img src="/static/front/rla-site.jpg">
					<figcaption>
						<b>General View of Site</b>
						Mecklenburg County, Virginia (1962)
					</figcaption>
				</a>
			</figure>
		</div>
		
		<div class="half">
			<h2><i>Case Study:</i> CDR and RLA staff work to preserve and provide access to digital research collections</h2>

			<p>Founded in 1939, the <a href="http://rla.unc.edu">Research Laboratories of Archaeology</a> (RLA) was the first center for the study of North Carolina archaeology. Serving the interests of students, scholars, and the general public, it is currently one of the leading institutes for archaeological teaching and research in the South. Located within the University of North Carolina at Chapel Hill’s College of Arts and Sciences, it provides support for faculty and students working not only in North Carolina, but also throughout the Americas and overseas.</p>

			<p>With one of the nation’s finest collections of archaeological materials from the South, the RLA curates more than seven million artifacts along with more than 60,000 photographic negatives, photographs, and slides. Over the past 70 years, virtually all of the major discoveries in the understanding of North Carolina's ancient past can be attributed to the RLA or to researchers trained there.</p>

			<p>The Carolina Digital Repository is working with RLA staff to to preserve their large collection of photographs, slides, and publications, and to provide access to those collections online.</p>

			<p class="button"><a href="record?id=uuid:8ae56bbc-400e-496d-af4b-3c585e20dba1">Browse the RLA Collection</a></p>
		</div>

	</div>

</div>
