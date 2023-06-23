export default {
    methods: {
        pageEvent(recordData) {
            this.$gtag.event('record', {
                'event_category': recordData.briefObject.parentCollectionId,
                'event_label': `${recordData.briefObject.title}|${recordData.briefObject.id}`
            });
            this.matomoPageEvent(recordData);
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
                event: 'recordPageView',
                name: recordData.briefObject.title + "|" + recordData.briefObject.id,
                recordId: recordData.briefObject.id,
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
            this.matomoPageView(title)
        },

        matomoPageView(title) {
            window._mtm = window._mtm || [];
            window._mtm.push({
                event: 'pageViewEvent',
                name: title,
                recordId: null,
                recordTitle: null,
                resourceType: null,
                parentCollection: null
            });
        }
    }
}