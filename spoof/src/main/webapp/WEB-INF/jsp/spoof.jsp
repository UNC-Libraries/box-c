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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
<title>Spoofing</title>
<style type="text/css" media="screen">
  .memberships label {
    display: block;
  }
</style>
</head>
<body>

<%

final String SPOOF_COOKIE_PREFIX = "AUTHENTICATION_SPOOFING-";

String[] params = { "REMOTE_USER", "isMemberOf" };
String[] groups = { "unc:app:lib:cdr:admin", "unc:app:lib:cdr:adminltd", "unc:app:lib:cdr:accessCopiesPatron", "unc:app:lib:cdr:curator", "unc:app:lib:cdr:ingester", "unc:app:lib:cdr:metadataPatron", "unc:app:lib:cdr:observer", "unc:app:lib:cdr:patron", "unc:app:lib:cdr:processor", "unc:app:lib:cdr:SILS" };


// Read spoofed values from cookies

java.util.HashMap<String, String> values = new java.util.HashMap<String, String>();

if (request.getCookies() != null) {

  for (javax.servlet.http.Cookie c : request.getCookies()) {
  
    if (c.getName().startsWith(SPOOF_COOKIE_PREFIX)) {
    
      String key = c.getName().substring(SPOOF_COOKIE_PREFIX.length());
      String value = java.net.URLDecoder.decode(c.getValue(), "UTF-8");
    
      values.put(key, value);
    }
  
  }

}


if (request.getMethod() == "POST") {
  
  // Get the list of checked groups, and assemble and set isMemberOf.

  String[] membershipsParamValues = request.getParameterValues("memberships");

  StringBuilder builder = new StringBuilder();
  
  // Note that a null value for the memberships parameter values corresponds
  // to no checkboxes being checked.

  if (membershipsParamValues != null && membershipsParamValues.length > 0) {
    builder.append(membershipsParamValues[0]);
    for (int i = 1; i < membershipsParamValues.length; i++)
      builder.append(";").append(membershipsParamValues[i]);
  }
  
  // Add the otherMemberships parameter.
  
  String otherMembershipsParam = request.getParameter("otherMemberships");

  if (otherMembershipsParam != null && otherMembershipsParam.length() > 0) {
    if (builder.length() > 0)
      builder.append(";");
    builder.append(otherMembershipsParam);
  }

  if (builder.length() > 0)
    values.put("isMemberOf", builder.toString());
  else
    values.remove("isMemberOf");

  // If a parameter is not null, add it. For blank
  // params, remove the corresponding key.

  for (String key : params) {
    if (!key.equals("isMemberOf")) {
      String param = request.getParameter(key);

      if (param != null) {
        if (param.length() > 0)
          values.put(key, param);
        else
          values.remove(key);
      }
    }
  }

  // If the "clear" button was pressed, clear everything.
  
  if (request.getParameter("clear") != null)
    values.clear();
    
  // Write the resulting values to cookies.
  
  for (String key : params) {
    
    javax.servlet.http.Cookie cookie;
    
    if (values.containsKey(key)) {
      
      cookie = new Cookie(SPOOF_COOKIE_PREFIX + key, java.net.URLEncoder.encode(values.get(key), "UTF-8"));
      cookie.setMaxAge(-1);
      cookie.setPath("/");
      
    } else {
      
      cookie = new Cookie(SPOOF_COOKIE_PREFIX + key, "");
      cookie.setMaxAge(0);
      cookie.setPath("/");
      
    }
    
    response.addCookie(cookie);
    
  }
  
}

// Build the memberships set and the otherMemberships string from whatever we have in the values.

java.util.HashSet<String> memberships = new java.util.HashSet<String>();
java.util.HashSet<String> predefinedGroupsSet = new java.util.HashSet<String>(java.util.Arrays.asList(groups));
java.util.ArrayList<String> otherMembershipsList = new java.util.ArrayList<String>();

if (values.containsKey("isMemberOf")) {
  for (String group : values.get("isMemberOf").split(";")) {
    if (predefinedGroupsSet.contains(group))
      memberships.add(group);
    else
      otherMembershipsList.add(group);
  }
}

StringBuilder otherMembershipsBuilder = new StringBuilder();

for (String group : otherMembershipsList) {
  if (otherMembershipsBuilder.length() > 0)
    otherMembershipsBuilder.append(";");
  otherMembershipsBuilder.append(group);
}

String otherMemberships = otherMembershipsBuilder.toString();

%>

<form method="post" action="/spoof/">
  
  <table>
    <% for (String key : params) { %>
      <% if (!key.equals("isMemberOf")) { %>
        <tr>
          <td><%= key %></td>
          <td><input type="text" name="<%= key %>" value="<%= values.containsKey(key) ? values.get(key) : "" %>" ></td>
        </tr>
      <% } %>
    <% } %>
    <tr class="memberships">
      <td>isMemberOf</td>
      <td>
        <% for (String group : groups) { %>
          <label><input type="checkbox" name="memberships" value="<%= group %>" <%= memberships.contains(group) ? "checked" : "" %>> <%= group %></label>
        <% } %>
        <label>Other: <input name="otherMemberships" value="<%= otherMemberships %>"></label>
      </td>
    </tr>
  </table>
  
  <input type="submit" value="Set Spoofed Values">
  <input type="submit" name="clear" value="Clear">
  
</form>

<hr>

<%= values %>

</body>
</html>
