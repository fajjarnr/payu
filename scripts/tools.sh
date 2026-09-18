#!/bin/bash
set -euo pipefail

# PayU — dev + infra tools installer (rerunnable/idempotent)
# Usage: chmod +x scripts/tools.sh && ./scripts/tools.sh
#   ./scripts/tools.sh --check   # cek versi saja
#   ./scripts/tools.sh --infra   # hanya infra CLI (podman/skopeo/tkn/kustomize/dll)
#   ./scripts/tools.sh --dev     # hanya dev stack (java/node/uv/rtk/codegraph)

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
    if curl -fsSL "https://github.com/jqlang/jq/releases/latest/download/jq-linux-${ARCH}" -o "$BIN_DIR/jq" 2>/dev/null; then
      chmod +x "$BIN_DIR/jq"
    else
      rm -f "$BIN_DIR/jq" 2>/dev/null || true
    fi || true
  fi
}

ensure_path_in_shellrc() {
  # single quotes intentional: expand at shell startup, not now
  # shellcheck disable=SC2016
  local line='export PATH="$HOME/.local/bin:$PATH"'
  for rc in "$HOME/.bashrc" "$HOME/.zshrc"; do
    [ -f "$rc" ] || continue
    grep -Fq "$line" "$rc" 2>/dev/null || echo "$line" >> "$rc"
  done
}

# Download + extract GitHub release binary to $BIN_DIR (idempotent helper)
# usage: gh_release_bin <repo> <asset-substring> <output-name>
gh_release_bin() {
  local repo=$1 asset_match=$2 out=$3 url
  ensure_jq
  if ! command_exists jq; then echo -e "${RED}skip $out: jq missing${NC}"; return 1; fi
  if ! command_exists curl; then echo -e "${RED}skip $out: curl missing${NC}"; return 1; fi
  url=$(curl -fsSL "https://api.github.com/repos/${repo}/releases/latest" 2>/dev/null | jq -r --arg m "$asset_match" '.assets[] | select(.name | contains($m)) | .browser_download_url' 2>/dev/null | head -1 || true)
  if [ -z "${url:-}" ] || [ "$url" = "null" ]; then echo -e "${YELLOW}skip $out: asset not found ${repo} ${asset_match}${NC}"; return 1; fi
  echo -e "${GREEN}installing $out <- $url${NC}"
  if [[ "$url" == *.tar.gz ]]; then
    local tarball extract_dir found
    tarball="/tmp/${out}.tar.gz"
    extract_dir="/tmp/${out}_extract"
    rm -f "$tarball" 2>/dev/null || true
    rm -rf "$extract_dir" 2>/dev/null || true
    mkdir -p "$extract_dir"
    if ! curl -fsSL "$url" -o "$tarball" 2>/dev/null; then echo -e "${RED}fail: download $out gagal${NC}"; rm -f "$tarball" 2>/dev/null || true; return 1; fi
    if [ ! -s "$tarball" ]; then echo -e "${RED}fail: tarball $out kosong${NC}"; rm -f "$tarball" 2>/dev/null || true; return 1; fi
    if ! tar -xzf "$tarball" -C "$extract_dir" 2>/dev/null; then echo -e "${RED}fail: extract $out gagal${NC}"; rm -rf "$extract_dir" "$tarball" 2>/dev/null || true; return 1; fi
    # cari binary $out hanya di extract dir (jangan scan seluruh /tmp)
    found=$(find "$extract_dir" -type f -name "$out" 2>/dev/null | head -1 || true)
    if [ -n "${found:-}" ] && [ -f "$found" ]; then install -m 0755 "$found" "$BIN_DIR/$out"
    else echo -e "${YELLOW}warn: binary $out not found in tarball${NC}"; return 1; fi
    rm -rf "$extract_dir" "$tarball" 2>/dev/null || true
  else
    rm -f "$BIN_DIR/$out" 2>/dev/null || true
    if ! curl -fsSL "$url" -o "$BIN_DIR/$out" 2>/dev/null; then echo -e "${RED}fail: download $out gagal${NC}"; rm -f "$BIN_DIR/$out" 2>/dev/null || true; return 1; fi
    if [ ! -s "$BIN_DIR/$out" ]; then echo -e "${RED}fail: binary $out kosong${NC}"; rm -f "$BIN_DIR/$out" 2>/dev/null || true; return 1; fi
    chmod +x "$BIN_DIR/$out"
  fi
  "$BIN_DIR/$out" --version 2>/dev/null | head -1 || "$BIN_DIR/$out" version 2>/dev/null | head -1 || true
}

print_section() { echo ""; echo -e "${BLUE}════════════════════════════════════════════════${NC}"; echo -e "${BLUE}  $1${NC}"; echo -e "${BLUE}════════════════════════════════════════════════${NC}"; }

# ─────────────────────────────────────────────
# DEV STACK
# ─────────────────────────────────────────────
install_dev() {
  print_section "Dev stack (opencode/java/node/uv/rtk/codegraph/graphify)"

  # --- opencode ---
  if ! command_exists opencode; then
    curl -fsSL https://opencode.ai/install | bash
  else
    echo -e "${GREEN}opencode sudah ada: $(opencode --version 2>/dev/null | head -1)${NC}"
  fi

  # --- SDKMAN + Java 25 (backend/pom.xml:23) + Maven ---
  if [ ! -d "$HOME/.sdkman" ]; then
    curl -fsSL "https://get.sdkman.io" | bash
  fi
  # shellcheck disable=SC1091
  # sdkman internals need nounset off
  set +u
  # shellcheck disable=SC1091
  [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ] && source "$HOME/.sdkman/bin/sdkman-init.sh"
  if [ -d "$HOME/.sdkman/candidates/java/25-tem" ]; then
    sdk default java 25-tem 2>/dev/null || sdk use java 25-tem 2>/dev/null || true
  elif sdk current java 2>/dev/null | grep -q "25-tem"; then
    echo -e "${GREEN}java 25-tem sudah aktif${NC}"
  else
    sudo apt-get install -y -qq zip unzip 2>/dev/null || true
    sdk install java 25-tem 2>/dev/null || true
    sdk default java 25-tem 2>/dev/null || sdk use java 25-tem 2>/dev/null || true
  fi
  if ! command_exists mvn; then sdk install maven 2>/dev/null || true; else echo -e "${GREEN}maven sudah ada: $(mvn -v 2>/dev/null | head -1)${NC}"; fi
  set -u

  # --- Node.js 24 via nvm (frontend/web-app engines >=24) ---
  if [ ! -d "$HOME/.nvm" ]; then
    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash
  fi
  export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
  # shellcheck disable=SC1091
  # nvm.sh also needs nounset off
  set +u
  # shellcheck disable=SC1091
  [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
  set -u
  # nvm is a shell function, must run with nounset off to avoid unbound vars in nvm internals
  set +u
  if command -v nvm >/dev/null 2>&1; then
    nvm install 24 2>/dev/null || true
    nvm alias default 24 >/dev/null 2>&1 || true
    nvm use 24 2>/dev/null || true
  else
    echo -e "${YELLOW}skip node: nvm tidak tersedia setelah install${NC}"
  fi
  set -u
  command_exists node && node -v || true
  command_exists npm && npm -v || true

  # --- uv ---
  if ! command_exists uv; then
    curl -LsSf https://astral.sh/uv/install.sh | sh
    export PATH="$BIN_DIR:$HOME/.cargo/bin:$PATH"
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
      if command_exists podman; then podman machine init 2>/dev/null || true; podman machine start 2>/dev/null || true; fi
    else
      echo -e "${YELLOW}skip podman: no apt/dnf/brew — install manual${NC}"
    fi
  else
    echo -e "${GREEN}podman sudah ada: $(podman --version 2>/dev/null | head -1)${NC}"
  fi
  # podman-compose fallback via pipx/uv if apt didn't provide it
  if ! command_exists podman-compose; then
    if command_exists podman && podman compose version >/dev/null 2>&1; then
      echo -e "${GREEN}podman compose plugin sudah ada${NC}"
    else
      echo -e "${YELLOW}installing podman-compose...${NC}"
      if command_exists pipx; then pipx install podman-compose 2>/dev/null || true
      elif command_exists uv; then uv tool install podman-compose 2>/dev/null || true
      elif command_exists pip3; then pip3 install --user podman-compose 2>/dev/null || true
      fi
    fi
  fi
  if command_exists podman; then podman --version 2>/dev/null | head -1 || true; fi
  if command_exists podman-compose; then podman-compose --version 2>/dev/null | head -1 || true
  elif command_exists podman && podman compose version >/dev/null 2>&1; then podman compose version 2>/dev/null | head -1 || true
  fi

  # podman socket untuk Testcontainers (idempotent, no duplicate .bashrc)
  if command_exists podman && command_exists systemctl; then
    systemctl --user enable --now podman.socket 2>/dev/null || true
    local SOCK
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
  # NOTE: containers/skopeo tidak menyediakan binary Linux di GitHub releases,
  # jadi hanya install via package manager. Jangan pakai gh_release_bin fallback.
  if ! command_exists skopeo; then
    echo -e "${YELLOW}installing skopeo...${NC}"
    if command_exists apt-get; then
      sudo apt-get install -y -qq skopeo 2>/dev/null || true
    elif command_exists dnf; then
      sudo dnf install -y -q skopeo 2>/dev/null || true
    elif command_exists brew; then
      brew install skopeo 2>/dev/null || true
    fi
    if ! command_exists skopeo; then
      echo -e "${YELLOW}skip skopeo: install manual https://github.com/containers/skopeo/blob/main/INSTALL.md${NC}"
    fi
  else
    echo -e "${GREEN}skopeo sudah ada: $(skopeo --version 2>/dev/null | head -1)${NC}"
  fi

  # --- kubectl (stable) ---
  if ! command_exists kubectl; then
    echo -e "${GREEN}installing kubectl...${NC}"
    local KVER
    KVER=$(curl -fsSL https://dl.k8s.io/release/stable.txt 2>/dev/null || echo "v1.31.0")
    if [ -z "${KVER:-}" ]; then KVER="v1.31.0"; fi
    if curl -fsSL "https://dl.k8s.io/release/${KVER}/bin/linux/${ARCH}/kubectl" -o "$BIN_DIR/kubectl" 2>/dev/null && [ -s "$BIN_DIR/kubectl" ]; then
      chmod +x "$BIN_DIR/kubectl"
      kubectl version --client 2>/dev/null | head -1 || true
    else
      echo -e "${RED}fail: download kubectl ${KVER} gagal${NC}"
      rm -f "$BIN_DIR/kubectl" 2>/dev/null || true
    fi
  else echo -e "${GREEN}kubectl sudah ada: $(kubectl version --client 2>/dev/null | head -1)${NC}"; fi

  # --- oc (OpenShift CLI) — untuk oc get pods / oc apply -k ---
  if ! command_exists oc; then
    echo -e "${GREEN}installing oc...${NC}"
    # detect stable oc via mirror
    local OC_TAR
    OC_TAR="/tmp/openshift-client-linux.tar.gz"
    rm -f "$OC_TAR" 2>/dev/null || true
    if curl -fsSL "https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-linux.tar.gz" -o "$OC_TAR" 2>/dev/null && [ -s "$OC_TAR" ]; then
      if tar -xzf "$OC_TAR" -C /tmp 2>/dev/null; then
        if [ -f /tmp/oc ]; then install -m 0755 /tmp/oc "$BIN_DIR/oc" 2>/dev/null || true; fi
        if [ -f /tmp/kubectl ] && [ ! -f "$BIN_DIR/kubectl" ]; then install -m 0755 /tmp/kubectl "$BIN_DIR/kubectl" 2>/dev/null || true; fi
      else
        echo -e "${RED}fail: extract oc gagal${NC}"
      fi
      rm -f /tmp/oc /tmp/kubectl "$OC_TAR" 2>/dev/null || true
      command_exists oc && oc version --client 2>/dev/null | head -1 || true
    else echo -e "${YELLOW}skip oc: download failed${NC}"; rm -f "$OC_TAR" 2>/dev/null || true; fi
  else echo -e "${GREEN}oc sudah ada: $(oc version --client 2>/dev/null | head -1)${NC}"; fi

  # --- kustomize (standalone, oc sudah bundle tapi standalone berguna untuk local) ---
  if ! command_exists kustomize; then gh_release_bin "kubernetes-sigs/kustomize" "linux_${ARCH}" "kustomize" 2>/dev/null || true
  else echo -e "${GREEN}kustomize sudah ada: $(kustomize version --short 2>/dev/null || kustomize version 2>/dev/null | head -1)${NC}"; fi

  # --- helm (chart rendering, PayU pakai kustomize primary tapi helm berguna untuk operator charts) ---
  # helm rilis tarball di get.helm.sh, bukan GitHub asset .tar.gz langsung (GitHub cuma .asc). Pakai get.helm.sh.
  if ! command_exists helm; then
    local HELM_VER HELM_TAR
    HELM_VER="v3.18.4"
    HELM_TAR="/tmp/helm-${HELM_VER}-linux-${ARCH}.tar.gz"
    echo -e "${GREEN}installing helm ${HELM_VER} <- https://get.helm.sh/helm-${HELM_VER}-linux-${ARCH}.tar.gz${NC}"
    rm -f "$HELM_TAR" 2>/dev/null || true
    if curl -fsSL "https://get.helm.sh/helm-${HELM_VER}-linux-${ARCH}.tar.gz" -o "$HELM_TAR" 2>/dev/null && [ -s "$HELM_TAR" ]; then
      rm -rf "/tmp/linux-${ARCH:?}" 2>/dev/null || true
      if tar -xzf "$HELM_TAR" -C /tmp 2>/dev/null && [ -f "/tmp/linux-${ARCH}/helm" ]; then
        install -m 0755 "/tmp/linux-${ARCH}/helm" "$BIN_DIR/helm" 2>/dev/null || true
      else
        echo -e "${RED}fail: extract helm gagal${NC}"
      fi
      rm -rf "/tmp/linux-${ARCH:?}" "$HELM_TAR" 2>/dev/null || true
      command_exists helm && (helm version --short 2>/dev/null | head -1 || helm version 2>/dev/null | head -1) || true
    else echo -e "${YELLOW}skip helm: download failed${NC}"; rm -f "$HELM_TAR" 2>/dev/null || true; fi
  else echo -e "${GREEN}helm sudah ada: $(helm version --short 2>/dev/null | head -1 || helm version 2>/dev/null | head -1)${NC}"; fi

  # --- yq (YAML processor, dipakai scripts & kustomize patching) ---
  if ! command_exists yq; then gh_release_bin "mikefarah/yq" "yq_linux_${ARCH}" "yq" 2>/dev/null || true
  else echo -e "${GREEN}yq sudah ada: $(yq --version 2>/dev/null | head -1)${NC}"; fi

  # --- gh (GitHub CLI, untuk gh pr & API) ---
  if ! command_exists gh; then gh_release_bin "cli/cli" "linux_${ARCH}.tar.gz" "gh" 2>/dev/null || true
  else echo -e "${GREEN}gh sudah ada: $(gh --version 2>/dev/null | head -1)${NC}"; fi

  # --- tkn (Tekton CLI, untuk tkn pipeline/pipelinerun logs) ---
  if ! command_exists tkn; then
    # tektoncd/cli asset: tkn_0.46.0_Linux_x86_64.tar.gz (amd64) / tkn_..._Linux_aarch64.tar.gz (arm64)
    # NOTE: jangan pakai fallback contains("tkn_") — itu match Darwin duluan di head -1.
    local TKN_ARCH
    TKN_ARCH="x86_64"
    if [ "$ARCH" = "arm64" ]; then TKN_ARCH="aarch64"; fi
    gh_release_bin "tektoncd/cli" "Linux_${TKN_ARCH}.tar.gz" "tkn" 2>/dev/null || true
  else echo -e "${GREEN}tkn sudah ada: $(tkn version 2>/dev/null | head -1)${NC}"; fi

  # --- argocd (ArgoCD CLI, GitOps sync) ---
  if ! command_exists argocd; then gh_release_bin "argoproj/argo-cd" "argocd-linux-${ARCH}" "argocd" 2>/dev/null || true
  else echo -e "${GREEN}argocd sudah ada: $(argocd version --client 2>/dev/null | head -1)${NC}"; fi

  # --- jq (dependency gh_release_bin, pastiin ada untuk scripts lain) ---
  if ! command_exists jq; then ensure_jq; fi
  command_exists jq && echo -e "${GREEN}jq sudah ada: $(jq --version 2>/dev/null | head -1)${NC}" || true
}

check_versions() {
  print_section "Versi tools"
  for t in opencode java mvn node npm uv rtk codegraph graphify podman podman-compose skopeo tkn kustomize helm kubectl oc yq gh argocd jq python3; do
    if command_exists "$t"; then
      ver=""
      case "$t" in
        helm)      ver="$(helm version --short 2>/dev/null | head -1 || helm version 2>/dev/null | head -1)" ;;
        argocd)    ver="$(argocd version --client 2>/dev/null | head -1 || argocd version 2>/dev/null | head -1)" ;;
        kustomize) ver="$(kustomize version --short 2>/dev/null | head -1 || kustomize version 2>/dev/null | head -1)" ;;
        oc)        ver="$(oc version --client 2>/dev/null | head -1)" ;;
        kubectl)   ver="$(kubectl version --client 2>/dev/null | head -1)" ;;
        yq)        ver="$(yq --version 2>/dev/null | head -1)" ;;
        *)         ver="$("$t" --version 2>/dev/null | head -1 || "$t" version 2>/dev/null | head -1 || "$t" --version 2>&1 | head -1)" ;;
      esac
      # fallback generic if case produced empty
      if [ -z "$ver" ]; then ver="$("$t" --version 2>/dev/null | head -1 || "$t" version 2>/dev/null | head -1 || echo "installed")"; fi
      printf "${GREEN}%-18s${NC} %s\n" "$t" "$ver"
    else
      printf "${YELLOW}%-18s${NC} MISSING\n" "$t"
    fi
  done
  if command_exists podman && [ -S "/run/user/$(id -u)/podman/podman.sock" ]; then echo -e "${GREEN}podman socket        OK${NC}"
  elif command_exists podman; then echo -e "${YELLOW}podman socket        tidak aktif (systemctl --user start podman.socket)${NC}"; fi
  if command_exists java; then java -version 2>&1 | head -1 || true; fi
}

setup_podman_tc_only() {
  # shim for legacy --podman-tc from scripts/setup/install-tools.sh
  if command_exists podman && command_exists systemctl; then
    systemctl --user enable --now podman.socket 2>/dev/null || true
    local SOCK
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
    echo "  --dev     hanya dev stack (java/node/uv/rtk/codegraph/graphify/mcp)"
    echo "  --infra   hanya infra CLI (podman/skopeo/tkn/kustomize/helm/oc/kubectl/yq/gh/argocd)"
    echo "  --podman-tc hanya setup podman socket untuk Testcontainers (legacy)"
    echo "  --check   cek versi tools terinstall"
    echo "  --help    help"
    exit 0
    ;;
  *) install_dev; install_infra; check_versions ;;
esac

echo -e "${GREEN}Done. Pastikan ~/.local/bin di PATH (otomatis via .bashrc). Restart shell atau: source ~/.bashrc${NC}"
