#!/bin/bash
#
# PayU Supporting Tools Installer
# ================================
# Menginstall CLI pendukung workflow PayU (infra GitOps + Testcontainers).
#
# Usage:
#   chmod +x install-tools.sh
#   ./install-tools.sh             # Install semua tools
#   ./install-tools.sh --podman-tc # Hanya aktifkan podman socket utk Testcontainers
#   ./install-tools.sh --infra     # Hanya CLI infra (kustomize, yq, gh, kubectl)
#   ./install-tools.sh --check     # Cek versi tools yang terinstall
#
# Supported: Linux amd64/arm64. Sebagian tool butuh sudo (install ke /usr/local/bin).
#
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

BIN_DIR="${HOME}/.local/bin"
mkdir -p "$BIN_DIR"
PATH="$BIN_DIR:$PATH"

detect_arch() {
    case "$(uname -m)" in
        x86_64|amd64) echo "amd64" ;;
        aarch64|arm64) echo "arm64" ;;
        *) echo -e "${RED}Unsupported arch: $(uname -m)${NC}"; exit 1 ;;
    esac
}
ARCH=$(detect_arch)

print_section() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

# Download + extract a GitHub-release binary to $BIN_DIR.
# Handles both tarball assets and single-binary assets.
# usage: gh_release_bin <repo> <asset-substring> <output-name>
gh_release_bin() {
    local repo=$1 asset_match=$2 out=$3 url
    url=$(curl -sL "https://api.github.com/repos/${repo}/releases/latest" \
        | jq -r '.assets[] | select(.name | contains("'"$asset_match"'")) | .browser_download_url' | head -1)
    if [ -z "$url" ] || [ "$url" = "null" ]; then
        echo -e "${YELLOW}skip ${out}: asset not found for ${repo}${NC}"
        return
    fi
    echo -e "${GREEN}installing ${out} <- ${url}${NC}"
    if [[ "$url" == *.tar.gz ]]; then
        curl -sL "$url" -o "/tmp/${out}.tar.gz"
        tar -xzf "/tmp/${out}.tar.gz" -C /tmp
        find /tmp -type f -name "$out" -exec install -m 0755 {} "$BIN_DIR/$out" \; 2>/dev/null || true
    else
        curl -sL "$url" -o "$BIN_DIR/$out"
        chmod +x "$BIN_DIR/$out"
    fi
    "$out" version 2>/dev/null || "$out" --version 2>/dev/null || true
}

setup_podman_testcontainers() {
    print_section "Podman socket untuk Testcontainers"
    systemctl --user enable --now podman.socket 2>&1
    SOCK="/run/user/$(id -u)/podman/podman.sock"
    if [ -S "$SOCK" ]; then
        echo -e "${GREEN}podman socket OK: $SOCK${NC}"
        cat >> "${HOME}/.bashrc" <<'EOF'

# Testcontainers via rootless podman (PayU integration tests)
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/run/user/$(id -u)/podman/podman.sock
EOF
        echo -e "${GREEN}Testcontainers env ditambahkan ke ~/.bashrc${NC}"
    else
        echo -e "${RED}podman socket tidak aktif — periksa 'systemctl --user status podman.socket'${NC}"
        return 1
    fi
}

install_infra_cli() {
    print_section "CLI infra (kustomize, yq, gh, kubectl)"

    if ! command -v kustomize >/dev/null; then
        gh_release_bin "kubernetes-sigs/kustomize" "linux_${ARCH}" "kustomize"
    else
        echo -e "${GREEN}kustomize sudah ada: $(kustomize version --short 2>/dev/null || kustomize version)${NC}"
    fi

    if ! command -v yq >/dev/null; then
        gh_release_bin "mikefarah/yq" "linux_${ARCH}" "yq"
    else
        echo -e "${GREEN}yq sudah ada: $(yq --version)${NC}"
    fi

    if ! command -v gh >/dev/null; then
        gh_release_bin "cli/cli" "linux_${ARCH}.tar.gz" "gh"
    else
        echo -e "${GREEN}gh sudah ada: $(gh --version | head -1)${NC}"
    fi

    if ! command -v kubectl >/dev/null; then
        VER=$(curl -sL https://dl.k8s.io/release/stable.txt)
        echo -e "${GREEN}installing kubectl ${VER}${NC}"
        curl -sL "https://dl.k8s.io/release/${VER}/bin/linux/${ARCH}/kubectl" -o "${BIN_DIR}/kubectl"
        chmod +x "${BIN_DIR}/kubectl"
        kubectl version --client 2>/dev/null || true
    else
        echo -e "${GREEN}kubectl sudah ada: $(kubectl version --client 2>/dev/null | head -1)${NC}"
    fi
}

check_installed() {
    print_section "Versi tools"
    for t in kustomize yq gh kubectl oc helm podman podman-compose node npm java mvn python3; do
        if command -v "$t" >/dev/null 2>&1; then
            printf "${GREEN}%-16s${NC} %s\n" "$t" "$("$t" --version 2>/dev/null | head -1)"
        else
            printf "${YELLOW}%-16s${NC} MISSING\n" "$t"
        fi
    done
    if [ -S "/run/user/$(id -u)/podman/podman.sock" ]; then
        echo -e "${GREEN}podman socket        OK${NC}"
    else
        echo -e "${YELLOW}podman socket        tidak aktif (jalankan --podman-tc)${NC}"
    fi
}

case "${1:-}" in
    --podman-tc) setup_podman_testcontainers ;;
    --infra)     install_infra_cli ;;
    --check)     check_installed ;;
    *)
        setup_podman_testcontainers
        install_infra_cli
        check_installed
        ;;
esac

echo -e "${GREEN}Done. Pastikan ~/.local/bin ada di PATH (biasanya otomatis via .bashrc).${NC}"
