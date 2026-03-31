import DomPurify from 'dompurify';

export default {
    methods: {
        sanitizeText(text) {
            return DomPurify.sanitize(text, { ALLOWED_TAGS: ['#text'] });
        }
    }
}