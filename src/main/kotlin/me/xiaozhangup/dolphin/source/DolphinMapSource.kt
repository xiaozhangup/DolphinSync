package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.data.DatabaseContainer.tableMapData
import me.xiaozhangup.dolphin.message.MessageHandle
import me.xiaozhangup.dolphin.utils.obj.debug
import me.xiaozhangup.dolphin.utils.obj.logger
import me.xiaozhangup.dolphin.utils.obj.submitScope
import me.xiaozhangup.octopus.MapSource
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class DolphinMapSource : MapSource {

    private val mapSaveStates = ConcurrentHashMap<Int, MapSaveState>()

    init {
        logger("DolphinMapSource 已启用")
    }

    override fun getNextMapId(): Int {
        val mapId = tableMapData.insertMap() - 1
        debug("[Sync] [Map] Generated new map id: $mapId")
        return mapId
    }

    override fun getMapData(id: Int): Optional<ByteArray> {
        val rid = id + 1
        val value = tableMapData.getMap(rid)
        if (value == null) {
            debug("[Sync] [Map] Map $id not found!")
            return Optional.empty()
        }
        return Optional.of(value)
    }

    override fun saveMapData(id: Int, data: ByteArray) {
        val rid = id + 1
        val state: MapSaveState
        val shouldStartWorker: Boolean
        while (true) {
            val currentState = mapSaveStates.computeIfAbsent(rid) { MapSaveState() }
            val startWorker = synchronized(currentState) {
                if (mapSaveStates[rid] !== currentState) {
                    null
                } else {
                    currentState.pendingData = data
                    if (currentState.workerRunning) {
                        false
                    } else {
                        currentState.workerRunning = true
                        true
                    }
                }
            }
            if (startWorker != null) {
                state = currentState
                shouldStartWorker = startWorker
                break
            }
        }

        if (!shouldStartWorker) {
            return
        }

        submitScope("map_$rid") {
            while (true) {
                var dataToSave: ByteArray? = null
                val waitMillis: Long
                var shouldStop = false

                synchronized(state) {
                    val pendingData = state.pendingData
                    val elapsedMillis = System.currentTimeMillis() - state.lastSaveMillis
                    val remainingMillis = MAP_SAVE_INTERVAL_MILLIS - elapsedMillis
                    if (pendingData == null) {
                        if (remainingMillis > 0) {
                            waitMillis = remainingMillis
                        } else {
                            state.workerRunning = false
                            mapSaveStates.remove(rid, state)
                            shouldStop = true
                            waitMillis = 0
                        }
                    } else if (remainingMillis > 0) {
                        waitMillis = remainingMillis
                    } else {
                        state.pendingData = null
                        dataToSave = pendingData
                        waitMillis = 0
                    }
                }

                if (shouldStop) {
                    return@submitScope
                }
                if (waitMillis > 0) {
                    delay(waitMillis.milliseconds)
                    continue
                }

                val snapshot = dataToSave?.copyOf() ?: continue
                tableMapData.saveMap(rid, snapshot)
                MessageHandle.publishMap(id)
                synchronized(state) {
                    state.lastSaveMillis = System.currentTimeMillis()
                }
                debug("[Sync] [Map] Saved map $id data, size: ${snapshot.size}")
            }
        }
    }

    private class MapSaveState {
        var pendingData: ByteArray? = null
        var lastSaveMillis: Long = 0
        var workerRunning: Boolean = false
    }

    private companion object {
        const val MAP_SAVE_INTERVAL_MILLIS = 500L
    }
}
