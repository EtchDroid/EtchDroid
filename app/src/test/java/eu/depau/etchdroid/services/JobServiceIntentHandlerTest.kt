package eu.depau.etchdroid.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import eu.depau.etchdroid.AppBuildConfig
import eu.depau.etchdroid.broadcasts.dto.JobProgressUpdateBroadcastDTO
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.db.dao.JobDao
import eu.depau.etchdroid.db.model.Job
import eu.depau.etchdroid.services.job.JobService
import eu.depau.etchdroid.services.job.JobServiceIntentHandler
import eu.depau.etchdroid.services.job.dto.JobServiceIntentDTO
import eu.depau.etchdroid.testutils.job.MockFailingJobAction
import eu.depau.etchdroid.testutils.job.MockJobAction
import eu.depau.etchdroid.testutils.worker.MockFailingJobWorker
import eu.depau.etchdroid.testutils.worker.MockJobWorker
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.impl.JobProcedure
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.*
import kotlin.math.abs

@RunWith(MockitoJUnitRunner::class)
internal class JobServiceIntentHandlerTest {
    private var mockService: JobService? = null
    private var jobDao: JobDao? = null
    private var random = Random()

    @Before
    fun setUp() {
        // Used by JobServiceNotificationHandler to avoid building real notifications when testing
        AppBuildConfig.TEST_BUILD = true
        // Make sure the app uses Context to send broadcasts, so it's mocked
        AppBuildConfig.USE_LOCAL_BROADCASTS = false

        // Mock eu.depau.etchdroid.services.job.JobService
        mockService = mock(JobService::class.java)
        doReturn(mock(NotificationManager::class.java))
                .`when`(mockService!!).getSystemService(Context.NOTIFICATION_SERVICE)
        doReturn("penis")
                .`when`(mockService!!).getString(anyInt())
        doNothing()
                .`when`(mockService!!).startForeground(anyInt(), any())
        doNothing()
                .`when`(mockService!!).stopForeground(anyBoolean())

        jobDao = mock(JobDao::class.java)

        // Inject mock JobRepository into database mock instance
        val db = mock(EtchDroidDatabase::class.java)
        `when`(db.jobDao())
                .thenReturn(jobDao!!)

        // Duct tape-inject database instance into database companion object
        EtchDroidDatabase::class.java
                .getDeclaredMethod("access\$setINSTANCE\$cp", EtchDroidDatabase::class.java)
                .apply { isAccessible = true }
                .invoke(null, db)
    }

    @Test
    fun testBasicFlow() {
        // Create mock job
        val jobProcedure = JobProcedure(StringResBuilder(-1)).apply {
            add(MockJobAction(StringResBuilder(0), 1.0, 0, 10, showInGUI = true, runAlways = false))
            add(MockJobAction(StringResBuilder(1), 1.0, 0, 10, showInGUI = true, runAlways = false))
        }
        val job = Job(
                jobId = abs(random.nextInt().toLong()),
                jobProcedure = jobProcedure
        )
        testFlow(job)
    }


    @Test
    fun testCheckpointFlow() {
        // Create mock job
        val jobProcedure = JobProcedure(StringResBuilder(-1)).apply {
            add(MockJobAction(StringResBuilder(0), 1.0, 0, 15, showInGUI = true, runAlways = false))
            add(MockJobAction(StringResBuilder(1), 1.0, 0, 15, showInGUI = true, runAlways = false))
            add(MockJobAction(StringResBuilder(2), 1.0, 7, 15, showInGUI = true, runAlways = false))
            add(MockJobAction(StringResBuilder(3), 1.0, 0, 15, showInGUI = true, runAlways = false))
        }
        val job = Job(
                jobId = abs(random.nextInt().toLong()),
                jobProcedure = jobProcedure,
                checkpointActionIndex = 2
        )
        testFlow(job)
    }

    @Test
    fun testCheckpointAlwaysRunFlow() {
        // Create mock job
        val jobProcedure = JobProcedure(StringResBuilder(-1)).apply {
            add(MockJobAction(StringResBuilder(0), 1.0, 0, 15, showInGUI = true, runAlways = true))
            add(MockJobAction(StringResBuilder(1), 1.0, 0, 15, showInGUI = true, runAlways = false))
            add(MockJobAction(StringResBuilder(2), 1.0, 7, 15, showInGUI = true, runAlways = false))
            add(MockJobAction(StringResBuilder(3), 1.0, 0, 15, showInGUI = true, runAlways = false))
        }
        val job = Job(
                jobId = abs(random.nextInt().toLong()),
                jobProcedure = jobProcedure,
                checkpointActionIndex = 2
        )
        testFlow(job)
    }


    @Test
    fun testErrorAlwaysRunFlow() {
        // Create mock job
        val jobProcedure = JobProcedure(StringResBuilder(-1)).apply {
            add(MockJobAction(StringResBuilder(0), 1.0, 0, 15, showInGUI = true, runAlways = false))
            add(MockFailingJobAction(StringResBuilder(1), 1.0, 0, 15, showInGUI = true, runAlways = false))
            add(MockJobAction(StringResBuilder(2), 1.0, 7, 15, showInGUI = true, runAlways = false))
            add(MockJobAction(StringResBuilder(3), 1.0, 0, 15, showInGUI = true, runAlways = true))
        }
        val job = Job(
                jobId = abs(random.nextInt().toLong()),
                jobProcedure = jobProcedure
        )
        testFlow(job)
    }

    private fun testFlow(job: Job) {

        val jobId = job.jobId

        // Add job to mock Repository
        `when`(jobDao!!.getById(jobId))
                .thenReturn(job)

        `when`(mockService!!.sendBroadcast(any()))
                .thenAnswer {
                    val intent = it.arguments[0] as Intent
                    println("))) BROADCAST (((  ${intent.getSerializableExtra(JobProgressUpdateBroadcastDTO.EXTRA)}")
                    Unit
                }

        // Pass it to the intent handler
        val intentHandler = JobServiceIntentHandler(mockService!!, JobServiceIntentDTO(jobId))
        intentHandler.handle()

        val failingWorkerIndex = job.jobProcedure.indexOfFirst { it is MockFailingJobAction }

        // Check whether everything was called, in order
        val mockWorkers = job.jobProcedure
                .filterIndexed { index, action ->
                    index >= job.checkpointActionIndex && index <= failingWorkerIndex
                            || action.runAlways
                }
                .map { it.getWorker(mutableMapOf()) }
                .toTypedArray()

        val inOrder = inOrder(mockService, *mockWorkers)

        inOrder
                .verify(mockService!!, times(1))
                .startForeground(anyInt(), any())

        for (mockWorker in mockWorkers) {
            when (mockWorker) {
                is MockJobWorker        ->
                    inOrder
                            .verify(mockWorker, times(mockWorker.steps - mockWorker.startAt))
                            .runStep()
                is MockFailingJobWorker -> {
                    println(mockWorker)
                    inOrder
                            .verify(mockWorker, times(1))
                            .runStep()
                }
            }
        }

        inOrder
                .verify(mockService!!, times(1))
                .stopForeground(anyBoolean())
    }
}