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
        const turnstileEnabled = window.turnstileEnabled === 'true';

        let challengedPages = ['searchPages'];
        if (turnstileEnabled && window.challengefullRecord === 'true') {
            challengedPages.push('displayRecords')
        }

        return turnstileEnabled && challengedPages.includes(to.name) && !shouldSkipChallenge;
    } catch (error) {
        console.log(error);
        return false;
    }
}
