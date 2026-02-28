import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitHelperTest {
    private fun run(vararg command: String, workingDir: File): String {
        val process = ProcessBuilder(command.toList())
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val exit = process.waitFor()
        check(exit == 0) { "Command failed (${command.joinToString(" ")}): $output" }
        return output
    }

    private fun initRepo(path: File): File {
        path.mkdirs()
        run("git", "init", "-b", "main", workingDir = path)
        run("git", "config", "user.name", "Test User", workingDir = path)
        run("git", "config", "user.email", "test@example.com", workingDir = path)
        return path
    }

    @Test
    fun `findGitRepos finds nested repositories excluding hidden directories`() {
        val root = Files.createTempDirectory("git-helper-test").toFile()
        val visibleRepo = initRepo(File(root, "visible-repo"))
        val nestedRepo = initRepo(File(root, "apps/nested-repo"))
        initRepo(File(root, ".hidden/ignored-repo"))

        val helper = GitHelper()
        val repos = helper.findGitRepos(root).map { it.absolutePath }.toSet()

        assertTrue(visibleRepo.absolutePath in repos)
        assertTrue(nestedRepo.absolutePath in repos)
        assertFalse(repos.any { it.contains("ignored-repo") })
    }

    @Test
    fun `hasUncommittedChanges returns true when file is modified`() {
        val repo = initRepo(Files.createTempDirectory("git-helper-repo").toFile())
        val file = File(repo, "README.md")
        file.writeText("hello")

        val helper = GitHelper()
        assertTrue(helper.hasUncommittedChanges(repo))
    }

    @Test
    fun `hasUnpushedCommits returns false without upstream`() {
        val repo = initRepo(Files.createTempDirectory("git-helper-no-upstream").toFile())
        File(repo, "file.txt").writeText("content")
        run("git", "add", ".", workingDir = repo)
        run("git", "commit", "-m", "initial", workingDir = repo)

        val helper = GitHelper()
        assertFalse(helper.hasUnpushedCommits(repo))
    }

    @Test
    fun `hasUnpushedCommits detects local commit ahead of upstream`() {
        val root = Files.createTempDirectory("git-helper-upstream").toFile()
        val remote = File(root, "remote.git")
        run("git", "init", "--bare", remote.absolutePath, workingDir = root)

        val repo = File(root, "work")
        run("git", "clone", remote.absolutePath, repo.absolutePath, workingDir = root)
        run("git", "config", "user.name", "Test User", workingDir = repo)
        run("git", "config", "user.email", "test@example.com", workingDir = repo)

        File(repo, "a.txt").writeText("one")
        run("git", "add", ".", workingDir = repo)
        run("git", "commit", "-m", "first", workingDir = repo)
        run("git", "push", "-u", "origin", "main", workingDir = repo)

        val helper = GitHelper()
        assertFalse(helper.hasUnpushedCommits(repo))

        File(repo, "b.txt").writeText("two")
        run("git", "add", ".", workingDir = repo)
        run("git", "commit", "-m", "second", workingDir = repo)

        assertTrue(helper.hasUnpushedCommits(repo))
    }

    @Test
    fun `runCommand returns null for non-zero exit codes`() {
        val repo = initRepo(Files.createTempDirectory("git-helper-cmd").toFile())
        val helper = GitHelper()

        val result = helper.runCommand("git", "not-a-real-command", workingDir = repo)
        assertEquals(null, result)
        assertNotNull(helper.runCommand("git", "rev-parse", "--abbrev-ref", "HEAD", workingDir = repo))
    }
}
