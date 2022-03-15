import { mount } from '@vue/test-utils';
import slider from '@/components/slider';

let wrapper;

describe('slider.vue', () => {
    beforeEach(() => {
        wrapper = mount(slider, {
            props: {
                startRange: [1990, 2022],
                rangeValues: { min: 1900, max: 2022 }
            }
        });
    });

    it("displays a slider", () => {
        // Look for value class added by the slider
        expect(wrapper.find('.noUi-target').isVisible()).toBe(true)
    });

    // Not sure how to actually trigger a change on the slider, since it's sort of outside Vue
    it("emits updates when the slider values change", async() => {
        expect(wrapper.emitted()['sliderUpdated']).toBe(undefined);
        wrapper.vm.emitInfo([1994, 2000]);
        expect(wrapper.emitted().sliderUpdated[0][0]).toEqual([1994, 2000]);
    });

});