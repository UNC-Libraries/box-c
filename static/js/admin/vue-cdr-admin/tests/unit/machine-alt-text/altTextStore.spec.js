import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useAltTextStore } from '@/stores/alt-text';

describe('alt-text store', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
        fetchMock.resetMocks();
    });

    describe('fetchTableItems', () => {
        it('fetches all pages and appends metadata rows in order', async () => {
            const store = useAltTextStore();
            store.setCurrentUuid('uuid-1');

            fetchMock
                .mockResponseOnce(JSON.stringify({
                    resultCount: 250,
                    metadata: [{ id: 'id-1' }, { id: 'id-2' }]
                }))
                .mockResponseOnce(JSON.stringify({
                    resultCount: 250,
                    metadata: [{ id: 'id-3' }]
                }))
                .mockResponseOnce(JSON.stringify({
                    resultCount: 250,
                    metadata: [{ id: 'id-4' }]
                }));

            await store.fetchTableItems();

            expect(fetchMock).toHaveBeenCalledTimes(3);
            expect(fetchMock.mock.calls[0][0]).toContain('page=1');
            expect(fetchMock.mock.calls[1][0]).toContain('page=2');
            expect(fetchMock.mock.calls[2][0]).toContain('page=3');
            expect(store.totalPages).toBe(3);
            expect(store.currentPage).toBe(1); // reset page number after fetchin all items
            expect(store.items).toEqual([
                { id: 'id-1' },
                { id: 'id-2' },
                { id: 'id-3' },
                { id: 'id-4' }
            ]);
            expect(store.isLoading).toBe(false);
        });

        it('falls back to a single page when resultCount is not finite', async () => {
            const store = useAltTextStore();
            store.setCurrentUuid('uuid-1');

            fetchMock.mockResponseOnce(JSON.stringify({
                resultCount: null,
                metadata: [{ id: 'id-1' }]
            }));

            await store.fetchTableItems();

            expect(fetchMock).toHaveBeenCalledTimes(1);
            expect(store.totalPages).toBe(1);
            expect(store.currentPage).toBe(1);
            expect(store.items).toEqual([{ id: 'id-1' }]);
            expect(store.isLoading).toBe(false);
        });
    });
});

