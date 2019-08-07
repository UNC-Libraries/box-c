/**
 * Mixin setups up listners and and styles browse display buttons, which aren't acutally part of Vue
 * controlled DOM. See access/src/main/webapp/WEB-INF/jsp/fullRecord.jsp
 * @type {string}
 */
const gallery_display = 'gallery-display';
const structure_display = 'structure-display';
const is_selected = 'is-selected';
const browse_btns = 'browse-btns';

export default {
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