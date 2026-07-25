package dev.lucaargolo.nexo.util;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.NexoException;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public final class NexoUtils {

    private NexoUtils() {}

    public static <T> Extender<T> extend(@NotNull NexoMinecraft nexo, @NotNull Class<T> type) {
        return new Extender<>(nexo, type);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getField(@NotNull Class<?> clazz, @NotNull String name, @NotNull Object instance) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (Exception e) {
            throw new NexoException("Failed to read field " + clazz.getName() + "." + name, e);
        }
    }

    public static void setField(@NotNull Class<?> clazz, @NotNull String name, @NotNull Object instance, Object value) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            throw new NexoException("Failed to set field " + clazz.getName() + "." + name, e);
        }
    }

    public static <T> T loadPlatformClass(NexoMinecraft nexo, Class<T> clazz, Object... parameters) {
        return loadPlatformClass(nexo, null, clazz, parameters);
    }

    public static <T> T loadPlatformClass(NexoMinecraft nexo, String mod, Class<T> clazz, Object... parameters) {
        String originalName = clazz.getName();

        String commonClassPrefix = mod == null ? nexo.getPlatform() : nexo.isModLoaded(mod) ? nexo.getPlatform() : "Empty";
        String commonClassName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + commonClassPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);
        String clientClassPrefix = "Client" + commonClassPrefix;
        String clientClassName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + clientClassPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);

        if (nexo.getSide().isClient()) {
            try {
                Class<? extends T> clientPlatformClass = clazz.getClassLoader().loadClass(clientClassName).asSubclass(clazz);
                return instantiate(clientPlatformClass, parameters);
            } catch (Exception ignored) {
            }
        }
        try {
            Class<? extends T> commonPlatformClass = clazz.getClassLoader().loadClass(commonClassName).asSubclass(clazz);
            return instantiate(commonPlatformClass, parameters);
        } catch (Exception exception) {
            throw new NexoException("Failed to load platform class for " + clazz.getName(), exception);
        }
    }

    private static <T> T instantiate(Class<? extends T> type, Object[] parameters) throws ReflectiveOperationException {
        Constructor<?> selected = null;
        int selectedScore = Integer.MAX_VALUE;
        for (Constructor<?> constructor : type.getConstructors()) {
            Class<?>[] constructorTypes = constructor.getParameterTypes();
            if (constructorTypes.length != parameters.length) {
                continue;
            }
            int score = 0;
            for (int i = 0; i < constructorTypes.length; i++) {
                int parameterScore = getParameterScore(constructorTypes[i], parameters[i]);
                if (parameterScore < 0) {
                    score = -1;
                    break;
                }
                score += parameterScore;
            }
            if (score >= 0 && score < selectedScore) {
                selected = constructor;
                selectedScore = score;
            }
        }
        if (selected == null) {
            throw new NoSuchMethodException(type.getName());
        }

        Class<?>[] selectedTypes = selected.getParameterTypes();
        Object[] invocationParameters = parameters;
        for (int i = 0; i < selectedTypes.length; i++) {
            if (selectedTypes[i].isPrimitive()) {
                if (invocationParameters == parameters) {
                    invocationParameters = parameters.clone();
                }
                invocationParameters[i] = convertPrimitive(selectedTypes[i], parameters[i]);
            }
        }
        @SuppressWarnings("unchecked")
        T instance = (T) selected.newInstance(invocationParameters);
        return instance;
    }

    private static int getParameterScore(Class<?> type, Object parameter) {
        if (parameter == null) {
            return type.isPrimitive() ? -1 : 0;
        }
        return getParameterScore(type, parameter.getClass());
    }

    private static int getParameterScore(Class<?> type, Class<?> sourceType) {
        if (!type.isPrimitive()) {
            if (sourceType == boolean.class) {
                sourceType = Boolean.class;
            } else if (sourceType == byte.class) {
                sourceType = Byte.class;
            } else if (sourceType == short.class) {
                sourceType = Short.class;
            } else if (sourceType == char.class) {
                sourceType = Character.class;
            } else if (sourceType == int.class) {
                sourceType = Integer.class;
            } else if (sourceType == long.class) {
                sourceType = Long.class;
            } else if (sourceType == float.class) {
                sourceType = Float.class;
            } else if (sourceType == double.class) {
                sourceType = Double.class;
            }
            if (type == sourceType) {
                return 0;
            }
            return type.isAssignableFrom(sourceType) ? 1 : -1;
        }

        Class<?> sourcePrimitive = getPrimitiveType(sourceType);
        if (sourcePrimitive == null || type == void.class) {
            return -1;
        }
        if (type == boolean.class || sourcePrimitive == boolean.class) {
            return type == sourcePrimitive ? 2 : -1;
        }
        if (type == sourcePrimitive) {
            return 2;
        }
        if (canWidenPrimitive(sourcePrimitive, type)) {
            return 3 + primitiveRank(type) - primitiveRank(sourcePrimitive);
        }
        return 16 + Math.abs(primitiveRank(type) - primitiveRank(sourcePrimitive));
    }

    private static Class<?> getPrimitiveType(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) return boolean.class;
        if (type == byte.class || type == Byte.class) return byte.class;
        if (type == short.class || type == Short.class) return short.class;
        if (type == char.class || type == Character.class) return char.class;
        if (type == int.class || type == Integer.class) return int.class;
        if (type == long.class || type == Long.class) return long.class;
        if (type == float.class || type == Float.class) return float.class;
        if (type == double.class || type == Double.class) return double.class;
        return null;
    }

    private static boolean canWidenPrimitive(Class<?> sourceType, Class<?> targetType) {
        if (sourceType == byte.class) return targetType != char.class;
        if (sourceType == short.class) return targetType == int.class || targetType == long.class || targetType == float.class || targetType == double.class;
        if (sourceType == char.class) return targetType == int.class || targetType == long.class || targetType == float.class || targetType == double.class;
        if (sourceType == int.class) return targetType == long.class || targetType == float.class || targetType == double.class;
        if (sourceType == long.class) return targetType == float.class || targetType == double.class;
        return sourceType == float.class && targetType == double.class;
    }

    private static int primitiveRank(Class<?> type) {
        if (type == byte.class) return 0;
        if (type == short.class) return 1;
        if (type == int.class || type == char.class) return 2;
        if (type == long.class) return 3;
        if (type == float.class) return 4;
        return 5;
    }

    @SuppressWarnings("UnnecessaryBoxing")
    private static Object convertPrimitive(Class<?> type, Object parameter) {
        if (type == boolean.class || type == char.class && parameter instanceof Character) return parameter;
        if (type == char.class) return Character.valueOf((char) ((Number) parameter).intValue());

        Number number = parameter instanceof Character character ? Integer.valueOf(character) : (Number) parameter;
        if (type == byte.class) return Byte.valueOf(number.byteValue());
        if (type == short.class) return Short.valueOf(number.shortValue());
        if (type == int.class) return Integer.valueOf(number.intValue());
        if (type == long.class) return Long.valueOf(number.longValue());
        if (type == float.class) return Float.valueOf(number.floatValue());
        if (type == double.class) return Double.valueOf(number.doubleValue());
        throw new IllegalArgumentException("Unsupported primitive type: " + type.getName());
    }

    public static final class Extender<T> {

        private final NexoMinecraft nexo;
        private final Class<T> type;
        private final Set<Method> overrides = new LinkedHashSet<>();
        private DynamicType.Builder<? extends T> builder;
        private Class<? extends T> generatedClass;

        private Extender(NexoMinecraft nexo, Class<T> type) {
            this.nexo = nexo;
            this.type = type;
            if (type.isPrimitive() || type.isArray() || type.isInterface()) {
                throw new IllegalArgumentException(type.getName() + " is not a subclassable class");
            }
            if (Modifier.isFinal(type.getModifiers())) {
                throw new IllegalArgumentException(type.getName() + " is final");
            }
            if (type.isSealed()) {
                throw new IllegalArgumentException(type.getName() + " is sealed");
            }
            this.builder = new ByteBuddy().subclass(type, ConstructorStrategy.Default.IMITATE_SUPER_CLASS);
        }

        public <R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Function0<? super T, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[0], implementation, Function0.class);
        }

        public <P1, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Function1<? super T, ? super P1, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1}, implementation, Function1.class);
        }

        public <P1, P2, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Function2<? super T, ? super P1, ? super P2, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1, p2}, implementation, Function2.class);
        }

        public <P1, P2, P3, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Function3<? super T, ? super P1, ? super P2, ? super P3, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1, p2, p3}, implementation, Function3.class);
        }

        public <P1, P2, P3, P4, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Function4<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1, p2, p3, p4}, implementation, Function4.class);
        }

        public <P1, P2, P3, P4, P5, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Function5<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5}, implementation, Function5.class);
        }

        public <P1, P2, P3, P4, P5, P6, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Function6<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6}, implementation, Function6.class);
        }

        public <P1, P2, P3, P4, P5, P6, P7, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Class<P7> p7, @NotNull Function7<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6, p7}, implementation, Function7.class);
        }

        public <P1, P2, P3, P4, P5, P6, P7, P8, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Class<P7> p7, @NotNull Class<P8> p8, @NotNull Function8<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? super P8, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6, p7, p8}, implementation, Function8.class);
        }

        public <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Class<P7> p7, @NotNull Class<P8> p8, @NotNull Class<P9> p9, @NotNull Function9<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? super P8, ? super P9, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6, p7, p8, p9}, implementation, Function9.class);
        }

        public <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> Extender<T> override(@NotNull NexoUtils.At position, @NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Class<P7> p7, @NotNull Class<P8> p8, @NotNull Class<P9> p9, @NotNull Class<P10> p10, @NotNull Function10<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? super P8, ? super P9, ? super P10, ? extends R> implementation) {
            return override(position, memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6, p7, p8, p9, p10}, implementation, Function10.class);
        }

        private <F> Extender<T> override(At position, String memberName, Class<?> returnType, Class<?>[] parameterTypes, F implementation, Class<? super F> functionType) {
            ensureNotBuilt();

            for (Class<?> parameterType : parameterTypes) {
                Objects.requireNonNull(parameterType, "parameterType");
            }

            Method method = findMethod(type, memberName, returnType, parameterTypes);
            int modifiers = method.getModifiers();
            if (Modifier.isPrivate(modifiers)) {
                throw new IllegalArgumentException("Cannot override private method " + method);
            }
            if (Modifier.isStatic(modifiers)) {
                throw new IllegalArgumentException("Cannot override static method " + method);
            }
            if (Modifier.isFinal(modifiers)) {
                throw new IllegalArgumentException("Cannot override final method " + method);
            }
            if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers) && !type.getPackageName().equals(method.getDeclaringClass().getPackageName())) {
                throw new IllegalArgumentException("Cannot override package-private method " + method + " from " + type.getName());
            }
            if (position != At.REPLACE && Modifier.isAbstract(modifiers)) {
                throw new IllegalStateException("No superclass implementation is callable for " + method);
            }

            Method functionalMethod = functionType.getDeclaredMethods()[0];
            Implementation.Composable implementationCall = MethodCall.invoke(functionalMethod)
                    .on(implementation, functionType)
                    .withThis()
                    .withAllArguments()
                    .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
            if (position == At.AFTER_SUPER) {
                implementationCall = SuperMethodCall.INSTANCE.andThen(implementationCall);
            } else if (position == At.BEFORE_SUPER) {
                implementationCall = implementationCall.andThen(SuperMethodCall.INSTANCE);
            }
            return register(method, implementationCall);
        }

        public synchronized Class<? extends T> build() {
            if (generatedClass != null) {
                return generatedClass;
            }

            try {
                Class<? extends T> loaded = builder.make()
                        .load(type.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                        .getLoaded();
                generatedClass = loaded;
                return loaded;
            } catch (RuntimeException exception) {
                throw new NexoException("Failed to create subclass of " + type.getName(), exception);
            }
        }

        public T instantiate(Object... constructorArguments) {
            Objects.requireNonNull(constructorArguments, "constructorArguments");
            try {
                return NexoUtils.instantiate(build(), constructorArguments);
            } catch (ReflectiveOperationException exception) {
                throw new NexoException("Failed to instantiate subclass of " + type.getName(), exception);
            }
        }

        public T instantiate(@NotNull Class<?>[] constructorParameterTypes, Object... constructorArguments) {
            Objects.requireNonNull(constructorParameterTypes, "constructorParameterTypes");
            Objects.requireNonNull(constructorArguments, "constructorArguments");
            if (constructorParameterTypes.length != constructorArguments.length) {
                throw new IllegalArgumentException("Constructor parameter type count does not match constructor argument count");
            }

            try {
                Constructor<? extends T> constructor = build().getDeclaredConstructor(constructorParameterTypes);
                if (!constructor.canAccess(null)) {
                    constructor.setAccessible(true);
                }
                return constructor.newInstance(constructorArguments);
            } catch (ReflectiveOperationException exception) {
                throw new NexoException("Failed to instantiate subclass of " + type.getName() + " using constructor " + Arrays.toString(constructorParameterTypes), exception);
            }
        }

        private synchronized Extender<T> register(Method method, Implementation implementation) {
            ensureNotBuilt();
            Objects.requireNonNull(implementation, "implementation");
            if (!overrides.add(method)) {
                throw new IllegalStateException("An implementation is already registered for " + method);
            }

            builder = builder.method(ElementMatchers.named(method.getName())
                            .and(ElementMatchers.returns(method.getReturnType()))
                            .and(ElementMatchers.takesArguments(method.getParameterTypes())))
                    .intercept(implementation);
            return this;
        }

        private void ensureNotBuilt() {
            if (generatedClass != null) {
                throw new IllegalStateException("The subclass has already been built");
            }
        }

        private Method findMethod(Class<?> type, String memberName, Class<?> returnType, Class<?>[] parameterTypes) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                Method method = this.getFoundMethod(current, memberName, returnType, parameterTypes);
                if (method != null) {
                    return method;
                }
            }
            assert type != null;
            throw new IllegalArgumentException("No method " + returnType.getTypeName() + " " + memberName + Arrays.toString(parameterTypes) + " exists on " + type.getName());
        }

        private Method findInterfaceMethod(Class<?> type, String memberName, Class<?> returnType, Class<?>[] parameterTypes) {
            return this.getFoundMethod(type, memberName, returnType, parameterTypes);
        }

        private Method findDeclaredMethod(Class<?> owner, String memberName, Class<?> returnType, Class<?>[] parameterTypes) {
            String runtimeName = nexo.getMapping(owner, memberName, returnType, parameterTypes);
            for (Method method : owner.getDeclaredMethods()) {
                if (method.getName().equals(runtimeName) && method.getReturnType().equals(returnType) && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                    return method;
                }
            }
            return null;
        }

        @Nullable
        private Method getFoundMethod(Class<?> type, String memberName, Class<?> returnType, Class<?>[] parameterTypes) {
            Method method = findDeclaredMethod(type, memberName, returnType, parameterTypes);
            if (method != null) {
                return method;
            }
            for (Class<?> parent : type.getInterfaces()) {
                method = findInterfaceMethod(parent, memberName, returnType, parameterTypes);
                if (method != null) {
                    return method;
                }
            }
            return null;
        }
    }

    public enum At {
        REPLACE,
        AFTER_SUPER,
        BEFORE_SUPER
    }

    @FunctionalInterface public interface Function0<T, R> { R apply(T instance) throws Throwable; }
    @FunctionalInterface public interface Function1<T, P1, R> { R apply(T instance, P1 p1) throws Throwable; }
    @FunctionalInterface public interface Function2<T, P1, P2, R> { R apply(T instance, P1 p1, P2 p2) throws Throwable; }
    @FunctionalInterface public interface Function3<T, P1, P2, P3, R> { R apply(T instance, P1 p1, P2 p2, P3 p3) throws Throwable; }
    @FunctionalInterface public interface Function4<T, P1, P2, P3, P4, R> { R apply(T instance, P1 p1, P2 p2, P3 p3, P4 p4) throws Throwable; }
    @FunctionalInterface public interface Function5<T, P1, P2, P3, P4, P5, R> { R apply(T instance, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) throws Throwable; }
    @FunctionalInterface public interface Function6<T, P1, P2, P3, P4, P5, P6, R> { R apply(T instance, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6) throws Throwable; }
    @FunctionalInterface public interface Function7<T, P1, P2, P3, P4, P5, P6, P7, R> { R apply(T instance, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7) throws Throwable; }
    @FunctionalInterface public interface Function8<T, P1, P2, P3, P4, P5, P6, P7, P8, R> { R apply(T instance, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8) throws Throwable; }
    @FunctionalInterface public interface Function9<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, R> { R apply(T instance, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9) throws Throwable; }
    @FunctionalInterface public interface Function10<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> { R apply(T instance, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10) throws Throwable; }

}
