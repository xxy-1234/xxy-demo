#!/usr/bin/env bash
# 在 ECS 上执行（由 GitHub Actions 上传后 ssh 调用）。需 root 或具备 systemctl 权限。
set -euo pipefail
JAR="/root/xxy-demo/xxy-demo-0.0.1-SNAPSHOT.jar"
UNIT_SRC="/tmp/xxy-demo.service"
ENV_FILE="/root/xxy-demo/xxy-demo.env"

if [[ -f "$ENV_FILE" ]]; then
  echo "Using systemd + $ENV_FILE"
  install -m 644 "$UNIT_SRC" /etc/systemd/system/xxy-demo.service
  pkill -f "xxy-demo-0.0.1-SNAPSHOT.jar" || true
  sleep 2
  systemctl daemon-reload
  systemctl enable xxy-demo
  systemctl restart xxy-demo
  sleep 3
  if ! systemctl is-active --quiet xxy-demo; then
    echo "=== systemctl status ==="
    systemctl status xxy-demo --no-pager || true
    echo "=== journal (last 40 lines) ==="
    journalctl -u xxy-demo -n 40 --no-pager || true
    exit 1
  fi
  echo "xxy-demo is active."
else
  echo "No $ENV_FILE — using MYSQL_* from environment (GitHub Secrets path)"
  if [[ -z "${MYSQL_PASSWORD:-}" ]]; then
    echo "ERROR: 请在 ECS 创建 $ENV_FILE，或在 GitHub 配置 Secret MYSQL_PASSWORD"
    exit 1
  fi
  install -m 644 "$UNIT_SRC" /etc/systemd/system/xxy-demo.service 2>/dev/null || true
  cd /root/xxy-demo
  pkill -f "xxy-demo-0.0.1-SNAPSHOT.jar" || true
  sleep 1
  export MYSQL_HOST MYSQL_PORT MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD
  nohup java -Dspring.profiles.active=prod -jar "$JAR" >> app.log 2>&1 &
  sleep 8
  if ! pgrep -f "xxy-demo-0.0.1-SNAPSHOT.jar" >/dev/null; then
    echo "=== Java did not stay up; tail app.log ==="
    tail -n 60 app.log || true
    exit 1
  fi
  echo "nohup java is running."
fi
