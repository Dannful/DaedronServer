package me.dannly.plugins

import io.ktor.events.EventDefinition
import io.ktor.server.application.*
import io.ktor.util.*
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.java.KoinJavaComponent.getKoin

object KoinPlugin : BaseApplicationPlugin<Application, KoinApplication, Unit> {

    override val key: AttributeKey<Unit>
        get() = AttributeKey("Koin")

    override fun install(
        pipeline: Application,
        configure: KoinApplication.() -> Unit
    ) {
        val monitor = pipeline.environment.monitor
        val koinApplication = startKoin(appDeclaration = configure)
        monitor.raise(EventDefinition(), koinApplication)

        monitor.subscribe(ApplicationStopping) {
            monitor.raise(EventDefinition(), koinApplication)
            stopKoin()
            monitor.raise(EventDefinition(), koinApplication)
        }
    }
}

inline fun <reified T : Any> inject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = lazy { get<T>(qualifier, parameters) }


inline fun <reified T : Any> get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = getKoin().get<T>(qualifier, parameters)