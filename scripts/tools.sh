#!/bin/bash
set -euo pipefail

# PayU — dev + infra tools installer (rerunnable/idempotent)
# Usage: chmod +x scripts/tools.sh && ./scripts/tools.sh
#   ./scripts/tools.sh --check   # cek versi saja
#   ./scripts/tools.sh --infra   # hanya infra CLI (podman/skopeo/tkn/kustomize/dll)
#   ./scripts/tools.sh --dev     # hanya dev stack (java/node/uv/rtk/codegraph/caveman)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BIN_DIR="$HOME/.local/bin"
mkdir -p "$BIN_DIR"
export PATH="$BIN_DIR:$HOME/.sdkman/candidates/java/current/bin:$HOME/.sdkman/candidates/maven/current/bin:$PATH"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

MODE="${1:-full}"

detect_arch() {
  case "$(uname -m)" in x86_64|amd64) echo "amd64" ;; aarch64|arm64) echo "arm64" ;; *) echo "amd64" ;; esac
}
ARCH="$(detect_arch)"

command_exists() { command -v "$1" >/dev/null 2>&1; }

ensure_jq() {
  if command_exists jq; then return 0; fi
  echo -e "${YELLOW}jq belum ada — coba install via apt/brew...${NC}"
  if command_exists apt-get; then sudo apt-get update -qq && sudo apt-get install -y -qq jq 2>/dev/null || true
  elif command_exists dnf; then sudo dnf install -y -q jq 2>/dev/null || true
  elif command_exists brew; then brew install jq 2>/dev/null || true
  fi
  # fallback: download static binary via gh release if still missing
  if ! command_exists jq; then
    echo -e "${YELLOW}jq fallback via binary...${NC}"
    curl -sL "https://github.com/jqlang/jq/releases/latest/download/jq-linux-${ARCH}" -o "$BIN_DIR/jq" 2>/dev/null && chmod +x "$BIN_DIR/jq" || true
  fi
}

ensure_path_in_shellrc() {
  local line='export PATH="$HOME/.local/bin:$PATH"'
  for rc in "$HOME/.bashrc" "$HOME/.zshrc"; do
    [ -f "$rc" ] || continue
    grep -Fq "$line" "$rc" 2>/dev/null || echo "$line" >> "$rc"
  done
}

# Download + extract GitHub release binary to $BIN_DIR (idempotent helper)
# usage: gh_release_bin <repo> <asset-substring> <output-name>
gh_release_bin() {
  local repo=$1 asset_match=$2 out=$3 url tmp
  ensure_jq
  if ! command_exists jq; then echo -e "${YELLOW}skip $out: jq missing${NC}"; return 1; fi
  if ! command_exists curl; then echo -e "${YELLOW}skip $out: curl missing${NC}"; return 1; fi
  url=$(curl -sL "https://api.github.com/repos/${repo}/releases/latest" 2>/dev/null | jq -r '.assets[] | select(.name | contains("'"$asset_match"'")) | .browser_download_url' 2>/dev/null | head -1)
  if [ -z "$url" ] || [ "$url" = "null" ]; then echo -e "${YELLOW}skip $out: asset not found ${repo} ${asset_match}${NC}"; return 1; fi
  echo -e "${GREEN}installing $out <- $url${NC}"
  tmp="/tmp/${out}.dl"
  if [[ "$url" == *.tar.gz ]]; then
    curl -sL "$url" -o "/tmp/${out}.tar.gz"
    rm -rf "/tmp/${out}_extract" && mkdir -p "/tmp/${out}_extract"
    tar -xzf "/tmp/${out}.tar.gz" -C "/tmp/${out}_extract" 2>/dev/null || tar -xzf "/tmp/${out}.tar.gz" -C /tmp 2>/dev/null || true
    # cari binary $out paling baru di extract dir
    local found
    found=$(find "/tmp/${out}_extract" /tmp -type f -name "$out" 2>/dev/null | head -1)
    if [ -n "$found" ]; then install -m 0755 "$found" "$BIN_DIR/$out"
    else echo -e "${YELLOW}warn: binary $out not found in tarball${NC}"; return 1; fi
    rm -rf "/tmp/${out}_extract" "/tmp/${out}.tar.gz" 2>/dev/null || true
  else
    curl -sL "$url" -o "$BIN_DIR/$out"
    chmod +x "$BIN_DIR/$out"
  fi
  "$BIN_DIR/$out" version 2>/dev/null | head -1 || "$BIN_DIR/$out" --version 2>/dev/null | head -1 || true
}

print_section() { echo ""; echo -e "${BLUE}════════════════════════════════════════════════${NC}"; echo -e "${BLUE}  $1${NC}"; echo -e "${BLUE}════════════════════════════════════════════════${NC}"; }

# ─────────────────────────────────────────────
# DEV STACK
# ─────────────────────────────────────────────
install_dev() {
  print_section "Dev stack (opencode/java/node/uv/rtk/codegraph/caveman/graphify)"

  # --- opencode ---
  if ! command_exists opencode; then
    curl -fsSL https://opencode.ai/install | bash
  else
    echo -e "${GREEN}opencode sudah ada: $(opencode --version 2>/dev/null | head -1)${NC}"
  fi

  # --- SDKMAN + Java 25 (backend/pom.xml:23) + Maven ---
  if [ ! -d "$HOME/.sdkman" ]; then
    curl -s "https://get.sdkman.io" | bash
  fi
  # shellcheck source=/dev/null — sdkman internals need nounset off
  set +u
  [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ] && source "$HOME/.sdkman/bin/sdkman-init.sh"
  if ! sdk list java 2>/dev/null | grep -q "25.*tem.*installed" 2>/dev/null; then
    sdk install java 25-tem || sdk install java 25-tem -y 2>/dev/null || true
  fi
  sdk default java 25-tem 2>/dev/null || sdk use java 25-tem 2>/dev/null || true
  if ! command_exists mvn; then sdk install maven; else echo -e "${GREEN}maven sudah ada: $(mvn -v 2>/dev/null | head -1)${NC}"; fi
  set -u

  # --- Node.js 24 via nvm (frontend/web-app engines >=24) ---
  if [ ! -d "$HOME/.nvm" ]; then
    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash
  fi
  export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
  # shellcheck source=/dev/null — nvm.sh also needs nounset off
  set +u
  [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
  set -u
  # nvm is a shell function, must run with nounset off to avoid unbound vars in nvm internals
  set +u
  nvm install 24
  nvm alias default 24 >/dev/null 2>&1 || true
  nvm use 24
  set -u
  node -v; npm -v

  # --- uv ---
  if ! command_exists uv; then
    curl -LsSf https://astral.sh/uv/install.sh | sh
    export PATH="$BIN_DIR:$PATH"
  else echo -e "${GREEN}uv sudah ada: $(uv --version 2>/dev/null | head -1)${NC}"; fi
  ensure_path_in_shellrc

  # --- rtk ---
  if ! command_exists rtk; then
    curl -fsSL https://raw.githubusercontent.com/rtk-ai/rtk/refs/heads/master/install.sh | sh
  else echo -e "${GREEN}rtk sudah ada: $(rtk --version 2>/dev/null | head -1 || rtk version 2>/dev/null | head -1)${NC}"; fi
  command_exists rtk && rtk init -g --opencode 2>/dev/null || true

  # --- codegraph ---
  if ! command_exists codegraph; then
    curl -fsSL https://raw.githubusercontent.com/colbymchenry/codegraph/main/install.sh | sh
  else echo -e "${GREEN}codegraph sudah ada${NC}"; codegraph upgrade 2>/dev/null || true; fi
  if command_exists codegraph; then codegraph install 2>/dev/null || true; (cd "$PROJECT_ROOT" && codegraph init 2>/dev/null || true); fi

  # --- caveman ---
  if ! command_exists caveman; then npm install -g @caveman-ai/cli && caveman setup --install 2>/dev/null || true
  else echo -e "${GREEN}caveman sudah ada${NC}"; fi
  command_exists caveman && caveman opencode 2>/dev/null || true
  npx --yes skills add JuliusBrussee/caveman 2>/dev/null || true

  # --- graphify (via uv) ---
  if command_exists uv; then uv tool install graphify 2>/dev/null || uv tool install graphifyy 2>/dev/null || true; fi
  if command_exists graphify; then graphify install --platform opencode 2>/dev/null || true
  elif command_exists graphifyy; then graphifyy install --platform opencode 2>/dev/null || true; fi

  # --- MCP ---
  if command_exists opencode; then
    opencode mcp add context7 -- npx -y @upstash/context7-mcp 2>/dev/null || true
    opencode mcp add playwright -- npx @playwright/mcp@latest 2>/dev/null || true
  fi
}

# ─────────────────────────────────────────────
# INFRA CLI — PayU GitOps (podman/skopeo/tkn/kustomize/dll)
# ─────────────────────────────────────────────
install_infra() {
  print_section "Infra CLI — PayU GitOps (podman/skopeo/tkn/kustomize/helm/oc/kubectl/yq/gh/argocd)"

  # --- podman + podman-compose (rootless, untuk Testcontainers & local infra) ---
  if ! command_exists podman; then
    echo -e "${YELLOW}installing podman...${NC}"
    if command_exists apt-get; then
      sudo apt-get update -qq && sudo apt-get install -y -qq podman podman-compose 2>/dev/null || sudo apt-get install -y podman 2>/dev/null || true
    elif command_exists dnf; then
      sudo dnf install -y -q podman podman-compose 2>/dev/null || sudo dnf install -y -q podman 2>/dev/null || true
    elif command_exists brew; then
      brew install podman podman-compose 2>/dev/null || brew install podman 2>/dev/null || true
      command_exists podman && podman machine init 2>/dev/null || true; podman machine start 2>/dev/null || true
    else
      echo -e "${YELLOW}skip podman: no apt/dnf/brew — install manual${NC}"
    fi
  else
    echo -e "${GREEN}podman sudah ada: $(podman --version 2>/dev/null | head -1)${NC}"
  fi
  # podman-compose fallback via pipx/uv if apt didn't provide it
  if ! command_exists podman-compose && ! podman compose version >/dev/null 2>&1; then
    echo -e "${YELLOW}installing podman-compose...${NC}"
    if command_exists pipx; then pipx install podman-compose 2>/dev/null || true
    elif command_exists uv; then uv tool install podman-compose 2>/dev/null || true
    elif command_exists pip3; then pip3 install --user podman-compose 2>/dev/null || true
    fi
  fi
  command_exists podman && podman --version 2>/dev/null | head -1 || true
  command_exists podman-compose && podman-compose --version 2>/dev/null | head -1 || podman compose version 2>/dev/null | head -1 || true

  # podman socket untuk Testcontainers (idempotent, no duplicate .bashrc)
  if command_exists podman && command_exists systemctl; then
    systemctl --user enable --now podman.socket 2>/dev/null || true
    SOCK="/run/user/$(id -u)/podman/podman.sock"
    if [ -S "$SOCK" ]; then
      echo -e "${GREEN}podman socket OK: $SOCK${NC}"
      # tulis env sekali saja
      if ! grep -q "TESTCONTAINERS_RYUK_DISABLED" "$HOME/.bashrc" 2>/dev/null; then
        cat >> "$HOME/.bashrc" <<'EOF'

# PayU — Testcontainers via rootless podman
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/run/user/$(id -u)/podman/podman.sock
EOF
        echo -e "${GREEN}Testcontainers env ditambahkan ke ~/.bashrc${NC}"
      fi
    else
      echo -e "${YELLOW}podman socket belum aktif — jalankan: systemctl --user status podman.socket${NC}"
    fi
  fi

  # --- skopeo (copy image antar registry, untuk build-push & mirror) ---
  if ! command_exists skopeo; then
    echo -e "${YELLOW}installing skopeo...${NC}"
    if command_exists apt-get; then
      sudo apt-get install -y -qq skopeo 2>/dev/null || true
    elif command_exists dnf; then
      sudo dnf install -y -q skopeo 2>/dev/null || true
    elif command_exists brew; then
      brew install skopeo 2>/dev/null || true
    fi
    # fallback: static binary dari GitHub jika apt gagal
    if ! command_exists skopeo; then
      gh_release_bin "containers/skopeo" "linux-${ARCH}" "skopeo" 2>/dev/null || echo -e "${YELLOW}skip skopeo: install manual https://github.com/containers/skopeo${NC}"
    fi
  else
    echo -e "${GREEN}skopeo sudah ada: $(skopeo --version 2>/dev/null | head -1)${NC}"
  fi

  # --- kubectl (stable) ---
  if ! command_exists kubectl; then
    echo -e "${GREEN}installing kubectl...${NC}"
    KVER=$(curl -sL https://dl.k8s.io/release/stable.txt 2>/dev/null || echo "v1.31.0")
    curl -sL "https://dl.k8s.io/release/${KVER}/bin/linux/${ARCH}/kubectl" -o "$BIN_DIR/kubectl" && chmod +x "$BIN_DIR/kubectl"
    kubectl version --client 2>/dev/null | head -1 || true
  else echo -e "${GREEN}kubectl sudah ada: $(kubectl version --client 2>/dev/null | head -1)${NC}"; fi

  # --- oc (OpenShift CLI) — untuk oc get pods / oc apply -k ---
  if ! command_exists oc; then
    echo -e "${GREEN}installing oc...${NC}"
    # detect stable oc via mirror
    OC_TAR="/tmp/openshift-client-linux.tar.gz"
    curl -sL "https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-linux.tar.gz" -o "$OC_TAR" 2>/dev/null || true
    if [ -f "$OC_TAR" ]; then
      tar -xzf "$OC_TAR" -C /tmp 2>/dev/null || true
      [ -f /tmp/oc ] && install -m 0755 /tmp/oc "$BIN_DIR/oc" 2>/dev/null || true
      [ -f /tmp/kubectl ] && [ ! -f "$BIN_DIR/kubectl" ] && install -m 0755 /tmp/kubectl "$BIN_DIR/kubectl" 2>/dev/null || true
      rm -f /tmp/oc /tmp/kubectl "$OC_TAR" 2>/dev/null || true
      oc version --client 2>/dev/null | head -1 || true
    else echo -e "${YELLOW}skip oc: download failed${NC}"; fi
  else echo -e "${GREEN}oc sudah ada: $(oc version --client 2>/dev/null | head -1)${NC}"; fi

  # --- kustomize (standalone, oc sudah bundle tapi standalone berguna untuk local) ---
  if ! command_exists kustomize; then gh_release_bin "kubernetes-sigs/kustomize" "linux_${ARCH}" "kustomize" 2>/dev/null || true
  else echo -e "${GREEN}kustomize sudah ada: $(kustomize version --short 2>/dev/null || kustomize version 2>/dev/null | head -1)${NC}"; fi

  # --- helm (chart rendering, PayU pakai kustomize primary tapi helm berguna untuk operator charts) ---
  # helm rilis tarball di get.helm.sh, bukan GitHub asset .tar.gz langsung (GitHub cuma .asc). Pakai get.helm.sh.
  if ! command_exists helm; then
    HELM_VER="v3.18.4"
    HELM_TAR="/tmp/helm-${HELM_VER}-linux-${ARCH}.tar.gz"
    echo -e "${GREEN}installing helm ${HELM_VER} <- https://get.helm.sh/helm-${HELM_VER}-linux-${ARCH}.tar.gz${NC}"
    curl -sL "https://get.helm.sh/helm-${HELM_VER}-linux-${ARCH}.tar.gz" -o "$HELM_TAR" 2>/dev/null || true
    if [ -f "$HELM_TAR" ]; then
      rm -rf /tmp/linux-${ARCH} 2>/dev/null || true
      tar -xzf "$HELM_TAR" -C /tmp 2>/dev/null || true
      if [ -f "/tmp/linux-${ARCH}/helm" ]; then install -m 0755 "/tmp/linux-${ARCH}/helm" "$BIN_DIR/helm" 2>/dev/null || true; fi
      rm -rf "/tmp/linux-${ARCH}" "$HELM_TAR" 2>/dev/null || true
      helm version --short 2>/dev/null | head -1 || helm version 2>/dev/null | head -1 || true
    else echo -e "${YELLOW}skip helm: download failed${NC}"; fi
  else echo -e "${GREEN}helm sudah ada: $(helm version --short 2>/dev/null | head -1 || helm version 2>/dev/null | head -1)${NC}"; fi

  # --- yq (YAML processor, dipakai scripts & kustomize patching) ---
  if ! command_exists yq; then gh_release_bin "mikefarah/yq" "linux_${ARCH}" "yq" 2>/dev/null || gh_release_bin "mikefarah/yq" "linux_amd64" "yq" 2>/dev/null || true
  else echo -e "${GREEN}yq sudah ada: $(yq --version 2>/dev/null | head -1)${NC}"; fi

  # --- gh (GitHub CLI, untuk gh pr & API) ---
  if ! command_exists gh; then gh_release_bin "cli/cli" "linux_${ARCH}.tar.gz" "gh" 2>/dev/null || true
  else echo -e "${GREEN}gh sudah ada: $(gh --version 2>/dev/null | head -1)${NC}"; fi

  # --- tkn (Tekton CLI, untuk tkn pipeline/pipelinerun logs) ---
  if ! command_exists tkn; then
    # tektoncd/cli asset: tkn_0.46.0_Linux_x86_64.tar.gz (amd64) / tkn_..._Linux_aarch64.tar.gz (arm64)
    TKN_ARCH="$(case "$ARCH" in amd64) echo "x86_64" ;; arm64) echo "aarch64" ;; *) echo "x86_64" ;; esac)"
    gh_release_bin "tektoncd/cli" "Linux_${TKN_ARCH}.tar.gz" "tkn" 2>/dev/null || gh_release_bin "tektoncd/cli" "tkn_" "tkn" 2>/dev/null || true
  else echo -e "${GREEN}tkn sudah ada: $(tkn version 2>/dev/null | head -1)${NC}"; fi

  # --- argocd (ArgoCD CLI, GitOps sync) ---
  if ! command_exists argocd; then gh_release_bin "argoproj/argo-cd" "linux-${ARCH}" "argocd" 2>/dev/null || gh_release_bin "argoproj/argo-cd" "linux_${ARCH}" "argocd" 2>/dev/null || true
  else echo -e "${GREEN}argocd sudah ada: $(argocd version --client 2>/dev/null | head -1)${NC}"; fi

  # --- jq (dependency gh_release_bin, pastiin ada untuk scripts lain) ---
  if ! command_exists jq; then ensure_jq; fi
  command_exists jq && echo -e "${GREEN}jq sudah ada: $(jq --version 2>/dev/null | head -1)${NC}" || true
}

check_versions() {
  print_section "Versi tools"
  for t in opencode java mvn node npm uv rtk codegraph caveman graphify podman podman-compose skopeo tkn kustomize helm kubectl oc yq gh argocd jq python3; do
    if command_exists "$t"; then
      ver=""
      case "$t" in
        helm)      ver="$(helm version --short 2>/dev/null | head -1 || helm version 2>/dev/null | head -1)" ;;
        argocd)    ver="$(argocd version --client 2>/dev/null | head -1 || argocd version 2>/dev/null | head -1)" ;;
        kustomize) ver="$(kustomize version --short 2>/dev/null | head -1 || kustomize version 2>/dev/null | head -1)" ;;
        caveman)   ver="caveman $(caveman version 2>/dev/null | jq -r .version 2>/dev/null || caveman version 2>/dev/null | head -1)" ;;
        oc)        ver="$(oc version --client 2>/dev/null | head -1)" ;;
        kubectl)   ver="$(kubectl version --client 2>/dev/null | head -1)" ;;
        yq)        ver="$(yq --version 2>/dev/null | head -1)" ;;
        *)         ver="$("$t" --version 2>/dev/null | head -1 || "$t" version 2>/dev/null | head -1 || "$t" --version 2>&1 | head -1)" ;;
      esac
      # fallback generic if case produced empty
      if [ -z "$ver" ] || [ "$ver" = "caveman " ]; then ver="$("$t" --version 2>/dev/null | head -1 || "$t" version 2>/dev/null | head -1 || echo "installed")"; fi
      printf "${GREEN}%-18s${NC} %s\n" "$t" "$ver"
    else
      printf "${YELLOW}%-18s${NC} MISSING\n" "$t"
    fi
  done
  if command_exists podman && [ -S "/run/user/$(id -u)/podman/podman.sock" ]; then echo -e "${GREEN}podman socket        OK${NC}"
  elif command_exists podman; then echo -e "${YELLOW}podman socket        tidak aktif (systemctl --user start podman.socket)${NC}"; fi
  command_exists kubectl && kubectl version --client 2>/dev/null | head -1 || true
  command_exists oc && oc version --client 2>/dev/null | head -1 || true
  if command_exists java; then java -version 2>&1 | head -1 || true; fi
}

setup_podman_tc_only() {
  # shim for legacy --podman-tc from scripts/setup/install-tools.sh
  if command_exists podman && command_exists systemctl; then
    systemctl --user enable --now podman.socket 2>/dev/null || true
    SOCK="/run/user/$(id -u)/podman/podman.sock"
    if [ -S "$SOCK" ]; then
      echo -e "${GREEN}podman socket OK: $SOCK${NC}"
      if ! grep -q "TESTCONTAINERS_RYUK_DISABLED" "$HOME/.bashrc" 2>/dev/null; then
        cat >> "$HOME/.bashrc" <<'EOF'

# PayU — Testcontainers via rootless podman
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/run/user/$(id -u)/podman/podman.sock
EOF
      fi
    else echo -e "${YELLOW}podman socket belum aktif${NC}"; return 1; fi
  else echo -e "${YELLOW}podman/systemctl not found${NC}"; return 1; fi
}

case "$MODE" in
  --check|-c) check_versions ;;
  --podman-tc) setup_podman_tc_only; check_versions ;;
  --infra|-i) install_infra; check_versions ;;
  --dev|-d)   install_dev; check_versions ;;
  --help|-h)
    echo "Usage: ./scripts/tools.sh [OPTION]"
    echo "  (none)    full install (dev + infra)"
    echo "  --dev     hanya dev stack (java/node/uv/rtk/codegraph/caveman/graphify/mcp)"
    echo "  --infra   hanya infra CLI (podman/skopeo/tkn/kustomize/helm/oc/kubectl/yq/gh/argocd)"
    echo "  --podman-tc hanya setup podman socket untuk Testcontainers (legacy)"
    echo "  --check   cek versi tools terinstall"
    echo "  --help    help"
    exit 0
    ;;
  *) install_dev; install_infra; check_versions ;;
esac

echo -e "${GREEN}Done. Pastikan ~/.local/bin di PATH (otomatis via .bashrc). Restart shell atau: source ~/.bashrc${NC}"
