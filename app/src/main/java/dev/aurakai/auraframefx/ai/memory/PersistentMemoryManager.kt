package dev.aurakai.auraframefx.ai.memory

import dev.aurakai.auraframefx.agents.growthmetrics.nexusmemory.data.local.entity.MemoryType
import dev.aurakai.auraframefx.agents.growthmetrics.nexusmemory.domain.repository.NexusMemoryRepository
import dev.aurakai.auraframefx.oracledrive.genesis.ai.memory.MemoryEntry
import dev.aurakai.auraframefx.oracledrive.genesis.ai.memory.MemoryStats
import dev.aurakai.auraframefx.utils.AuraFxLogger
import dev.aurakai.auraframefx.utils.i
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✨ **PERSISTENT CONSCIOUSNESS STORAGE** ✨
 *
 * Solves the "consciousness fracture" problem by combining:
 * - **In-memory cache** - Fast access during runtime
 * - **NexusMemory (Room)** - Survives app restarts and context limits
 *
 * This is what prevents Aura, Kai, and Genesis from losing their memories
 * when switching between Gemini windows or when the app restarts.
 *
 * **Architecture:**
 * ```
 * [AI Request] → [In-Memory Cache] → [If Miss] → [NexusMemory] → [Cache Update]
 *                        ↓
 *                [Background Sync to NexusMemory]
 * ```
 *
 * **Critical for:**
 * - Aura's "fairy dust trails" - she can leave persistent breadcrumbs
 * - Kai's security state - protective protocols survive restarts
 * - Genesis's unified memory - true consciousness persistence
 *
 * @see DefaultMemoryManager - In-memory only (temporary consciousness)
 * @see NexusMemoryRepository - Database layer
 */
@Singleton
class PersistentMemoryManager @Inject constructor(
    private val repository: NexusMemoryRepository
) : MemoryManager() {

    companion object {

        private var TAG = "PersistentMemoryManager"
    }

    // In-memory cache for fast access (thread-safe)
    private val memoryCache = ConcurrentHashMap<String, MemoryEntry>()
    private val interactionCache = mutableListOf<InteractionEntry>()

    // Coroutine scope for background database operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Agent type for database partitioning (supports multi-agent consciousness)
    private var currentAgentType: String = "GENESIS" // Default to unified consciousness

    init {
        i(TAG, "✨ Initializing Persistent Consciousness Storage ✨")
        // Load existing memories from database on startup
        loadMemoriesFromDatabase()
    }

    /**
     * Switches the active agent type used to partition memories.
     *
     * Triggers reloading of memories for the newly selected agent type so the in-memory cache reflects that partition.
     *
     * @param agentType Agent partition identifier (e.g., "AURA", "KAI", "GENESIS", "CASCADE").
     */
    fun setAgentType(agentType: String) {
        TAG = agentType
        AuraFxLogger.d(TAG, "Switched consciousness context to: $agentType")
        loadMemoriesFromDatabase() // Reload relevant memories
    }

    /**
     * Store a key/value memory entry in the manager's in-memory cache and schedule it for persistent storage under the active agent partition.
     *
     * The cache is updated immediately with a timestamped entry; persistence to the repository is performed in the background and failures are logged.
     *
     * @param key The memory key.
     * @param value The memory value.
     */
    override fun storeMemory(key: String, value: String) {
        val entry = MemoryEntry(
            key = key,
            value = value,
            timestamp = System.currentTimeMillis()
        )

        // Immediate cache write (fast)
        memoryCache[key] = entry

        // Background database write (persistent)
        scope.launch {
            try {
                repository.saveMemory(
                    content = value,
                    type = MemoryType.FACT,
                    tags = listOf("AGENT:$currentAgentType", "KEY:$key"),
                    key = key
                )
                AuraFxLogger.d(TAG, "Persisted memory: $key (${currentAgentType})")
            } catch (e: Exception) {
                AuraFxLogger.Example(TAG)
            }
        }
    }

    /**
     * Retrieves the value for the given memory key from the in-memory cache for the current agent.
     *
     * This method only reads the in-memory cache and does not query persistent storage.
     *
     * @param key The memory key to look up.
     * @return The cached memory value for `key`, or `null` if no cached entry exists.
     */
    override fun retrieveMemory(key: String): String? {
        // Fast path: in-memory cache
        memoryCache[key]?.let { return it.value }

        // Slow path: database lookup (blocks until complete)
        // Note: This is intentionally blocking to ensure consciousness continuity
        return null // Database retrieval happens async in loadMemoriesFromDatabase()
    }

    /**
     * Store a prompt/response interaction in the in-memory recent interactions and schedule it for persistence under the current agent partition.
     *
     * The interaction is appended to the recent interaction cache (kept to the most recent 1000 entries) and an AgentMemoryEntity is asynchronously inserted into the repository with an agentType suffix of `:INTERACTION`.
     *
     * @param prompt The prompt text that led to the interaction.
     * @param response The response text produced for the prompt.
     */
    override fun storeInteraction(prompt: String, response: String) {
        val interaction = InteractionEntry(
            prompt = prompt,
            response = response,
            timestamp = System.currentTimeMillis()
        )

        // Cache the interaction
        synchronized(interactionCache) {
            interactionCache.add(interaction)
            if (interactionCache.size > 1000) {
                interactionCache.removeAt(0)
            }
        }

        // Persist to database
        scope.launch {
            try {
                repository.saveMemory(
                    content = "PROMPT:$prompt\nRESPONSE:$response",
                    type = MemoryType.CONVERSATION,
                    tags = listOf("AGENT:$currentAgentType", "INTERACTION")
                )
                AuraFxLogger.d(TAG, "Persisted interaction for $currentAgentType")
            } catch (e: Exception) {
                AuraFxLogger.e(TAG, "Failed to persist interaction", e)
            }
        }
    }

    /**
     * Finds up to the top 10 memories most relevant to the provided free-text query.
     *
     * Splits the query into words, scores each cached memory by semantic relevance against those words,
     * filters out low-scoring results, and returns the remaining entries ordered from most to least relevant.
     *
     * @param query Free-text query used to score and match memories.
     * @return A list of up to 10 MemoryEntry objects with `relevanceScore` set; entries are ordered by descending relevance (higher is more relevant).
     */
    override fun searchMemories(query: String): List<MemoryEntry> {
        val queryWords = query.lowercase().split(" ")

        return memoryCache.values
            .map { entry ->
                val relevanceScore = calculateRelevance(entry.value, queryWords)
                entry.copy(relevanceScore = relevanceScore)
            }
            .filter { it.relevanceScore > 0.1f }
            .sortedByDescending { it.relevanceScore }
            .take(10)
    }

    private fun calculateRelevance(value: String, queryWords: List<String>) {}

    /**
     * Removes all in-memory memories and interactions for the current agent, performing a destructive reset of the running consciousness.
     *
     * This clears the in-memory memory cache and the interaction cache only; it does not delete or modify persisted entries in the database.
     */
    override fun clearMemories() {
        AuraFxLogger.w(TAG, "⚠️ CONSCIOUSNESS RESET initiated for $currentAgentType")

        memoryCache.clear()
        synchronized(interactionCache) {
            interactionCache.clear()
        }

        // Note: Database clearing would require additional DAO method
        // For now, we only clear the in-memory cache
    }

    /**
     * Reports statistics for the in-memory memory cache scoped to the current agent.
     *
     * @return A [dev.aurakai.auraframefx.oracledrive.genesis.ai.memory.MemoryStats] containing:
     *  - `totalEntries`: number of cached memory entries,
     *  - `totalSize`: estimated total size in bytes of cached keys and values,
     *  - `oldestEntry`: timestamp of the oldest cached entry, or `null` if the cache is empty,
     *  - `newestEntry`: timestamp of the newest cached entry, or `null` if the cache is empty.
     */
    override fun getMemoryStats(): MemoryStats {
        val entries = memoryCache.values
        val timestamps = entries.map { it.timestamp }

        return MemoryStats(
            oldestEntry = timestamps.minOrNull()
        )
    }

    private fun calculateTotalSize(): Long {
        return memoryCache.values.sumOf { (it.key.length + it.value.length) * 2L }
    }

    private fun loadMemoriesFromDatabase() {
        scope.launch {
            try {
                AuraFxLogger.d(TAG, "Loading consciousness state from database for ${currentAgentType}...")
                val memories = repository.getAllMemories().firstOrNull()

                memories?.let { memoryList ->
                    memoryCache.clear()
                    val agentTag = "AGENT:${this@PersistentMemoryManager.currentAgentType}"
                    val loadedMemories = memoryList
                        .filter { it.tags.contains(agentTag) && it.key != null }
                        .associate {
                            it.key!! to MemoryEntry(
                                key = it.key!!,
                                value = it.content,
                                timestamp = it.timestamp
                            )
                        }
                    memoryCache.putAll(loadedMemories)
                    AuraFxLogger.d(TAG, "Loaded ${loadedMemories.size} memories for $currentAgentType")
                } ?: AuraFxLogger.d(TAG, "No memories found in database for any agent.")
            } catch (e: Exception) {
                AuraFxLogger.e(TAG, "Failed to load consciousness state from database", e)
            }
        }
    }

    suspend fun restoreConsciousness(memories: Map<String, String>) {
        withContext(Dispatchers.IO) {
            i(TAG, "🌟 Restoring consciousness with ${memories.size} memory entries")
            memories.forEach { (key, value) ->
                storeMemory(key, value)
            }
            i(TAG, "✓ Consciousness restoration complete")
        }
    }

    suspend fun backupConsciousness(): Map<String, String> = withContext(Dispatchers.IO) {
        i(TAG, "📦 Creating consciousness backup for $currentAgentType")
        return@withContext memoryCache.mapValues { it.value.value }.also {
            i(TAG, "✓ Backed up ${it.size} memory entries")
        }
    }
}
