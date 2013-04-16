<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %> 
<%@page trimDirectiveWhitespaces="true" %>

<link rel="stylesheet" type="text/css" href="/static/css/admin/admin_forms.css" />
<link rel="stylesheet" type="text/css" href="/static/css/admin/jqueryui-editable.css" />

<%
	// Retrieving the static CDR ACL Namespace object
	pageContext.setAttribute("aclNS", edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_ACL_NS);
%>

<div class="edit_acls">
	<h3>Access Settings</h3>
	<div class="form_field">
		<label>Published</label>
		<c:set var="publishedAttr" value="${targetACLs.getAttribute('published', aclNS)}" />
		<a class="boolean_toggle" data-field="published">
			<c:choose>
				<c:when test="${not empty publishedAttr && publishedAttr.value == 'false'}">No</c:when>
				<c:otherwise>Yes</c:otherwise>
			</c:choose>
		</a>
		<c:if test="${targetMetadata.status.contains('Parent Unpublished')}">
			&nbsp;(Parent is unpublished)
		</c:if>
	</div>
	<div class="form_field">
		<label>Embargo</label>
		<c:set var="embargoAttr" value="${targetACLs.getAttribute('embargo-until', aclNS)}" />
		<a href="#" class="add_embargo" data-type="combodate">
			<c:if test="${not empty embargoAttr}">
				<fmt:parseDate value="${embargoAttr.value}" var="parsedEmbargo" pattern="yyyy-MM-dd'T'hh:mm:ss" />
				<fmt:formatDate value="${parsedEmbargo}" pattern="MM/dd/yyyy"/>
			</c:if>
		</a>
	</div>
	<div class="form_field">
		<label>Discoverable</label>
		<c:set var="discoverableAttr" value="${targetACLs.getAttribute('discoverable', aclNS)}" />
		<a class="boolean_toggle" data-field="discoverable">
			<c:choose>
				<c:when test="${not empty discoverableAttr && discoverableAttr.value == 'false'}">
					No
				</c:when>
				<c:otherwise>
					Yes
				</c:otherwise>
			</c:choose>
		</a>
	</div>
	<h3>Roles Granted</h3>
	<div class="form_field">
		<label>Inherit from parents?</label>
		<c:set var="inheritAttr" value="${targetACLs.getAttribute('inherit', aclNS)}" />
		<a class="inherit_toggle" data-field="inherit">
			<c:choose>
				<c:when test="${not empty inheritAttr && inheritAttr.value == 'false'}">
					No
					<c:set var="inheritanceDisabled" value="inheritance_disabled" />
				</c:when>
				<c:otherwise>
					Yes
				</c:otherwise>
			</c:choose>
		</a>
	</div>
	<div class="clear"></div>
	<table class="roles_granted ${inheritanceDisabled}">
		<c:forEach items="${rolesGranted}" var="roleEntry">
			<tr class="role_groups" data-value="${roleEntry.key}">
				<td class="role">${roleEntry.key}</td>
				<td class="groups">
					<c:forEach items="${roleEntry.value}" var="groupEntry">
						<c:choose>
							<c:when test="${groupEntry.inherited}">
								<span class="inherited">${groupEntry.roleName}</span>
							</c:when>
							<c:otherwise>
								<span>${groupEntry.roleName}</span><a class="remove_group">x</a>
							</c:otherwise>
						</c:choose>
						<br/>
					</c:forEach>
				</td>
			</tr>
		</c:forEach>
		<%-- Role editing zone --%>
		<tr class="edit_role_granted">
			<td>
			</td>
			<td>
				<a>Edit Roles</a>
			</td>
		</tr>
		<tr class="add_role_granted">
			<td class="role">
				<select class="add_role_name">
					<option value="">Role</option>
					<option value="metadata-patron">Metadata Patron</option>
					<option value="access-copies-patron">Access Copies Patron</option>
					<option value="patron">Patron</option>
					<option value="observer">Observer</option>
					<option value="ingester">Ingester</option>
					<option value="processor">Processor</option>
					<option value="curator">Curator</option>
				</select>
			</td>
			<td>
				<input type="text" placeholder="group" value="" class="add_group_name"/>
				<input type="button" value="Add role" class="add_role_button"/>
			</td>
		</tr>
	</table>
	<div class="update_field">
		<input type="button" value="Update" class="update_button"/>
	</div>
</div>

<c:set var="escapedXML" value="${cdr:objectToJSON(accessControlXML)}" />
<script>
	var escaped = "<c:out value='${cdr:objectToJSON(accessControlXML)}'/>";
	var aclNS = "${aclNS.getURI()}";
	escaped = escaped.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"').replace(/&#034;/g, '"').replace(/^"/, '').replace(/"$/, '');
	require({
		baseUrl: '/static/js/',
		paths: {
			'jquery' : 'jquery.min',
			'jquery-ui' : 'jquery-ui.min',
			'qtip' : 'jquery.qtip.min',
			'PID' : 'admin/src/PID',
			'EditAccessControlForm' : 'admin/src/EditAccessControlForm',
			'ModalLoadingOverlay' : 'admin/src/ModalLoadingOverlay',
			'AlertHandler' : 'admin/src/AlertHandler',
			'ConfirmationDialog' : 'admin/src/ConfirmationDialog',
			'editable' : 'jqueryui-editable.min',
			'moment' : 'moment.min'
		},
		shim: {
			'jquery-ui' : ['jquery'],
			'qtip' : ['jquery'],
			'editable' : ['jquery']
		}
	}, ['module', 'jquery', 'EditAccessControlForm'], function(module, $){
		var accessControlModel = $.parseXML(escaped);
		$(".edit_acls").editAccessControlForm({'xml': accessControlModel, 'namespace': aclNS, 
			'containingDialog': $('.containingDialog'), 'updateUrl' : "acl/${pid.replace(':', '/')}", 'pid' : '${pid}',
					'groupSuggestionsURL' : 'acl/getGroups'});
	});
</script>