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
<div>
	<div class="search-query-text collection-list has-text-centered">
		<h2>Collections</h2>
	</div>

	<div class="columns">
		<div class="column is-12 collection-browse">
			<div id="app"></div>
		</div>
	</div>
</div>
<script type="module" crossorigin src="static/js/vue-access-vendor.js"></script>
<script type="module" crossorigin src="static/js/vue-access-index.js"></script>