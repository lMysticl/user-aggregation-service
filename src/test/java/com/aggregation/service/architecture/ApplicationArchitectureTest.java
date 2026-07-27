package com.aggregation.service.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.aggregation.service",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ApplicationArchitectureTest {

    @ArchTest
    static final ArchRule core_does_not_depend_on_outer_adapters =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "..domain..",
                            "..application.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..controller..",
                            "..dto.."
                    );

    @ArchTest
    static final ArchRule domain_has_no_framework_dependencies =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "io.micrometer..",
                            "com.mongodb.."
                    );

    @ArchTest
    static final ArchRule application_has_no_web_or_persistence_dependencies =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.web..",
                            "org.springframework.data..",
                            "jakarta.persistence..",
                            "com.mongodb.."
                    );

    @ArchTest
    static final ArchRule controllers_do_not_bypass_application_ports =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..adapter..");
}
