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
        this.setLayout({ top: Math.min(i + 1, this.items.length - 1), prune: Math.max(0, i + 1) });
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
