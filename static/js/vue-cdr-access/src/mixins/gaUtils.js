export default {
    methods: {
        pageEvent(recordData) {
            let collection = recordData.briefObject.parentCollectionName || '';

            if (collection === '' && recordData.briefObject.type === 'Collection') {
                collection = recordData.briefObject.title;
            }
            if (collection === '') {
                collection = '(no collection)';
            }

            this.$gtag.event('record', {
                'event_category': recordData.briefObject.parentCollectionId,
                'event_label': `${recordData.briefObject.title}|${recordData.briefObject.id}`,
                'value': `${recordData.briefObject.title}|${recordData.briefObject.id}`
            });
        },

        pageView(title) {
            this.$gtag.pageview({
                page_title: `Digital Collections Repository - ${title}`,
                page_path: this.$route.path,
                page_location: window.location.href
            })
        }
    }
}