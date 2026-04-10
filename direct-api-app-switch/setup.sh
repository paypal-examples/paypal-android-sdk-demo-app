#!/usr/bin/env bash
# PayPal App Switch Android Sample — Zero-Config Setup
#
# Usage: ./setup.sh

set -euo pipefail

# ── Colors ────────────────────────────────────────────────────────────────────

GREEN='\033[32m'
RED='\033[31m'
YELLOW='\033[33m'
CYAN='\033[36m'
BOLD='\033[1m'
RESET='\033[0m'

DASHBOARD_URL="https://developer.paypal.com/dashboard/applications/sandbox"
PROPS_FILE="local.properties"
PROPS_EXAMPLE="local.properties.example"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$SCRIPT_DIR"

echo ""
echo -e "${BOLD}${CYAN}PayPal App Switch Android Sample Setup${RESET}"
echo ""

# ── Step 1: Ensure local.properties exists ────────────────────────────────────

if [ -f "$PROPS_FILE" ]; then
    echo -e "${GREEN}local.properties already exists.${RESET}"
else
    if [ -f "$PROPS_EXAMPLE" ]; then
        cp "$PROPS_EXAMPLE" "$PROPS_FILE"
        echo -e "${GREEN}Created local.properties from local.properties.example.${RESET}"
    else
        cat > "$PROPS_FILE" <<EOF
# PayPal Sandbox Credentials
PAYPAL_CLIENT_ID=
PAYPAL_CLIENT_SECRET=
RETURN_DOMAIN=https://example.com
EOF
        echo -e "${GREEN}Created local.properties with defaults.${RESET}"
    fi
fi

# ── Step 2: Check if credentials are set ──────────────────────────────────────

CLIENT_ID=$(grep -E '^PAYPAL_CLIENT_ID=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
CLIENT_SECRET=$(grep -E '^PAYPAL_CLIENT_SECRET=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')

if [ -z "$CLIENT_ID" ] || [ -z "$CLIENT_SECRET" ]; then
    echo ""
    echo -e "${BOLD}Paste your sandbox credentials.${RESET}"
    echo -e "Get them at: ${CYAN}${DASHBOARD_URL}${RESET}"
    echo ""

    read -rp "Client ID: " CLIENT_ID
    read -rp "Client Secret: " CLIENT_SECRET

    if [ -z "$CLIENT_ID" ] || [ -z "$CLIENT_SECRET" ]; then
        echo -e "${RED}Both Client ID and Client Secret are required.${RESET}"
        exit 1
    fi

    # Write credentials into local.properties (replace existing lines)
    if grep -q '^PAYPAL_CLIENT_ID=' "$PROPS_FILE"; then
        sed -i '' "s|^PAYPAL_CLIENT_ID=.*|PAYPAL_CLIENT_ID=${CLIENT_ID}|" "$PROPS_FILE"
    else
        echo "PAYPAL_CLIENT_ID=${CLIENT_ID}" >> "$PROPS_FILE"
    fi

    if grep -q '^PAYPAL_CLIENT_SECRET=' "$PROPS_FILE"; then
        sed -i '' "s|^PAYPAL_CLIENT_SECRET=.*|PAYPAL_CLIENT_SECRET=${CLIENT_SECRET}|" "$PROPS_FILE"
    else
        echo "PAYPAL_CLIENT_SECRET=${CLIENT_SECRET}" >> "$PROPS_FILE"
    fi

    echo ""
    echo -e "${GREEN}Credentials saved to local.properties.${RESET}"
else
    echo -e "${GREEN}Credentials already configured.${RESET}"
fi

# ── Step 3: Print instructions ────────────────────────────────────────────────

echo ""
echo -e "${BOLD}Quick Start:${RESET}"
echo -e "  1. Open this folder in ${CYAN}Android Studio${RESET}"
echo -e "  2. Wait for Gradle sync to complete"
echo -e "  3. Select a device or emulator (API 26+)"
echo -e "  4. Press ${CYAN}Run${RESET} (Shift+F10)"
echo -e "  5. Tap ${CYAN}Pay with PayPal${RESET} to test App Switch"
echo ""
echo -e "  Or from the command line:"
echo -e "    ${CYAN}./gradlew assembleDebug${RESET}"
echo ""
