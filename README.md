<h1 align="center">Kurs DI DSL</h1>

<p align="center">
  <a href="https://central.sonatype.com/artifact/kr.urbansoft.tools/kurs-di-dsl"><img src="https://img.shields.io/maven-central/v/kr.urbansoft.tools/kurs-di-dsl?color=blue&style=flat-square" alt="Maven Central"></a>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square" alt="License: MIT"></a>
</p>

> **A lightweight, zero-dependency Kotlin DI DSL for environments where heavy DI frameworks are unavailable.**

When you are building KSP (Kotlin Symbol Processing) plugins, CLI tools, or lightweight SDKs, bringing in heavy DI containers like Spring or Koin is often overkill or technically impossible.

**Kurs DI DSL** is a strictly programmatic, zero-reflection, and zero-codegen Dependency Injection tool. It provides an elegant DSL to wire your dependencies while natively supporting Clean/Hexagonal Architecture layers.

## ✨ Features

- **Zero Dependencies:** Pure Kotlin standard library. No transitive dependencies.
- **No Reflection & No Code Generation:** Fast startup without reflection or runtime scanning.
- **Auto-Resolution (Order Independent):** You don't need to manually sort your dependencies. Kurs DI automatically retries pending injections until the graph is fully resolved.
- **Architecture Semantic Layers:** Built-in semantic layers (`externalInfra`, `infra`, `outPort`, `useCase`, `inboundAdapter`) to keep your DI configuration organized.

## 📦 Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
  implementation("kr.urbansoft.tools:kurs-di-dsl:1.0.0")
}
```

## 🚀 Quick Start & Usage

Configuring your dependency graph is as simple as calling `KursDI()`, defining your layers, and calling `build()`.

Here is a real-world example from a KSP Plugin project:

```kotlin
import kr.urbansoft.tools.KursDI

val diContainer = KursDI()
  // 1. External Infrastructure (e.g., KSP Environment variables)
  .externalInfra {
    add<CodeGenerator> { codeGenerator }
    add<KSClassDeclaration> { classDeclaration }
    add<KSPLogger> { kspLogger }
  }
  // 2. Infrastructure (Databases, Stores, External Services)
  .infra {
    add { ContextAnnotationStore.create() }
    // Use `it()` to seamlessly inject previously or subsequently defined dependencies
    add { CodeGeneratorStore.create(it()) }
    add { Logger(it(), it(), it(), it()) }
  }
  // 3. Out Ports (Adapters for external communication)
  .outPort {
    add<LoadSourcePort> { LoadSourceAdapter(it()) }
    add<PrintLogPort> { PrintLogAdapter(it()) }
  }
  // 4. Use Cases (Core Business Logic)
  .useCase {
    add<CollectSourceUseCase> { CollectSourceService(it(), it()) }
    add<GenerateFileUseCase> { GenerateFileService() }
  }
  // 5. Inbound Adapters (Entry points)
  .inboundAdapter {
    add { SourceCollector(it(), it(), it(), it(), it(), it()) }
  }
  // 6. Build the graph!
  .build()

// Retrieve and use your root component
val sourceCollector = diContainer.get<SourceCollector>()
sourceCollector.collect()
```

If you don't need built-in layers, you just write this:

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
  .add<CollectSourceUseCase> { CollectSourceService(it(), it()) }
  .add<GenerateFileUseCase> { GenerateFileService() }
  .add { SourceCollector(it(), it(), it(), it(), it(), it()) }
  .build()
```

### 🧠 How it Works: The Magic of `it()`

When you register a component using `add { ... }`, the lambda provides an `Injecting` instance (commonly named `it`).
By simply invoking `it()`, the DSL automatically infers the required type and fetches it from the container.

```kotlin
// Instead of this:
add { Logger(it.get<CodeGenerator>(), it.get<KSPLogger>()) }

// You just write this:
add { Logger(it(), it()) }
```

### 🔄 Order Independence (Deferred Resolution)

Don't worry about the order of registration. If `Component A` requires `Component B`, but `B` is declared *after* `A`, Kurs DI catches it internally and defers the creation of `A` until `B` is ready.

If a circular dependency exists, it will fail fast with a clear exception message showing exactly which types could not be resolved.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](https://www.google.com/search?q=LICENSE) file for details.