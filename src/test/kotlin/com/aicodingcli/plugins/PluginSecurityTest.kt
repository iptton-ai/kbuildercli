package com.aicodingcli.plugins

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class PluginSecurityTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var securityManager: PluginSecurityManager
    private lateinit var testPlugin: Plugin
    
    @BeforeEach
    fun setUp() {
        securityManager = PluginSecurityManager()
        testPlugin = createTestPlugin()
    }
    
    @Test
    fun `should create sandbox with correct permissions`() {
        val sandbox = securityManager.createSandbox(testPlugin)
        
        assertNotNull(sandbox)
        assertEquals("test-plugin", sandbox.pluginId)
        
        val info = sandbox.getInfo()
        assertEquals("test-plugin", info.pluginId)
        assertTrue(info.allowedPaths.contains("/tmp"))
        assertTrue(info.allowedNetworkHosts.contains("api.example.com"))
        assertTrue(info.allowedCommands.contains("git"))
        assertTrue(info.hasConfigAccess)
        assertTrue(info.hasHistoryAccess)
    }
    
    @Test
    fun `should validate file access permissions`() {
        val sandbox = securityManager.createSandbox(testPlugin)
        
        // Test allowed path
        assertTrue(sandbox.checkFileAccess("/tmp/test.txt"))
        assertTrue(sandbox.checkFileAccess("/tmp/subdir/file.txt"))
        
        // Test disallowed path
        assertFalse(sandbox.checkFileAccess("/etc/passwd"))
        assertFalse(sandbox.checkFileAccess("/root/secret.txt"))
        
        // Test with actual temp directory
        val tempFile = File(tempDir.toFile(), "test.txt")
        tempFile.writeText("test content")
        
        // Create plugin with temp directory permission
        val tempPlugin = createTestPluginWithPath(tempDir.toString())
        val tempSandbox = securityManager.createSandbox(tempPlugin)
        
        assertTrue(tempSandbox.checkFileAccess(tempFile.absolutePath))
    }
    
    @Test
    fun `should validate network access permissions`() {
        val sandbox = securityManager.createSandbox(testPlugin)
        
        // Test allowed hosts
        assertTrue(sandbox.checkNetworkAccess("api.example.com"))
        assertTrue(sandbox.checkNetworkAccess("api.github.com")) // wildcard match for *.github.com
        
        // Test disallowed hosts
        assertFalse(sandbox.checkNetworkAccess("malicious.com"))
        assertFalse(sandbox.checkNetworkAccess("evil.example.org"))
        
        // Test wildcard permission
        val wildcardPlugin = createTestPluginWithNetworkWildcard()
        val wildcardSandbox = securityManager.createSandbox(wildcardPlugin)
        
        assertTrue(wildcardSandbox.checkNetworkAccess("any.domain.com"))
        assertTrue(wildcardSandbox.checkNetworkAccess("example.org"))
    }
    
    @Test
    fun `should validate command execution permissions`() {
        val sandbox = securityManager.createSandbox(testPlugin)
        
        // Test allowed commands
        assertTrue(sandbox.checkCommandExecution("git"))
        assertTrue(sandbox.checkCommandExecution("git status")) // prefix match
        
        // Test disallowed commands
        assertFalse(sandbox.checkCommandExecution("rm"))
        assertFalse(sandbox.checkCommandExecution("sudo"))
        
        // Test wildcard permission
        val wildcardPlugin = createTestPluginWithCommandWildcard()
        val wildcardSandbox = securityManager.createSandbox(wildcardPlugin)
        
        assertTrue(wildcardSandbox.checkCommandExecution("any-command"))
        assertTrue(wildcardSandbox.checkCommandExecution("dangerous-command"))
    }
    
    @Test
    fun `should validate config and history access`() {
        val sandbox = securityManager.createSandbox(testPlugin)
        
        assertTrue(sandbox.checkConfigAccess())
        assertTrue(sandbox.checkHistoryAccess())
        
        // Test plugin without permissions
        val restrictedPlugin = createRestrictedTestPlugin()
        val restrictedSandbox = securityManager.createSandbox(restrictedPlugin)
        
        assertFalse(restrictedSandbox.checkConfigAccess())
        assertFalse(restrictedSandbox.checkHistoryAccess())
    }
    
    @Test
    fun `should check plugin permissions correctly`() {
        val fileOperation = PluginOperation(
            type = PluginOperationType.FILE_READ,
            requiredPermission = PluginPermission.FileSystemPermission(
                allowedPaths = listOf("/tmp"),
                readOnly = true
            ),
            description = "Read file operation"
        )
        
        assertTrue(securityManager.validatePermissions(testPlugin, fileOperation))
        
        val unauthorizedOperation = PluginOperation(
            type = PluginOperationType.FILE_WRITE,
            requiredPermission = PluginPermission.FileSystemPermission(
                allowedPaths = listOf("/etc"),
                readOnly = false
            ),
            description = "Write to system directory"
        )
        
        assertFalse(securityManager.validatePermissions(testPlugin, unauthorizedOperation))
    }
    
    @Test
    fun `should handle sandbox removal`() {
        val sandbox = securityManager.createSandbox(testPlugin)
        assertNotNull(securityManager.getSandbox("test-plugin"))
        
        securityManager.removeSandbox("test-plugin")
        assertNull(securityManager.getSandbox("test-plugin"))
    }
    
    @Test
    fun `should validate permission requests`() {
        val safeFilePermission = PluginPermission.FileSystemPermission(
            allowedPaths = listOf("/tmp"),
            readOnly = true
        )
        
        val result = PluginSecurityPolicy.validatePermissionRequest(safeFilePermission)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        
        val dangerousFilePermission = PluginPermission.FileSystemPermission(
            allowedPaths = listOf("/"),
            readOnly = false
        )
        
        val dangerousResult = PluginSecurityPolicy.validatePermissionRequest(dangerousFilePermission)
        assertFalse(dangerousResult.isValid)
        assertTrue(dangerousResult.errors.any { it.contains("Dangerous file system access") })
        
        val networkWildcardPermission = PluginPermission.NetworkPermission(
            allowedHosts = listOf("*")
        )
        
        val networkResult = PluginSecurityPolicy.validatePermissionRequest(networkWildcardPermission)
        assertTrue(networkResult.isValid) // Valid but with warnings
        assertTrue(networkResult.warnings.any { it.contains("Unrestricted network access") })
        
        val dangerousSystemPermission = PluginPermission.SystemPermission(
            allowedCommands = listOf("rm", "sudo")
        )
        
        val systemResult = PluginSecurityPolicy.validatePermissionRequest(dangerousSystemPermission)
        assertFalse(systemResult.isValid)
        assertTrue(systemResult.errors.any { it.contains("Dangerous system command") })
    }
    
    @Test
    fun `should create default security policy`() {
        val defaultPolicy = PluginSecurityPolicy.createDefaultPolicy("test-plugin")
        
        assertEquals(2, defaultPolicy.size)
        
        val fsPermission = defaultPolicy.filterIsInstance<PluginPermission.FileSystemPermission>().first()
        assertTrue(fsPermission.readOnly)
        assertTrue(fsPermission.allowedPaths.isNotEmpty())
        
        val networkPermission = defaultPolicy.filterIsInstance<PluginPermission.NetworkPermission>().first()
        assertTrue(networkPermission.allowedHosts.isNotEmpty())
    }
    
    @Test
    fun `should handle plugin security exceptions`() {
        val exception = PluginSecurityException(
            pluginId = "test-plugin",
            operation = PluginOperationType.FILE_WRITE,
            message = "Unauthorized file access"
        )
        
        assertEquals("test-plugin", exception.pluginId)
        assertEquals(PluginOperationType.FILE_WRITE, exception.operation)
        assertEquals("Unauthorized file access", exception.message)
        assertTrue(exception is SecurityException)
    }
    
    private fun createTestPlugin(): Plugin {
        return object : Plugin {
            override val metadata = PluginMetadata(
                id = "test-plugin",
                name = "Test Plugin",
                version = "1.0.0",
                description = "Test plugin",
                author = "Test Author",
                mainClass = "com.test.TestPlugin",
                permissions = listOf(
                    PluginPermission.FileSystemPermission(
                        allowedPaths = listOf("/tmp"),
                        readOnly = true
                    ),
                    PluginPermission.NetworkPermission(
                        allowedHosts = listOf("api.example.com", "*.github.com")
                    ),
                    PluginPermission.SystemPermission(
                        allowedCommands = listOf("git")
                    ),
                    PluginPermission.ConfigPermission,
                    PluginPermission.HistoryPermission
                )
            )
            
            override fun initialize(context: PluginContext) {}
            override fun shutdown() {}
        }
    }
    
    private fun createTestPluginWithPath(path: String): Plugin {
        return object : Plugin {
            override val metadata = PluginMetadata(
                id = "test-plugin-path",
                name = "Test Plugin with Path",
                version = "1.0.0",
                description = "Test plugin with specific path",
                author = "Test Author",
                mainClass = "com.test.TestPlugin",
                permissions = listOf(
                    PluginPermission.FileSystemPermission(
                        allowedPaths = listOf(path),
                        readOnly = false
                    )
                )
            )
            
            override fun initialize(context: PluginContext) {}
            override fun shutdown() {}
        }
    }
    
    private fun createTestPluginWithNetworkWildcard(): Plugin {
        return object : Plugin {
            override val metadata = PluginMetadata(
                id = "test-plugin-network",
                name = "Test Plugin with Network Wildcard",
                version = "1.0.0",
                description = "Test plugin with network wildcard",
                author = "Test Author",
                mainClass = "com.test.TestPlugin",
                permissions = listOf(
                    PluginPermission.NetworkPermission(
                        allowedHosts = listOf("*")
                    )
                )
            )
            
            override fun initialize(context: PluginContext) {}
            override fun shutdown() {}
        }
    }
    
    private fun createTestPluginWithCommandWildcard(): Plugin {
        return object : Plugin {
            override val metadata = PluginMetadata(
                id = "test-plugin-command",
                name = "Test Plugin with Command Wildcard",
                version = "1.0.0",
                description = "Test plugin with command wildcard",
                author = "Test Author",
                mainClass = "com.test.TestPlugin",
                permissions = listOf(
                    PluginPermission.SystemPermission(
                        allowedCommands = listOf("*")
                    )
                )
            )
            
            override fun initialize(context: PluginContext) {}
            override fun shutdown() {}
        }
    }
    
    private fun createRestrictedTestPlugin(): Plugin {
        return object : Plugin {
            override val metadata = PluginMetadata(
                id = "restricted-plugin",
                name = "Restricted Plugin",
                version = "1.0.0",
                description = "Plugin with no permissions",
                author = "Test Author",
                mainClass = "com.test.RestrictedPlugin",
                permissions = emptyList()
            )
            
            override fun initialize(context: PluginContext) {}
            override fun shutdown() {}
        }
    }
}
