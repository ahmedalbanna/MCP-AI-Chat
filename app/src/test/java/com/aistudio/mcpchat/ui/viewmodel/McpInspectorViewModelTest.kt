package com.aistudio.mcpchat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.aistudio.mcpchat.data.model.McpResource
import com.aistudio.mcpchat.data.model.McpResourceContent
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.data.repository.McpRepository
import com.aistudio.mcpchat.data.repository.ResourceEvent
import com.aistudio.mcpchat.data.repository.ResourceSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class McpInspectorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: McpRepository
    private lateinit var viewModel: McpInspectorViewModel

    private val serversFlow = MutableStateFlow<List<McpServer>>(emptyList())
    private val toolsFlow = MutableStateFlow<List<com.aistudio.mcpchat.data.model.McpTool>>(emptyList())
    private val logsFlow = MutableStateFlow<List<com.aistudio.mcpchat.data.model.McpLogItem>>(emptyList())
    private val resourceEvents = MutableSharedFlow<ResourceEvent>(extraBufferCapacity = 8)
    private val subscribedFlow = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    private val server = McpServer(
        id = "srv",
        name = "Test Server",
        description = "",
        endpointUrl = "http://mcp.test",
        isEnabled = true,
    )
    private val resource = McpResource(uri = "file://notes.txt", name = "notes", mimeType = "text/plain")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.serversFlow } returns serversFlow
        every { repository.allToolsFlow } returns toolsFlow
        every { repository.logsFlow } returns logsFlow
        every { repository.resourceEvents } returns resourceEvents
        every { repository.subscribedResources } returns subscribedFlow
        serversFlow.value = listOf(server)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun kotlinx.coroutines.test.TestScope.createViewModel() {
        viewModel = McpInspectorViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.servers.collect {} }
    }

    @Test
    fun `selectResourceServer loads and maps the resource catalog`() = runTest {
        coEvery { repository.listResources(server) } returns listOf(resource)
        createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.resourceSummaries.collect {} }
        advanceUntilIdle()

        viewModel.selectResourceServer("srv")
        advanceUntilIdle()

        val summaries = viewModel.resourceSummaries.value
        assertEquals(1, summaries.size)
        assertEquals(ResourceSummary("srv", "Test Server", resource), summaries[0])
        assertEquals("srv", viewModel.selectedResourceServerId.value)
    }

    @Test
    fun `openResource reads content through the repository`() = runTest {
        coEvery { repository.listResources(server) } returns listOf(resource)
        coEvery { repository.readResource(server, resource.uri) } returns
            McpResourceContent(resource.uri, "text/plain", "hello")
        createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.viewedResourceContent.collect {} }
        advanceUntilIdle()

        viewModel.selectResourceServer("srv")
        advanceUntilIdle()
        viewModel.openResource(resource)
        advanceUntilIdle()

        assertEquals(resource, viewModel.viewedResource.value)
        assertEquals("hello", viewModel.viewedResourceContent.value?.text)
    }

    @Test
    fun `push update refreshes the viewed resource content`() = runTest {
        coEvery { repository.listResources(server) } returns listOf(resource)
        coEvery { repository.readResource(server, resource.uri) } returns
            McpResourceContent(resource.uri, "text/plain", "v1")
        createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.viewedResourceContent.collect {} }
        advanceUntilIdle()

        viewModel.selectResourceServer("srv")
        advanceUntilIdle()
        viewModel.openResource(resource)
        advanceUntilIdle()
        assertEquals("v1", viewModel.viewedResourceContent.value?.text)

        resourceEvents.tryEmit(
            ResourceEvent.Updated("srv", "Test Server", resource.uri, McpResourceContent(resource.uri, "text/plain", "v2")),
        )
        advanceUntilIdle()

        assertEquals("v2", viewModel.viewedResourceContent.value?.text)
    }

    @Test
    fun `push update for another resource does not disturb the view`() = runTest {
        coEvery { repository.listResources(server) } returns listOf(resource)
        coEvery { repository.readResource(server, resource.uri) } returns
            McpResourceContent(resource.uri, "text/plain", "v1")
        createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.viewedResourceContent.collect {} }
        advanceUntilIdle()

        viewModel.selectResourceServer("srv")
        advanceUntilIdle()
        viewModel.openResource(resource)
        advanceUntilIdle()

        resourceEvents.tryEmit(
            ResourceEvent.Updated("srv", "Test Server", "file://other.txt", null),
        )
        advanceUntilIdle()

        assertEquals("v1", viewModel.viewedResourceContent.value?.text)
    }

    @Test
    fun `toggleSubscription flips between subscribe and unsubscribe`() = runTest {
        every { repository.isSubscribed("srv", resource.uri) } returnsMany listOf(false, true)
        coEvery { repository.subscribeToResource(server, resource.uri) } returns true
        createViewModel()
        advanceUntilIdle()

        viewModel.selectResourceServer("srv")
        advanceUntilIdle()

        viewModel.toggleSubscription(resource)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.subscribeToResource(server, resource.uri) }

        viewModel.toggleSubscription(resource)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.unsubscribeFromResource(server, resource.uri) }
    }

    @Test
    fun `initial state has no resources selected`() = runTest {
        createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.resourceSummaries.value.isEmpty())
        assertNull(viewModel.selectedResourceServerId.value)
        assertNull(viewModel.viewedResource.value)
        assertNull(viewModel.viewedResourceContent.value)
    }
}
