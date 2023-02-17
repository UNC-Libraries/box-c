export default {
    methods: {
        /**
         * Vite really, really wants all images to be in the project being built and referenced via imports.
         * This doesn't make sense for some of our images. The project won't build unless an import or url is given.
         * So just return the image url for images that are external to the Vue project.
         * @param image
         */
        nonVueStaticImageUrl(image) {
            return `https://${window.location.host}/static/images/${image}`;
        }
    }
}