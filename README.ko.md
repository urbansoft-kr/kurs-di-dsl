<h1 align="center">Kurs DI DSL</h1>

<p align="center">
  <a href="https://central.sonatype.com/artifact/kr.urbansoft.tools/kurs-di-dsl"><img src="https://img.shields.io/maven-central/v/kr.urbansoft.tools/kurs-di-dsl?color=blue&style=flat-square" alt="Maven Central"></a>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square" alt="License: MIT"></a>
</p>

*다른 언어로도 이 문서의 내용을 읽을 수 있습니다: [English](README.md), [🇰🇷 한국어](README.ko.md)*

> **무거운 DI 프레임워크를 사용할 수 없는 환경을 위한 가볍고 의존성이 전혀 없는 코틀린 DI DSL**

KSP(Kotlin Symbol Processing) 플러그인, CLI 도구, 경량 SDK 등을 만들 때, Spring이나 Koin 같은 무거운 DI 컨테이너를 사용하는 것은 너무 과도하거나 기술적으로 불가능합니다.

**Kurs DI DSL**은 순수 코드 기반의 리플렉션 및 코드 생성 없는 의존성 주입 도구입니다. **Kurs DI DSL**은 클린/헥사고날 아키텍처 레이어를 지원하면서 의존성을 주입하기 위한 우아한 문법(DSL)을 제공합니다.   

## ✨ 기능

- **의존성 없음:** 순수 코틀린 표준 라이브러리만 사용하여 전이 의존성이 없습니다.
- **리플렉션 없음 & 코드 생성 없음:** 리플렉션과 런타임 스캔 없이 빠른 시작이 가능합니다.
- **순서 독립적으로 자동 주입:** 의존성 설정 순서를 수동으로 관리하지 않아도 됩니다. Kurs DI는 정의 순서에 관계없이 가능한 모든 의존성을 주입합니다.
- **아키텍처 레이어:** 내장된 아키텍처 레이어(`externalInfra`, `infra`, `outPort`, `useCase`, `inboundAdapter`)를 통해 의존성 설정 코드를 관리할 수 있습니다.

## 📦 설치

`build.gradle.kts`에 아래 내용을 추가하세요:

```kotlin
dependencies {
  implementation("kr.urbansoft.tools:kurs-di-dsl:1.0.2")
}
```

## 🚀 빠른 시작 및 사용법

KursDI()를 호출한 뒤, 의존성을 설정하고, build()를 호출하여 DI 컨테이너를 생성하세요.

아래는 실제 KSP 플러그인 프로젝트에서 가져온 예제입니다:

```kotlin
import kr.urbansoft.tools.KursDI

val diContainer = KursDI()
  // 1. 외부 인프라
  .externalInfra {
    add<CodeGenerator> { codeGenerator }
    add<KSClassDeclaration> { classDeclaration }
    add<KSPLogger> { kspLogger }
  }
  // 2. 인프라
  .infra {
    add { ContextAnnotationStore.create() }
    // `it()`을 사용하여 KursDI() 내에 정의된 의존성을 주입합니다.
    add { CodeGeneratorStore.create(it()) }
    add { Logger(it(), it(), it(), it()) }
  }
  // 3. 아웃 포트
  .outPort {
    add<LoadSourcePort> { LoadSourceAdapter(it()) }
    add<PrintLogPort> { PrintLogAdapter(it()) }
  }
  // 4. 도메인 서비스
  .domainService {
    add { DoSomethingDomainService() }
  }
  // 5. 유즈케이스
  .useCase {
    add<CollectSourceUseCase> { CollectSourceService(it(), it()) }
    add<GenerateFileUseCase> { GenerateFileService() }
  }
  // 6. 인바운드 어댑터
  .inboundAdapter {
    add { SourceCollector(it(), it(), it(), it(), it(), it()) }
  }
  // 7. 빌드
  .build()

// 빌드된 컨테이너에서 원하는 인스턴스를 꺼내 사용하세요. 
val sourceCollector = diContainer<SourceCollector>()
sourceCollector.collect()
```

내장된 아키텍처 레이어가 필요없다면, 아래와 같이 사용할 수 있습니다:

```kotlin
val diContainer = KursDI()
  .add<CodeGenerator> { codeGenerator }
  .add<KSClassDeclaration> { classDeclaration }
  .add<KSPLogger> { kspLogger }
  .add { ContextAnnotationStore.create() }
  .add { CodeGeneratorStore.create(it()) }
  .add { Logger(it(), it(), it(), it()) }
  .add<LoadSourcePort> { LoadSourceAdapter(it()) }
  .add<PrintLogPort> { PrintLogAdapter(it()) }
  .add { DoSomethingDomainService() }
  .add<CollectSourceUseCase> { CollectSourceService(it(), it()) }
  .add<GenerateFileUseCase> { GenerateFileService() }
  .add { SourceCollector(it(), it(), it(), it(), it(), it()) }
  .build()
```

### 🧠 `it()`의 마법

`add { ... }`를 이용하여 컴포넌트를 등록할 때, 람다는 `Injecting` 인스턴스(일반적으로 `it`)를 제공합니다.

생성자에 구체적인 인스턴스를 지정하는 대신, `it()`만 넣어주면 **Kurs DI DSL**이 자동으로 필요한 자료형을 추론하고 파라미터의 자료형에 맞는 인스턴스를 컨테이너에서 찾아 넣어줍니다.

```kotlin
// 이렇게 쓰는 대신:
add { Logger(it.get<CodeGenerator>(), it.get<KSPLogger>()) }

// 이렇게 써보세요!
add { Logger(it(), it()) }
```

### 🔄 순서 독립성

의존성 등록 순서에 대해 걱정하지 않아도 됩니다. 만약 `A 컴포넌트`에 `B 컴포넌트`가 필요한데, `B`가 `A`보다 *나중에* 정의되었다고 해도, **Kurs DI DSL**은 내부적으로 이를 감지하고, `B`가 준비된 후에 `A`를 생성합니다.

만약 순환 참조가 존재하거나 필요한 의존성을 찾을 수 없을 때는, 어떤 자료형이 해결되지 않았는지를 포함한 예외 메시지와 함께 DI 컨테이너 빌드에 실패합니다.

## 📄 라이센스

이 프로젝트는 MIT License 를 따릅니다 - 상세 내용은 [LICENSE](LICENSE) 파일을 확인하세요.