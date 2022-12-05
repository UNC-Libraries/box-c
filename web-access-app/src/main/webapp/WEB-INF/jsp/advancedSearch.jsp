<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>
<%@ page import="edu.unc.lib.boxc.search.api.SearchFieldKey" %>

<div class="content-wrap">
<div class="contentarea">
	<h2>Advanced Search</h2>
</div>
<form id="advanced-search-form">
	<div class="lightest">
		<div class="twocol lightest shadowtop">
			<div class="contentarea">
				<h3>Search for</h3>
				<p class="clear has-text-weight-bold">
					<label for="anywhere">Anywhere</label> <input id="anywhere" name="${SearchFieldKey.DEFAULT_INDEX.getUrlParam()}" class="advsearch_text" type="text" />
				</p>
				<p class="clear has-text-weight-bold">
					<label for="title">Title</label> <input id="title" name="${SearchFieldKey.TITLE_INDEX.getUrlParam()}" class="advsearch_text" type="text" />
				</p>
				<p class="clear has-text-weight-bold">
					<label for="contributor">Creator/Contributor</label> <input id="contributor"name="${SearchFieldKey.CONTRIBUTOR_INDEX.getUrlParam()}" class="advsearch_text" type="text" />
				</p>
				<p class="clear has-text-weight-bold">
					<label for="subject">Subject</label> <input id="subject" name="${SearchFieldKey.SUBJECT_INDEX.getUrlParam()}" class="advsearch_text" type="text" />
				</p>
				<p class="clear has-text-weight-bold">
					<label for="subject">Collection Number</label> <input id="collection_id" name="${SearchFieldKey.COLLECTION_ID.getUrlParam()}" class="advsearch_text" type="text" />
				</p>
			</div>
		</div>
		<div class="twocol light shadowtop">
			<div class="contentarea">
				<h3>Limit By</h3>
				<select name="${SearchFieldKey.PARENT_COLLECTION.getUrlParam()}" class="advsearch_select" aria-label="collection">
					<option value="">Collection</option>
					<c:forEach items="${collectionList}" var="collectionRecord">
						<option value="<c:out value='${collectionRecord.id}' />"><c:out value="${collectionRecord.title}" /></option>
					</c:forEach>
				</select>
				<select name="${SearchFieldKey.FILE_FORMAT_CATEGORY.getUrlParam()}" class="advsearch_select" aria-label="format">
					<option value="">Format</option>
					<c:forEach items="${formats}" var="formatEntry">
						<option value="${formatEntry}"><c:out value="${formatEntry}"/></option>
					</c:forEach>
				</select>
				
				<p class="clear"><br /><br /></p>
				<p class="clear has-text-weight-bold">
					<label>Date Deposited</label> from <input aria-label="deposited-start-date" name="${SearchFieldKey.DATE_ADDED.getUrlParam()}Start" class="advsearch_date" type="text" />
						 to <input aria-label="deposited-end-date" name="${SearchFieldKey.DATE_ADDED.getUrlParam()}End" class="advsearch_date" type="text" />
						<a class="date_field_tooltip" title="Enter dates in YYYY.  Leave the right hand date empty to search for items with dates starting at the left hand date, and vice versa.">?</a>
				</p>
				<p class="clear has-text-weight-bold">
					<label>Date Created</label> from <input aria-label="created-start-date" name="${SearchFieldKey.DATE_CREATED_YEAR.getUrlParam()}Start" class="advsearch_date" type="text" />
						 to <input aria-label="created-end-date" name="${SearchFieldKey.DATE_CREATED_YEAR.getUrlParam()}End" class="advsearch_date" type="text" />
					<a class="date_field_tooltip" title="Enter dates in YYYY format.  Leave the right hand date empty to search for items with dates starting at the left hand date, and vice versa.">?</a>
				</p>
			</div>
		</div>
	
		<div class="onecol white">
			<div class="contentarea">
				<input type="submit" class="right" id="advsearch_submit" value="Search" />
			</div>
		</div>
	
	</div>
</form>
</div>
<link rel="stylesheet" type="text/css" href="/static/css/jquery-ui.css"/>
<link rel="stylesheet" type="text/css" href="/static/css/jquery.qtip.min.css"/>
<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/public/advancedSearch"></script>