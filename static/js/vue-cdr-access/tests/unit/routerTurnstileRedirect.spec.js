import { beforeEach, describe, expect, it, vi } from 'vitest';
import { shouldRedirectToTurnstile } from '@/routerGuard';

describe('router turnstile guard', () => {
    let accessStore;

    beforeEach(() => {
        accessStore = {
            isLoggedIn: false,
            uncIP: false,
            validToken: false,
            username: '',
            viewAdmin: false,
            setUsername: vi.fn(function (username) {
                this.username = username || '';
            }),
            setIsLoggedIn: vi.fn(function () {
                this.isLoggedIn = this.username !== '';
            }),
            setViewAdmin: vi.fn(function (canViewAdmin) {
                this.viewAdmin = canViewAdmin === 'true';
            }),
            setUncIP: vi.fn(function (isUnc) {
                this.uncIP = isUnc;
            }),
            setValidToken: vi.fn(function (isValid) {
                this.validToken = isValid;
            })
        };

        fetchMock.resetMocks();

        window.turnstileEnabled = 'True';
    });

    it.each([
        ['logged in user', { username: 'patron' }],
        ['UNC IP user', { uncIpAddress: 'true' }],
        ['valid token user', { validTurnstileToken: 'true' }]
    ])('does not redirect for %s', async (_label, headerOverrides) => {
        fetchMock.mockResponseOnce('', {
            headers: {
                username: headerOverrides.username || '',
                'can-view-admin': 'false',
                'unc-ip-address': headerOverrides.uncIpAddress || 'false',
                'valid-turnstile-token': headerOverrides.validTurnstileToken || 'false'
            }
        });

        const result = await shouldRedirectToTurnstile({ name: 'searchRecords' }, accessStore);

        expect(result).toBe(false);
    });

    it('redirects anonymous users without UNC access or token', async () => {
        fetchMock.mockResponseOnce('', {
            headers: {
                username: '',
                'can-view-admin': 'false',
                'unc-ip-address': 'false',
                'valid-turnstile-token': 'false'
            }
        });

        const result = await shouldRedirectToTurnstile({ name: 'searchRecords' }, accessStore);

        expect(result).toBe(true);
        expect(fetchMock).toHaveBeenCalledWith('/api/userInformation', {
            method: 'HEAD',
            cache: 'no-store'
        });
    });
});

