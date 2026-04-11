#!/usr/bin/env python3
"""
Rental Commerce 로드맵 오케스트레이터.

전체 티켓 큐를 관리하고, 각 티켓에 대해 워크플로우 파이프라인을 자동 실행한다.

사용법:
  python3 harness-orchestrator.py add <id> <title> [--priority N]
  python3 harness-orchestrator.py add-bulk
  python3 harness-orchestrator.py queue
  python3 harness-orchestrator.py next
  python3 harness-orchestrator.py next-batch
  python3 harness-orchestrator.py current
  python3 harness-orchestrator.py done [ticket_id]
  python3 harness-orchestrator.py skip [ticket_id] [reason]
  python3 harness-orchestrator.py worker-prompt <ticket_id>
  python3 harness-orchestrator.py progress
  python3 harness-orchestrator.py agent-prompt
  python3 harness-orchestrator.py reset
"""

import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
QUEUE_FILE = SCRIPT_DIR / "orchestrator-queue.json"
WORKFLOW_SCRIPT = SCRIPT_DIR / "harness-workflow.py"


def load_queue():
    if QUEUE_FILE.exists():
        with open(QUEUE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return default_queue()


def save_queue(q):
    q["updated_at"] = datetime.now(timezone.utc).isoformat()
    with open(QUEUE_FILE, "w", encoding="utf-8") as f:
        json.dump(q, f, indent=2, ensure_ascii=False)


def default_queue():
    return {
        "tickets": [],
        "current_index": -1,
        "completed": [],
        "skipped": [],
        "started_at": None,
        "updated_at": None,
    }


def set_workflow_ticket(ticket_id, title, url=None):
    subprocess.run(
        ["python3", str(WORKFLOW_SCRIPT), "reset"],
        cwd=PROJECT_ROOT, capture_output=True,
    )
    args = ["python3", str(WORKFLOW_SCRIPT), "set-ticket", ticket_id, title]
    if url:
        args.append(url)
    subprocess.run(args, cwd=PROJECT_ROOT, capture_output=True)


def cmd_add(args):
    if len(args) < 2:
        print("Usage: add <id> <title> [--priority N] [--url URL] [--deps id1,id2]", file=sys.stderr)
        sys.exit(1)
    ticket_id = args[0]
    title = args[1]
    priority = 0
    url = None
    deps = []
    i = 2
    while i < len(args):
        if args[i] == "--priority" and i + 1 < len(args):
            priority = int(args[i + 1]); i += 2
        elif args[i] == "--url" and i + 1 < len(args):
            url = args[i + 1]; i += 2
        elif args[i] == "--deps" and i + 1 < len(args):
            deps = args[i + 1].split(","); i += 2
        else:
            i += 1
    q = load_queue()
    for t in q["tickets"]:
        if t["id"] == ticket_id:
            print(f"이미 존재하는 티켓: {ticket_id}", file=sys.stderr); sys.exit(1)
    q["tickets"].append({
        "id": ticket_id, "title": title, "url": url,
        "priority": priority, "deps": deps, "status": "pending",
        "added_at": datetime.now(timezone.utc).isoformat(),
    })
    if not q["started_at"]:
        q["started_at"] = datetime.now(timezone.utc).isoformat()
    save_queue(q)
    print(f"티켓 추가: {ticket_id} — {title} (priority: {priority})")


def cmd_add_bulk(args):
    try:
        raw = sys.stdin.read()
        data = json.loads(raw)
    except (json.JSONDecodeError, KeyError):
        print("JSON 파싱 실패.", file=sys.stderr); sys.exit(1)
    tickets = data if isinstance(data, list) else data.get("tickets", [])
    q = load_queue()
    existing_ids = {t["id"] for t in q["tickets"]}
    added = 0
    for item in tickets:
        tid = item.get("id") or item.get("ticket_id")
        title = item.get("title") or item.get("name", "Untitled")
        if not tid or tid in existing_ids:
            continue
        q["tickets"].append({
            "id": tid, "title": title, "url": item.get("url"),
            "priority": item.get("priority", 0), "deps": item.get("deps", []),
            "status": "pending", "added_at": datetime.now(timezone.utc).isoformat(),
        })
        existing_ids.add(tid); added += 1
    if not q["started_at"]:
        q["started_at"] = datetime.now(timezone.utc).isoformat()
    save_queue(q)
    print(f"{added}개 티켓 추가 완료 (총 {len(q['tickets'])}개)")


def _get_available_tickets(q):
    completed_ids = set(q["completed"])
    available = []
    for i, t in enumerate(q["tickets"]):
        if t["status"] != "pending":
            continue
        deps_met = all(d in completed_ids for d in t.get("deps", []))
        if deps_met:
            available.append((i, t))
    return available


def cmd_queue(args):
    q = load_queue()
    tickets = q["tickets"]
    if not tickets:
        print("큐가 비어있습니다."); return
    current_idx = q["current_index"]
    print(f"로드맵 큐 ({len(tickets)}개 티켓)")
    print(f"{'─'*60}")
    for i, t in enumerate(tickets):
        if t["status"] == "done": marker = "✅"
        elif t["status"] == "skipped": marker = "⏭️"
        elif i == current_idx: marker = "▶️"
        else: marker = "⏳"
        deps = f" (deps: {','.join(t['deps'])})" if t.get("deps") else ""
        print(f"  {marker} {i+1}. [{t['id']}] {t['title']}{deps}")


def cmd_next(args):
    q = load_queue()
    available = _get_available_tickets(q)
    if not available:
        remaining = sum(1 for t in q["tickets"] if t["status"] == "pending")
        print("모든 티켓 완료!" if remaining == 0 else f"의존성 미충족 pending 티켓 {remaining}개.")
        return
    next_idx, next_ticket = available[0]
    next_ticket["status"] = "in_progress"
    q["current_index"] = next_idx
    save_queue(q)
    set_workflow_ticket(next_ticket["id"], next_ticket["title"], next_ticket.get("url"))
    total = len(q["tickets"])
    done = sum(1 for t in q["tickets"] if t["status"] == "done")
    print(f"[오케스트레이터] 다음 티켓 시작 ({done+1}/{total})")
    print(f"  ID: {next_ticket['id']}")
    print(f"  제목: {next_ticket['title']}")
    print(f"  → 테스트 케이스부터 작성하세요.")


def cmd_current(args):
    q = load_queue()
    idx = q["current_index"]
    if idx < 0 or idx >= len(q["tickets"]):
        print("현재 작업 중인 티켓 없음."); return
    t = q["tickets"][idx]
    print(f"현재 티켓: [{t['id']}] {t['title']} ({t['status']})")


def cmd_done(args):
    q = load_queue()
    if args:
        ticket_id = args[0]
        for t in q["tickets"]:
            if t["id"] == ticket_id:
                t["status"] = "done"
                t["completed_at"] = datetime.now(timezone.utc).isoformat()
                if ticket_id not in q["completed"]:
                    q["completed"].append(ticket_id)
                save_queue(q)
                done_count = sum(1 for x in q["tickets"] if x["status"] == "done")
                print(f"[오케스트레이터] {ticket_id} 완료! ({done_count}/{len(q['tickets'])})")
                return
        print(f"Ticket {ticket_id} not found", file=sys.stderr); sys.exit(1)
    idx = q["current_index"]
    if idx < 0 or idx >= len(q["tickets"]):
        print("현재 작업 중인 티켓 없음."); return
    t = q["tickets"][idx]
    t["status"] = "done"
    t["completed_at"] = datetime.now(timezone.utc).isoformat()
    q["completed"].append(t["id"])
    q["current_index"] = -1
    save_queue(q)
    done_count = sum(1 for x in q["tickets"] if x["status"] == "done")
    remaining = len(q["tickets"]) - done_count
    print(f"[오케스트레이터] {t['id']} 완료! ({done_count}/{len(q['tickets'])})")
    if remaining > 0:
        print(f"  남은 티켓: {remaining}개"); cmd_next([])
    else:
        print("  모든 티켓 완료!")


def cmd_skip(args):
    q = load_queue()
    if args and any(t["id"] == args[0] for t in q["tickets"]):
        ticket_id = args[0]
        reason = " ".join(args[1:]) if len(args) > 1 else "skipped"
        for t in q["tickets"]:
            if t["id"] == ticket_id:
                t["status"] = "skipped"; t["skip_reason"] = reason
                q["skipped"].append({"id": ticket_id, "reason": reason})
                save_queue(q)
                print(f"[오케스트레이터] {ticket_id} 스킵 ({reason})"); return
    else:
        idx = q["current_index"]
        if idx < 0 or idx >= len(q["tickets"]):
            print("현재 작업 중인 티켓 없음."); return
        t = q["tickets"][idx]
        reason = " ".join(args) if args else "skipped"
        t["status"] = "skipped"; t["skip_reason"] = reason
        q["skipped"].append({"id": t["id"], "reason": reason})
        q["current_index"] = -1
        save_queue(q)
        print(f"[오케스트레이터] {t['id']} 스킵 ({reason})"); cmd_next([])


def cmd_progress(args):
    q = load_queue()
    tickets = q["tickets"]
    if not tickets:
        print("큐가 비어있습니다."); return
    total = len(tickets)
    done = sum(1 for t in tickets if t["status"] == "done")
    in_progress = sum(1 for t in tickets if t["status"] == "in_progress")
    pending = sum(1 for t in tickets if t["status"] == "pending")
    pct = done * 100 // total if total > 0 else 0
    bar_len = 30
    filled = bar_len * done // total if total > 0 else 0
    bar = "█" * filled + "░" * (bar_len - filled)
    print(f"진행률: [{bar}] {pct}%")
    print(f"  ✅ 완료: {done} | 🔧 진행중: {in_progress} | ⏳ 대기: {pending} | 총: {total}")


def cmd_reset(args):
    q = default_queue()
    save_queue(q)
    subprocess.run(["python3", str(WORKFLOW_SCRIPT), "reset"], cwd=PROJECT_ROOT, capture_output=True)
    print("[오케스트레이터] 큐 초기화 + 워크플로우 리셋 완료")


def main():
    if len(sys.argv) < 2:
        print(__doc__); sys.exit(1)
    cmd = sys.argv[1]
    args = sys.argv[2:]
    commands = {
        "add": cmd_add, "add-bulk": cmd_add_bulk, "queue": cmd_queue,
        "next": cmd_next, "current": cmd_current, "done": cmd_done,
        "skip": cmd_skip, "progress": cmd_progress, "reset": cmd_reset,
    }
    if cmd not in commands:
        print(f"Unknown command: {cmd}", file=sys.stderr); sys.exit(1)
    commands[cmd](args)


if __name__ == "__main__":
    main()
