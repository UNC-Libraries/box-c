import createFetchMock from 'vitest-fetch-mock';
import { vi, beforeEach } from 'vitest';

const fetchMocker = createFetchMock(vi);

// sets globalThis.fetch and globalThis.fetchMock to our mocked version
fetchMocker.enableMocks();

beforeEach(() => {
    fetchMock.resetMocks();
    //fetchMock.mockResponse(JSON.stringify(''));
});
