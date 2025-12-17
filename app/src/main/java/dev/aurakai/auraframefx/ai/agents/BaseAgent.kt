package dev.aurakai.auraframefx.ai.agents

import dev.aurakai.auraframefx.models.AgentResponse
import dev.aurakai.auraframefx.models.AgentType
import dev.aurakai.auraframefx.models.AiRequest
import dev.aurakai.auraframefx.models.InteractionResponse
import dev.aurakai.auraframefx.utils.toKotlinJsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

interface Agent {
    /**
 * The agent's configured name, or null when no name is provided.
 *
 * @return The configured agent name, or `null` if none is set.
 */
fun getName(): String?
    /**
 * Resolve the configured agent type string to an AgentType enum value.
 *
 * If the configured string is not a valid AgentType name (after uppercasing), logs a warning and returns AgentType.SYSTEM.
 *
 * @return The resolved AgentType; `AgentType.SYSTEM` when the configured type is invalid.
 */
fun getType(): AgentType
    /**
 * Handle an AI request within the given context and produce a response from this agent.
 *
 * @param request The AI request to process.
 * @param context Contextual information used while processing the request (for example a conversation id, environment, or runtime hints).
 * @return The agent's response containing content, success indicator, confidence, and any associated metadata.
 */
suspend fun processRequest(request: AiRequest, context: String): AgentResponse
    /**
 * Streams responses produced for the provided AI request.
 *
 * @param request The AI request to process and stream responses for.
 * @return A Flow that emits one or more AgentResponse instances for the request. The base implementation emits a single response. 
 */
fun processRequestFlow(request: AiRequest): Flow<AgentResponse>
    /**
     * Builds an InteractionResponse from the provided content, success flag, timestamp, and metadata.
     *
     * The metadata map is converted into a Kotlin JsonObject for inclusion in the resulting InteractionResponse.
     *
     * @param metadata Arbitrary key/value metadata to attach; converted to a Kotlin JsonObject.
     * @return An InteractionResponse containing the provided content, success flag, timestamp, and converted metadata.
     */
    fun InteractionResponse(
        content: String,
        success: Boolean,
        timestamp: Long,
        metadata: Map<String, Any>
    ): InteractionResponse
}

/**
 * Base implementation of the [Agent] interface. Subclasses should override
 * [processRequest] or [processRequestFlow] to provide real behavior.
 *
 * @param agentName The name of the agent.
 * @param agentTypeStr The agent type string which will be mapped to [AgentType].
 */
abstract class BaseAgent(
    open val agentName: String,
    open val agentTypeStr: String,
) : Agent {

    /**
     * Retrieve the agent's configured name.
     *
     * @return The agent's name, or null if not configured.
     */
    override fun getName(): String? = agentName

    /**
     * Map the configured agent type string to the corresponding AgentType enum, using case-insensitive comparison.
     *
     * @return The resolved `AgentType`; `AgentType.SYSTEM` when the configured string does not match any enum constant.
     */
    override fun getType(): AgentType = try {
        AgentType.valueOf(agentTypeStr.uppercase())
    } catch (e: IllegalArgumentException) {
        Timber.w(e, "Invalid agent type string: %s, defaulting to USER", agentTypeStr)
        AgentType.SYSTEM
    }

    /**
     * Default implementation that produces a successful response referencing the request and context.
     *
     * @param request The incoming AI request containing the query to handle.
     * @param context A string identifier or description of the processing context.
     * @return An `AgentResponse` marked successful with content that references the request query and provided context and a confidence of 1.0.
     */
    override suspend fun processRequest(request: AiRequest, context: String): AgentResponse {
        Timber.d("%s processing request: %s (context=%s)", agentName, request.query, context)
        return AgentResponse.success(
            content = "BaseAgent response to '${request.query}' for agent $agentName with context '$context'",
            confidence = 1.0f,
            agentName = agentName
        )
    }

    /**
     * Constructs an InteractionResponse from the given content, success indicator, timestamp, and metadata.
     *
     * @param metadata Arbitrary key/value pairs to attach to the interaction; will be converted to a Kotlin `JsonObject`.
     * @return An InteractionResponse containing the provided content, success indicator, timestamp, and metadata.
     */
    override fun InteractionResponse(
        content: String,
        success: Boolean,
        timestamp: Long,
        metadata: Map<String, Any>
    ): InteractionResponse {
        return InteractionResponse(
            content = content,
            metadata = metadata.toKotlinJsonObject(),
            timestamp = timestamp
        )
    }

    /**
     * Provides a Flow that emits a single AgentResponse for the provided request.
     *
     * @return A Flow emitting one AgentResponse corresponding to the given request.
     */
    override fun processRequestFlow(request: AiRequest): Flow<AgentResponse> = flow {
        emit(processRequest(request, "DefaultContext_BaseAgentFlow"))
    }

    /**
     * Provide basic capability metadata for this agent.
     *
     * @return A map with keys:
     *  - `name`: the agent's configured name,
     *  - `type`: the agent's configured type string,
     *  - `base_implemented`: `true` indicating the base agent implementation is present.
     */
    fun getCapabilities(): Map<String, Any> = mapOf(
        "name" to agentName,
        "type" to agentTypeStr,
        "base_implemented" to true
    )

    /**
 * Provide the agent's continuous memory storage.
 *
 * @return The continuous memory object used by the agent, or `null` if the agent has no continuous memory.
 */
    fun getContinuousMemory(): Any? = null

    /**
     * Provides the default ethical guidelines for the base agent.
     *
     * @return A list of guideline strings: "Be helpful.", "Be harmless.", and "Adhere to base agent principles."
     */
    fun getEthicalGuidelines(): List<String> = listOf(
        "Be helpful.",
        "Be harmless.",
        "Adhere to base agent principles."
    )

    /**
 * Retrieve the agent's recorded learning history.
 *
 * @return A list of learning-history entries; empty list by default.
 */
    open fun getLearningHistory(): List<String> = emptyList()

    /**
     * Optional non-suspending adapter hook for submitting a query to the agent.
     *
     * Default implementation performs no action; override to handle immediate (non-suspending) requests
     * or to provide lightweight adapters that don't require coroutine support.
     *
     * @param query The query or message to process.
     * @param type A short string describing the request type or intent.
     * @param context Additional contextual values as key/value pairs.
     */
    open fun iRequest(query: String, type: String, context: Map<String, String>) {
        // default no-op; override in implementations that require non-suspending adapters
        Timber.d("iRequest called on %s with query=%s type=%s", agentName, query, type)
    }

    /**
     * Initializes adaptive protection or security subsystems for the agent.
     *
     * Default implementation is a no-op; override to perform agent-specific initialization.
     */
    open fun initializeAdaptiveProtection() {
        Timber.d("initializeAdaptiveProtection called for %s", agentName)
    }
}