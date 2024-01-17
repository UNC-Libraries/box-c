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