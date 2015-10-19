function Peek(element, template, columnWidth) {
  
  this.$element = $(element);
  this.element = this.$element[0];
  
  this.$container = $("<div class=\"peek-container\"></div>").appendTo(this.$element);
  this.$columns = $("<div class=\"peek-columns\"></div>").appendTo(this.$container);
  
  this.template = template;
  this.columnWidth = columnWidth;
  
  this.columns = [];
  this.specs = [];
  this.items = [];
  this.inprogress = 0;
  
}

Peek.prototype.add = function(specs) {
  
  this.specs = this.specs.concat(specs);
  
}

Peek.prototype.start = function() {
  
  $(window).on("resize", _.debounce(_.bind(this.layout, this), 100));
  setInterval(_.bind(this.load, this), 100);
  setInterval(_.bind(this.fill, this), 100);
  setInterval(_.bind(this.shift, this), 4000);
  
  this.layout();
  
}

Peek.prototype.offset = function(amt) {
  
  _.invoke(this.columns, "offset", amt);
  
}

Peek.prototype.didRemoveItems = function(items) {
  
  this.add(_.pluck(items, "spec"));
  
}

Peek.prototype.layout = function() {
  
  // Amount of space taken up by left and right columns
  
  var parentWidth = this.$element.width();
  var leftRightSpace = Math.max(0, Math.ceil((parentWidth - this.columnWidth) / 2));
  
  // How many columns we want, always with one center column
  
  var leftRightCount = Math.ceil(leftRightSpace / this.columnWidth);
  var count = 1 + (leftRightCount * 2);
  
  // Add and remove, and reposition
  
  if (this.columns.length != count) {
  
    // Add columns if we don't have enough
  
    while (this.columns.length < count) {
    
      var column;
    
      column = new Column();
      column.delegate = this;
      column.$element.css("width", this.columnWidth + "px");
    
      if (this.columns.length % 2 == 0) {
        this.columns.unshift(column);
        this.$columns[0].insertBefore(column.$element[0], this.$columns[0].firstChild);
      } else {
        this.columns.push(column);
        this.$columns[0].appendChild(column.$element[0]);
      }
    
    }
  
    // Remove columns if we have too many
  
    while (this.columns.length > count) {
    
      var column;
    
      if (this.columns.length % 2 == 0) {
        column = this.columns.shift();
      } else {
        column = this.columns.pop();
      }
    
      this.didRemoveItems(column.items);
      this.$columns[0].removeChild(column.$element[0]);
    
    }
    
    // Adjust positioning

    for (var i = 0; i < this.columns.length; i++) {
      this.columns[i].$element.css("left", (i * this.columnWidth) + "px");
    }
  
    this.$columns.css("width", (this.columnWidth * count) + "px");
    this.$columns.css("marginLeft", -Math.round((this.columnWidth * count) / 2) + "px");
    
  }
  
}

Peek.prototype.loadItem = function(spec) {
  
  var $element = $(this.template(spec).replace(new RegExp("^\\s*"), ""));
  var image = $element.find("img").eq(0);
  
  if (image) {
    
    image.on("load", _.bind(function() {
      this.items.push({ spec: spec, $element: $element });
      this.inprogress--;
    }, this));

    image.on("error", _.bind(function() {
      this.inprogress--;
    }, this));

    this.inprogress++;
    
  } else {
    
    throw "Couldn't retrieve image for evaluated item template";
    
  }
  
}

Peek.prototype.load = function() {
  
  // If we have items, don't load any.
  
  if (this.items.length > 0) {
    return;
  }
  
  // Ensure that we aren't loading more than 10 items at a time.
  
  var count = Math.max(10 - this.inprogress, 0);
  
  for (var i = 0; i < count; i++) {
    var spec = this.specs.shift();
    
    if (spec) {
      this.loadItem(spec);
    } else {
      break;
    }
  }
   
}

Peek.prototype.fill = function() {
  
  while (this.items.length > 0 && _.any(this.columns, function(c) { return c.getFreeHeight() > 0; })) {
    
    var column = _.max(this.columns, function(c) { return c.getFreeHeight(); });
    column.push(this.items.shift());
    
  }
   
}

Peek.prototype.shift = function() {
  
  var shiftable = _.filter(this.columns, function(c) { return c.canShift(); });
  
  if (shiftable.length == 0) {
    return;
  }
  
  var column = shiftable[Math.floor(Math.random() * shiftable.length  )];
  
  column.shift();
   
}

function Column() {
  
  this.$element = $("<div class=\"peek-column\"></div>");
  this.$inner = $("<div class=\"peek-column-inner\"></div>").appendTo(this.$element);
  
  this.items = [];
  this.dragging = false;
  this.backfilling = false;
  this.hover = false;
  
  this.nudge = -(50 + Math.floor(Math.random() * 30));
  
  this.$element.on("mousedown", _.bind(this.dragStart, this));
  $(window).on("mousemove", _.bind(this.dragMove, this));
  $(window).on("mouseup", _.bind(this.dragEnd, this));
  this.$element.on("contextmenu", _.bind(this.dragCancel, this));
  this.$element.on("click", _.bind(this.onClick, this));
  
  this.$element.on("mouseover", _.bind(this.hoverStart, this));
  this.$element.on("mouseout", _.bind(this.hoverEnd, this));
  
  this.setOffset(0);
  
}

Column.prototype.getFreeHeight = function() {
  
  if (this.items.length == 0) {
    return this.getHeight();
  } else if (this.backfilling) {
    return this.getOffset();
  } else {
    return (this.getHeight() - this.getOffset()) - this.getItemMeasurements(this.items.length - 1).bottom;
  }
  
}

Column.prototype.canShift = function() {
  
  return this.items.length > 0 && !this.dragging && this.getFreeHeight() <= 0 && !this.hover && !this.backfilling;
  
}

Column.prototype.setOffset = function(offset) {
  
  this.$inner.css({ marginTop: (offset + this.nudge) + "px" });
  
}

Column.prototype.getOffset = function() {
  
  return parseInt(this.$inner.css("marginTop")) - this.nudge;
  
}

Column.prototype.getHeight = function() {
  
  return this.$element.height() - this.nudge;
  
}

Column.prototype.prune = function(count) {

  if (count < 0 || count > this.items.length) {
    throw "Prune count out of bounds";
  }

  var offset = this.getOffset();
  var pruned = this.items.splice(0, count);

  for (var i = 0; i < pruned.length; i++) {
    offset += pruned[i].$element.outerHeight();
    pruned[i].$element.remove();
  }

  this.setOffset(offset);
  
  if (this.delegate) {
    this.delegate.didRemoveItems(pruned);
  }

}

// Get measurements for the item at index relative to the top of the .column-inner element.

Column.prototype.getItemMeasurements = function(index) {

  if (index < 0 || index > this.items.length - 1) {
    throw "Measurement index out of bounds";
  }
  
  var position = this.items[index].$element.position();
  var height = this.items[index].$element.outerHeight();
  var offset = this.getOffset() + this.nudge;
  
  return {
    top: position.top - offset,
    bottom: (position.top - offset) + height,
    middle: (position.top - offset) + (height / 2)
  };

}

Column.prototype.setLayout = function(layout) {

  // Stop animations

  this.$inner.stop();

  // Animate, prune on completion
  
  var complete, offset;

  if (layout.prune) {
    complete = (function(context, count) { return function() { context.prune(count); } })(this, layout.prune);
  }

  if (typeof layout.top !== "undefined") {
    offset = -this.getItemMeasurements(layout.top).top;
  } else if (typeof layout.bottom !== "undefined") {
    offset = this.getHeight() - this.getItemMeasurements(layout.bottom).bottom;
  }
  

  if (typeof offset !== "undefined") {
    this.$inner.animate({ marginTop: (offset + this.nudge) + "px" }, { complete: complete, duration: 1000 });
  } else if (typeof prune !== "undefined") {
    complete.call();
  }

}

Column.prototype.hoverStart = function(e) {
  
  this.hover = true;
  
}

Column.prototype.hoverEnd = function(e) {
  
  this.hover = false;
  
}

Column.prototype.dragStart = function(e) {
  
  e.preventDefault();
  
  // Stop animations
  
  this.$inner.stop();
  
  // Find the drag offset
  
  this.dragOffset = this.getOffset() - e.pageY;
  this.initialDragPosition = e.pageY;
  
  // Start dragging
  
  this.dragging = true;
    
  this.dragMoved = false;
  
}

Column.prototype.dragMove = function(e) {
  
  if (this.dragging) {
  
    e.preventDefault();
    
    this.dragMoved = true;
    
    this.backfilling = false;
    
    this.setOffset(this.dragOffset + e.pageY);
    
  }
  
}

Column.prototype.onClick = function(e) {
  
  if (this.dragMoved) {
    
    e.preventDefault();
    
  }
  
}

Column.prototype.dragCancel = function(e) {
  
  if (this.dragging) {
  
    this.dragging = false;
    
    this.release();
    
  }
  
}

Column.prototype.dragEnd = function(e) {
  
  if (this.dragging) {
    
    e.preventDefault();
    
    this.dragging = false;
    
    this.release();
    
  }
  
}

Column.prototype.release = function() {
  
  // If there aren't any items
  
  if (this.items.length == 0) {
    this.setLayout({ top: 0 });
    return;
  }
  
  // If the top item is below the top of the column
  
  var offset = this.getOffset();
  
  if (offset > 0) {
    this.setLayout({ top: 0 });
    return;
  }
  
  // If there aren't any items overflowing the column
  
  var height = this.getHeight();
  var bottom = this.getItemMeasurements(this.items.length - 1).bottom;
  
  if (bottom <= height) {
    this.setLayout({ top: 0 });
    return;
  }
  
}

Column.prototype.push = function(item) {
  
  if (this.backfilling && this.getOffset() > 0) {
    
    this.items.unshift(item);
    item.$element.prependTo(this.$inner).css({ opacity: 0 }).animate({ opacity: 1 });
    
    var height = item.$element.outerHeight();
    
    this.setOffset(this.getOffset() - height);
    
    if (this.getOffset() <= 0) {
      this.backfilling = false;
    }
  
  } else {
  
    this.items.push(item);
    item.$element.appendTo(this.$inner).css({ opacity: 0 }).animate({ opacity: 1 });
    
  }
  
}

Column.prototype.shift = function() {
  
  if (this.items.length > 0 && !this.dragging && !this.backfilling) {
    
    // Find the first item which is more than 50% onscreen, snap the top to the next item (if there is one), and prune.
    
    var offset = this.getOffset();
    
    for (var i = 0; i < this.items.length; i++) {
      if (this.getItemMeasurements(i).middle + offset > 0) {
        this.setLayout({ top: Math.min(i + 1, this.items.length - 1), prune: i });
        break;
      }
    }
    
  }
  
}

Column.prototype.offset = function(amt) {
  
  var offset = this.getOffset() + amt;

  if (offset > 0) {
  
    this.$inner.stop();
  
    this.setOffset(offset);
    this.backfilling = true;
    
  } else {
    
    this.backfilling = false;
    
  }
  
}

$("#peek").addClass("active");

$(function() {
  
  var source = "<div class=\"item\"> \
    <a href=\"https://cdr.lib.unc.edu/record/<%= data.pid %>\"> \
      <div class=\"image\"> \
        <img src=\"/shared/peek/thumbnails/<%= data.path %>\"> \
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

  $.getJSON("/shared/peek/peek.json", function(items) {

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

    $(window).scroll(function() {
      if (!$(document.body).hasClass("peek")) {
        $("#peek .peek-columns").css({
          marginTop: ($(window).scrollTop() * 0.1) + "px"
        });
      }
    });
    
    peek.add(_.shuffle(items));
    peek.start();
    
  });

});
