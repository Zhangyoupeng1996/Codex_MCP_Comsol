# Codex Workspace Setup

This repository is intended to be portable across Codex workspaces. The live
workstation copy currently used for validation is:

```text
D:\AIResearch\Projects\01_Simulation\mcp-servers\COMSOL_Multiphysics_MCP
```

Do not commit machine-local runtime state, generated COMSOL models, proprietary
COMSOL documentation PDFs, or virtual environments. These paths are ignored by
Git:

```text
.venv/
comsol_models/
comsol_outputs/
.comsol_runtime*/
knowledge_base/
pdf/
```

## Codex MCP Configuration

For Codex Desktop, configure the COMSOL MCP server in
`C:\Users\<user>\.codex\config.toml`:

```toml
[mcp_servers.comsol]
command = 'D:\AIResearch\Projects\01_Simulation\mcp-servers\COMSOL_Multiphysics_MCP\.venv\Scripts\python.exe'
args = ["-m", "src.server"]
cwd = 'D:\AIResearch\Projects\01_Simulation\mcp-servers\COMSOL_Multiphysics_MCP'
startup_timeout_sec = 180.0

[mcp_servers.comsol.env]
HF_ENDPOINT = "https://hf-mirror.com"
```

Adjust the paths if the repository is checked out elsewhere.

## Runtime Check

Start a COMSOL server on port 2036, then verify the same Python client path used
by the MCP server:

```powershell
python scripts\test_comsol_connection.py --host localhost --port 2036
```

Expected output includes the COMSOL version and the loaded model names:

```text
Connected to COMSOL 6.3 at localhost:2036
Loaded models: [...]
```

After Codex reloads the MCP server, a successful `comsol_status` tool call
should report `connected: true` and the COMSOL version.

## Cleaning Old Workspace Processes

After moving from an older workspace, stop only the stale COMSOL MCP Python
processes that point to the old checkout. Keep `comsolmphserver.exe` running if
COMSOL itself is healthy.

Use PowerShell to inspect paths before stopping processes:

```powershell
Get-Process python -ErrorAction SilentlyContinue |
  Where-Object { $_.Path -match 'COMSOL_Multiphysics_MCP' } |
  Select-Object Id,Path
```

Then stop only processes whose `Path` points to the old workspace.
