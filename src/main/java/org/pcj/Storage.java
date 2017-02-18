/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import org.pcj.internal.faulttolerance.FaultTolerancePolicy;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * External class with methods do handle shared variables.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public abstract class Storage implements org.pcj.internal.storage.InternalStorage {

    private transient final Map<String, Field> sharedFields = new HashMap<>();

    private transient final Map<String, Semaphore> semaphores = new HashMap<>();
    private transient FaultTolerancePolicy policy;

    protected Storage() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Shared.class)) {
                String key = field.getName();

                if (sharedFields.containsKey(key)) {
                    throw new ArrayStoreException("Duplicate key value (" + key + ")");
                }

                if (field.getType().isPrimitive() == false
                        && Serializable.class.isAssignableFrom(field.getType()) == false) {
                    throw new ClassCastException("Field (" + key + ") is not serializable");
                }

                field.setAccessible(true);
                sharedFields.put(key, field);
                semaphores.put(field.getName(), new Semaphore(0));
            }
        }
    }

    /**
     * Gets names of all Shared variables of the Storage
     *
     * @return array with names of all Shared variables
     */
    @Override
    final public String[] getSharedFields() {
        return sharedFields.keySet().toArray(new String[0]);
    }

    private Field getField(String name) {
        final Field field = sharedFields.get(name);
        if (field == null) {
            throw new ArrayStoreException("Key not found (" + name + ")");
        }
        return field;
    }

    private Object getArrayElement(Object array, int[] indexes, int length) throws ArrayIndexOutOfBoundsException, IllegalArgumentException, ClassCastException {
        for (int index = 0; index < length; ++index) {
            if (array.getClass().isArray() == false) {
                throw new ClassCastException("Wrong dimension at point " + index + ".");
            } else if (Array.getLength(array) <= indexes[index]) {
                throw new ArrayIndexOutOfBoundsException("Wrong size at point " + index + ".");
            }
            array = Array.get(array, indexes[index]);
        }

        return array;
    }

    private boolean isAssignableFrom(String variable, Class<?> clazz, int... indexes) {
        Class<?> fieldClass = getField(variable).getType();

        for (int i = 0; i < indexes.length; ++i) {
            if (fieldClass.isArray() == false) {
                return false;
            }
            fieldClass = fieldClass.getComponentType();
        }

        if (clazz == null) {
            return !fieldClass.isPrimitive();
        }

        if (fieldClass.isAssignableFrom(clazz)) {
            return true;
        }
        if (fieldClass.isPrimitive()) {
            return (fieldClass.equals(boolean.class) && clazz.equals(Boolean.class))
                    || (fieldClass.equals(byte.class) && clazz.equals(Byte.class))
                    || (fieldClass.equals(short.class) && clazz.equals(Short.class))
                    || (fieldClass.equals(int.class) && clazz.equals(Integer.class))
                    || (fieldClass.equals(long.class) && clazz.equals(Long.class))
                    || (fieldClass.equals(float.class) && clazz.equals(Float.class))
                    || (fieldClass.equals(double.class) && clazz.equals(Double.class))
                    || (fieldClass.equals(char.class) && clazz.equals(Character.class));
        }
        return false;
    }

    /**
     * Checks if value can be assigned to variable stored in
     * Storage
     *
     * @param variable name of variable stored in Storage
     * @param value    to check
     * @return true if the value can be assigned to the
     * variable
     */
    @Override
    final public boolean isAssignable(String variable, Object value, int... indexes) {
        Class<?> clazz = null;
        if (value != null) {
            clazz = value.getClass();
        }
        return isAssignableFrom(variable, clazz, indexes);
    }

    /**
     * Returns variable from Storages
     *
     * @param variable name of Shared variable
     * @param indexes  (optional) indexes into the array
     *
     * @return value of variable[indexes] or variable if indexes omitted
     *
     * @throws ClassCastException             there is more indexes than variable dimension
     * @throws ArrayIndexOutOfBoundsException one of indexes is out of bound
     */
    @Override
    @SuppressWarnings("unchecked")
    final public <T> T get(String variable, int... indexes) throws ArrayIndexOutOfBoundsException, ClassCastException {
        try {
            final Field field = getField(variable);

            Object fieldValue;
            synchronized (field) {
                fieldValue = field.get(this);
            }

            if (indexes.length == 0) {
                return (T) fieldValue;
            } else {
                Object array = getArrayElement(fieldValue, indexes, indexes.length - 1);
                if (array.getClass().isArray() == false) {
                    throw new ClassCastException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                } else if (Array.getLength(array) <= indexes[indexes.length - 1]) {
                    throw new ArrayIndexOutOfBoundsException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                }

                return (T) Array.get(array, indexes[indexes.length - 1]);
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Puts new value of variable to Storage into the array, or as variable
     * value if indexes omitted
     *
     * @param variable name of Shared variable
     * @param newValue new value of variable
     * @param indexes  (optional) indexes into the array
     *
     * @throws ClassCastException             there is more indexes than variable dimension
     *                                        or value cannot be assigned to the variable
     * @throws ArrayIndexOutOfBoundsException one of indexes is out of bound
     */
    @Override
    final public <T> void put(String variable, T newValue, int... indexes) throws ArrayIndexOutOfBoundsException, ClassCastException {
        if (isAssignable(variable, newValue, indexes) == false) {
            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                    + " to the type of variable '" + variable + "'");
        }
        try {
            final Field field = getField(variable);

            if (indexes.length == 0) {
                field.set(this, newValue);
            } else {
                Object array = getArrayElement(field.get(this), indexes, indexes.length - 1);

                if (array == null) {
                    throw new ClassCastException("Cannot put value to " + variable + " - NullPointerException.");
                } else if (array.getClass().isArray() == false) {
                    throw new ClassCastException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                } else if (Array.getLength(array) <= indexes[indexes.length - 1]) {
                    throw new ArrayIndexOutOfBoundsException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                }

                Array.set(array, indexes[indexes.length - 1], newValue);
            }

            semaphores.get(variable).release();
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Compare and set. Atomically sets newValue when expectedValue is set in
     * variable (on specified, optional indexes). Method returns value of
     * variable before executing variable.
     *
     * @param <T>           type of variable
     * @param variable      variable name
     * @param expectedValue expected value of variable
     * @param newValue         new value for variable
     * @param indexes       optional indexes
     *
     * @return variable value before CAS
     */
    @SuppressWarnings("unchecked")
    @Override
    public final <T> T cas(String variable, T expectedValue, T newValue, int... indexes) throws ClassCastException, ArrayIndexOutOfBoundsException {
        if (isAssignable(variable, newValue, indexes) == false) {
            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                    + " to the type of variable '" + variable + "'");
        }

        try {
            final Field field = getField(variable);

            if (indexes.length == 0) {
                synchronized (field) {
                    T fieldValue = (T) field.get(this);
                    if (fieldValue == expectedValue
                            || (fieldValue != null && fieldValue.equals(expectedValue))) {
                        field.set(this, newValue);
                    }

                    semaphores.get(variable).release();

                    return fieldValue;
                }
            } else {
                Object array = getArrayElement(field.get(this), indexes, indexes.length - 1);

                if (array == null) {
                    throw new ClassCastException("Cannot put value to " + variable + " - NullPointerException.");
                } else if (array.getClass().isArray() == false) {
                    throw new ClassCastException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                } else if (Array.getLength(array) <= indexes[indexes.length - 1]) {
                    throw new ArrayIndexOutOfBoundsException("Cannot put value to " + variable + Arrays.toString(indexes) + ".");
                }

                synchronized (field) {
                    T fieldValue = (T) Array.get(array, indexes[indexes.length - 1]);

                    if (fieldValue == expectedValue
                            || (fieldValue != null && fieldValue.equals(expectedValue))) {
                        Array.set(array, indexes[indexes.length - 1], newValue);
                    }

                    semaphores.get(variable).release();

                    return fieldValue;
                }
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Tells to monitor variable. Set the field modification counter to zero.
     *
     * @param variable name of Shared variable
     */
    @Override
    final public void monitor(String variable) {
        semaphores.get(variable).drainPermits();
    }

    /**
     * Pauses current Thread and wait for modification of variable.
     * <p>
     * The same as calling waitFor method using
     * <code>waitFor(variable, 1)</code>. After modification decreases the field
     * modification counter by one.
     *
     * @param variable name of Shared variable
     */
    @Override
    final public int waitFor(String variable) {
        return waitFor(variable, 1);
    }

    /**
     * Pauses current Thread and wait for <code>count</code> modifications of
     * variable. After modification decreases the field modification counter by
     * <code>count</code>.
     *
     * @param variable name of Shared variable
     * @param count    number of modifications. If 0 - the method exits immediately.
     *
     *
     */
    @Override
    public final int waitFor(String variable, int count) {
        if (count < 0) {
            throw new IllegalArgumentException(String.format("Value count is less than zero (%d)", count));
        }

        Thread thread = Thread.currentThread();
        policy.register(thread);
        final Field field = getField(variable);

        Semaphore semaphore = semaphores.get(field.getName());
        if (count == 0) {
            semaphore.availablePermits();
        }

        try {
            semaphore.acquire(count);
        } catch (InterruptedException e) {
            semaphore.drainPermits();
            policy.failOnNewFailure();
        }

        policy.unregister(thread);

        return semaphore.availablePermits();
    }

    @Override
    public void setFaultTolerancePolicy(FaultTolerancePolicy policy) {
        this.policy = policy;
        System.out.println("Set policy for:  " + this);
    }
}
