import fetchUtils from '@/mixins/fetchUtils.js';

const { fetchWrapper } = fetchUtils.methods;

const jsonHeader = { headers: { 'Content-Type': 'application/json' } };

describe('fetchUtils', () => {
    beforeEach(() => {
        fetchMock.resetMocks();
    });

    it("returns parsed JSON when the response is ok and json_response is true", async () => {
        const data = { name: 'test' };
        fetchMock.mockResponseOnce(JSON.stringify(data), jsonHeader);

        const result = await fetchWrapper('/api/test');
        expect(result).toEqual(data);
    });

    it("returns text when json_response is false", async () => {
        fetchMock.mockResponseOnce('plain text response');
        const result = await fetchWrapper('/api/test', false);
        expect(result).toBe('plain text response');
    });

    it("throws an error when the response is not ok", async () => {
        fetchMock.mockResponseOnce('Not Found', { status: 404 });
        await expect(fetchWrapper('/api/test')).rejects.toThrow('Network response was not ok');
    });

    it("attaches the response object to the thrown error when the response is not ok", async () => {
        fetchMock.mockResponseOnce('Server Error', { status: 500 });

        let thrownError;
        try {
            await fetchWrapper('/api/test');
        } catch (error) {
            thrownError = error;
        }

        expect(thrownError.response).toBeDefined();
        expect(thrownError.response.status).toBe(500);
    });

    it("sends a GET request by default", async () => {
        fetchMock.mockResponseOnce(JSON.stringify({}), jsonHeader);
        await fetchWrapper('/api/test');
        expect(fetchMock.mock.calls[0][1].method).toBe('GET');
    });

    it("sends a request with custom options", async () => {
        const customOptions = {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key: 'value' })
        };
        fetchMock.mockResponseOnce(JSON.stringify({ created: true }), jsonHeader);

        const result = await fetchWrapper('/api/test', true, customOptions);

        expect(fetchMock.mock.calls[0][1].method).toBe('PUT');
        expect(fetchMock.mock.calls[0][1].body).toBe(JSON.stringify({ key: 'value' }));
        expect(result).toEqual({ created: true });
    });

    it("throws when the network request fails entirely", async () => {
        fetchMock.mockRejectOnce(new Error('Network Error'));
        await expect(fetchWrapper('/api/test')).rejects.toThrow('Network Error');
    });

    it("calls fetch with the provided URL", async () => {
        fetchMock.mockResponseOnce(JSON.stringify({}), jsonHeader);
        await fetchWrapper('/api/specific-endpoint');
        expect(fetchMock.mock.calls[0][0]).toBe('/api/specific-endpoint');
    });
});

