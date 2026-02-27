import createFetchMock from 'vitest-fetch-mock';
import { vi } from 'vitest';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { join } from 'node:path';

const coverageTmpDir = join(process.cwd(), 'coverage', '.tmp');
const coverageDir = dirname(coverageTmpDir);
try {
    mkdirSync(coverageDir, { recursive: true });
    mkdirSync(coverageTmpDir, { recursive: true });
} catch (error) {
    // Best-effort; tests should still run even if the directory cannot be created.
}

const fetchMocker = createFetchMock(vi);

// sets globalThis.fetch and globalThis.fetchMock to our mocked version
fetchMocker.enableMocks();