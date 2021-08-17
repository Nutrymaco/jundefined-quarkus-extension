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

        try (var classWriter = new JundefinedObjectMapperCustomizerClassWriter(gizmoAdaptor, targetClassNames)) {
            classWriter.write();
        }
    }
}
