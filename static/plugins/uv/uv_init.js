(function() {
    let $UV = document.getElementById('jp2_viewer');
    $UV.setAttribute('data-url', window.location.pathname.split('/')[2]);

    try {
        let iiifurlAdaptor = new UV.IIIFURLAdaptor()
        let data = iiifurlAdaptor.getInitialData({
            manifest: 'jp2Proxy/' + $UV.dataset.url + '/jp2/manifest',
            locales: [{ name: "en-GB" }]
        });
        let viewer = UV.init('jp2_viewer', data);
        iiifurlAdaptor.bindTo(viewer);

        viewer.on('configure', function ({config, cb}) {
            cb({
                "options": {
                    "limitLocales": true,
                    "useArrowKeysToNavigate": true
                },
                "modules": {
                    "headerPanel": {
                        "options": {
                            "settingsButtonEnabled": false
                        }
                    },
                    "footerPanel": {
                        "options": {
                            "downloadEnabled": false,
                            "moreInfoEnabled": false
                        }
                    }
                }
            });
        });

        $UV.style.display = "block";
        $UV.classList.remove("not_loaded");
    } catch (e) {
        $UV.classList.remove("not_loaded");
        $UV.style.height = "30px";
        $UV.innerHTML = "<div class='error'>Sorry, an error occurred while loading the image.</div>";
        document.querySelector('body').classList.remove("full_screen");
    }
})();