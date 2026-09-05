package dev.lucaargolo.nexo.util;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.NexoException;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethodHandle;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class Utils {

    // The generated class owns the cache, so handles cannot keep an unloaded mod class loader alive.
    private static final @NotNull ClassValue<ConcurrentHashMap<Object, Object>> SUPER_FUNCTIONS = new ClassValue<>() {
        @Override
        protected @NotNull ConcurrentHashMap<Object, Object> computeValue(@NotNull Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private Utils() {}

    public static <T> Extender<T> extend(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Class<? extends T> type) {
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

    public static <T> T loadPlatformClass(NexoMinecraft<?, ?, ?, ?> nexo, Class<? super T> clazz, Object... parameters) {
        return loadPlatformClass(nexo, null, clazz, parameters);
    }

    public static <T> T loadPlatformClass(NexoMinecraft<?, ?, ?, ?> nexo, String mod, Class<? super T> clazz, Object... parameters) {
        String originalName = clazz.getName();

        String commonClassPrefix = mod == null ? nexo.getPlatform() : nexo.isModLoaded(mod) ? nexo.getPlatform() : "Empty";
        String commonClassName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + commonClassPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);
        String clientClassPrefix = "Client" + commonClassPrefix;
        String clientClassName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + clientClassPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);

        if (nexo.getSide().isClient()) {
            try {
                Class<?> clientPlatformClass = clazz.getClassLoader().loadClass(clientClassName).asSubclass(clazz);
                return instantiate(clientPlatformClass, parameters);
            } catch (Exception ignored) {
            }
        }
        try {
            Class<?> commonPlatformClass = clazz.getClassLoader().loadClass(commonClassName).asSubclass(clazz);
            return instantiate(commonPlatformClass, parameters);
        } catch (Exception exception) {
            throw new NexoException("Failed to load platform class for " + clazz.getName(), exception);
        }
    }

    public static boolean isExtendable(@NotNull Class<?> type) {
        int modifiers = type.getModifiers();
        return !type.isPrimitive()
                && !type.isArray()
                && !type.isInterface()
                && !Modifier.isFinal(modifiers)
                && !type.isSealed();
    }

    private static <T> T instantiate(Class<?> type, Object[] parameters) throws ReflectiveOperationException {
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

    private static <F> @NotNull F asFunction(@NotNull Class<F> type, @Nullable MethodHandle superMethod, @NotNull Class<?> receiverType, @NotNull MethodType methodType) {
        ConcurrentHashMap<Object, Object> functions = SUPER_FUNCTIONS.get(receiverType);
        // Concrete handles distinguish methods with identical signatures. Empty targets may share by type;
        // their receiver-inclusive arity also uniquely determines the FunctionN interface.
        Object key = superMethod != null ? superMethod : methodType;
        Object function = functions.get(key);
        if (function == null) {
            function = functions.computeIfAbsent(key, ignored -> MethodHandleProxies.asInterfaceInstance(type,
                    superMethod != null ? superMethod : MethodHandles.empty(methodType)));
        }
        return type.cast(function);
    }

    /**
     * Builds a subclass whose overrides receive an unbound superclass callback. Each callback may omit,
     * repeat, or reorder superclass invocation and change its arguments or result. Supply the intercepted
     * instance (or another compatible generated instance) explicitly when invoking the superclass callback.
     * If no superclass or unambiguous interface default implementation exists, that callback returns the
     * JVM default value: zero/false for primitives, null for references, and null for a boxed void result.
     * Exceptions propagate unchanged. A non-void primitive override must not return null.
     * Reference arguments may be null only where the intercepted method's own contract permits it.
     * Superclass callbacks may be retained; they do not retain an instance, but retaining one can keep its
     * generated class alive. Their reuse does not make the receiver or its methods thread-safe.
     */
    public static final class Extender<T> {

        private final NexoMinecraft<?, ?, ?, ?> nexo;
        private final Class<? extends T> type;
        private final Set<Class<?>> interfaces = new LinkedHashSet<>();
        private final Set<Method> overrides = new LinkedHashSet<>();

        private DynamicType.Builder<? extends T> builder;
        private @Nullable Implementation.Composable initializer;
        private Class<? extends T> generatedClass;

        private Extender(NexoMinecraft<?, ?, ?, ?> nexo, Class<? extends T> type) {
            this.nexo = nexo;
            this.type = type;
            if (!isExtendable(type)) {
                throw new IllegalArgumentException(type.getName() + " is not a subclassable class");
            }
            this.builder = new ByteBuddy().subclass(type, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING);
        }

        public Class<? extends T> type() {
            return type;
        }

        /** Returns whether the mapped method requires an implementation rather than overriding a concrete method. */
        public boolean isAbstract(@NotNull String memberName, @NotNull Class<?> returnType, @NotNull Class<?> @NotNull ... parameterTypes) {
            return Modifier.isAbstract(findMethod(type, memberName, returnType, parameterTypes).getModifiers());
        }

        public synchronized <I> Extender<T> implement(@NotNull Class<I> interfaceType, @NotNull Function0<? super T, ? extends I> implementation) {
            ensureNotBuilt();
            Objects.requireNonNull(interfaceType, "interfaceType");
            Objects.requireNonNull(implementation, "implementation");
            if (!interfaceType.isInterface()) {
                throw new IllegalArgumentException(interfaceType.getName() + " is not an interface");
            }
            if (interfaceType.isSealed()) {
                throw new IllegalArgumentException(interfaceType.getName() + " is sealed");
            }
            if (!Modifier.isPublic(interfaceType.getModifiers()) && !type.getPackageName().equals(interfaceType.getPackageName())) {
                throw new IllegalArgumentException("Cannot implement package-private interface " + interfaceType.getName() + " from " + type.getName());
            }
            if (!interfaces.add(interfaceType)) {
                throw new IllegalStateException(interfaceType.getName() + " is already implemented");
            }

            String suffix = Integer.toString(interfaces.size());
            String fieldName = "$nexo$interface$" + suffix;
            String setterName = "$nexo$setInterface$" + suffix;
            MethodCall factoryCall = (MethodCall) MethodCall.invoke(ElementMatchers.named("apply")
                            .and(ElementMatchers.takesArguments(Object.class)))
                    .on(implementation, Function0.class)
                    .withThis()
                    .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
            Implementation.Composable initializerCall = MethodCall.invoke(ElementMatchers.named(setterName)
                            .and(ElementMatchers.takesArguments(interfaceType)))
                    .withMethodCall(factoryCall)
                    .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

            builder = builder.defineField(fieldName, interfaceType, Visibility.PRIVATE)
                    .defineMethod(setterName, void.class, Visibility.PRIVATE)
                    .withParameters(interfaceType)
                    .intercept(FieldAccessor.ofField(fieldName))
                    .implement(interfaceType)
                    .intercept(MethodDelegation.toField(fieldName));
            initializer = initializer == null ? initializerCall : initializer.andThen(initializerCall);
            return this;
        }

        public synchronized Extender<T> initialize(@NotNull Function0<? super T, ?> initializer) {
            ensureNotBuilt();
            Objects.requireNonNull(initializer, "initializer");
            MethodCall initializerCall = (MethodCall) MethodCall.invoke(ElementMatchers.named("apply")
                            .and(ElementMatchers.takesArguments(Object.class)))
                    .on(initializer, Function0.class)
                    .withThis()
                    .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
            this.initializer = this.initializer == null ? initializerCall : this.initializer.andThen(initializerCall);
            return this;
        }

        public <R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Override0<? super T, R> implementation) {
            return override(memberName, returnType, new Class<?>[0], implementation, Override0.class);
        }

        public <P1, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Override1<? super T, ? super P1, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1}, implementation, Override1.class);
        }

        public <P1, P2, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Override2<? super T, ? super P1, ? super P2, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1, p2}, implementation, Override2.class);
        }

        public <P1, P2, P3, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Override3<? super T, ? super P1, ? super P2, ? super P3, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1, p2, p3}, implementation, Override3.class);
        }

        public <P1, P2, P3, P4, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Override4<? super T, ? super P1, ? super P2, ? super P3, ? super P4, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1, p2, p3, p4}, implementation, Override4.class);
        }

        public <P1, P2, P3, P4, P5, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Override5<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5}, implementation, Override5.class);
        }

        public <P1, P2, P3, P4, P5, P6, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Override6<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6}, implementation, Override6.class);
        }

        public <P1, P2, P3, P4, P5, P6, P7, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Class<P7> p7, @NotNull Override7<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6, p7}, implementation, Override7.class);
        }

        public <P1, P2, P3, P4, P5, P6, P7, P8, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Class<P7> p7, @NotNull Class<P8> p8, @NotNull Override8<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? super P8, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6, p7, p8}, implementation, Override8.class);
        }

        public <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Class<P7> p7, @NotNull Class<P8> p8, @NotNull Class<P9> p9, @NotNull Override9<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? super P8, ? super P9, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6, p7, p8, p9}, implementation, Override9.class);
        }

        public <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<R> returnType, @NotNull Class<P1> p1, @NotNull Class<P2> p2, @NotNull Class<P3> p3, @NotNull Class<P4> p4, @NotNull Class<P5> p5, @NotNull Class<P6> p6, @NotNull Class<P7> p7, @NotNull Class<P8> p8, @NotNull Class<P9> p9, @NotNull Class<P10> p10, @NotNull Override10<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? super P8, ? super P9, ? super P10, R> implementation) {
            return override(memberName, returnType, new Class<?>[]{p1, p2, p3, p4, p5, p6, p7, p8, p9, p10}, implementation, Override10.class);
        }

        private <F> @NotNull Extender<T> override(@NotNull String memberName, @NotNull Class<?> returnType, @NotNull Class<?> @NotNull [] parameterTypes, @NotNull F implementation, @NotNull Class<?> implementationType) {
            ensureNotBuilt();
            Objects.requireNonNull(memberName, "memberName");
            Objects.requireNonNull(returnType, "returnType");
            Objects.requireNonNull(implementation, "implementation");
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
            Implementation implementationCall = MethodDelegation.withDefaultConfiguration()
                    .filter(ElementMatchers.named("intercept"))
                    .to(implementation, implementationType);
            return register(method, implementationCall);
        }

        public synchronized Class<? extends T> build() {
            if (generatedClass != null) {
                return generatedClass;
            }

            try {
                if (initializer != null) {
                    builder = builder.constructor(ElementMatchers.any()).intercept(SuperMethodCall.INSTANCE.andThen(initializer));
                }
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
                return Utils.instantiate(build(), constructorArguments);
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
                method = getFoundMethod(parent, memberName, returnType, parameterTypes);
                if (method != null) {
                    return method;
                }
            }
            return null;
        }
    }

    @FunctionalInterface public interface Function0<T, R> { @Nullable R apply(@NotNull T instance) throws Throwable; }
    @FunctionalInterface public interface Function1<T, P1, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1) throws Throwable; }
    @FunctionalInterface public interface Function2<T, P1, P2, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1, @Nullable P2 p2) throws Throwable; }
    @FunctionalInterface public interface Function3<T, P1, P2, P3, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3) throws Throwable; }
    @FunctionalInterface public interface Function4<T, P1, P2, P3, P4, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4) throws Throwable; }
    @FunctionalInterface public interface Function5<T, P1, P2, P3, P4, P5, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5) throws Throwable; }
    @FunctionalInterface public interface Function6<T, P1, P2, P3, P4, P5, P6, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6) throws Throwable; }
    @FunctionalInterface public interface Function7<T, P1, P2, P3, P4, P5, P6, P7, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6, @Nullable P7 p7) throws Throwable; }
    @FunctionalInterface public interface Function8<T, P1, P2, P3, P4, P5, P6, P7, P8, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6, @Nullable P7 p7, @Nullable P8 p8) throws Throwable; }
    @FunctionalInterface public interface Function9<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6, @Nullable P7 p7, @Nullable P8 p8, @Nullable P9 p9) throws Throwable; }
    @FunctionalInterface public interface Function10<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> { @Nullable R apply(@NotNull T instance, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6, @Nullable P7 p7, @Nullable P8 p8, @Nullable P9 p9, @Nullable P10 p10) throws Throwable; }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override0<T, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function0<? super T, ? extends R> superCall) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function0.class), superMethod, instance.getClass(), methodType));
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override1<T, P1, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function1<? super T, ? super P1, ? extends R> superCall, @Nullable P1 p1) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function1.class), superMethod, instance.getClass(), methodType), p1);
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override2<T, P1, P2, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function2<? super T, ? super P1, ? super P2, ? extends R> superCall, @Nullable P1 p1, @Nullable P2 p2) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1, @Argument(1) @Nullable P2 p2) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function2.class), superMethod, instance.getClass(), methodType), p1, p2);
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override3<T, P1, P2, P3, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function3<? super T, ? super P1, ? super P2, ? super P3, ? extends R> superCall, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1, @Argument(1) @Nullable P2 p2, @Argument(2) @Nullable P3 p3) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function3.class), superMethod, instance.getClass(), methodType), p1, p2, p3);
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override4<T, P1, P2, P3, P4, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function4<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? extends R> superCall, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1, @Argument(1) @Nullable P2 p2, @Argument(2) @Nullable P3 p3, @Argument(3) @Nullable P4 p4) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function4.class), superMethod, instance.getClass(), methodType), p1, p2, p3, p4);
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override5<T, P1, P2, P3, P4, P5, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function5<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? extends R> superCall, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1, @Argument(1) @Nullable P2 p2, @Argument(2) @Nullable P3 p3, @Argument(3) @Nullable P4 p4, @Argument(4) @Nullable P5 p5) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function5.class), superMethod, instance.getClass(), methodType), p1, p2, p3, p4, p5);
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override6<T, P1, P2, P3, P4, P5, P6, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function6<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? extends R> superCall, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1, @Argument(1) @Nullable P2 p2, @Argument(2) @Nullable P3 p3, @Argument(3) @Nullable P4 p4, @Argument(4) @Nullable P5 p5, @Argument(5) @Nullable P6 p6) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function6.class), superMethod, instance.getClass(), methodType), p1, p2, p3, p4, p5, p6);
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override7<T, P1, P2, P3, P4, P5, P6, P7, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function7<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? extends R> superCall, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6, @Nullable P7 p7) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1, @Argument(1) @Nullable P2 p2, @Argument(2) @Nullable P3 p3, @Argument(3) @Nullable P4 p4, @Argument(4) @Nullable P5 p5, @Argument(5) @Nullable P6 p6, @Argument(6) @Nullable P7 p7) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function7.class), superMethod, instance.getClass(), methodType), p1, p2, p3, p4, p5, p6, p7);
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override8<T, P1, P2, P3, P4, P5, P6, P7, P8, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function8<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? super P8, ? extends R> superCall, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6, @Nullable P7 p7, @Nullable P8 p8) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1, @Argument(1) @Nullable P2 p2, @Argument(2) @Nullable P3 p3, @Argument(3) @Nullable P4 p4, @Argument(4) @Nullable P5 p5, @Argument(5) @Nullable P6 p6, @Argument(6) @Nullable P7 p7, @Argument(7) @Nullable P8 p8) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function8.class), superMethod, instance.getClass(), methodType), p1, p2, p3, p4, p5, p6, p7, p8);
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override9<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function9<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? super P8, ? super P9, ? extends R> superCall, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6, @Nullable P7 p7, @Nullable P8 p8, @Nullable P9 p9) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1, @Argument(1) @Nullable P2 p2, @Argument(2) @Nullable P3 p3, @Argument(3) @Nullable P4 p4, @Argument(4) @Nullable P5 p5, @Argument(5) @Nullable P6 p6, @Argument(6) @Nullable P7 p7, @Argument(7) @Nullable P8 p8, @Argument(8) @Nullable P9 p9) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function9.class), superMethod, instance.getClass(), methodType), p1, p2, p3, p4, p5, p6, p7, p8, p9);
        }
    }

    /** Around-method callback; superclass invocation and lifetime follow the contract of {@link Extender}. */
    @FunctionalInterface
    public interface Override10<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> {
        @Nullable R apply(@NotNull T instance, @NotNull Function10<? super T, ? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? super P6, ? super P7, ? super P8, ? super P9, ? super P10, ? extends R> superCall, @Nullable P1 p1, @Nullable P2 p2, @Nullable P3 p3, @Nullable P4 p4, @Nullable P5 p5, @Nullable P6 p6, @Nullable P7 p7, @Nullable P8 p8, @Nullable P9 p9, @Nullable P10 p10) throws Throwable;

        default @RuntimeType @Nullable R intercept(@This @NotNull T instance, @SuperMethodHandle(nullIfImpossible = true) @Nullable MethodHandle superMethod, @Origin @NotNull MethodType methodType, @Argument(0) @Nullable P1 p1, @Argument(1) @Nullable P2 p2, @Argument(2) @Nullable P3 p3, @Argument(3) @Nullable P4 p4, @Argument(4) @Nullable P5 p5, @Argument(5) @Nullable P6 p6, @Argument(6) @Nullable P7 p7, @Argument(7) @Nullable P8 p8, @Argument(8) @Nullable P9 p9, @Argument(9) @Nullable P10 p10) throws Throwable {
            return apply(instance, asFunction(Nexo.type(Function10.class), superMethod, instance.getClass(), methodType), p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
        }
    }
}
