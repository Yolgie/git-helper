#!/usr/bin/env kotlin

import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

data class GitStatus(
    val path: String,
    val isGitRepo: Boolean,
    val hasUncommittedChanges: Boolean,
    val hasUnpushedCommits: Boolean,
    val currentBranch: String?,
    val remoteUrl: String?
)

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val timedOut: Boolean = false
)

interface CommandExecutor {
    fun run(args: List<String>, workingDir: File, timeout: Duration = Duration.ofSeconds(10)): CommandResult?
}

class ProcessCommandExecutor : CommandExecutor {
    override fun run(args: List<String>, workingDir: File, timeout: Duration): CommandResult? {
        return try {
            val process = ProcessBuilder()
                .command(args)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            val finished = process.waitFor(timeout.seconds, TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                return CommandResult(exitCode = -1, output = output, timedOut = true)
            }

            CommandResult(exitCode = process.exitValue(), output = output)
        } catch (_: Exception) {
            null
        }
    }
}

class GitHelper(private val executor: CommandExecutor = ProcessCommandExecutor()) {
    fun runCommand(vararg command: String, workingDir: File): String? {
        val result = executor.run(command.toList(), workingDir) ?: return null
        return if (result.exitCode == 0 && !result.timedOut) result.output else null
    }

    fun isGitRepo(dir: File): Boolean = File(dir, ".git").exists()

    fun hasUncommittedChanges(dir: File): Boolean {
        val status = runCommand("git", "status", "--porcelain", workingDir = dir)
        return !status.isNullOrEmpty()
    }

    fun hasUnpushedCommits(dir: File): Boolean {
        // Require an upstream branch to avoid false positives/failures on local-only branches.
        val upstream = runCommand("git", "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}", workingDir = dir)
            ?: return false

        val count = runCommand("git", "rev-list", "--count", "$upstream..HEAD", workingDir = dir)
            ?.toIntOrNull()
            ?: return false

        return count > 0
    }

    fun getCurrentBranch(dir: File): String? = runCommand("git", "rev-parse", "--abbrev-ref", "HEAD", workingDir = dir)

    fun getRemoteUrl(dir: File): String? = runCommand("git", "config", "--get", "remote.origin.url", workingDir = dir)

    fun getGitStatus(dir: File): GitStatus {
        val isRepo = isGitRepo(dir)
        return GitStatus(
            path = dir.absolutePath,
            isGitRepo = isRepo,
            hasUncommittedChanges = if (isRepo) hasUncommittedChanges(dir) else false,
            hasUnpushedCommits = if (isRepo) hasUnpushedCommits(dir) else false,
            currentBranch = if (isRepo) getCurrentBranch(dir) else null,
            remoteUrl = if (isRepo) getRemoteUrl(dir) else null
        )
    }

    fun findGitRepos(baseDir: File): List<File> {
        val repos = mutableListOf<File>()

        baseDir.listFiles()?.forEach { file ->
            if (file.isDirectory && !file.name.startsWith(".")) {
                if (isGitRepo(file)) {
                    repos.add(file)
                } else {
                    repos.addAll(findGitRepos(file))
                }
            }
        }

        return repos
    }

    fun statusAll(baseDir: String = ".") {
        val repos = findGitRepos(File(baseDir))

        println("=== Git Repository Status ===")
        repos.forEach { repo ->
            val status = getGitStatus(repo)
            println("\n📁 ${repo.name} (${status.path})")
            println("   Branch: ${status.currentBranch ?: "unknown"}")
            println("   Remote: ${status.remoteUrl ?: "none"}")
            println("   Uncommitted changes: ${if (status.hasUncommittedChanges) "YES" else "NO"}")
            println("   Unpushed commits: ${if (status.hasUnpushedCommits) "YES" else "NO"}")
        }
    }

    fun fetchAll(baseDir: String = ".") {
        val repos = findGitRepos(File(baseDir))

        println("=== Fetching All Repositories ===")
        repos.forEach { repo ->
            println("\n🔄 Fetching ${repo.name}...")
            val result = runCommand("git", "fetch", workingDir = repo)
            println("   ${if (result != null) "✅ Success" else "❌ Failed"}")
        }
    }

    fun pushAll(baseDir: String = ".") {
        val repos = findGitRepos(File(baseDir))

        println("=== Pushing All Repositories ===")
        repos.forEach { repo ->
            val status = getGitStatus(repo)
            if (status.hasUncommittedChanges) {
                println("\n⚠️  ${repo.name}: Has uncommitted changes, skipping push")
                return@forEach
            }

            println("\n⬆️  Pushing ${repo.name}...")
            val result = runCommand("git", "push", workingDir = repo)
            println("   ${if (result != null) "✅ Success" else "❌ Failed"}")
        }
    }

    fun uncommittedFiles(baseDir: String = ".") {
        val repos = findGitRepos(File(baseDir))

        println("=== Repositories with Uncommitted Changes ===")
        repos.forEach { repo ->
            val status = getGitStatus(repo)
            if (status.hasUncommittedChanges) {
                println("\n📝 ${repo.name} (${status.path})")
                val changes = runCommand("git", "status", "--porcelain", workingDir = repo)
                changes?.lines()?.forEach { line ->
                    if (line.isNotBlank()) println("   $line")
                }
            }
        }
    }

    fun unpushedCommits(baseDir: String = ".") {
        val repos = findGitRepos(File(baseDir))

        println("=== Repositories with Unpushed Commits ===")
        repos.forEach { repo ->
            val status = getGitStatus(repo)
            if (status.hasUnpushedCommits && status.currentBranch != null) {
                println("\n🚀 ${repo.name} (${status.path})")
                val commits = runCommand("git", "log", "--oneline", "@{u}..HEAD", workingDir = repo)
                commits?.lines()?.forEach { line ->
                    if (line.isNotBlank()) println("   $line")
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    val helper = GitHelper()
    val baseDir = if (args.size > 1) args[1] else "."

    when (args.getOrNull(0)) {
        "status" -> helper.statusAll(baseDir)
        "fetch" -> helper.fetchAll(baseDir)
        "push" -> helper.pushAll(baseDir)
        "uncommitted" -> helper.uncommittedFiles(baseDir)
        "unpushed" -> helper.unpushedCommits(baseDir)
        else -> {
            println("Usage: ./gradlew run --args=\"<command> [base_directory]\"")
            println("Commands:")
            println("  status      - Show status of all Git repositories")
            println("  fetch       - Fetch all repositories")
            println("  push        - Push all repositories (skips repos with uncommitted changes)")
            println("  uncommitted - Show repositories with uncommitted changes")
            println("  unpushed    - Show repositories with unpushed commits")
            println("\nExample: ./gradlew run --args=\"status ~/workspace\"")
        }
    }
}
