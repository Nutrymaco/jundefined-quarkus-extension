package com.nutrymaco.value;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.gizmo.*;
import io.quarkus.jackson.ObjectMapperCustomizer;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.quarkus.arc.processor.MethodDescriptors.OBJECT_CONSTRUCTOR;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

class JundefinedObjectMapperCustomizerClassWriter implements AutoCloseable {

    private final static String CLASS_NAME = "com.nutrymaco.value.JundefinedObjectMapperCustomizer";

    private final static MethodDescriptor CLASS_FOR_NAME = MethodDescriptor
            .ofMethod(Class.class, "forName", Class.class, String.class);

    private final static MethodDescriptor LIST_ADD = MethodDescriptor.ofMethod(
            List.class, "add", boolean.class, Object.class);

    private final static MethodDescriptor SET_UP_OBJECT_MAPPER = MethodDescriptor
            .ofMethod(ObjectMapperFactory.class, "setUpObjectMapper", void.class, ObjectMapper.class);

    public static final MethodDescriptor OBJECT_MAPPER_FACTORY_CONSTRUCTOR_FROM_TARGET_CLASSES = MethodDescriptor.
            ofConstructor(ObjectMapperFactory.class, List.class);

    private final Collection<String> targetClassNames;
    private final ClassCreator classCreator;
    
    private FieldCreator objectMapperFactoryField;
    
    JundefinedObjectMapperCustomizerClassWriter(ClassOutput classOutput, Collection<String> targetClassNames) {
        this.targetClassNames = targetClassNames;
        this.classCreator = ClassCreator.builder()
                .className(CLASS_NAME)
                .interfaces(ObjectMapperCustomizer.class)
                .classOutput(classOutput)
                .build();
    }


    void write() {
        writeAnnotations();
        writeFields();
        writeConstructor();
        writeMethods();
    }

    private void writeAnnotations() {
        classCreator.addAnnotation(ApplicationScoped.class);
    }

    private void writeFields() {
        objectMapperFactoryField = classCreator
                .getFieldCreator("objectMapperFactory", ObjectMapperFactory.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
    }

    private void writeConstructor() {
        MethodCreator constructor = classCreator.getMethodCreator("<init>", "V");

        constructor.invokeSpecialMethod(OBJECT_CONSTRUCTOR, constructor.getThis());

        ResultHandle localTargetClasses = writeCollectingTargetClassesIntoLocalVariable(constructor);

        ResultHandle newObjectMapperFactory = constructor.newInstance(
                OBJECT_MAPPER_FACTORY_CONSTRUCTOR_FROM_TARGET_CLASSES, localTargetClasses);

        constructor.writeInstanceField(objectMapperFactoryField.getFieldDescriptor(),
                constructor.getThis(), newObjectMapperFactory);

        constructor.returnValue(null);
    }

    private ResultHandle writeCollectingTargetClassesIntoLocalVariable(MethodCreator currentMethod) {
        ResultHandle localTargetClasses = currentMethod.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
        for (String targetClassName : targetClassNames) {
            ResultHandle targetClass =
                    currentMethod.invokeStaticMethod(CLASS_FOR_NAME, currentMethod.load(targetClassName));
            currentMethod.invokeInterfaceMethod(LIST_ADD, localTargetClasses, targetClass);
        }
        return localTargetClasses;
    }

    private void writeMethods() {
        writeCustomize();
    }

    private void writeCustomize() {
        MethodCreator customize = classCreator.getMethodCreator(
                "customize", void.class, ObjectMapper.class);

        ResultHandle objectMapperToCustomize = customize.getMethodParam(0);

        customize.invokeVirtualMethod(SET_UP_OBJECT_MAPPER,
                customize.readInstanceField(objectMapperFactoryField.getFieldDescriptor(), customize.getThis()),
                objectMapperToCustomize);

        customize.returnValue(null);
    }
    
    @Override
    public void close() {
        classCreator.close();
    }
}
