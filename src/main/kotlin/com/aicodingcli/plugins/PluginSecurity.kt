package com.aicodingcli.plugins

import java.io.File
import java.net.URL
import java.security.Permission
import java.util.concurrent.ConcurrentHashMap

/**
 * Plugin security manager for controlling plugin access to system resources
 */
class PluginSecurityManager {
    private val pluginSandboxes = ConcurrentHashMap<String, PluginSandbox>()
    
    /**
     * Validate if a plugin has the required permissions for an operation
     */
    fun validatePermissions(plugin: Plugin, operation: PluginOperation): Boolean {
        val requiredPermission = operation.requiredPermission
        return plugin.metadata.permissions.any { permission ->
            isPermissionCompatible(permission, requiredPermission)
        }
    }
    
    /**
     * Create a sandbox for a plugin based on its permissions
     */
    fun createSandbox(plugin: Plugin): PluginSandbox {
        val sandbox = PluginSandbox(
            pluginId = plugin.metadata.id,
            allowedPaths = extractAllowedPaths(plugin.metadata.permissions),
            allowedNetworkHosts = extractAllowedNetworkHosts(plugin.metadata.permissions),
            allowedCommands = extractAllowedCommands(plugin.metadata.permissions),
            hasConfigAccess = hasConfigPermission(plugin.metadata.permissions),
            hasHistoryAccess = hasHistoryPermission(plugin.metadata.permissions)
        )
        
        pluginSandboxes[plugin.metadata.id] = sandbox
        return sandbox
    }
    
    /**
     * Remove sandbox for a plugin
     */
    fun removeSandbox(pluginId: String) {
        pluginSandboxes.remove(pluginId)
    }
    
    /**
     * Get sandbox for a plugin
     */
    fun getSandbox(pluginId: String): PluginSandbox? {
        return pluginSandboxes[pluginId]
    }
    
    /**
     * Check if a plugin can access a file path
     */
    fun checkFileAccess(pluginId: String, path: String, write: Boolean = false): Boolean {
        val sandbox = pluginSandboxes[pluginId] ?: return false
        return sandbox.checkFileAccess(path, write)
    }
    
    /**
     * Check if a plugin can access a network host
     */
    fun checkNetworkAccess(pluginId: String, host: String): Boolean {
        val sandbox = pluginSandboxes[pluginId] ?: return false
        return sandbox.checkNetworkAccess(host)
    }
    
    /**
     * Check if a plugin can execute a system command
     */
    fun checkCommandExecution(pluginId: String, command: String): Boolean {
        val sandbox = pluginSandboxes[pluginId] ?: return false
        return sandbox.checkCommandExecution(command)
    }
    
    private fun isPermissionCompatible(granted: PluginPermission, required: PluginPermission): Boolean {
        return when {
            granted::class == required::class -> {
                when (granted) {
                    is PluginPermission.FileSystemPermission -> {
                        val requiredFs = required as PluginPermission.FileSystemPermission
                        granted.allowedPaths.any { allowedPath ->
                            requiredFs.allowedPaths.any { requiredPath ->
                                requiredPath.startsWith(allowedPath)
                            }
                        } && (!requiredFs.readOnly || granted.readOnly)
                    }
                    is PluginPermission.NetworkPermission -> {
                        val requiredNet = required as PluginPermission.NetworkPermission
                        granted.allowedHosts.containsAll(requiredNet.allowedHosts) ||
                        granted.allowedHosts.contains("*")
                    }
                    is PluginPermission.SystemPermission -> {
                        val requiredSys = required as PluginPermission.SystemPermission
                        granted.allowedCommands.containsAll(requiredSys.allowedCommands) ||
                        granted.allowedCommands.contains("*")
                    }
                    is PluginPermission.ConfigPermission -> true
                    is PluginPermission.HistoryPermission -> true
                }
            }
            else -> false
        }
    }
    
    private fun extractAllowedPaths(permissions: List<PluginPermission>): List<String> {
        return permissions.filterIsInstance<PluginPermission.FileSystemPermission>()
            .flatMap { it.allowedPaths }
    }
    
    private fun extractAllowedNetworkHosts(permissions: List<PluginPermission>): List<String> {
        return permissions.filterIsInstance<PluginPermission.NetworkPermission>()
            .flatMap { it.allowedHosts }
    }
    
    private fun extractAllowedCommands(permissions: List<PluginPermission>): List<String> {
        return permissions.filterIsInstance<PluginPermission.SystemPermission>()
            .flatMap { it.allowedCommands }
    }
    
    private fun hasConfigPermission(permissions: List<PluginPermission>): Boolean {
        return permissions.any { it is PluginPermission.ConfigPermission }
    }
    
    private fun hasHistoryPermission(permissions: List<PluginPermission>): Boolean {
        return permissions.any { it is PluginPermission.HistoryPermission }
    }
}

/**
 * Plugin sandbox that enforces security restrictions
 */
class PluginSandbox(
    val pluginId: String,
    private val allowedPaths: List<String>,
    private val allowedNetworkHosts: List<String>,
    private val allowedCommands: List<String>,
    private val hasConfigAccess: Boolean,
    private val hasHistoryAccess: Boolean
) {
    
    /**
     * Check if the plugin can access a file path
     */
    fun checkFileAccess(path: String, write: Boolean = false): Boolean {
        val normalizedPath = File(path).canonicalPath
        
        return allowedPaths.any { allowedPath ->
            val normalizedAllowedPath = File(allowedPath).canonicalPath
            normalizedPath.startsWith(normalizedAllowedPath)
        }
    }
    
    /**
     * Check if the plugin can access a network host
     */
    fun checkNetworkAccess(host: String): Boolean {
        return allowedNetworkHosts.contains(host) ||
               allowedNetworkHosts.contains("*") ||
               allowedNetworkHosts.any { allowedHost ->
                   when {
                       allowedHost.startsWith("*.") -> {
                           val domain = allowedHost.substring(2)
                           host.endsWith(".$domain") || host == domain
                       }
                       else -> host.endsWith(".$allowedHost") || host == allowedHost
                   }
               }
    }
    
    /**
     * Check if the plugin can execute a system command
     */
    fun checkCommandExecution(command: String): Boolean {
        return allowedCommands.contains(command) ||
               allowedCommands.contains("*") ||
               allowedCommands.any { allowedCommand ->
                   command.startsWith(allowedCommand)
               }
    }
    
    /**
     * Check if the plugin can access configuration
     */
    fun checkConfigAccess(): Boolean = hasConfigAccess
    
    /**
     * Check if the plugin can access conversation history
     */
    fun checkHistoryAccess(): Boolean = hasHistoryAccess
    
    /**
     * Get sandbox information
     */
    fun getInfo(): PluginSandboxInfo {
        return PluginSandboxInfo(
            pluginId = pluginId,
            allowedPaths = allowedPaths,
            allowedNetworkHosts = allowedNetworkHosts,
            allowedCommands = allowedCommands,
            hasConfigAccess = hasConfigAccess,
            hasHistoryAccess = hasHistoryAccess
        )
    }
}

/**
 * Information about a plugin sandbox
 */
data class PluginSandboxInfo(
    val pluginId: String,
    val allowedPaths: List<String>,
    val allowedNetworkHosts: List<String>,
    val allowedCommands: List<String>,
    val hasConfigAccess: Boolean,
    val hasHistoryAccess: Boolean
)

/**
 * Plugin operation that requires specific permissions
 */
data class PluginOperation(
    val type: PluginOperationType,
    val requiredPermission: PluginPermission,
    val description: String
)

/**
 * Types of plugin operations
 */
enum class PluginOperationType {
    FILE_READ,
    FILE_WRITE,
    NETWORK_REQUEST,
    COMMAND_EXECUTION,
    CONFIG_ACCESS,
    HISTORY_ACCESS
}

/**
 * Security exception thrown when a plugin violates security restrictions
 */
class PluginSecurityException(
    val pluginId: String,
    val operation: PluginOperationType,
    message: String,
    cause: Throwable? = null
) : SecurityException(message, cause)

/**
 * Plugin security policy that defines default security settings
 */
object PluginSecurityPolicy {
    
    /**
     * Default allowed paths for plugins (relative to user home)
     */
    val DEFAULT_ALLOWED_PATHS = listOf(
        System.getProperty("user.home") + "/.aicodingcli/plugins",
        System.getProperty("user.home") + "/.aicodingcli/temp",
        System.getProperty("java.io.tmpdir")
    )
    
    /**
     * Default allowed network hosts
     */
    val DEFAULT_ALLOWED_HOSTS = listOf(
        "api.openai.com",
        "api.anthropic.com",
        "localhost",
        "127.0.0.1"
    )
    
    /**
     * Default allowed commands (very restrictive)
     */
    val DEFAULT_ALLOWED_COMMANDS = emptyList<String>()
    
    /**
     * Create a default security policy for a plugin
     */
    fun createDefaultPolicy(pluginId: String): List<PluginPermission> {
        return listOf(
            PluginPermission.FileSystemPermission(
                allowedPaths = DEFAULT_ALLOWED_PATHS,
                readOnly = true
            ),
            PluginPermission.NetworkPermission(
                allowedHosts = DEFAULT_ALLOWED_HOSTS
            )
        )
    }
    
    /**
     * Validate if a permission request is reasonable
     */
    fun validatePermissionRequest(permission: PluginPermission): PluginValidationResult {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        when (permission) {
            is PluginPermission.FileSystemPermission -> {
                permission.allowedPaths.forEach { path ->
                    if (path == "/" || path == "C:\\" || path.contains("..")) {
                        errors.add("Dangerous file system access requested: $path")
                    }
                    if (!permission.readOnly && path.startsWith("/")) {
                        warnings.add("Write access to system paths requested: $path")
                    }
                }
            }
            is PluginPermission.NetworkPermission -> {
                if (permission.allowedHosts.contains("*")) {
                    warnings.add("Unrestricted network access requested")
                }
            }
            is PluginPermission.SystemPermission -> {
                if (permission.allowedCommands.contains("*")) {
                    errors.add("Unrestricted system command execution requested")
                }
                permission.allowedCommands.forEach { command ->
                    if (command in listOf("rm", "del", "format", "sudo", "su")) {
                        errors.add("Dangerous system command requested: $command")
                    }
                }
            }
            else -> {
                // Config and History permissions are generally safe
            }
        }
        
        return PluginValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
