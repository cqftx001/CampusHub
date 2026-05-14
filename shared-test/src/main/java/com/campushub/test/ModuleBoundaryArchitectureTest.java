package com.campushub.test;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.campushub", importOptions = ImportOption.DoNotIncludeTests.class)
public class ModuleBoundaryArchitectureTest {

    @ArchTest
    static final ArchRule identity_impl_should_not_be_accessed_from_outside =
        noClasses()
            .that().resideOutsideOfPackages("..identity.impl..", "..bootstrap..")
            .should().dependOnClassesThat().resideInAPackage("..identity.impl..");

    @ArchTest
    static final ArchRule catalog_impl_should_not_be_accessed_from_outside =
        noClasses()
            .that().resideOutsideOfPackages("..catalog.impl..", "..bootstrap..")
            .should().dependOnClassesThat().resideInAPackage("..catalog.impl..");

    @ArchTest
    static final ArchRule trading_impl_should_not_be_accessed_from_outside =
        noClasses()
            .that().resideOutsideOfPackages("..trading.impl..", "..bootstrap..")
            .should().dependOnClassesThat().resideInAPackage("..trading.impl..");

    @ArchTest
    static final ArchRule messaging_impl_should_not_be_accessed_from_outside =
        noClasses()
            .that().resideOutsideOfPackages("..messaging.impl..", "..bootstrap..")
            .should().dependOnClassesThat().resideInAPackage("..messaging.impl..");

    @ArchTest
    static final ArchRule media_impl_should_not_be_accessed_from_outside =
        noClasses()
            .that().resideOutsideOfPackages("..media.impl..", "..bootstrap..")
            .should().dependOnClassesThat().resideInAPackage("..media.impl..");

    @ArchTest
    static final ArchRule api_packages_should_only_depend_on_shared_kernel =
        classes().that().resideInAPackage("..api..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..api..",
                "com.campushub.shared..",
                "java..",
                "javax..",
                "jakarta..",
                "lombok.."
            );

    @ArchTest
    static final ArchRule entities_should_be_package_private =
        classes().that().areAnnotatedWith(Entity.class)
            .should().bePackagePrivate();

    @ArchTest
    static final ArchRule no_cyclic_dependencies =
        slices().matching("com.campushub.(*)..").should().beFreeOfCycles();
}
