export default {
    methods: {
        matomoSetup() {
            let _mtm = window._mtm = window._mtm || [];
            _mtm.push({'mtm.startTime': (new Date().getTime()), 'event': 'mtm.Start'});
            let d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
            g.async=true; g.src='https://analytics-qa.lib.unc.edu/js/container_tNSvp9t8.js'; s.parentNode.insertBefore(g,s);
        }
    }
}