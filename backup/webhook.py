#!/usr/bin/env python3
"""
Backup Webhook Service
提供HTTP API接口用于触发备份、恢复、查看日志等操作
"""

from flask import Flask, jsonify, request
import subprocess
import os
import json
import glob
from datetime import datetime

app = Flask(__name__)

BACKUP_DIR = "/backups"
LOG_FILE = f"{BACKUP_DIR}/backup.log"
METADATA_FILE = f"{BACKUP_DIR}/metadata.json"


def run_command(command, timeout=300):
    """执行shell命令并返回结果"""
    try:
        result = subprocess.run(
            command,
            shell=True,
            capture_output=True,
            text=True,
            timeout=timeout
        )
        return {
            "success": result.returncode == 0,
            "stdout": result.stdout,
            "stderr": result.stderr,
            "returncode": result.returncode
        }
    except subprocess.TimeoutExpired:
        return {
            "success": False,
            "error": f"Command timeout after {timeout} seconds"
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }


@app.route('/health', methods=['GET'])
def health_check():
    """健康检查"""
    return jsonify({
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat()
    })


@app.route('/backup/trigger', methods=['POST'])
def trigger_backup():
    """手动触发备份"""
    backup_type = request.json.get('type', 'manual') if request.json else 'manual'

    result = run_command('/scripts/backup.sh', timeout=600)

    if result['success']:
        return jsonify({
            "success": True,
            "message": "Backup completed successfully",
            "type": backup_type,
            "timestamp": datetime.utcnow().isoformat()
        })
    else:
        return jsonify({
            "success": False,
            "message": "Backup failed",
            "error": result.get('stderr', result.get('error', 'Unknown error'))
        }), 500


@app.route('/backup/list', methods=['GET'])
def list_backups():
    """列出所有备份文件"""
    backup_type = request.args.get('type', 'all')  # all, daily, weekly, monthly

    backups = []

    if backup_type in ['all', 'daily']:
        backups.extend(get_backup_files('daily'))
    if backup_type in ['all', 'weekly']:
        backups.extend(get_backup_files('weekly'))
    if backup_type in ['all', 'monthly']:
        backups.extend(get_backup_files('monthly'))

    # 按时间倒序排序
    backups.sort(key=lambda x: x['timestamp'], reverse=True)

    return jsonify({
        "success": True,
        "backups": backups,
        "total": len(backups)
    })


def get_backup_files(backup_type):
    """获取指定类型的备份文件"""
    backup_dir = f"{BACKUP_DIR}/{backup_type}"
    files = glob.glob(f"{backup_dir}/*.sql.gz")

    result = []
    for filepath in files:
        try:
            stat = os.stat(filepath)
            filename = os.path.basename(filepath)

            result.append({
                "filename": filename,
                "filepath": filepath,
                "type": backup_type,
                "size": stat.st_size,
                "timestamp": datetime.fromtimestamp(stat.st_mtime).isoformat(),
                "mtime": stat.st_mtime
            })
        except Exception as e:
            print(f"Error reading file {filepath}: {e}")

    return result


@app.route('/backup/restore', methods=['POST'])
def restore_backup():
    """恢复备份"""
    data = request.json
    if not data:
        return jsonify({"success": False, "error": "Missing request body"}), 400

    filename = data.get('filename')
    confirm_db_name = data.get('confirmDbName')

    # 验证必需参数
    if not filename:
        return jsonify({"success": False, "error": "Missing filename"}), 400

    if not confirm_db_name:
        return jsonify({"success": False, "error": "Missing confirmation"}), 400

    # 验证数据库名称
    expected_db_name = os.environ.get('DB_NAME', '')
    if confirm_db_name != expected_db_name:
        return jsonify({
            "success": False,
            "error": "Database name confirmation failed"
        }), 400

    # 查找备份文件
    backup_file = find_backup_file(filename)
    if not backup_file:
        return jsonify({
            "success": False,
            "error": f"Backup file not found: {filename}"
        }), 404

    # 执行恢复（非交互式，跳过5秒等待）
    result = run_command(f'/scripts/restore.sh "{backup_file}"', timeout=1800)

    if result['success']:
        return jsonify({
            "success": True,
            "message": "Database restored successfully",
            "filename": filename,
            "timestamp": datetime.utcnow().isoformat()
        })
    else:
        return jsonify({
            "success": False,
            "message": "Restore failed",
            "error": result.get('stderr', result.get('error', 'Unknown error'))
        }), 500


def find_backup_file(filename):
    """查找备份文件（在所有目录中搜索）"""
    for backup_type in ['daily', 'weekly', 'monthly']:
        filepath = f"{BACKUP_DIR}/{backup_type}/{filename}"
        if os.path.exists(filepath):
            return filepath
    return None


@app.route('/backup/logs', methods=['GET'])
def get_logs():
    """获取备份日志"""
    lines = request.args.get('lines', 100, type=int)
    log_type = request.args.get('type', 'backup')  # backup or restore

    log_file = f"{BACKUP_DIR}/{log_type}.log"

    if not os.path.exists(log_file):
        return jsonify({
            "success": True,
            "logs": [],
            "message": "Log file not found"
        })

    try:
        # 读取最后N行
        result = run_command(f'tail -n {lines} "{log_file}"')

        if result['success']:
            log_lines = result['stdout'].split('\n')
            return jsonify({
                "success": True,
                "logs": log_lines,
                "total": len(log_lines)
            })
        else:
            return jsonify({
                "success": False,
                "error": "Failed to read log file"
            }), 500
    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/backup/metadata', methods=['GET'])
def get_metadata():
    """获取备份元数据"""
    if not os.path.exists(METADATA_FILE):
        return jsonify({
            "success": True,
            "metadata": [],
            "message": "Metadata file not found"
        })

    try:
        with open(METADATA_FILE, 'r') as f:
            metadata = json.load(f)

        return jsonify({
            "success": True,
            "metadata": metadata
        })
    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/backup/status', methods=['GET'])
def get_status():
    """获取备份服务状态"""
    status = {
        "healthy": True,
        "disk_usage": get_disk_usage(),
        "latest_backups": {
            "daily": get_latest_backup('daily'),
            "weekly": get_latest_backup('weekly'),
            "monthly": get_latest_backup('monthly')
        },
        "retention_policy": {
            "daily": os.environ.get('BACKUP_RETENTION_DAYS', '7') + ' days',
            "weekly": os.environ.get('BACKUP_RETENTION_WEEKS', '4') + ' weeks',
            "monthly": os.environ.get('BACKUP_RETENTION_MONTHS', '6') + ' months'
        }
    }

    return jsonify({
        "success": True,
        "status": status
    })


def get_disk_usage():
    """获取磁盘使用情况"""
    try:
        result = run_command(f'df -h {BACKUP_DIR} | tail -1')
        if result['success']:
            parts = result['stdout'].split()
            if len(parts) >= 5:
                return {
                    "total": parts[1],
                    "used": parts[2],
                    "available": parts[3],
                    "use_percent": parts[4]
                }
    except Exception:
        pass

    return None


def get_latest_backup(backup_type):
    """获取最新的备份文件信息"""
    files = get_backup_files(backup_type)
    if files:
        # 已经按mtime排序
        latest = max(files, key=lambda x: x['mtime'])
        return {
            "filename": latest['filename'],
            "size": latest['size'],
            "timestamp": latest['timestamp']
        }
    return None


if __name__ == '__main__':
    # 监听所有接口，端口5000
    app.run(host='0.0.0.0', port=5000, debug=False)
