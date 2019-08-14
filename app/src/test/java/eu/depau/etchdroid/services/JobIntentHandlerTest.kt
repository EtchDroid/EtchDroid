package eu.depau.etchdroid.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import eu.depau.etchdroid.AppBuildConfig
import eu.depau.etchdroid.broadcasts.dto.JobProgressUpdateBroadcastDTO
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.db.entity.Job
import eu.depau.etchdroid.db.repository.JobRepository
import eu.depau.etchdroid.services.job.JobIntentHandler
import eu.depau.etchdroid.services.job.JobService
import eu.depau.etchdroid.services.job.dto.JobServiceIntentDTO
import eu.depau.etchdroid.testutils.job.MockJobAction
import eu.depau.etchdroid.testutils.worker.MockJobWorker
import eu.depau.etchdroid.utils.job.impl.JobProcedure
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.*
import kotlin.math.abs

@RunWith(MockitoJUnitRunner::class)
internal class JobIntentHandlerTest {
    private var mockService: JobService? = null
    private var jobRepo: JobRepository? = null
    private var random = Random()

    @Before
    fun setUp() {
        // Used by JobServiceNotificationHandler to avoid building real notifications when testing
        AppBuildConfig.TEST_BUILD = true
        // Make sure the app uses Context to send broadcasts, so it's mocked
        AppBuildConfig.USE_LOCAL_BROADCASTS = false

        // Mock eu.depau.etchdroid.services.job.JobService
        mockService = mock(JobService::class.java)
        Mockito
                .doReturn(mock(NotificationManager::class.java))
                .`when`(mockService!!).getSystemService(Context.NOTIFICATION_SERVICE)
        Mockito
                .doReturn("penis")
                .`when`(mockService!!).getString(anyInt())
        Mockito
                .doNothing()
                .`when`(mockService!!).startForeground(anyInt(), any())
        Mockito
                .doNothing()
                .`when`(mockService!!).stopForeground(anyBoolean())

        jobRepo = mock(JobRepository::class.java)

        // Inject mock JobRepository into database mock instance
        val db = mock(EtchDroidDatabase::class.java)
        Mockito
                .`when`(db.jobRepository())
                .thenReturn(jobRepo!!)

        // Duct tape-inject database instance into database companion object
        EtchDroidDatabase::class.java
                .getDeclaredMethod("access\$setINSTANCE\$cp", EtchDroidDatabase::class.java)
                .apply { isAccessible = true }
                .invoke(null, db)
    }

    @Test
    fun testBasicFlow() {
        // Create mock job
        val jobProcedure = JobProcedure(-1).apply {
            add(MockJobAction(0, 1.0, 0, 10))
            add(MockJobAction(1, 1.0, 0, 10))
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
        val jobProcedure = JobProcedure(-1).apply {
            add(MockJobAction(0, 1.0, 0, 15))
            add(MockJobAction(1, 1.0, 0, 15))
            add(MockJobAction(2, 1.0, 7, 15))
            add(MockJobAction(3, 1.0, 0, 15))
        }
        val job = Job(
                jobId = abs(random.nextInt().toLong()),
                jobProcedure = jobProcedure,
                checkpointActionIndex = 2
        )
        testFlow(job)
    }

    private fun testFlow(job: Job) {

        val jobId = job.jobId

        // Add job to mock Repository
        Mockito
                .`when`(jobRepo!!.getById(jobId))
                .thenReturn(job)

        Mockito
                .`when`(mockService!!.sendBroadcast(any()))
                .thenAnswer {
                    val intent = it.arguments[0] as Intent
                    println("))) BROADCAST (((  ${intent.getSerializableExtra(JobProgressUpdateBroadcastDTO.EXTRA)}")
                    Unit
                }

        // Pass it to the intent handler
        val intentHandler = JobIntentHandler(mockService!!, JobServiceIntentDTO(jobId))
        intentHandler.handle()

        // Check whether everything was called, in order
        val mockWorkers = job.jobProcedure
                .subList(job.checkpointActionIndex, job.jobProcedure.size)
                .map { it.getWorker() as MockJobWorker }
                .toTypedArray()

        val inOrder = Mockito.inOrder(mockService, *mockWorkers)

        inOrder
                .verify(mockService!!, times(1))
                .startForeground(anyInt(), any())

        for (mockWorker in mockWorkers) {
            inOrder
                    .verify(mockWorker, times(mockWorker.steps - mockWorker.startAt))
                    .runStep()
        }

        inOrder
                .verify(mockService!!, times(1))
                .stopForeground(anyBoolean())
    }
}