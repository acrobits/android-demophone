package cz.acrobits.demophone.android.services

import cz.acrobits.libsoftphone.Instance
import cz.acrobits.libsoftphone.support.TerminateTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

class DemoStateManager(private val coroutineScope: CoroutineScope)
{
    private val restartSyncMutex = Mutex()

    fun respawn()
    {
        Instance.State.respawn()
    }

    suspend fun terminate() = suspendCancellableCoroutine { continuation ->  
        val terminateTask = object : TerminateTask() {
            override fun onTerminated()
            {
                continuation.resume(true)
            }
        }

        continuation.invokeOnCancellation { 
            terminateTask.cancel()
        }

        terminateTask.execute()
    }

    fun restartSdkSync()
    {
        coroutineScope.launch {
            restartSyncMutex.withLock { 
                terminate()
                delay(1.seconds)
                respawn()
            }
        }
    }
}
