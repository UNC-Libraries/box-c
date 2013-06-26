var source = "<div class=\"item\"> \
  <a href=\"https://cdr.lib.unc.edu/record?id=<%= data.pid %>\"> \
    <div class=\"image\"> \
      <img src=\"/static/peek/thumbnails/<%= data.path %>\"> \
    </div> \
    <div class=\"description\"> \
      <div class=\"title\"><%= data.title %></div> \
      <% if (data.creators.length > 0) { %><div class=\"other\"><%= data.creators.join(\"; \") %></div><% } %> \
      <% if (data.collection) { %><div class=\"other\"><%= data.collection %></div><% } %> \
    </div> \
  </a> \
</div>";

var template = _.template(source, null, { variable: "data" });

var peek = new Peek("#peek", template, 195);

$.getJSON("/static/peek/peek.json", function(items) {
  peek.add(_.shuffle(items));
  peek.start();
});

$("#peek-enter").on("click", function() {
  window.location.hash = "p";
});

$("#peek-exit").on("click", function() {
  window.location.hash = "";
});

$(window).on("hashchange", function() {
  $(document.body).toggleClass("peek", window.location.hash == "#p");
});

$(document).on("keydown", function(e) {
  if (e.keyCode == 27) {
    window.location.hash = "";
  }
});

$(document.body).toggleClass("peek", window.location.hash == "#p");
