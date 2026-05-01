import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useAltTextStore } from '@/stores/alt-text';

describe('alt-text store', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
        fetchMock.resetMocks();
    });

    describe('extractTagPaneValues', () => {
        it('reads displayValue, searchValue, and count from MG_CONTENT_TAGS facet values', () => {
            const store = useAltTextStore();

            const values = store.extractTagPaneValues([
                {
                    name: 'OTHER_FACET',
                    values: [{ displayValue: 'Ignore', searchValue: 'ignore' }]
                },
                {
                    name: 'MG_CONTENT_TAGS',
                    values: [
                        { displayValue: 'People Visible', searchValue: 'people_visible', count: 1 },
                        { displayValue: 'Text Present', searchValue: 'text_present', count: 3 }
                    ]
                }
            ]);

            expect(values).toEqual([
                { label: 'People Visible', searchValue: 'people_visible', count: 1 },
                { label: 'Text Present', searchValue: 'text_present', count: 3 }
            ]);
        });

        it('returns an empty list when MG_CONTENT_TAGS is not present', () => {
            const store = useAltTextStore();
            expect(store.extractTagPaneValues([])).toEqual([]);
            expect(store.extractTagPaneValues(null)).toEqual([]);
        });
    });


    describe('fetchTableItemsPage', () => {
        it('builds tagPaneValues from MG_CONTENT_TAGS facetFields', async () => {
            const store = useAltTextStore();
            store.setCurrentUuid('uuid-1');

            fetchMock.mockResponseOnce(JSON.stringify({
                metadata: [],
                recordsTotal: 2,
                recordsFiltered: 2,
                facetFields: [
                    {
                        name: 'MG_CONTENT_TAGS',
                        values: [
                            { displayValue: 'People Visible', searchValue: 'people_visible', count: 1 },
                            { displayValue: 'Text Present', searchValue: 'text_present', count: 1 }
                        ]
                    }
                ]
            }));

            const result = await store.fetchTableItemsPage({ start: 0, length: 25, search: '' });

            expect(result.recordsTotal).toBe(2);
            expect(store.tagPaneValues).toEqual([
                { label: 'People Visible', searchValue: 'people_visible', count: 1 },
                { label: 'Text Present', searchValue: 'text_present', count: 1 }
            ]);
        });
    });
});

