export function parseEnvBoolean(value, fallback) {
    if (value === undefined || value === null || value === '') {
        return fallback;
    }
    if (typeof value === 'boolean') {
        return value;
    }
    return String(value).trim().toLowerCase() === 'true';
}

export async function shouldRedirectToTurnstile(to, store) {
    // Can't use the fetchWrapper mixin here, as the router is not a component and doesn't have access to mixins.
    try {
        const response = await fetch('/api/userInformation', {
            method: 'HEAD',
            cache: 'no-store'
        });

        store.setUsername(response.headers.get('username'));
        store.setIsLoggedIn();
        store.setViewAdmin(response.headers.get('can-view-admin'));
        store.setUncIP(response.headers.get('unc-ip-address') === 'true');
        store.setValidToken(response.headers.get('valid-turnstile-token') === 'true');

        // Issue a challenge only for anonymous non-UNC users without a valid token.
        const shouldSkipChallenge = store.isLoggedIn || store.uncIP || store.validToken;
        const turnstileEnabled = parseEnvBoolean(window.turnstileEnabled, true);
        const challengeFullRecord = parseEnvBoolean(window.challengeFullRecord, false);

        const routeName = to?.name;
        const isSearchRoute = routeName === 'searchRecords';
        const isDisplayRoute = routeName === 'displayRecords';

        // Any route outside the challenge allowlist should always bypass turnstile.
        const canChallengeRoute = isSearchRoute || (challengeFullRecord && isDisplayRoute);
        if (!canChallengeRoute) {
            return false;
        }

        return turnstileEnabled && !shouldSkipChallenge;
    } catch (error) {
        console.log(error);
        return false;
    }
}
