package mix.client.mixui.visuals.furiah.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class Swap {
    private Swap() {
    }

    public static <T, S extends T> S upgrade(Object current, Class<T> base, Class<S> sub) {
        return null;
    }

    public static <T> T downgrade(Object current, Class<T> base) {
        return base.cast(current);
    }

    public static boolean ours(Object value, Class<?> sub) {
        return value != null && value.getClass().getName().equals(sub.getName());
    }

    public static void set(Field field, Object owner, Object value) {
        if (field != null) {
            try {
                field.setAccessible(true);
                field.set(owner, value);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    public static int getInt(Field field, Object owner) {
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.getInt(owner);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return 0;
    }

    public static void setInt(Field field, Object owner, int value) {
        if (field != null) {
            try {
                field.setAccessible(true);
                field.setInt(owner, value);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    public static void rebind(Object holder, Object from, Object to) {
        if (holder == null || from == null) {
            return;
        }
        for (Class<?> type = holder.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                    try {
                        field.setAccessible(true);
                        if (field.get(holder) == from) {
                            field.set(holder, to);
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
        }
    }

    public static Object get(Field field, Object owner) {
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(owner);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }
}
