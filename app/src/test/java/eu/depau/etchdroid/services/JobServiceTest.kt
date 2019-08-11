package eu.depau.etchdroid.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import eu.depau.etchdroid.AppBuildConfig
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.db.entity.Job
import eu.depau.etchdroid.db.repository.JobRepository
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
internal class JobServiceTest {
    private var service: JobService? = null
    private var mockContext: Context? = null
    private var jobRepo: JobRepository? = null
    private var random = Random()

    @Before
    fun setUp() {
        // Used by JobServiceNotificationHandler to avoid building real notifications when testing
        AppBuildConfig.TEST_BUILD = true

        mockContext = mock(Context::class.java)
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

        // Create a clean JobService and mock its Android framework methods
        service = spy(JobService::class.java)
        Mockito
                .doReturn(mock(NotificationManager::class.java))
                .`when`(service!!).getSystemService(Context.NOTIFICATION_SERVICE)
        Mockito
                .doReturn("penis")
                .`when`(service!!).getString(anyInt())
        Mockito
                .doNothing()
                .`when`(service!!).startForeground(anyInt(), any())
        Mockito
                .doNothing()
                .`when`(service!!).stopForeground(anyBoolean())

    }

    @Test
    fun testBasicFlow() {
        // Create mock job
        val jobProcedure = JobProcedure(-1).apply {
            add(MockJobAction(1, 1.0))
            add(MockJobAction(2, 1.0))
        }
        val job = Job(
                jobId = abs(random.nextInt().toLong()),
                jobProcedure = jobProcedure
        )
        val jobId = job.jobId

        // Add job to mock Repository
        Mockito
                .`when`(jobRepo!!.getById(jobId))
                .thenReturn(job)

        // Create mock Intent to pass to the service
        val serviceIntent = mock(Intent::class.java) as Intent
        Mockito
                .doReturn(JobServiceIntentDTO(jobId))
                .`when`(serviceIntent).getParcelableExtra<JobServiceIntentDTO>(JobServiceIntentDTO.EXTRA)

        // Pass it to onHandleIntent (with reflection cause it's protected)
        service!!::class.java
                .getDeclaredMethod("onHandleIntent", Intent::class.java)
                .apply { isAccessible = true }
                .invoke(service, serviceIntent)

        // Check whether everything was called, in order
        val mockWorkers = jobProcedure
                .map { it.getWorker() as MockJobWorker }
                .toTypedArray()

        val inOrder = Mockito.inOrder(*mockWorkers, service)

        inOrder
                .verify(service!!, times(1))
                .startForeground(anyInt(), any())

        for (mockWorker in mockWorkers) {
            inOrder
                    .verify(mockWorker, times(mockWorker.steps))
                    .runStep()
        }

        inOrder
                .verify(service!!, times(1))
                .stopForeground(anyBoolean())
    }
}