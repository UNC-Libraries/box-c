Universal Image Viewer Setup

1. Update the UV version in package.json
2. Check that the dependencies in the dist/lib/offline.js haven't been updated. If they have they need to be copied to 
   offline.js at the plugin root, save for jQuery, to avoid an issue with the supplied jQuery.
3. Then deploy static as normal