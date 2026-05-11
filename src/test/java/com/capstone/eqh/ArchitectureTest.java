package com.capstone.eqh;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.capstone.eqh", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // domain 서비스·리포지터리는 global 인증·권한 로직에 의존하지 않아야 함 (CLAUDE.md 규칙)
    // 예외: domain.user.service (UserAuthService는 JWT를 직접 다루는 인증 서비스)
    // 예외: domain.*.controller (Spring Security @AuthenticationPrincipal 패턴 허용)
    @ArchTest
    static final ArchRule domainServiceDoesNotDependOnGlobalAuth =
        noClasses().that()
            .resideInAPackage("..domain..")
            .and().resideOutsideOfPackage("..domain.user.service..")
            .and().resideOutsideOfPackage("..domain..controller..")
            .should().dependOnClassesThat().resideInAPackage("..global.jwt..")
            .orShould().dependOnClassesThat().resideInAPackage("..global.oauth2..")
            .orShould().dependOnClassesThat().resideInAPackage("..global.security..")
            .because("도메인 서비스·리포지터리는 global 인증·권한 로직에 의존하면 안 됩니다");

    // domain 내부 Controller → Service → Repository 단방향 계층 강제
    // global 패키지(Spring Security 통합)가 domain을 참조하는 것은 허용
    @ArchTest
    static final ArchRule domainLayeringRule =
        layeredArchitecture().consideringAllDependencies()
            .layer("Controller").definedBy("..domain..controller..")
            .layer("Service").definedBy("..domain..service..")
            .layer("Repository").definedBy("..domain..repository..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .ignoreDependency(
                JavaClass.Predicates.resideInAPackage("..global.."),
                JavaClass.Predicates.resideInAPackage("..domain..")
            )
            .because("domain 내부에서 Controller → Service → Repository 단방향 의존성을 유지해야 합니다");

    // DTO 네이밍 규칙: 내부 클래스(중첩 DTO)는 제외하고 최상위 클래스에만 적용
    @ArchTest
    static final ArchRule requestDtoNamingRule =
        classes().that()
            .resideInAPackage("..dto.request..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("RequestDto")
            .because("request/ 패키지의 최상위 DTO 클래스명은 RequestDto로 끝나야 합니다");

    @ArchTest
    static final ArchRule responseDtoNamingRule =
        classes().that()
            .resideInAPackage("..dto.response..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("ResponseDto")
            .because("response/ 패키지의 최상위 DTO 클래스명은 ResponseDto로 끝나야 합니다");
}
