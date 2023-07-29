package io.github.caseforge.awaken.core;

import static io.github.caseforge.awaken.asm.Opcodes.AASTORE;
import static io.github.caseforge.awaken.asm.Opcodes.ACC_PRIVATE;
import static io.github.caseforge.awaken.asm.Opcodes.ACC_PUBLIC;
import static io.github.caseforge.awaken.asm.Opcodes.ACC_STATIC;
import static io.github.caseforge.awaken.asm.Opcodes.ACC_SUPER;
import static io.github.caseforge.awaken.asm.Opcodes.ACONST_NULL;
import static io.github.caseforge.awaken.asm.Opcodes.ALOAD;
import static io.github.caseforge.awaken.asm.Opcodes.ANEWARRAY;
import static io.github.caseforge.awaken.asm.Opcodes.ARETURN;
import static io.github.caseforge.awaken.asm.Opcodes.ASTORE;
import static io.github.caseforge.awaken.asm.Opcodes.BIPUSH;
import static io.github.caseforge.awaken.asm.Opcodes.CHECKCAST;
import static io.github.caseforge.awaken.asm.Opcodes.DUP;
import static io.github.caseforge.awaken.asm.Opcodes.GETFIELD;
import static io.github.caseforge.awaken.asm.Opcodes.GETSTATIC;
import static io.github.caseforge.awaken.asm.Opcodes.INVOKEINTERFACE;
import static io.github.caseforge.awaken.asm.Opcodes.INVOKESPECIAL;
import static io.github.caseforge.awaken.asm.Opcodes.INVOKESTATIC;
import static io.github.caseforge.awaken.asm.Opcodes.INVOKEVIRTUAL;
import static io.github.caseforge.awaken.asm.Opcodes.NEW;
import static io.github.caseforge.awaken.asm.Opcodes.POP;
import static io.github.caseforge.awaken.asm.Opcodes.PUTFIELD;
import static io.github.caseforge.awaken.asm.Opcodes.PUTSTATIC;
import static io.github.caseforge.awaken.asm.Opcodes.RETURN;
import static io.github.caseforge.awaken.asm.Opcodes.V1_8;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.caseforge.awaken.Bindable;
import io.github.caseforge.awaken.Enumable;
import io.github.caseforge.awaken.Provider;
import io.github.caseforge.awaken.Validator;
import io.github.caseforge.awaken.asm.ClassWriter;
import io.github.caseforge.awaken.asm.MethodVisitor;
import io.github.caseforge.awaken.asm.Type;

public class Coder extends ClassLoader {

    public static final String SUFFIX_PROXY = ".Proxy";

    public static final String SUFFIX_INVOKER = ".Invoker";

    public static final String SUFFIX_REQUEST = ".Request";

    public static final String SUFFIX_RESPONSE = ".Response";

    public static final String SUFFIX_PROVIDER = ".Provider";

    public static final String SUFFIX_VALIDATOR = ".Validator";

    private Map<String, Method> ptm = new HashMap<String, Method>();

    private Map<Method, String> mtp = new HashMap<Method, String>();

    public Class<?> getProxyType(Class<?> itf) throws Exception {
        return loadClass(itf.getName() + SUFFIX_PROXY);
    }

    public Class<?> getInvokerType(Method method) throws Exception {
        return loadClass(pkgOf(method) + SUFFIX_INVOKER);
    }

    public Class<?> getRequestType(Method method) throws Exception {
        return loadClass(pkgOf(method) + SUFFIX_REQUEST);
    }

    public Class<?> getResponseType(Method method) throws Exception {
        return loadClass(pkgOf(method) + SUFFIX_RESPONSE);
    }

    public Class<?> getProviderType(Method method) throws Exception {
        return loadClass(pkgOf(method) + SUFFIX_PROVIDER);
    }

    public Class<?> getValidatorType(Method method) throws Exception {
        return loadClass(pkgOf(method) + SUFFIX_VALIDATOR);
    }

    protected String pkgOf(Method method) {
        if (mtp.containsKey(method)) {
            return mtp.get(method);
        }

        String key = "yyds.x" + mtp.size();
        mtp.put(method, key);
        ptm.put(key, method);
        return key;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            String pkg = name.substring(0, name.lastIndexOf('.'));
            Method method = ptm.get(pkg);
            byte[] bs = null;

            if (name.endsWith(SUFFIX_REQUEST)) {
                bs = dumpRequest(pkg, method);
            } else if (name.endsWith(SUFFIX_RESPONSE)) {
                bs = dumpResponse(pkg, method);
            } else if (name.endsWith(SUFFIX_INVOKER)) {
                bs = dumpInvoker(pkg, method);
            } else if (name.endsWith(SUFFIX_PROXY)) {
                bs = dumpProxy(Class.forName(pkg));
            } else if (name.endsWith(SUFFIX_PROVIDER)) {
                bs = dumpProvider(pkg, method);
            } else if (name.endsWith(SUFFIX_VALIDATOR)) {
                bs = dumpValidator(pkg, method);
            } else {
                throw new ClassNotFoundException(name);
            }
            
            String dumpPath = System.getProperty("awaken.dump.path");
            if (dumpPath != null) {
                FileOutputStream fos = null;
                try {
                    File dir = new File(dumpPath);
                    File classFile = new File(dir, name.replace('.', '/') + ".class");
                    
                    classFile.getParentFile().mkdirs();
                    fos = new FileOutputStream(classFile);
                    fos.write(bs);
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }
            }
            return defineClass(name, bs, 0, bs.length);
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }

    }

    protected String toInternalName(String name) {
        return name.replace('.', '/');
    }

    protected String toSignature(String text) {
        if (text.indexOf("<") == -1) {
            return null;
        }
        text = text.replace(" ", "");
        StringBuilder builder = new StringBuilder();
        builder.append("L");
        char[] charArray = text.toCharArray();
        for (char c : charArray) {
            if ('.' == c) {
                builder.append('/');
            } else if ('<' == c) {
                builder.append(c);
                builder.append('L');
            } else if (',' == c) {
                builder.append(';');
                builder.append('L');
            } else if ('>' == c) {
                builder.append(';');
                builder.append(c);
            } else {
                builder.append(c);
            }
        }
        builder.append(';');
        return builder.toString();
    }

    protected String upperFirst(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /**
     * @param fieldTypeDesc
     * @return
     */
    protected int xLoad(String fieldTypeDesc) {
        if (fieldTypeDesc.length() == 1) {
            return VmOps.valueOf(fieldTypeDesc).xLoad;
        }
        return VmOps.A.xLoad;
    }

    /**
     * @param fieldTypeDesc
     * @return
     */
    protected int xReturn(String fieldTypeDesc) {
        if (fieldTypeDesc.length() == 1) {
            return VmOps.valueOf(fieldTypeDesc).xReturn;
        }
        return VmOps.A.xReturn;
    }

    /**
     * @param fieldTypeDesc
     * @return
     */
    protected int xStore(String fieldTypeDesc) {
        if (fieldTypeDesc.length() == 1) {
            return VmOps.valueOf(fieldTypeDesc).xStore;
        }
        return VmOps.A.xStore;
    }

    protected Attribute[] toAttrs(Method method) {
        int len = method.getParameterCount();
        Attribute[] attrs = new Attribute[len];

        java.lang.reflect.Type[] genericFieldTypes = method.getGenericParameterTypes();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < len; i++) {
            Parameter parameter = parameters[i];
            attrs[i] = new Attribute(parameter.getName(), parameter.getType(), Type.getDescriptor(parameter.getType()), toSignature(genericFieldTypes[i].toString()));
        }

        return attrs;
    }

    protected void unBox(Class<?> returnType, MethodVisitor mv) {
        if (Integer.TYPE.equals(returnType)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Integer.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        } else if (Byte.TYPE.equals(returnType)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Byte.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
        } else if (Short.TYPE.equals(returnType)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Short.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
        } else if (Long.TYPE.equals(returnType)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Long.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        } else if (Float.TYPE.equals(returnType)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Float.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
        } else if (Double.TYPE.equals(returnType)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Double.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
        } else if (Boolean.TYPE.equals(returnType)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Boolean.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        } else if (Character.TYPE.equals(returnType)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Character.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
        } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(returnType));
        }
    }

    private void box(Class<?> type, MethodVisitor mv) {
        if (Integer.TYPE.equals(type)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if (Byte.TYPE.equals(type)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
        } else if (Short.TYPE.equals(type)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
        } else if (Long.TYPE.equals(type)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        } else if (Float.TYPE.equals(type)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        } else if (Double.TYPE.equals(type)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if (Boolean.TYPE.equals(type)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        } else if (Character.TYPE.equals(type)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
        }
    }

    private void buildGetSet(String typeName, ClassWriter cw, Attribute attr) {
        MethodVisitor mv;
        String fieldName = attr.getName();
        String fieldTypeDesc = attr.getDescriptor();
        String fieldSignature = attr.getSignature();

        // GET
        String getSignature = (fieldSignature == null) ? null : ("()" + fieldSignature);
        mv = cw.visitMethod(ACC_PUBLIC, "get" + upperFirst(fieldName), "()" + fieldTypeDesc, getSignature, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, typeName, fieldName, fieldTypeDesc);
        mv.visitInsn(xReturn(fieldTypeDesc));
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // SET
        String setSignature = (fieldSignature == null) ? null : ("(" + fieldSignature + ")V");
        mv = cw.visitMethod(ACC_PUBLIC, "set" + upperFirst(fieldName), "(" + fieldTypeDesc + ")V", setSignature, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);

        mv.visitVarInsn(xLoad(fieldTypeDesc), 1);
        mv.visitFieldInsn(PUTFIELD, typeName, fieldName, fieldTypeDesc);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void buildConstructor(String superTypeName, ClassWriter cw) {
        MethodVisitor mv;
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superTypeName, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private byte[] dumpRequest(String pkg, Method method) throws Exception {
        String typeName = toInternalName(pkg + SUFFIX_REQUEST);
        String superTypeName = Type.getInternalName(Object.class);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv = null;

        cw.visit(V1_8, ACC_PUBLIC, typeName, null, superTypeName, new String[] { Type.getInternalName(Enumable.class) });

        Attribute[] attrs = toAttrs(method);
        int len = attrs.length;

        // 生成请求的字段
        for (int i = 0; i < len; i++) {
            Attribute attr = attrs[i];
            cw.visitField(ACC_PRIVATE, attr.getName(), attr.getDescriptor(), attr.getSignature(), null).visitEnd();
        }

        // 生成构造函数
        buildConstructor(superTypeName, cw);

        // 生成GET SET
        for (int i = 0; i < len; i++) {
            Attribute attr = attrs[i];
            buildGetSet(typeName, cw, attr);
        }

        mv = cw.visitMethod(ACC_PUBLIC, "names", "()[Ljava/lang/String;", null, null);
        mv.visitIntInsn(BIPUSH, len);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
        for (int i = 0; i < len; i++) {
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, i);
            mv.visitLdcInsn(attrs[i].getName());
            mv.visitInsn(AASTORE);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "values", "()[Ljava/lang/Object;", null, null);
        mv.visitIntInsn(BIPUSH, len);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < len; i++) {
            Attribute attr = attrs[i];
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, i);
            mv.visitIntInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, typeName, attr.getName(), attr.getDescriptor());
            box(attr.getType(), mv);
            mv.visitInsn(AASTORE);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private byte[] dumpResponse(String pkg, Method method) throws Exception {
        String typeName = toInternalName(pkg + SUFFIX_RESPONSE);
        String superTypeName = Type.getInternalName(Object.class);

        java.lang.reflect.Type genericReturnType = method.getGenericReturnType();
        Class<?> returnType = method.getReturnType();
        boolean returnVoid = Void.TYPE.equals(returnType);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv = null;

        cw.visit(V1_8, ACC_PUBLIC, typeName, null, superTypeName, new String[] { Type.getInternalName(Bindable.class) });

        int len = returnVoid ? 1 : 2;

        Attribute[] attrs = new Attribute[len];

        attrs[0] = new Attribute("code", int.class, "I", null);
        if (!returnVoid) {
            attrs[1] = new Attribute("data", returnType, Type.getDescriptor(returnType), toSignature(genericReturnType.toString()));
        }

        // 生成属性
        for (int i = 0; i < len; i++) {
            Attribute attr = attrs[i];
            cw.visitField(ACC_PRIVATE, attr.getName(), attr.getDescriptor(), attr.getSignature(), null).visitEnd();
        }

        // 生成构造方法
        buildConstructor(superTypeName, cw);

        // 生成GET SET
        for (int i = 0; i < len; i++) {
            Attribute attr = attrs[i];
            buildGetSet(typeName, cw, attr);
        }

        mv = cw.visitMethod(ACC_PUBLIC, "bind", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        if (!returnVoid) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            unBox(returnType, mv);
            mv.visitFieldInsn(PUTFIELD, typeName, attrs[1].getName(), attrs[1].getDescriptor());
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "value", "()Ljava/lang/Object;", null, null);
        mv.visitCode();
        if (returnVoid) {
            mv.visitInsn(ACONST_NULL);
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, typeName, attrs[1].getName(), attrs[1].getDescriptor());
            box(returnType, mv);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private byte[] dumpInvoker(String pkg, Method method) throws Exception {
        Class<?> serviceType = method.getDeclaringClass();
        String typeName = toInternalName(pkg + SUFFIX_INVOKER);
        String superTypeName = Type.getInternalName(AbstractInvoker.class);

        String targetTypeName = Type.getInternalName(serviceType);
        String targetTypeDesc = Type.getDescriptor(serviceType);

        String requestTypeName = toInternalName((pkg + SUFFIX_REQUEST));
        String requestTypeDesc = "L" + requestTypeName + ";";

        String responseTypeName = toInternalName((pkg + SUFFIX_RESPONSE));
        String responseTypeDesc = "L" + responseTypeName + ";";

        Attribute[] attrs = toAttrs(method);
        int len = attrs.length;

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv = null;

        cw.visit(V1_8, ACC_PUBLIC, typeName, null, superTypeName, null);

        // 生成委托字段
        cw.visitField(ACC_PRIVATE, "target", targetTypeDesc, null, null).visitEnd();

        // 生成构造函数
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superTypeName, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 生成setTarget方法
        mv = cw.visitMethod(ACC_PUBLIC, "setTarget", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, targetTypeName);
        mv.visitFieldInsn(PUTFIELD, typeName, "target", targetTypeDesc);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // 生成getRequestType方法
        mv = cw.visitMethod(ACC_PUBLIC, "getRequestType", "()Ljava/lang/Class;", "()Ljava/lang/Class<*>;", null);
        mv.visitCode();
        mv.visitLdcInsn(Type.getType(requestTypeDesc));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 生成getRequestType方法
        mv = cw.visitMethod(ACC_PUBLIC, "getResponseType", "()Ljava/lang/Class;", "()Ljava/lang/Class<*>;", null);
        mv.visitCode();
        mv.visitLdcInsn(Type.getType(responseTypeDesc));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 处理逻辑invoke
        Class<?> returnType = method.getReturnType();

        mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;", null, new String[] { "java/lang/Exception" });
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, requestTypeName);
        mv.visitVarInsn(ASTORE, 2);

        mv.visitTypeInsn(NEW, responseTypeName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, responseTypeName, "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 3);

        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, typeName, "target", targetTypeDesc);

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, requestTypeName, "get" + upperFirst(attrs[i].getName()), "()" + attrs[i].getDescriptor(), false);
            }
        }

        boolean interfaceFlag = serviceType.isInterface();
        mv.visitMethodInsn(interfaceFlag ? INVOKEINTERFACE : INVOKEVIRTUAL, targetTypeName, method.getName(), Type.getMethodDescriptor(method), interfaceFlag);

        if (!Void.TYPE.equals(returnType)) {
            mv.visitMethodInsn(INVOKEVIRTUAL, responseTypeName, "setData", "(" + Type.getDescriptor(returnType) + ")V", false);
            mv.visitVarInsn(ALOAD, 3);
        }

        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private byte[] dumpProxy(Class<?> itf) throws Exception {
        String serviceTypeName = itf.getName();

        String typeName = toInternalName(serviceTypeName + SUFFIX_PROXY);
        String superTypeName = Type.getInternalName(AbstractProxy.class);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv = null;

        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, typeName, null, superTypeName, new String[] { Type.getInternalName(itf) });

        Method[] methods = itf.getMethods();
        int methodLength = methods.length;

        String methodTypeDescriptor = Type.getDescriptor(Method.class);
        for (int i = 0; i < methodLength; i++) {
            cw.visitField(ACC_PRIVATE + ACC_STATIC, "M" + i, methodTypeDescriptor, null, null).visitEnd();
        }

        mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();

        for (int i = 0; i < methodLength; i++) {
            Method method = methods[i];
            mv.visitLdcInsn(Type.getType(itf));
            mv.visitLdcInsn(method.getName());
            Class<?>[] parameterTypes = method.getParameterTypes();
            int parameterLength = parameterTypes.length;

            mv.visitIntInsn(BIPUSH, parameterLength);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");

            for (int j = 0; j < parameterLength; j++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, j);
                Class<?> parameterType = parameterTypes[j];
                if (Integer.TYPE.equals(parameterType)) {
                    mv.visitFieldInsn(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
                } else if (Long.TYPE.equals(parameterType)) {
                    mv.visitFieldInsn(GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
                } else if (Float.TYPE.equals(parameterType)) {
                    mv.visitFieldInsn(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
                } else if (Double.TYPE.equals(parameterType)) {
                    mv.visitFieldInsn(GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
                } else if (Byte.TYPE.equals(parameterType)) {
                    mv.visitFieldInsn(GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
                } else if (Short.TYPE.equals(parameterType)) {
                    mv.visitFieldInsn(GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
                } else if (Character.TYPE.equals(parameterType)) {
                    mv.visitFieldInsn(GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
                } else if (Boolean.TYPE.equals(parameterType)) {
                    mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
                } else {
                    mv.visitLdcInsn(Type.getType(parameterType));
                }

                mv.visitInsn(AASTORE);
            }

            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
            mv.visitFieldInsn(PUTSTATIC, typeName, "M" + i, "Ljava/lang/reflect/Method;");
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // 生成构造函数
        buildConstructor(superTypeName, cw);

        for (int methodIndex = 0; methodIndex < methodLength; methodIndex++) {
            Method method = methods[methodIndex];
            buildCallProxyMethod(typeName, method, methodIndex, cw);
        }

        return cw.toByteArray();
    }

    private void buildCallProxyMethod(String typeName, Method method, int methodIndex, ClassWriter cw) {

        String methodPkg = pkgOf(method);
        String methodName = method.getName();
        Attribute[] attrs = toAttrs(method);
        int len = attrs.length;

        String requestTypeName = toInternalName((methodPkg + SUFFIX_REQUEST));

        String responseTypeName = toInternalName((methodPkg + SUFFIX_RESPONSE));
        String responseTypeDesc = "L" + responseTypeName + ";";

        String methodDescriptor = Type.getMethodDescriptor(method);
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        String[] exceptions = null;

        if (exceptionTypes != null && exceptionTypes.length > 0) {
            exceptions = new String[exceptionTypes.length];
            for (int i = 0; i < exceptionTypes.length; i++) {
                exceptions[i] = Type.getInternalName(exceptionTypes[i]);
            }
        }

        List<TypeAndIndex> parameterTypeAndIndexList = new ArrayList<TypeAndIndex>();

        int offset = 1;
        for (int i = 0; i < len; i++) {
            Attribute attr = attrs[i];
            String paramTypeDesc = attr.getDescriptor();

            TypeAndIndex typeAndIndex = new TypeAndIndex();
            typeAndIndex.setIndex(i + offset);
            typeAndIndex.setType(paramTypeDesc);
            typeAndIndex.setName(attr.getName());
            typeAndIndex.setXload(xLoad(paramTypeDesc));
            typeAndIndex.setXstore(xStore(paramTypeDesc));
            typeAndIndex.setXreturn(xReturn(paramTypeDesc));
            parameterTypeAndIndexList.add(typeAndIndex);

            if ("J".equals(paramTypeDesc) || "D".equals(paramTypeDesc)) {
                offset++;
            }
        }

        TypeAndIndex requestTypeAndIndex = new TypeAndIndex();
        requestTypeAndIndex.setIndex(len + offset);
        requestTypeAndIndex.setType(requestTypeName);

        MethodVisitor mv = null;
        mv = cw.visitMethod(ACC_PUBLIC, methodName, methodDescriptor, toSignature(methodDescriptor), exceptions);
        mv.visitCode();

        mv.visitTypeInsn(NEW, requestTypeAndIndex.getType());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, requestTypeAndIndex.getType(), "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, requestTypeAndIndex.getIndex());

        for (TypeAndIndex parameterTypeAndIndex : parameterTypeAndIndexList) {
            mv.visitVarInsn(ALOAD, requestTypeAndIndex.getIndex());
            mv.visitVarInsn(parameterTypeAndIndex.getXload(), parameterTypeAndIndex.getIndex());
            mv.visitMethodInsn(INVOKEVIRTUAL, requestTypeAndIndex.getType(), "set" + upperFirst(parameterTypeAndIndex.getName()), "(" + parameterTypeAndIndex.getType() + ")V", false);
        }

        Class<?> returnType = method.getReturnType();
        String returnTypeDescriptor = Type.getDescriptor(returnType);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETSTATIC, typeName, "M" + methodIndex, "Ljava/lang/reflect/Method;");

        mv.visitVarInsn(ALOAD, requestTypeAndIndex.getIndex());
        mv.visitLdcInsn(Type.getType(responseTypeDesc));
        mv.visitMethodInsn(INVOKEVIRTUAL, typeName, "handle", "(Ljava/lang/reflect/Method;Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;", false);

        if (Void.TYPE.equals(returnType)) {
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
        } else {
            mv.visitTypeInsn(CHECKCAST, responseTypeName);
            mv.visitMethodInsn(INVOKEVIRTUAL, responseTypeName, "getData", "()" + returnTypeDescriptor, false);
            String descriptor = Type.getDescriptor(returnType);
            if (descriptor.length() != 1) {
                mv.visitTypeInsn(CHECKCAST, Type.getInternalName(returnType));
            }
            mv.visitInsn(xReturn(returnTypeDescriptor));
        }

        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private byte[] dumpProvider(String pkg, Method method) throws Exception {
        String typeName = toInternalName(pkg + SUFFIX_PROVIDER);
        String superTypeName = Type.getInternalName(Object.class);

        String referName = "refer";
        Class<?> referType = method.getDeclaringClass();
        String referTypeDescriptor = Type.getDescriptor(referType);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv = null;

        cw.visit(V1_8, ACC_PUBLIC, typeName, null, superTypeName, new String[] { Type.getInternalName(Provider.class), Type.getInternalName(Referable.class) });

        cw.visitField(ACC_PRIVATE, referName, referTypeDescriptor, null, null).visitEnd();

        Attribute[] attrs = toAttrs(method);
        int len = attrs.length;

        // 生成请求的字段
        for (int i = 1; i < len; i++) {
            Attribute attr = attrs[i];
            cw.visitField(ACC_PRIVATE, attr.getName(), attr.getDescriptor(), attr.getSignature(), null).visitEnd();
        }

        // 生成构造函数
        buildConstructor(superTypeName, cw);

        // 生成GET SET
        for (int i = 1; i < len; i++) {
            Attribute attr = attrs[i];
            buildGetSet(typeName, cw, attr);
        }

        // 生成 void setRefer(Object value)方法
        mv = cw.visitMethod(ACC_PUBLIC, "setRefer", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);

        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(referType));
        mv.visitFieldInsn(PUTFIELD, typeName, "refer", referTypeDescriptor);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "provide", "(Ljava/lang/Object;)Ljava/lang/Object;", null, new String[] { "java/lang/Exception" });
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, typeName, referName, referTypeDescriptor);

        mv.visitVarInsn(ALOAD, 1);
        unBox(attrs[0].getType(), mv);

        for (int i = 1; i < len; i++) {
            Attribute attr = attrs[i];
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, typeName, attr.getName(), attr.getDescriptor());
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(referType), method.getName(), Type.getMethodDescriptor(method), false);

        box(method.getReturnType(), mv);

        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private byte[] dumpValidator(String pkg, Method method) throws Exception {
        String typeName = toInternalName(pkg + SUFFIX_VALIDATOR);
        String superTypeName = Type.getInternalName(Object.class);

        String referName = "refer";
        Class<?> referType = method.getDeclaringClass();
        String referTypeDescriptor = Type.getDescriptor(referType);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv = null;

        cw.visit(V1_8, ACC_PUBLIC, typeName, null, superTypeName, new String[] { Type.getInternalName(Validator.class), Type.getInternalName(Referable.class) });

        cw.visitField(ACC_PRIVATE, referName, referTypeDescriptor, null, null).visitEnd();

        Attribute[] attrs = toAttrs(method);
        int len = attrs.length;

        // 生成请求的字段
        for (int i = 1; i < len; i++) {
            Attribute attr = attrs[i];
            cw.visitField(ACC_PRIVATE, attr.getName(), attr.getDescriptor(), attr.getSignature(), null).visitEnd();
        }

        // 生成构造函数
        buildConstructor(superTypeName, cw);

        // 生成GET SET
        for (int i = 1; i < len; i++) {
            Attribute attr = attrs[i];
            buildGetSet(typeName, cw, attr);
        }

        // 生成 void setRefer(Object value)方法
        mv = cw.visitMethod(ACC_PUBLIC, "setRefer", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);

        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(referType));
        mv.visitFieldInsn(PUTFIELD, typeName, "refer", referTypeDescriptor);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "validate", "(Ljava/lang/Object;)V", null, new String[] { "java/lang/Exception" });
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, typeName, referName, referTypeDescriptor);

        mv.visitVarInsn(ALOAD, 1);
        unBox(attrs[0].getType(), mv);

        for (int i = 1; i < len; i++) {
            Attribute attr = attrs[i];
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, typeName, attr.getName(), attr.getDescriptor());
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(referType), method.getName(), Type.getMethodDescriptor(method), false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
