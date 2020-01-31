import { createLocalVue, shallowMount } from '@vue/test-utils'
import VueRouter from 'vue-router';
import pagination from '@/components/pagination.vue'
import routeUtils from '@/mixins/routeUtils.js';

const localVue = createLocalVue();
localVue.use(VueRouter);
const router = new VueRouter();
const gallery = 'gallery-display';
const list_display = 'list-display';
let wrapper;

describe('routeUtils', () => {
    beforeEach(() => {
        // Set wrapper using any component that uses routeUtils mixin to avoid test warnings about missing template
        wrapper = shallowMount(pagination, {
            localVue,
            router
        });
    });

    it("sets default url parameters for browse view if none are given", () => {
        const defaults = {
            rows: 20,
            start: 0,
            sort: 'title,normal',
            browse_type: list_display,
            works_only: false
        };

        let results = wrapper.vm.urlParams();

        expect(results.rows).toEqual(defaults.rows);
        expect(results.start).toEqual(defaults.start);
        expect(results.sort).toEqual(defaults.sort);
        expect(results.browse_type).toEqual(defaults.browse_type);
        expect(results.works_only).toEqual(defaults.works_only);
    });

    it("sets default url parameters for search view if none are given", () => {
        const defaults = {
            'a.setStartRow': 0,
            rows: 20,
            sort: 'default,normal',
            facetSelect: 'collection,format'
        };

        let results = wrapper.vm.urlParams({}, true);

        expect(results.rows).toEqual(defaults.rows);
        expect(results['a.setStartRow']).toEqual(defaults['a.setStartRow']);
        expect(results.sort).toEqual(defaults.sort);
        expect(results.facetSelect).toEqual(defaults.facetSelect);
    });

    it("updates url parameters for a browse view", () => {
        let defaults = {
            rows: 20,
            start: 0,
            sort: 'title,normal',
            browse_type: list_display,
            works_only: false
        };

        defaults.types = 'Work';
        let results = wrapper.vm.urlParams({types: 'Work'});

        expect(results.rows).toEqual(defaults.rows);
        expect(results.start).toEqual(defaults.start);
        expect(results.sort).toEqual(defaults.sort);
        expect(results.browse_type).toEqual(defaults.browse_type);
        expect(results.works_only).toEqual(defaults.works_only);
        expect(results.types).toEqual(defaults.types);
    });

    it("updates url parameters for a search view", () => {
        const defaults = {
            'a.setStartRow': 0,
            rows: 20,
            sort: 'default,normal',
            facetSelect: 'collection,format'
        };

        let results = wrapper.vm.urlParams({'a.setStartRow': 20}, true);

        expect(results.rows).toEqual(defaults.rows);
        expect(results['a.setStartRow']).toEqual(20);
        expect(results.sort).toEqual(defaults.sort);
        expect(results.facetSelect).toEqual(defaults.facetSelect);
    });

    it("formats a url string from an object", () => {
        const defaults = {
            rows: 20,
            start: 0,
            sort: 'title,normal',
            browse_type: gallery,
            works_only: false
        };
        let formatted = `?rows=20&start=0&sort=title%2Cnormal&browse_type=${gallery}&works_only=false`;
        expect(wrapper.vm.formatParamsString(defaults)).toEqual(formatted);
    });

    it("updates work type", () => {
        // Admin units
        expect(wrapper.vm.updateWorkType(true, false).types).toEqual('Collection');
        expect(wrapper.vm.updateWorkType(true, true).types).toEqual('Collection');

        // Works only
        wrapper.vm.$router.currentRoute.query.works_only = 'true';
        expect(wrapper.vm.updateWorkType(false, true).types).toEqual('Work');

        // All work types
        wrapper.vm.$router.currentRoute.query.works_only = 'false';
        expect(wrapper.vm.updateWorkType(false, false).types).toEqual('Work,Folder');
    });

    it("coerces works only value to a boolean from a string", () => {
        expect(wrapper.vm.coerceWorksOnly(true)).toEqual(true);
        expect(wrapper.vm.coerceWorksOnly('true')).toEqual(true);
        expect(wrapper.vm.coerceWorksOnly(false)).toEqual(false);
        expect(wrapper.vm.coerceWorksOnly('false')).toEqual(false);
    });
});