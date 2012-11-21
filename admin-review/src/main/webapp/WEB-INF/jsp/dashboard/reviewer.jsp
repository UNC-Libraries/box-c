<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="contentarea">
	<p class="description_text">
		Welcome, <c:out value="${sessionScope.user.userName}"/>. This is brief filler text to introduce you to the CDR Admin interface. There would be a link to <a
			href="">instructions</a> here and similar things. It should not fill up too much space, but give users some idea of
		where to start. At this point I am just typing to fill space.
	</p>
	<%--<b><c:import url="/testinclude.jsp" /></b> --%>
	<div class="collection_list">
		<c:import url="search/collectionList.jsp" />
	</div>
</div>