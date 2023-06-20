export default {
    methods: {
        pageEvent(recordData) {
            this.$gtag.event('record', {
                'event_category': recordData.briefObject.parentCollectionId,
                'event_label': `${recordData.briefObject.title}|${recordData.briefObject.id}`
            });
        },

        matomoPageEvent(recordData) {
            let collection = recordData.briefObject.parentCollectionName || '';
            if (collection === '' && recordData.briefObject.type === 'Collection') {
                collection = recordData.briefObject.title;
            }
            if (collection === '') {
                collection = '(no collection)';
            }

            window._mtm = window._mtm || [];
            window._mtm.push({
                recordUUID: recordData.briefObject.id,
                recordTitle: recordData.briefObject.title,
                resourceType: recordData.resourceType,
                parentCollection: collection
            });
        },

        pageView(title) {
            this.$gtag.pageview({
                page_title: `Digital Collections Repository - ${title}`,
                page_path: this.$route.path,
                page_location: window.location.href
            });
        },

        matomoPageView(title) {
            window._mtm = window._mtm || []
            window._mtm.push(['setCustomUrl', window.location.pathname]);
            window._mtm.push(['setDocumentTitle', title]);
            window._mtm.push(['trackPageView']);
        }
    }
}