package edu.unc.lib.boxc.common.test;

import java.lang.reflect.Field;

/**
 * Helper methods for test classes.
 *
 * @author bbpennel
 * @date Mar 21, 2014
 */
public class TestHelpers {

    /**
     * Set a field on object parent to the given object using reflection. This allows setting of injected fields
     *
     * Copyright 2013 DuraSpace, Inc.
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     * 
     * @param parent
     * @param name
     * @param obj
     */
    public static void setField(final Object parent, final String name, final Object obj) {
        /* check the parent class too if the field could not be found */
        try {
            final Field f = findField(parent.getClass(), name);
            f.setAccessible(true);
            f.set(parent, obj);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Locates the field on the given class or any of its super classes.
     *
     * @param clazz
     * @param name
     * @return
     * @throws NoSuchFieldException
     */
    private static Field findField(final Class<?> clazz, final String name) throws NoSuchFieldException {
        for (final Field f : clazz.getDeclaredFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        if (clazz.getSuperclass() == null) {
            throw new NoSuchFieldException("Field " + name + " could not be found");
        }
        return findField(clazz.getSuperclass(), name);
    }
}
