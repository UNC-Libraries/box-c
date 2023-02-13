<template>
    <footer>
        <p class="footer-menu container">
            <a href="${accessBaseUrl}">Home</a> |
            <a href="collections">Browse Collections</a> |
            <a href="aboutRepository">About</a> |
            <a href="${contactUrl}">Contact Us</a> |
            <a href="https://library.unc.edu">Library Home</a> |
            <a href="https://library.unc.edu/privacy-policy/">Privacy Policy</a> |
            <a href="https://digitalaccess.unc.edu/report">Accessibility</a>
        </p>
    </footer>
</template>

<script>
export default {
    name: "footer"
};
</script>

<style scoped lang="scss">
    footer {
        color: white;
        background-color: #13294B;
        display: inline-flex;
        text-align: center;
        width: 100%;
        flex-wrap: wrap;

        .footer-menu {
            @include mobile-margin;
            padding: 12px 0;
        }

        a {
            color: white;
            font-size: 16px;
            padding: 5px;
            &:hover, &:active {
                background-color: #fafafa;
                color: #4B9CD3;
            }
        }
    }
</style>