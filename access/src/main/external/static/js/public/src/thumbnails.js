define('thumbnails', ['jquery'], function($) {
  
  $(".thumbnail img").each(function() {
    var thumbnail = $(this).closest(".thumbnail");
    
    if (!this.complete) {
      thumbnail.addClass("placeholder");
      
      $(this).on("load", function() {
        thumbnail.removeClass("placeholder")
      });
    }
  });

});