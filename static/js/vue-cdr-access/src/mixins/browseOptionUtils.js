/**
 * Mixin setups up listeners and and styles browse display buttons, which aren't actually part of Vue
 * controlled DOM. See access/src/main/webapp/WEB-INF/jsp/fullRecord.jsp
 */
import routeUtils from '../mixins/routeUtils';

const gallery_display = 'gallery-display';
const structure_display = 'structure-display';
const is_selected = 'is-selected';
const browse_btns = 'browse-btns';

export default {
    mixins: [routeUtils],

    data() {
        return {
            browse_type: gallery_display
        }
    },

    methods: {
        setMode(e) {
            e.preventDefault();
            this.browse_type = e.target.id;
            this.setButtonColor();
        },

        setButtonColor() {
            let gallery = document.getElementById(gallery_display);
            let structure = document.getElementById(structure_display);

            gallery.classList.remove(is_selected);
            structure.classList.remove(is_selected);

            if (this.browse_type === gallery_display) {
                gallery.classList.add(is_selected);
            } else {
                structure.classList.add(is_selected);
            }
        },

        browseTypeFromUrl() {
            let current_url_params = this.urlParams();
            if ('browse_type' in current_url_params) {
                this.browse_type = current_url_params.browse_type;
                localStorage.setItem('dcr-browse-type', this.browse_type);
            } else {
                let browse = localStorage.getItem('dcr-browse-type');

                if (browse !== null) {
                    this.browse_type = browse;
                } else {
                    this.browse_type = 'gallery-display';
                    localStorage.setItem('dcr-browse-type', this.browse_type);
                }
            }

            this.setButtonColor();
        },

        setBrowseEvents() {
            let browse_action_btns = document.getElementById(browse_btns);
            browse_action_btns.addEventListener('click', this.setMode, false);
            browse_action_btns.addEventListener('touchstart', this.setMode, false);
        },

        clearBrowseEvents() {
            let browse_action_btns = document.getElementById(browse_btns);
            browse_action_btns.removeEventListener('click', this.setMode, false);
            browse_action_btns.removeEventListener('touchstart', this.setMode, false);
        }
    }
}