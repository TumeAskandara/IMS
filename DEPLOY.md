# VPS Deployment Guide

## Prerequisites
- Ubuntu 22.04+ VPS with root access
- Domain name pointing to VPS IP
- GitHub repository with Actions secrets configured

## 1. Initial VPS Setup

### Install Docker
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# Log out and back in
```

### Harden SSH
```bash
# Edit /etc/ssh/sshd_config:
#   Port <your-custom-port>
#   PermitRootLogin no
#   PasswordAuthentication no
#   PubkeyAuthentication yes
sudo systemctl restart sshd
```

### Configure UFW Firewall
```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow <your-ssh-port>/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### Install fail2ban
```bash
sudo apt install fail2ban -y
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

## 2. Application Setup

### Clone and Configure
```bash
sudo mkdir -p /opt/ims
sudo chown $USER:$USER /opt/ims
cd /opt/ims
git clone <your-repo-url> .
cp .env.example .env
```

### Edit `.env`
Generate strong values:
```bash
# Generate database password
openssl rand -base64 32

# Generate JWT secret (64-char hex)
openssl rand -hex 32
```

Fill in all values in `.env` — database credentials, JWT secret, domain, and email.

### Initial SSL Certificate
```bash
# First, update nginx.conf: replace 'your-domain.com' with your actual domain

# Start nginx temporarily for the ACME challenge
docker compose up -d nginx

# Obtain certificate
docker compose run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d your-domain.com \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email

# Restart everything
docker compose down
docker compose up -d
```

### Set Up Auto-Renewal Cron
```bash
chmod +x nginx/ssl-renew.sh
# Add to crontab:
# 0 3 * * * /opt/ims/nginx/ssl-renew.sh >> /var/log/ssl-renew.log 2>&1
crontab -e
```

## 3. GitHub Actions Secrets

Add these in your repo → Settings → Secrets and variables → Actions:

| Secret | Value |
|--------|-------|
| `SSH_HOST` | Your VPS IP address |
| `SSH_USER` | Your SSH username |
| `SSH_KEY` | Contents of your private SSH key |
| `SSH_PORT` | Your custom SSH port |

## 4. Verify Deployment

```bash
# All services running
docker compose ps

# Health check
curl -k https://localhost/actuator/health

# Security headers
curl -I https://your-domain.com

# HTTP redirects to HTTPS
curl -I http://your-domain.com

# PostgreSQL NOT accessible from host (should fail)
psql -h localhost -p 7432 -U imsadmin imsdb
```

## 5. Maintenance

```bash
# View logs
docker compose logs -f app
docker compose logs -f nginx

# Restart services
docker compose restart app

# Update manually
docker compose pull app
docker compose up -d --remove-orphans

# Database backup
docker compose exec postgres pg_dump -U imsadmin -p 7432 imsdb > backup_$(date +%Y%m%d).sql
```
