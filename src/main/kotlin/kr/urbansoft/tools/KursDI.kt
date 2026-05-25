package kr.urbansoft.tools

import kotlin.reflect.KClass

class KursDI {
  val unspecified: Unspecified = Unspecified()
  private val externalInfra: ExternalInfra = ExternalInfra()
  private val infra: Infra = Infra()
  private val outPort: OutPort = OutPort()
  private val domainService: DomainService = DomainService()
  private val useCase: UseCase = UseCase()
  private val inboundAdapter: InboundAdapter = InboundAdapter()

  inline fun <reified T : Any> add(noinline factory: (Injecting) -> T): KursDI = apply {
    unspecified.store.list.add(Store.Item(T::class, factory))
  }

  fun externalInfra(block: ExternalInfra.() -> Unit): KursDI = apply { block(externalInfra) }

  fun infra(block: Infra.() -> Unit): KursDI = apply { block(infra) }

  fun outPort(block: OutPort.() -> Unit): KursDI = apply { block(outPort) }

  fun domainService(block: DomainService.() -> Unit): KursDI = apply { block(domainService) }

  fun useCase(block: UseCase.() -> Unit): KursDI = apply { block(useCase) }

  fun inboundAdapter(block: InboundAdapter.() -> Unit): KursDI = apply { block(inboundAdapter) }

  fun build(): Injected {
    val map: MutableMap<KClass<*>, Any> = mutableMapOf()
    val injecting = Injecting(map)

    var pending =
      listOf(externalInfra, infra, outPort, domainService, useCase, inboundAdapter, unspecified)
        .flatMap { it.store.list }
    while (pending.isNotEmpty()) {
      val nextPending = mutableListOf<Pair<Store.Item, DependencyNotReadyException>>()
      var progressMade = false
      for (item in pending) {
        try {
          map[item.type] = item.factory(injecting)
          progressMade = true
        } catch (e: DependencyNotReadyException) {
          nextPending.add(item to e)
        }
      }
      if (!progressMade) {
        val maybeRootCause =
          nextPending
            .filter { (_, exception) ->
              nextPending.find { it.first.type == exception.missedKClass } == null
            }
            .joinToString("\n") { (_, exception) -> "  - ${exception.message}" }
        val unresolvedDependencies =
          nextPending.joinToString("\n") { (item, exception) ->
            "  - ${item.type.qualifiedName}: ${exception.message}"
          }
        error(
          "Dependency resolution failed! A circular dependency exists, or the following objects are missing their dependencies:\n[Recommendation: Try resolving these first]\n${maybeRootCause}\n[Unresolved Dependencies]\n${unresolvedDependencies}"
        )
      }

      pending = nextPending.map { (item, _) -> item }
    }

    return Injected(injecting.map)
  }

  data class Store(val list: MutableList<Item> = mutableListOf()) {
    data class Item(val type: KClass<*>, val factory: (Injecting) -> Any)
  }

  abstract class Layer(val store: Store) {
    inline fun <reified T : Any> add(noinline factory: (Injecting) -> T) {
      store.list.add(Store.Item(T::class, factory))
    }
  }

  class Unspecified : Layer(Store())

  class ExternalInfra : Layer(Store())

  class Infra : Layer(Store())

  class OutPort : Layer(Store())

  class DomainService : Layer(Store())

  class UseCase : Layer(Store())

  class InboundAdapter : Layer(Store())

  class Injecting(val map: MutableMap<KClass<*>, Any>) {
    inline fun <reified T : Any> get(): T =
      map[T::class] as? T ?: throw DependencyNotReadyException(T::class)

    inline operator fun <reified T : Any> invoke(): T = get()
  }

  class Injected(dependencies: Map<KClass<*>, Any>) {
    val dependencies: Map<KClass<*>, Any> = dependencies.toMap()

    inline fun <reified T : Any> get(): T =
      dependencies[T::class] as? T ?: error("Dependency not found: ${T::class.qualifiedName}")

    inline operator fun <reified T : Any> invoke(): T = get()
  }
}

class DependencyNotReadyException(val missedKClass: KClass<*>) :
  RuntimeException("${missedKClass.qualifiedName} is missing", null, false, false)
