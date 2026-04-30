import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useAltTextStore } from '@/stores/alt-text';

describe('alt-text store', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
        fetchMock.resetMocks();
    });

    describe('parseTagCounts', () => {
        it('returns null for invalid input', () => {
            const store = useAltTextStore();

            expect(store.parseTagCounts(null)).toBeNull();
            expect(store.parseTagCounts('invalid')).toBeNull();
            expect(store.parseTagCounts(5)).toBeNull();
        });

        it('normalizes array entries with finite counts', () => {
            const store = useAltTextStore();

            const result = store.parseTagCounts([
                { tag: 'people_visible', count: 3 },
                { tag: 'named_individuals', count: 2 },
                { tag: 'skip-null', count: Infinity },
                { tag: null, count: 1 }
            ]);

            expect(result).toEqual({
                people_visible: 3,
                named_individuals: 2
            });
        });

        it('normalizes object maps with numeric coercion', () => {
            const store = useAltTextStore();

            const result = store.parseTagCounts({
                people_visible: '4',
                named_individuals: 1,
                skip_nan: 'NaN',
                skip_inf: Infinity
            });

            expect(result).toEqual({
                people_visible: 4,
                named_individuals: 1
            });
        });
    });

    describe('fetchGlobalTagCounts', () => {
        it('returns empty object without uuid and does not fetch', async () => {
            const store = useAltTextStore();

            const result = await store.fetchGlobalTagCounts();

            expect(result).toEqual({});
            expect(fetchMock).not.toHaveBeenCalled();
        });

        it('fetches once per uuid and reuses cached counts', async () => {
            const store = useAltTextStore();
            store.setCurrentUuid('uuid-1');

            fetchMock.mockResponseOnce(JSON.stringify({
                metadata: [
                    { mgContentTags: ['people_visible', 'named_individuals'] },
                    { mgContentTags: ['people_visible'] }
                ]
            }));

            const first = await store.fetchGlobalTagCounts();
            const second = await store.fetchGlobalTagCounts();

            expect(first).toEqual({ people_visible: 2, named_individuals: 1 });
            expect(second).toEqual({ people_visible: 2, named_individuals: 1 });
            expect(fetchMock).toHaveBeenCalledTimes(1);
            expect(store.globalTagCountsLoadedForUuid).toBe('uuid-1');
        });

        it('fetches again when uuid changes', async () => {
            const store = useAltTextStore();

            store.setCurrentUuid('uuid-1');
            fetchMock.mockResponseOnce(JSON.stringify({
                metadata: [{ mgContentTags: ['tag-a'] }]
            }));
            await store.fetchGlobalTagCounts();

            store.setCurrentUuid('uuid-2');
            fetchMock.mockResponseOnce(JSON.stringify({
                metadata: [{ mgContentTags: ['tag-b'] }, { mgContentTags: ['tag-b'] }]
            }));
            const result = await store.fetchGlobalTagCounts();

            expect(result).toEqual({ 'tag-b': 2 });
            expect(fetchMock).toHaveBeenCalledTimes(2);
            expect(store.globalTagCountsLoadedForUuid).toBe('uuid-2');
        });
    });
});

