<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div id="search_menu">
	<div class="query_menu">
		<div>
			<h3>Search</h3>
			<div>
				<form>
					<div class="search_inputwrap">
						<input name="query" class="search_text" type="text" placeholder="Search"/>
						<select name="queryType" class="index_select">
							<option value="anywhere">Anywhere</option>
							<option value="titleIndex">Title</option>
							<option value="contributorIndex">Contributor</option>
							<option value="subjectIndex">Subject</option>
						</select>
					</div>
					<input type="submit" value="Go" class="search_submit"/>
					<p class="search_within">
						<input type="radio" value="" name="searchWithin" />Everything
						<input type="radio" checked="checked" name="searchWithin" />Current folder
						<input type="radio" name="searchWithin" />Within results
					</p>
				</form>
			</div>
		</div>
	</div>
	<div class="filter_menu">
		<div>
			<h3>Structure</h3>
			<c:set var="structureDataUrl">structure/<c:if test="${not empty containerBean}">${containerBean.pid.path}</c:if>?view=facet&depth=1&queryp=list</c:set>
			<div data-href="${structureDataUrl}" id="structure_facet">
				<div class="center"><img src="/static/images/admin/loading-small.gif"/></div>
			</div>
		</div>
		<div>
			<h3>Refine your search</h3>
			<div>
				<div class="center"><img src="/static/images/admin/loading-small.gif"/></div>
			</div>
		</div>
		<div>
			<h3>Access Control</h3>
			<div>
				<div class="center"><img src="/static/images/admin/loading-small.gif"/></div>
			</div>
		</div>
		<div>
			<h3>Content</h3>
			<div>
				<div class="center"><img src="/static/images/admin/loading-small.gif"/></div>
			</div>
		</div>
	</div>
</div>