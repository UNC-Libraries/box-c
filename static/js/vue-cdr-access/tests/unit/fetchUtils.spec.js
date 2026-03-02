import fetchUtils from '@/mixins/fetchUtils.js';

const { fetchWrapper } = fetchUtils.methods;

describe('fetchUtils.js', () => {
    beforeEach(() => {
        global.fetch = vi.fn();
    });

    afterEach(() => {
        vi.resetAllMocks();
    });

    describe('successful responses', () => {
        it('returns parsed JSON by default (json_response = true)', async () => {
            const data = { key: 'value' };
            global.fetch.mockResolvedValue({ ok: true, json: vi.fn().mockResolvedValue(data) });

            const result = await fetchWrapper('https://example.com');
            expect(result).toEqual(data);
        });

        it('returns text when json_response is false', async () => {
            global.fetch.mockResolvedValue({ ok: true, text: vi.fn().mockResolvedValue('plain text') });

            const result = await fetchWrapper('https://example.com', false);
            expect(result).toBe('plain text');
        });

        it('returns empty string when json_response is false and body is empty', async () => {
            global.fetch.mockResolvedValue({ ok: true, text: vi.fn().mockResolvedValue('') });

            const result = await fetchWrapper('https://example.com', false);
            expect(result).toBe('');
        });

        it('calls fetch with the provided URL', async () => {
            global.fetch.mockResolvedValue({ ok: true, json: vi.fn().mockResolvedValue({}) });

            await fetchWrapper('https://example.com/test');
            expect(global.fetch).toHaveBeenCalledWith('https://example.com/test', expect.any(Object));
        });

        it('uses GET method by default', async () => {
            global.fetch.mockResolvedValue({ ok: true, json: vi.fn().mockResolvedValue({}) });

            await fetchWrapper('https://example.com');
            expect(global.fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({ method: 'GET' })
            );
        });

        it('uses a PUT method when specified in options', async () => {
            global.fetch.mockResolvedValue({ ok: true, json: vi.fn().mockResolvedValue({}) });

            await fetchWrapper('https://example.com', true, { method: 'PUT', headers: {} });
            expect(global.fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({ method: 'PUT' })
            );
        });

        it('uses a DELETE method when specified in options', async () => {
            global.fetch.mockResolvedValue({ ok: true, json: vi.fn().mockResolvedValue({}) });

            await fetchWrapper('https://example.com', true, { method: 'DELETE', headers: {} });
            expect(global.fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({ method: 'DELETE' })
            );
        });

        it('uses a POST method when specified in options', async () => {
            global.fetch.mockResolvedValue({ ok: true, json: vi.fn().mockResolvedValue({}) });

            await fetchWrapper('https://example.com', true, { method: 'POST', body: '{}', headers: {} });
            expect(global.fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({ method: 'POST' })
            );
        });
    });

    describe('error responses', () => {
        it('throws when response.ok is false', async () => {
            global.fetch.mockResolvedValue({ ok: false, status: 500, statusText: 'Internal Server Error' });

            await expect(fetchWrapper('https://example.com')).rejects.toThrow('Network response was not ok');
        });

        it('attaches the response object to the thrown error', async () => {
            const mockResponse = { ok: false, status: 404, statusText: 'Not Found' };
            global.fetch.mockResolvedValue(mockResponse);

            try {
                await fetchWrapper('https://example.com');
            } catch (e) {
                expect(e.response).toBe(mockResponse);
            }
        });

        it('throws on 401 Unauthorized', async () => {
            global.fetch.mockResolvedValue({ ok: false, status: 401, statusText: 'Unauthorized' });

            await expect(fetchWrapper('https://example.com')).rejects.toThrow('Network response was not ok');
        });

        it('throws on 403 Forbidden', async () => {
            global.fetch.mockResolvedValue({ ok: false, status: 403, statusText: 'Forbidden' });

            await expect(fetchWrapper('https://example.com')).rejects.toThrow('Network response was not ok');
        });

        it('throws on network failure (fetch rejects)', async () => {
            global.fetch.mockRejectedValue(new TypeError('Failed to fetch'));

            await expect(fetchWrapper('https://example.com')).rejects.toThrow('Failed to fetch');
        });

        it('propagates the original error type on network failure', async () => {
            global.fetch.mockRejectedValue(new TypeError('Failed to fetch'));

            await expect(fetchWrapper('https://example.com')).rejects.toBeInstanceOf(TypeError);
        });
    });
});

