package net.craftminecraft.bukkit.ipwhitelist;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Small library trying to be even just a tiny bit more optimized.
 * @author Robin
 */
public class ReflectUtils {

    Map<String, Method> methods = new HashMap<String, Method>();
    Map<String, Field> fields = new HashMap<String, Field>();

    public boolean fieldExists(String fieldname, Object obj) {
        String fieldid = obj.getClass().getName() + " " + fieldname;
        if (fields.containsKey(fieldid)) {
            return true;
        }

        for (Field f : obj.getClass().getDeclaredFields()) {
            if (f.getName().equals(fieldname)) {
                fields.put(fieldid, f);
                return true;
            }
        }
        for (Field f : obj.getClass().getFields()) {
            if (f.getName().equals(fieldname)) {
                fields.put(fieldid, f);
                return true;
            }
        }
        return false;
    }

    public Object getField(String fieldname, Object obj) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        String fieldid = obj.getClass().getName() + " " + fieldname;
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (f.getName().equals(fieldname)) {
                f.setAccessible(true);
                fields.put(fieldid, f);
                return f.get(obj);
            }
        }
        for (Field f : obj.getClass().getFields()) {
            if (f.getName().equals(fieldname)) {
                f.setAccessible(true);
                fields.put(fieldid, f);
                return f.get(obj);
            }
        }
        return null;
    }

    public boolean methodExists(String methodname, Object obj, Object... args) {
        Class[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args.getClass();
        }

        String methodid = getMethodId(methodname, obj, paramTypes);

        if (methods.containsKey(methodid)) {
            return true;
        }

        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodname) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
                methods.put(methodid, m);
                return true;
            }
        }
        for (Method m : obj.getClass().getMethods()) {
            if (m.getName().equals(methodname) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
                methods.put(methodid, m);
                return true;
            }
        }

        return false;
    }

    public Object invokeMethod(String methodname, Object obj, Object... args) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args.getClass();
        }

        String methodid = getMethodId(methodname, obj, paramTypes);

        if (methods.containsKey(methodid)) {
            return methods.get(methodid).invoke(obj, args);
        }

        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodname) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
                m.setAccessible(true);
                methods.put(methodid, m);
                return m.invoke(obj, args);
            }
        }
        for (Method m : obj.getClass().getMethods()) {
            if (m.getName().equals(methodname) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
                m.setAccessible(true);
                methods.put(methodid, m);
                return m.invoke(obj, args);
            }
        }
        return null;
    }

    public String getMethodId(String methodname, Object obj, Class[] paramTypes) {
        StringBuilder b = new StringBuilder(obj.getClass().getName() + "." + methodname + "(");
        if (paramTypes.length > 0) {
            b.append(paramTypes[0].getName());
            for (int i = 1; i < paramTypes.length; i++) {
                b.append(", ");
                b.append(paramTypes[i].getName());
            }
        }
        b.append(");");
        return b.toString();
    }
}
