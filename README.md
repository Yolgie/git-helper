# Git Helper

A simple Kotlin command-line tool for batch Git operations across multiple repositories.

## Features

- **Status Check**: View status of all Git repositories in a directory
- **Batch Fetch**: Fetch all repositories at once
- **Batch Push**: Push all repositories (skips those with uncommitted changes)
- **Uncommitted Files**: List repositories with uncommitted changes
- **Unpushed Commits**: List repositories with unpushed commits

## Prerequisites

- Java SDK installed with JAVA_HOME configured
- Kotlin compiler installed
- Git installed and configured

## Installation

1. Download `GitHelper.kts`
2. Make sure Kotlin is installed (`kotlin -version`)
3. Run the script directly

## Usage

```bash
kotlin GitHelper.kts <command> [base_directory]
```

### Commands

- `status` - Show status of all Git repositories
- `fetch` - Fetch all repositories
- `push` - Push all repositories (skips repos with uncommitted changes)
- `uncommitted` - Show repositories with uncommitted changes
- `unpushed` - Show repositories with unpushed commits

### Examples

```bash
# Check status of all repos in current directory
kotlin GitHelper.kts status

# Check status of all repos in a specific directory
kotlin GitHelper.kts status C:\workspace

# Fetch all repositories
kotlin GitHelper.kts fetch

# Push all repositories (safe - skips repos with uncommitted changes)
kotlin GitHelper.kts push

# Find repositories with uncommitted changes
kotlin GitHelper.kts uncommitted

# Find repositories with unpushed commits
kotlin GitHelper.kts unpushed
```

## How It Works

The tool recursively searches for Git repositories (directories containing `.git` folder) in the specified directory and performs the requested operation on each one. It's designed to be safe and will skip potentially destructive operations on repositories with uncommitted changes.

## Output

The tool provides clear, colored output showing:
- Repository name and path
- Current branch
- Remote URL
- Status of uncommitted changes
- Status of unpushed commits
- Success/failure of operations

---

*This code was created with [Claude](https://claude.ai) AI assistant.*