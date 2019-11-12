module.exports = {
    configureWebpack: (config) => {
        config.devtool = 'source-map'
    },

    chainWebpack: config => {
        config.optimization.minimize(false);

        if (config.plugins.has('optimize-css')) {
            config.plugins.delete('optimize-css');
        }
    }
};