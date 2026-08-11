# Kubernetes & OpenShift MCP Server Integration Guide

> Guide for integrating Red Hat's **`kubernetes-mcp-server`** with OpenShift 4.19+ and AI assistant environments (**Antigravity**, **Codex**, **OpenCode**, and **VS Code**).

## Architecture Overview

The [Kubernetes MCP Server](https://github.com/containers/kubernetes-mcp-server) provides AI agents with safe, read-only or authorized access to query cluster resources (Pods, Deployments, Custom Resource Definitions, Events, Logs, and metrics).

```
+-------------------------------------------------------------------+
| AI Assistant (Antigravity / Codex / OpenCode / VS Code)           |
+-------------------------------------------------------------------+
                               |
                        MCP Protocol (stdio)
                               v
+-------------------------------------------------------------------+
| kubernetes-mcp-server --read-only --kubeconfig ~/.kube/mcp-viewer  |
+-------------------------------------------------------------------+
                               |
                         OpenShift REST API
                               v
+-------------------------------------------------------------------+
| OpenShift 4.19+ / Kubernetes Cluster                              |
| ServiceAccount: system:serviceaccount:mcp:mcp-viewer (cluster-reader) |
+-------------------------------------------------------------------+
```

---

## 1. Quick Setup Script

Run the automated setup script to create the `mcp-viewer` ServiceAccount, bind the `cluster-reader` ClusterRole, mint a time-bound Token, and output `~/.kube/mcp-viewer.kubeconfig`:

```bash
./scripts/setup-kubernetes-mcp.sh mcp mcp-viewer 24h
```

To clean up resources when no longer needed:
```bash
./scripts/cleanup-kubernetes-mcp.sh mcp mcp-viewer
```

---

## 2. Configuration Across AI Assistive Tooling

### Antigravity (`~/.gemini/config/mcp_config.json`)
```json
{
  "mcpServers": {
    "kubernetes": {
      "command": "npx",
      "args": [
        "-y",
        "kubernetes-mcp-server@latest",
        "--read-only",
        "--kubeconfig",
        "/home/ubuntu/.kube/mcp-viewer.kubeconfig"
      ]
    }
  }
}
```

### Codex (`~/.codex/config.toml`)
```toml
[mcp_servers.kubernetes]
command = "npx"
args = [
  "-y",
  "kubernetes-mcp-server@latest",
  "--read-only",
  "--kubeconfig",
  "/home/ubuntu/.kube/mcp-viewer.kubeconfig"
]
```

### OpenCode (`~/.config/opencode/opencode.jsonc`)
```jsonc
"kubernetes": {
  "type": "local",
  "command": [
    "npx",
    "-y",
    "kubernetes-mcp-server@latest",
    "--read-only",
    "--kubeconfig",
    "$HOME/.kube/mcp-viewer.kubeconfig"
  ]
}
```

### VS Code Workspace (`.vscode/mcp.json`)
```json
{
  "servers": {
    "kubernetes": {
      "type": "stdio",
      "command": "npx",
      "args": [
        "-y",
        "kubernetes-mcp-server@latest",
        "--read-only",
        "--kubeconfig",
        "${userHome}/.kube/mcp-viewer.kubeconfig"
      ]
    }
  }
}
```

---

## 3. Example AI Prompts & Troubleshooting Workflows

With the server running, you can issue natural language commands such as:
- *"Show nodes and their current status"*
- *"List all pods that are not in Running state"*
- *"Get recent events in namespace payu-dev"*
- *"Help me diagnose deployment payu-account-service"*
- *"Get logs for pod payu-auth-service-xxx"*

---

## Security & Compliance

- **Least Privilege**: The ServiceAccount is bound to `cluster-reader` (read-only) or namespace-scoped `view`.
- **Read-Only Enforced**: Pass `--read-only` flag to `kubernetes-mcp-server` to block write/delete operations at the protocol level.
- **Short-Lived Tokens**: Tokens are generated via OpenShift TokenRequest API (`--duration=24h`).
