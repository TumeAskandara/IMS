#!/bin/bash
# ==================================================
# Let's Encrypt certificate renewal script
# Run via cron: 0 3 * * * /path/to/ssl-renew.sh
# ==================================================

set -euo pipefail

COMPOSE_FILE="/opt/ims/docker-compose.yml"

echo "[$(date)] Starting certificate renewal..."

# Attempt renewal
docker compose -f "$COMPOSE_FILE" run --rm certbot renew --quiet

# Reload nginx to pick up new certs
docker compose -f "$COMPOSE_FILE" exec nginx nginx -s reload

echo "[$(date)] Certificate renewal complete."
