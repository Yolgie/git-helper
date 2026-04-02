# Git Helper

A Kotlin command-line tool for batch Git operations across multiple repositories.

## Features

- **Status Check**: View status of all Git repositories in a directory
- **Batch Fetch**: Fetch all repositories at once
- **Batch Push**: Push all repositories (skips those with uncommitted changes)
- **Uncommitted Files**: List repositories with uncommitted changes
- **Unpushed Commits**: List repositories with unpushed commits (requires an upstream branch)

## Prerequisites

- Java 21+ SDK installed (project toolchain targets Java 21)
- Gradle 8+
- Git installed and configured

## Usage

Run via Gradle:

```bash
gradle run --args="<command> [base_directory]"
```

### Commands

- `status` - Show status of all Git repositories
- `fetch` - Fetch all repositories
- `push` - Push all repositories (skips repos with uncommitted changes)
- `uncommitted` - Show repositories with uncommitted changes
- `unpushed` - Show repositories with unpushed commits

## Testing

```bash
gradle test
```

Current tests cover:
- Repository discovery (including nested and hidden-directory behavior)
- Detection of uncommitted changes
- Detection of unpushed commits with and without upstream
- Command error handling

## CI Quality Gate

GitHub Pull Requests run the `Gradle Tests` workflow (`.github/workflows/gradle-test.yml`) as a quality gate.
A PR is expected to pass this workflow before merge.

## How It Works

The tool recursively searches for Git repositories (directories containing a `.git` entry) in the specified directory and performs the requested operation on each one. It skips push operations for repositories with uncommitted changes.
