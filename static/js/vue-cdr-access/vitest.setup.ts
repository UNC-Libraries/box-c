import { afterEach, vi } from 'vitest';
import mockAxios from 'vitest-mock-axios';

// Automatically reset the mock state after every test
afterEach(() => {
    mockAxios.reset();
});

// Explicitly tell Vitest to mock axios using the file created in step 1
vi.mock('axios');