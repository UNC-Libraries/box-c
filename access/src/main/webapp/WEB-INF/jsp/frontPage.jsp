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

<main>
	<div class="collection-links">
		<div class="collection-link-row">
			<a href="">
				<img src="/static/front/nc-collection.png" alt="North Carolina Collection" aria-hidden="true">
				<span>North Carolina Collection</span>
			</a>
			<a href="">
				<img src="/static/front/university-archives.png" alt="Universtiy Archives" aria-hidden="true">
				<span>University Archives</span>
			</a>
		</div>
		<div class="collection-link-row">
			<a href="">
				<img src="/static/front/southern-historical-collection.png" alt="Southern Historical Collection" aria-hidden="true">
				<span>Southern Historical Collection</span>
			</a>
			<a href="">
				<img src="/static/front/nc-collection.png" alt="North Carolina Collection" aria-hidden="true">
				<span>North Carolina Collection</span>
			</a>
		</div>
	</div>
	<div class="info-row">
		<div class="info container">
			<h3>What's in the repository?</h3>
			<div class="info-icons">
				<div><i class="fas fa-image"></i><span><c:out value="${formatCounts.image}"/> images</span></div>
				<div><i class="fas fa-video"></i><span><c:out value="${formatCounts.video}"/> video files</span></div>
				<div><i class="fas fa-music"></i><span><c:out value="${formatCounts.audio}"/> audio files</span></div>
				<div><i class="fas fa-file-alt"></i><span><c:out value="${formatCounts.text}"/> texts</span></div>
			</div>
			<p>Interested in seeing more?</p>
			<p>See <a href="">more digital collections</a> or visit the <a href="https://library.unc.edu/wilson/shc">Wilson Special Collections Library</a> website.</p>
		</div>
	</div>
</main>