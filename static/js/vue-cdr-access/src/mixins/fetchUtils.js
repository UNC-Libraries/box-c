/*
Utility to return request data from the Box-c backend  as JSON or text, and to throw errors for non-OK responses
*/
export default {
    methods: {
        async fetchWrapper(url, json_response = true, options = { method: 'GET',  headers: { 'Content-Type': 'application/json' } }) {
            const response = await fetch(url, options);
            if (!response.ok) {
                const error = new Error('Network response was not ok');
                error.response = response;
                throw error;
            }

            return json_response ? await response.json() : await response.text();
        },
    }
}