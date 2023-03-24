import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import abstract from '@/components/full_record/abstract.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';
import cloneDeep from 'lodash.clonedeep';

const briefObject = {
    abstractText: 'Lorem ipsum dolor sit amet',
    id: '9e3b1754-8e39-494d-8cc4-44e0ddf264d9'
}

let wrapper, router;

describe('abstract.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });

        wrapper = mount(abstract, {
            global: {
                plugins: [i18n, router]
            },
            props: {
                briefObject: briefObject
            }
        });
    });

    it('show full text for short abstract', () => {
        expect(wrapper.vm.truncateAbstract).toBe(false);
        expect(wrapper.find('.abstract-text').exists()).toBe(false);
        expect(wrapper.find('.abstract').text()).toEqual(briefObject.abstractText);
    });

    it('truncates long abstracts', async () => {
        expect(wrapper.vm.truncateAbstract).toBe(false);
        let longAbstract = cloneDeep(briefObject);
        longAbstract.abstractText = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit,' +
            ' sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Id cursus metus aliquam eleifend' +
            ' mi in nulla. In hendrerit gravida rutrum quisque non. Libero volutpat sed cras ornare arcu dui. Diam' +
            ' maecenas sed enim ut sem viverra aliquet eget. Malesuada fames ac turpis egestas sed tempus. In' +
            ' egestas erat imperdiet sed euismod nisi. Ut pharetra sit amet aliquam id diam maecenas ultricies.' +
            ' Eget nullam non nisi est. Risus viverra adipiscing at in tellus integer feugiat scelerisque varius.' +
            ' Sit amet risus nullam eget felis eget nunc lobortis. Dui vivamus arcu felis bibendum ut tristique' +
            ' et egestas quis. Eget nunc scelerisque viverra mauris in aliquam sem. Facilisi nullam vehicula' +
            ' ipsum a. Odio facilisis mauris sit amet massa vitae tortor. Donec et odio pellentesque diam.' +
            ' Commodo quis imperdiet massa tincidunt. Sagittis eu volutpat odio facilisis mauris sit amet massa.' +
            'Nunc aliquet bibendum enim facilisis gravida neque convallis. Arcu non sodales neque sodales' +
            ' ut etiam. Posuere lorem ipsum dolor sit amet consectetur. Non pulvinar neque laoreet suspendisse' +
            ' interdum consectetur libero id faucibus. Netus et malesuada fames ac turpis. Sit amet mauris' +
            ' commodo quis imperdiet massa tincidunt. Venenatis urna cursus eget nunc. Aliquet lectus proin' +
            ' nibh nisl condimentum id. Pellentesque habitant morbi tristique senectus et netus et malesuada.' +
            ' Pulvinar elementum integer enim neque volutpat ac tincidunt. Sed viverra tellus in hac habitasse' +
            ' platea. Dui id ornare arcu odio ut sem. Tincidunt arcu non sodales neque sodales ut etiam sit.' +
            ' Quisque non tellus orci ac auctor augue. Vitae aliquet nec ullamcorper sit amet risus. Tempor' +
            ' nec feugiat nisl pretium. Posuere lorem ipsum dolor sit amet consectetur adipiscing elit.' +
            ' Pellentesque eu tincidunt tortor aliquam.';

        await wrapper.setProps({
            recordData: longAbstract
        });

        let show_more = wrapper.find('.abstract-text');
        expect(wrapper.vm.truncateAbstract).toBe(true);
        expect(show_more.exists()).toBe(true);
        expect(wrapper.vm.abstractLinkText).toEqual('Read more');

        // Show full abstract
        await show_more.trigger('click');
        expect(wrapper.find('.abstract').text()).toEqual(longAbstract.abstractText + '... Read less');

        // Close abstract
        await show_more.trigger('click');
        expect(wrapper.find('.abstract').text()).toEqual(longAbstract.abstractText.substring(0, 350) + '... Read more');
    });
});