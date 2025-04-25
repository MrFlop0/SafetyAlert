//package alex.kaplenkov.safetyalert.data.repository
//
//import alex.kaplenkov.safetyalert.data.db.ViolationDao
//import alex.kaplenkov.safetyalert.data.db.entity.ViolationEntity
//import alex.kaplenkov.safetyalert.domain.model.Violation
//import android.content.Context
//import android.graphics.Bitmap
//import androidx.arch.core.executor.testing.InstantTaskExecutorRule
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.flow.flowOf
//import kotlinx.coroutines.test.StandardTestDispatcher
//import kotlinx.coroutines.test.TestScope
//import kotlinx.coroutines.test.advanceUntilIdle
//import kotlinx.coroutines.test.resetMain
//import kotlinx.coroutines.test.runTest
//import kotlinx.coroutines.test.setMain
//import org.junit.After
//import org.junit.Assert.assertEquals
//import org.junit.Assert.assertNotNull
//import org.junit.Assert.assertNull
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.junit.rules.TemporaryFolder
//import org.junit.runner.RunWith
//import org.mockito.ArgumentCaptor
//import org.mockito.ArgumentMatchers.any
//import org.mockito.ArgumentMatchers.anyInt
//import org.mockito.Captor
//import org.mockito.Mock
//import org.mockito.Mockito
//import org.mockito.Mockito.`when`
//import org.mockito.Mockito.doAnswer
//import org.mockito.Mockito.verify
//import org.mockito.MockitoAnnotations
//import org.robolectric.RobolectricTestRunner
//import org.robolectric.annotation.Config
//import java.io.File
//import java.io.FileOutputStream
//
//@ExperimentalCoroutinesApi
//@RunWith(RobolectricTestRunner::class)
//@Config(manifest = Config.DEFAULT_MANIFEST_NAME)
//class LocalViolationRepositoryTest {
//
//    @get:Rule
//    val instantTaskExecutorRule = InstantTaskExecutorRule()
//
//    @get:Rule
//    val tempFolder = TemporaryFolder()
//
//    @Mock
//    private lateinit var violationDao: ViolationDao
//
//    @Mock
//    private lateinit var context: Context
//
//    @Mock
//    private lateinit var bitmap: Bitmap
//
//    @Captor
//    private lateinit var entityCaptor: ArgumentCaptor<ViolationEntity>
//
//    private lateinit var repository: LocalViolationRepository
//    private val testDispatcher = StandardTestDispatcher()
//    private val testScope = TestScope(testDispatcher)
//
//    private val testViolationEntity = ViolationEntity(
//        id = 1L,
//        type = "Test Type",
//        confidence = 0.9f,
//        imagePath = "/test/path/image.jpg",
//        timestamp = "2023-01-01T12:00:00",
//        description = "Test Description",
//        location = "Test Location",
//        sessionId = "test-session-id"
//    )
//
//    private val testViolation = Violation(
//        id = 1L,
//        type = "Test Type",
//        confidence = 0.9f,
//        imageUri = null,
//        timestamp = "2023-01-01T12:00:00",
//        description = "Test Description",
//        location = "Test Location",
//        sessionId = "test-session-id"
//    )
//
//    @Before
//    fun setup() {
//        MockitoAnnotations.openMocks(this)
//        Dispatchers.setMain(testDispatcher)
//
//        // Setup the context mock to return a filesDir in our temporary folder
//        val testDir = tempFolder.newFolder("files")
//        `when`(context.filesDir).thenReturn(testDir)
//
//        // Setup mock for bitmap's compress method
//        doAnswer { invocation ->
//            val outputStream = invocation.arguments[2] as FileOutputStream
//            outputStream.write("test image data".toByteArray())
//            true
//        }.`when`(bitmap).compress(any(), anyInt(), any())
//
//        repository = LocalViolationRepository(violationDao, context)
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//    }
//
//    @Test
//    fun `getAllViolations should return mapped violations from DAO`() = testScope.runTest {
//        // Given
//        val entities = listOf(testViolationEntity)
//        `when`(violationDao.getAllViolations()).thenReturn(flowOf(entities))
//
//        // When
//        val result = repository.getAllViolations().first()
//
//        // Then
//        assertEquals(1, result.size)
//
//        val violation = result[0]
//        assertEquals(testViolationEntity.id, violation.id)
//        assertEquals(testViolationEntity.type, violation.type)
//        assertEquals(testViolationEntity.confidence, violation.confidence)
//        assertEquals(testViolationEntity.timestamp, violation.timestamp)
//        assertEquals(testViolationEntity.description, violation.description)
//        assertEquals(testViolationEntity.location, violation.location)
//        assertEquals(testViolationEntity.sessionId, violation.sessionId)
//
//    }
//
//    @Test
//    fun `deleteViolation should delete image file and entity`() = testScope.runTest {
//        // Given
//        val violationId = 1L
//
//        // Create a test image file
//        val violationsDir = File(context.filesDir, "violations")
//        violationsDir.mkdirs()
//        val testImageFile = File(violationsDir, "test_image.jpg")
//        testImageFile.createNewFile()
//        testImageFile.writeText("test data")
//
//        val entityWithTestImage = testViolationEntity.copy(imagePath = testImageFile.absolutePath)
//        `when`(violationDao.getViolationById(violationId)).thenReturn(entityWithTestImage)
//
//        // When
//        repository.deleteViolation(violationId)
//        advanceUntilIdle()
//
//        // Then
//        verify(violationDao).deleteViolation(violationId)
//        assert(!testImageFile.exists()) // File should be deleted
//    }
//
//    @Test
//    fun `deleteViolation does nothing when violation not found`() = testScope.runTest {
//        // Given
//        val violationId = 99L
//        `when`(violationDao.getViolationById(violationId)).thenReturn(null)
//
//        // When
//        repository.deleteViolation(violationId)
//        advanceUntilIdle()
//
//        // Then - should not throw any errors
//    }
//
//    @Test
//    fun `getViolationById should return mapped violation`() = testScope.runTest {
//        val violationId = 1L
//        `when`(violationDao.getViolationById(violationId)).thenReturn(testViolationEntity)
//
//        // When
//        val result = repository.getViolationById(violationId).first()
//
//        // Then
//        assertNotNull(result)
//        assertEquals(testViolationEntity.id, result?.id)
//        assertEquals(testViolationEntity.type, result?.type)
//        assertEquals(testViolationEntity.confidence, result?.confidence)
//        assertEquals(testViolationEntity.description, result?.description)
//    }
//
//    @Test
//    fun `getViolationById should return null when violation not found`() = testScope.runTest {
//        // Given
//        val violationId = 99L
//        `when`(violationDao.getViolationById(violationId)).thenReturn(null)
//
//        // When
//        val result = repository.getViolationById(violationId).first()
//
//        // Then
//        assertNull(result)
//    }
//
//    @Test
//    fun `saveImageToStorage should create proper directory and file`() = testScope.runTest {
//        // Given - already setup in the setup() method
//
//        // When
//        repository.saveViolation(testViolation, bitmap)
//        advanceUntilIdle()
//
//        // Then
//        val violationsDir = File(context.filesDir, "violations")
//        assert(violationsDir.exists())
//        assert(violationsDir.list()?.isNotEmpty() == true)
//
//        val savedFile = violationsDir.listFiles()?.first()
//        assertNotNull(savedFile)
//        assert(savedFile!!.name.startsWith("IMG_"))
//        assert(savedFile.name.endsWith(".jpg"))
//        assert(savedFile.exists())
//        assertEquals("test image data", savedFile.readText()) // Verify content was written
//    }
//}