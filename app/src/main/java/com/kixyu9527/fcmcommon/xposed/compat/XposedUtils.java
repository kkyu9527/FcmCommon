package com.kixyu9527.fcmcommon.xposed.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class XposedUtils {

    public static Method tryFindMethodMostParam(ClassLoader classLoader, String className, String methodName) {
        Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
        if (clazz != null) {
            return tryFindMethodMostParam(clazz, methodName);
        }
        return null;
    }

    public static Method tryFindMethodMostParam(Class<?> clazz, String methodName) {
        Method bestMatch = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                if (bestMatch == null || method.getParameterTypes().length > bestMatch.getParameterTypes().length) {
                    bestMatch = method;
                }
            }
        }
        return bestMatch;
    }

    public static XC_MethodHook.Unhook findAndHookMethodAnyParam(
        Class<?> clazz,
        String methodName,
        XC_MethodHook callbacks,
        Object... parameterTypes
    ) {
        Method bestMatch = null;
        int matchCount = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                Class<?>[] methodParamTypes = method.getParameterTypes();
                int currentMatch = 0;
                for (int i = 0; i < Math.min(methodParamTypes.length, parameterTypes.length); i++) {
                    if (parameterTypes[i] == methodParamTypes[i]) {
                        currentMatch++;
                    }
                }
                if (currentMatch >= matchCount) {
                    matchCount = currentMatch;
                    bestMatch = method;
                }
            }
        }
        if (bestMatch == null) {
            throw new NoSuchMethodError(clazz.getName() + "#" + methodName);
        }
        return XposedBridge.hookMethod(
            XposedHelpers.findMethodExact(clazz, methodName, bestMatch.getParameterTypes()),
            callbacks
        );
    }

    public static XC_MethodHook.Unhook tryFindAndHookMethod(
        Class<?> clazz,
        String methodName,
        int parameterCount,
        XC_MethodHook callbacks
    ) {
        try {
            return findAndHookMethod(clazz, methodName, parameterCount, callbacks);
        } catch (NoSuchMethodError error) {
            return null;
        }
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
        Class<?> clazz,
        String methodName,
        int parameterCount,
        XC_MethodHook callbacks
    ) {
        Method method = findMethod(clazz, methodName, parameterCount);
        return XposedBridge.hookMethod(
            XposedHelpers.findMethodExact(clazz, methodName, method.getParameterTypes()),
            callbacks
        );
    }

    public static Method findMethod(Class<?> clazz, String methodName, int parameterCount) {
        Method method = null;
        for (Method candidate : clazz.getDeclaredMethods()) {
            if (candidate.getName().equals(methodName) && candidate.getParameterTypes().length == parameterCount) {
                method = candidate;
            }
        }
        if (method == null) {
            throw new NoSuchMethodError(clazz.getName() + "#" + methodName);
        }
        method.setAccessible(true);
        return method;
    }

    public static XC_MethodHook.Unhook findAndHookConstructorAnyParam(
        Class<?> clazz,
        XC_MethodHook callbacks,
        Class<?>... parameterTypes
    ) {
        Constructor<?> bestMatch = null;
        int matchCount = 0;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            Class<?>[] constructorParamTypes = constructor.getParameterTypes();
            int currentMatch = 0;
            for (int i = 0; i < Math.min(constructorParamTypes.length, parameterTypes.length); i++) {
                if (parameterTypes[i] == constructorParamTypes[i]) {
                    currentMatch++;
                }
            }
            if (currentMatch >= matchCount) {
                matchCount = currentMatch;
                bestMatch = constructor;
            }
        }
        if (bestMatch == null) {
            throw new NoSuchMethodError(clazz.getName());
        }
        return XposedBridge.hookMethod(
            XposedHelpers.findConstructorExact(clazz, bestMatch.getParameterTypes()),
            callbacks
        );
    }

    public static Object getObjectFieldByPath(Object object, String fieldPath) {
        Object current = object;
        for (String fieldName : fieldPath.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = XposedHelpers.getObjectField(current, fieldName);
        }
        return current;
    }
}
