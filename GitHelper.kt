#!/usr/bin/env kotlin

import java.io.File
import java.util.concurrent.TimeUnit

data class GitStatus(
    val path: String,
    val isGitRepo: Boolean,
    val hasUncommittedChanges: Boolean,
    val hasUnpushedCommits: Boolean,
    val currentBranch: String?,
    val remoteUrl: String?
)

class GitHelper {
    fun runCommand(command: String, workingDir: File): String? {
        return try {
            val process = ProcessBuilder()
                .command(command.split(" "))
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
            
            val result = process.inputStream.bufferedReader().readText()
            process.waitFor(10, TimeUnit.SECONDS)
            
            if (process.exitValue() == 0) result.trim() else null
        } catch (e: Exception) {
            null
        }
    }
    
    fun isGitRepo(dir: File): Boolean {
        return File(dir, ".git").exists()
    }
    
    fun hasUncommittedChanges(dir: File): Boolean {
        val status = runCommand("git status --porcelain", dir)
        return !status.isNullOrEmpty()
    }
    
    fun hasUnpushedCommits(dir: File): Boolean {
        val currentBranch = runCommand("git rev-parse --abbrev-ref HEAD", dir) ?: return false
        val localCommit = runCommand("git rev-parse HEAD", dir) ?: return false
        val remoteCommit = runCommand("git rev-parse origin/$currentBranch", dir)
        
        return remoteCommit != null && localCommit != remoteCommit
    }
    
    fun getCurrentBranch(dir: File): String? {
        return runCommand("git rev-parse --abbrev-ref HEAD", dir)
    }
    
    fun getRemoteUrl(dir: File): String? {
        return runCommand("git config --get remote.origin.url", dir)
    }
    
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
            val result = runCommand("git fetch", repo)
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
            val result = runCommand("git push", repo)
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
                val changes = runCommand("git status --porcelain", repo)
                changes?.lines()?.forEach { line ->
                    println("   $line")
                }
            }
        }
    }
    
    fun unpushedCommits(baseDir: String = ".") {
        val repos = findGitRepos(File(baseDir))
        
        println("=== Repositories with Unpushed Commits ===")
        repos.forEach { repo ->
            val status = getGitStatus(repo)
            if (status.hasUnpushedCommits) {
                println("\n🚀 ${repo.name} (${status.path})")
                val commits = runCommand("git log --oneline origin/${status.currentBranch}..HEAD", repo)
                commits?.lines()?.forEach { line ->
                    println("   $line")
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
            println("Usage: kotlin GitHelper.kt <command> [base_directory]")
            println("Commands:")
            println("  status      - Show status of all Git repositories")
            println("  fetch       - Fetch all repositories")
            println("  push        - Push all repositories (skips repos with uncommitted changes)")
            println("  uncommitted - Show repositories with uncommitted changes")
            println("  unpushed    - Show repositories with unpushed commits")
            println("\nExample: kotlin GitHelper.kt status ~/workspace")
        }
    }
}