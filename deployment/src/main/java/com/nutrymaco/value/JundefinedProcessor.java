package com.nutrymaco.value;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.*;
import io.quarkus.jackson.ObjectMapperCustomizer;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.quarkus.arc.processor.MethodDescriptors.OBJECT_CONSTRUCTOR;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;


public class JundefinedProcessor {

    public static final String FEATURE_NAME = "jundefined";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void generateObjectMapperFactoryBean(
            BuildProducer<GeneratedBeanBuildItem> generatedBeanClasses,
            CombinedIndexBuildItem indexBuildItem) throws NoSuchMethodException, NoSuchFieldException {

        GeneratedBeanGizmoAdaptor gizmoAdaptor = new GeneratedBeanGizmoAdaptor(
                generatedBeanClasses);

        Set<String> targetClassNames = indexBuildItem.getIndex().getKnownClasses().stream()
                .filter(classInfo -> classInfo.annotations().containsKey(DotName.createSimple("com.nutrymaco.value.Undefined")))
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toSet());

        try (ClassCreator classCreator = ClassCreator.builder()
                .className("com.nutrymaco.value.JundefinedObjectMapperCustomizer")
                .interfaces(ObjectMapperCustomizer.class)
                .classOutput(gizmoAdaptor)
                .build()) {

            classCreator.addAnnotation(ApplicationScoped.class);

            FieldCreator objectMapperFactoryField = classCreator
                    .getFieldCreator("objectMapperFactory", ObjectMapperFactory.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);

            MethodCreator constructor = classCreator.getMethodCreator("<init>", "V");

            constructor.invokeSpecialMethod(OBJECT_CONSTRUCTOR, constructor.getThis());

            ResultHandle list = constructor.newInstance(
                    MethodDescriptor.ofConstructor(ArrayList.class));
            for (String targetClassName : targetClassNames) {
                ResultHandle transformerInstance = constructor.invokeStaticMethod(
                        getClassForNameMethod(), constructor.load(targetClassName));

                constructor.invokeInterfaceMethod(
                        LIST_ADD,
                        list, transformerInstance);
            }

            ResultHandle newObjectMapperFactory = constructor.newInstance(MethodDescriptor.
                    ofConstructor(ObjectMapperFactory.class, List.class), list);

            constructor.writeInstanceField(objectMapperFactoryField.getFieldDescriptor(),
                    constructor.getThis(), newObjectMapperFactory);

            constructor.returnValue(null);

            MethodCreator customize = classCreator.getMethodCreator(
                    "customize", void.class, ObjectMapper.class);

            ResultHandle objectMapperToCustomize = customize.getMethodParam(0);

            customize.invokeVirtualMethod(getSetUpObjectMapperMethod(),
                    customize.readInstanceField(objectMapperFactoryField.getFieldDescriptor(), customize.getThis()),
                    objectMapperToCustomize);

            customize.returnValue(null);
        }

    }

    private MethodDescriptor getSetUpObjectMapperMethod() throws NoSuchMethodException {
        return MethodDescriptor
                .ofMethod(ObjectMapperFactory.class.getMethod("setUpObjectMapper", ObjectMapper.class));
    }

    private MethodDescriptor getClassForNameMethod() throws NoSuchMethodException {
        return MethodDescriptor
                .ofMethod(Class.class.getMethod("forName", String.class));
    }

    private static final MethodDescriptor ITERATOR_NEXT = MethodDescriptor
            .ofMethod(
                    Iterator.class, "next", Object.class);
    private static final MethodDescriptor ITERATOR_HAS_NEXT = MethodDescriptor
            .ofMethod(Iterator.class, "hasNext", boolean.class);
    private static final MethodDescriptor LIST_ITERATOR = MethodDescriptor
            .ofMethod(List.class,
                    "iterator", Iterator.class);
    private static final MethodDescriptor STRING_IS_BLANK = MethodDescriptor
            .ofMethod(String.class,
                    "isBlank", boolean.class);
    private static final MethodDescriptor LIST_ADD = MethodDescriptor.ofMethod(
            List.class, "add",
            boolean.class, Object.class);
}
