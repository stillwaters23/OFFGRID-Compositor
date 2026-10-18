#!/usr/bin/env python3
"""Independent adb walk for PR 56 head 6c7936e.

Taps use centers from `uiautomator dump`. This is not RoyaltyPhoneFixTest.
Cold starts record ActivityTaskManager Displayed lines and any ANR dialog.
"""

from __future__ import annotations

import json
import os
import re
import struct
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(os.environ.get("GITHUB_WORKSPACE", Path(__file__).resolve().parents[3]))
EVID = ROOT / "docs" / "qa" / "independent_qa" / "evidence"
DUMPS = EVID / "dumps"
STEPS = EVID / "steps"
COLD = EVID / "cold"
PACKAGE = "com.offgrid.compositor"
ACTIVITY = "com.offgrid.compositor/.MainActivity"
EMPTY = "com.qa.empty/.EmptyActivity"
WIDTH = 1080
HEIGHT = 2400

results: dict = {"items": {}, "cold": {}}


def progress(message: str) -> None:
    line = f"PROGRESS {time.strftime('%H:%M:%S', time.gmtime())} {message}"
    print(line, flush=True)
    try:
        EVID.mkdir(parents=True, exist_ok=True)
        with (EVID / "progress.log").open("a", encoding="utf-8") as handle:
            handle.write(line + "\n")
    except OSError:
        pass


def adb(args: list[str], timeout: int = 120) -> subprocess.CompletedProcess:
    shown = " ".join(args)
    progress(f"adb({timeout}s) {shown}")
    try:
        completed = subprocess.run(
            ["timeout", "--signal=KILL", str(timeout), "adb", *args],
            capture_output=True,
            text=True,
            timeout=timeout + 15,
        )
    except subprocess.TimeoutExpired as error:
        progress(f"TIMEOUT adb {shown}")
        return subprocess.CompletedProcess(args, 124, error.stdout or "", error.stderr or "timeout")
    if completed.returncode in (124, 137):
        progress(f"TIMEOUT({timeout}s) adb {shown}")
    return completed


def shot(name: str) -> None:
    path = STEPS / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    progress(f"screencap {name}")
    try:
        with path.open("wb") as handle:
            subprocess.run(
                ["timeout", "--signal=KILL", "20", "adb", "exec-out", "screencap", "-p"],
                stdout=handle,
                timeout=30,
                check=False,
            )
    except subprocess.TimeoutExpired:
        progress(f"TIMEOUT screencap {name}")
        path.write_bytes(b"")


def logcat(*filters: str) -> str:
    args = ["logcat", "-d", "-v", "time"]
    if filters:
        args.append("-s")
        args.extend(filters)
    return adb(args, timeout=20).stdout or ""


def parse_nodes(path: Path) -> list[dict]:
    if not path.exists() or path.stat().st_size == 0:
        return []
    try:
        tree = ET.parse(path)
    except ET.ParseError:
        return []
    found = []
    for element in tree.iter("node"):
        match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", element.attrib.get("bounds", ""))
        if not match:
            continue
        x1, y1, x2, y2 = (int(part) for part in match.groups())
        found.append(
            {
                "text": element.attrib.get("text", ""),
                "desc": element.attrib.get("content-desc", ""),
                "bounds": (x1, y1, x2, y2),
                "clickable": element.attrib.get("clickable") == "true",
                "checked": element.attrib.get("checked") == "true",
                "class": element.attrib.get("class", ""),
            }
        )
    return found


def dump(name: str) -> list[dict]:
    DUMPS.mkdir(parents=True, exist_ok=True)
    remote = "/data/local/tmp/qa_ui.xml"
    local = DUMPS / f"{name}.xml"
    progress(f"uiautomator dump {name}")
    adb(["shell", "rm", "-f", remote], timeout=15)
    for attempt in range(2):
        ran = adb(["shell", "uiautomator", "dump", remote], timeout=25)
        text = f"{ran.stdout}\n{ran.stderr}"
        if "hierchary dumped" in text or "hierarchy dumped" in text:
            break
        progress(f"uiautomator dump {name} retry {attempt + 1}")
        time.sleep(0.5)
    adb(["pull", remote, str(local)], timeout=20)
    nodes = parse_nodes(local)
    # The immersive hint is its own window. A plain dump then hides the app.
    if any(node["text"] == "Viewing full screen" for node in nodes):
        progress(f"uiautomator dump {name} windows")
        ran = adb(["shell", "uiautomator", "dump", "--windows", remote], timeout=25)
        if ran.returncode == 0:
            adb(["pull", remote, str(local.with_name(local.stem + "_windows.xml"))], timeout=20)
            windows = parse_nodes(local.with_name(local.stem + "_windows.xml"))
            if any(node["text"] in {"Unlock", "LOCKED"} or "Lock the panel" in node["desc"] for node in windows):
                return windows
    return nodes


def blob(nodes: list[dict]) -> str:
    return "\n".join(f"{node['text']}\n{node['desc']}" for node in nodes)


def on_home(nodes: list[dict]) -> bool:
    text = blob(nodes)
    return "AR Video Deck folder" in text and "Panel settings folder" in text


def center(node: dict) -> tuple[int, int]:
    x1, y1, x2, y2 = node["bounds"]
    return (x1 + x2) // 2, (y1 + y2) // 2


def on_screen(node: dict) -> bool:
    x, y = center(node)
    x1, y1, x2, y2 = node["bounds"]
    return x2 - x1 > 4 and y2 - y1 > 4 and 0 <= x < WIDTH and 0 <= y < HEIGHT


def hint_boxes(nodes: list[dict]) -> list[tuple[int, int, int, int]]:
    boxes = []
    for node in nodes:
        text = f"{node['text']} {node['desc']}"
        if "Viewing full screen" in text or node["text"] in {"Got it", "GOT IT"}:
            boxes.append(node["bounds"])
    return boxes


def covered_by_hint(node: dict, nodes: list[dict]) -> bool:
    x, y = center(node)
    x1, y1, x2, y2 = node["bounds"]
    for hx1, hy1, hx2, hy2 in hint_boxes(nodes):
        if hx1 <= x <= hx2 and hy1 <= y <= hy2:
            return True
        if not (x2 < hx1 or hx2 < x1 or y2 < hy1 or hy2 < y1):
            overlap_h = min(y2, hy2) - max(y1, hy1)
            if overlap_h > 8:
                return True
    return False


def find(nodes: list[dict], needle: str, exact: bool = False) -> dict | None:
    hits = []
    low = needle.lower()
    for node in nodes:
        if not on_screen(node):
            continue
        fields = (node["desc"], node["text"])
        if exact:
            matched = any(field == needle for field in fields)
        else:
            matched = any(low in field.lower() for field in fields)
        if matched:
            hits.append(node)
    if not hits:
        return None

    def score(node: dict) -> tuple:
        x1, y1, x2, y2 = node["bounds"]
        area = (x2 - x1) * (y2 - y1)
        desc_hit = 0 if (node["desc"] == needle or low in node["desc"].lower()) else 1
        clickable = 0 if node["clickable"] else 1
        return (desc_hit, clickable, area)

    hits.sort(key=score)
    return hits[0]


def tap(node: dict) -> None:
    x, y = center(node)
    adb(["shell", "input", "tap", str(x), str(y)], timeout=20)


def long_press(node: dict) -> None:
    x, y = center(node)
    adb(["shell", "input", "swipe", str(x), str(y), str(x), str(y), "900"], timeout=20)


def swipe_list() -> None:
    adb(["shell", "input", "swipe", "540", "1700", "540", "900", "280"], timeout=20)


def key_back() -> None:
    adb(["shell", "input", "keyevent", "4"], timeout=15)


def anr_dialog(nodes: list[dict]) -> bool:
    text = blob(nodes)
    curly = "isn\u2019t responding"
    if "isn't responding" in text or curly in text:
        return True
    return "Wait" in text and ("Close app" in text or "Close App" in text)


def anr_lines(log: str) -> list[str]:
    return [line for line in log.splitlines() if "ANR in" in line or "isn't responding" in line or "isn\u2019t responding" in line]


def displayed_ms(line: str) -> int | None:
    match = re.search(r"\+(?:(\d+)m)?(?:(\d+)s)?(?:(\d+)ms)?", line)
    if not match or not any(match.groups()):
        return None
    minutes = int(match.group(1) or 0)
    seconds = int(match.group(2) or 0)
    millis = int(match.group(3) or 0)
    return minutes * 60000 + seconds * 1000 + millis


def read_screen() -> tuple[int, int]:
    global WIDTH, HEIGHT
    ran = adb(["shell", "wm", "size"], timeout=15)
    match = re.findall(r"(\d+)x(\d+)", ran.stdout or "")
    if match:
        WIDTH, HEIGHT = (int(part) for part in match[-1])
    return WIDTH, HEIGHT


def yellow_sample() -> int | None:
    try:
        progress("screencap raw yellow sample")
        raw = subprocess.run(
            ["timeout", "--signal=KILL", "20", "adb", "exec-out", "screencap"],
            capture_output=True,
            timeout=30,
        ).stdout
    except subprocess.TimeoutExpired:
        return None
    if len(raw) < 16:
        return None
    width, height, _fmt = struct.unpack_from("<III", raw, 0)
    pixels = raw[12:]
    expected = width * height * 4
    if width < 100 or height < 100 or len(pixels) < expected:
        return None
    count = 0
    step = 16 * 4
    for index in range(0, expected, step):
        red, green, blue = pixels[index], pixels[index + 1], pixels[index + 2]
        if red > 200 and green > 170 and blue < 90:
            count += 1
    return count


def cold_one(kind: str, index: int, start_args: list[str], displayed_prefix: str, early_yellow: bool) -> dict:
    name = f"{kind}_{index:02d}"
    progress(f"cold {name} begin")
    adb(["shell", "am", "force-stop", start_args[0]], timeout=20)
    time.sleep(0.5)
    adb(["logcat", "-c"], timeout=20)
    yellow = None
    if early_yellow:
        progress(f"cold {name} am start")
        proc = subprocess.Popen(
            ["timeout", "--signal=KILL", "45", "adb", "shell", "am", "start", "-W", *start_args[1:]],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        time.sleep(1.2)
        yellow = yellow_sample()
        shot(f"../cold/{name}_early")
        try:
            out, err = proc.communicate(timeout=55)
        except subprocess.TimeoutExpired:
            proc.kill()
            out, err = proc.communicate()
            progress(f"TIMEOUT cold {name} am start")
        started = f"{out}\n{err}"
    else:
        ran = adb(["shell", "am", "start", "-W", *start_args[1:]], timeout=45)
        started = f"{ran.stdout}\n{ran.stderr}"
    deadline = time.time() + 20
    shown = ""
    while time.time() < deadline:
        log = logcat("ActivityTaskManager:I", "ActivityManager:E", "OffgridStartup:I")
        for line in log.splitlines():
            if "Displayed" in line and displayed_prefix in line:
                shown = line.strip()
                break
        if shown:
            break
        time.sleep(0.5)
    log = logcat("ActivityTaskManager:I", "ActivityManager:E", "OffgridStartup:I")
    if not shown:
        for line in log.splitlines():
            if "Displayed" in line and displayed_prefix in line:
                shown = line.strip()
                break
    nodes = dump(f"cold_{name}")
    shot(f"../cold/{name}")
    (COLD / f"{name}.log").write_text(started + "\n\n" + log, encoding="utf-8", errors="replace")
    dialog = anr_dialog(nodes)
    lines = anr_lines(log)
    startup = [line.strip() for line in log.splitlines() if "OffgridStartup" in line]
    row = {
        "displayed": shown,
        "displayed_ms": displayed_ms(shown) if shown else None,
        "status_ok": "Status: ok" in started,
        "launch_state": (re.search(r"LaunchState:\s*(\S+)", started) or [None, None])[1],
        "anr_dialog": dialog,
        "anr_lines": lines,
        "anr_in_target": any(displayed_prefix.split("/")[0] in line for line in lines),
        "startup": startup,
        "yellow_sample": yellow,
        "start_output": started.strip()[:1200],
    }
    print(f"COLD {name} displayed={row['displayed']} anr_dialog={dialog} anr_lines={len(lines)}", flush=True)
    return row


def cold_series() -> None:
    COLD.mkdir(parents=True, exist_ok=True)
    settings = []
    for index in range(1, 11):
        settings.append(
            cold_one(
                "settings",
                index,
                ["com.android.settings", "-a", "android.settings.SETTINGS"],
                "com.android.settings",
                False,
            )
        )
    empty = []
    for index in range(1, 11):
        empty.append(cold_one("empty", index, ["com.qa.empty", "-n", EMPTY], "com.qa.empty", False))
    offgrid = []
    for index in range(1, 11):
        offgrid.append(
            cold_one("offgrid", index, [PACKAGE, "-n", ACTIVITY], "com.offgrid.compositor", index == 1)
        )
    results["cold"] = {"settings": settings, "empty": empty, "offgrid": offgrid}
    seed_host_log(offgrid)


def seed_host_log(offgrid: list[dict]) -> None:
    """The instrumented cold-start test reads this file. Write it only after our own 10."""
    clean = True
    lines = []
    for index, row in enumerate(offgrid, 1):
        lines.append(f"independent_cold_{index}")
        lines.append(row.get("displayed") or "displayed=missing")
        lines.append(f"anr_dialog={row['anr_dialog']} status_ok={row['status_ok']} launch={row['launch_state']}")
        if row["anr_dialog"] or row["anr_in_target"] or not row["status_ok"]:
            clean = False
        if row["launch_state"] not in (None, "COLD", "UNKNOWN"):
            clean = False
            lines.append(f"launch_note={row['launch_state']}")
    if clean:
        lines.append("cold_ok=10")
        text = "\n".join(lines) + "\n"
        local = COLD / "offgrid-cold-starts.log"
        local.write_text(text, encoding="utf-8")
        adb(["push", str(local), "/data/local/tmp/offgrid-cold-starts.log"], timeout=20)
        results["cold_ok_seeded"] = True
    else:
        results["cold_ok_seeded"] = False
    print(f"cold_ok_seeded={results['cold_ok_seeded']}", flush=True)


def unlock_strip(nodes: list[dict]) -> dict | None:
    """The on-screen Unlock control. The immersive hint window can hide it from a plain dump."""
    node = find(nodes, "Unlock", exact=True)
    if node is not None and node["desc"] != "Unlock the panel" and node["text"] != "Unlock the panel":
        return node
    top = adb(["shell", "dumpsys", "activity", "top"], timeout=25).stdout or ""
    # Visible views lead their flags with V. A gone unlock_button must not be tapped.
    match = re.search(
        r"\{[0-9a-f]+ V[^\n]{0,160}?(\d+),(\d+)-(\d+),(\d+)[^\n]{0,200}unlock_button",
        top,
    )
    if match is None:
        return None
    box = tuple(int(part) for part in match.groups())
    progress(f"unlock_button from dumpsys {box}")
    return {
        "text": "Unlock",
        "desc": "Unlock",
        "bounds": box,
        "clickable": True,
        "checked": False,
        "class": "dumpsys",
    }


def clear_locks(nodes: list[dict]) -> list[dict]:
    """A persisted Broadcast Lock hides the panel. An edit lock covers the folders."""
    strip = unlock_strip(nodes)
    locked_label = find(nodes, "LOCKED", exact=True) is not None or strip is not None and strip["class"] == "dumpsys"
    if strip is not None and locked_label and not covered_by_hint(strip, nodes):
        tap(strip)
        time.sleep(0.8)
        nodes = dump("cleared_broadcast")
    edit = find(nodes, "Unlock the panel", exact=True)
    if edit is not None and not covered_by_hint(edit, nodes):
        tap(edit)
        time.sleep(0.6)
        nodes = dump("cleared_edit")
    return nodes


def ensure_home(tag: str) -> list[dict]:
    nodes = clear_locks(dump(f"{tag}_home_probe"))
    if on_home(nodes) and find(nodes, "Lock the panel", exact=True):
        return nodes
    for index in range(6):
        nodes = dump(f"{tag}_up_{index}")
        if on_home(nodes):
            return nodes
        back = find(nodes, "Go back one folder", exact=True)
        if back and not covered_by_hint(back, nodes):
            tap(back)
        else:
            key_back()
        time.sleep(0.6)
    adb(["shell", "am", "force-stop", PACKAGE], timeout=20)
    time.sleep(0.4)
    adb(["shell", "am", "start", "-W", "-n", ACTIVITY], timeout=45)
    deadline = time.time() + 30
    while time.time() < deadline:
        nodes = dump(f"{tag}_relaunch")
        if on_home(nodes):
            return nodes
        time.sleep(0.4)
    return nodes


def folder_shown(nodes: list[dict], name: str) -> bool:
    """True when this folder is the open page, including after the status line moves on."""
    if on_home(nodes):
        return False
    if f"{name} is open" in blob(nodes):
        return True
    heading = f"📁 {name}"
    has_title = any(node["text"] == heading or node["text"] == name for node in nodes)
    has_crumb = any("›" in node["text"] and node["text"].rstrip().endswith(name) for node in nodes)
    return has_title and has_crumb


def open_folder(name: str) -> tuple[list[dict] | None, str]:
    needle = f"{name} folder"
    for attempt in range(16):
        nodes = dump(f"find_{slug(name)}_{attempt}")
        if folder_shown(nodes, name):
            return nodes, "already open"
        node = find(nodes, needle)
        if node is None:
            swipe_list()
            time.sleep(0.45)
            continue
        if covered_by_hint(node, nodes):
            shot(f"hint_covers_{slug(name)}")
            return nodes, f"fullscreen hint covers {needle}"
        shot(f"before_open_{slug(name)}")
        tap(node)
        time.sleep(0.8)
        opened = dump(f"opened_{slug(name)}_{attempt}")
        shot(f"after_open_{slug(name)}")
        opened_text = blob(opened)
        if folder_shown(opened, name):
            return opened, "opened"
    return None, f"could not open {name}"


def slug(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")


def require_screen(tag: str, nodes: list[dict] | None, must: list[str], forbid_home: bool) -> str | None:
    shot(tag)
    if not nodes:
        return "uiautomator dump missing"
    text = blob(nodes)
    if forbid_home and on_home(nodes):
        return "still on the home grid"
    missing = [item for item in must if item.lower() not in text.lower()]
    if missing:
        return "missing " + ", ".join(missing)
    return None


def mark(item: str, status: str, detail: str) -> None:
    results["items"][item] = {"result": status, "detail": detail}
    print(f"ITEM {item} {status}: {detail}", flush=True)
    shot(f"{item}_{status.lower()}")


def status_text(nodes: list[dict]) -> str:
    for node in nodes:
        if node["desc"].startswith("Locked:") or "is open" in node["desc"] or "is now" in node["desc"] or "copied" in node["desc"].lower():
            return node["desc"]
    for node in nodes:
        if node["desc"].startswith("Terminal is on") or node["desc"].startswith("Choose how folders look"):
            return node["desc"]
    return ""


def check_title(nodes: list[dict]) -> None:
    deadline = time.time() + 8
    while time.time() < deadline and "OFFGRID TERMINAL" not in blob(nodes):
        time.sleep(0.6)
        nodes = dump("title_wait")
    if "OFFGRID TERMINAL" not in blob(nodes):
        # A cold home grid draws the folders and omits the header until a folder is open.
        opened, detail = open_folder("Panel settings")
        if opened is None:
            mark("title_fit", "BLOCKED", detail)
            return
        nodes = opened
    problem = require_screen("title_before", nodes, ["OFFGRID TERMINAL"], False)
    if problem:
        mark("title_fit", "BLOCKED", problem)
        return
    node = find(nodes, "OFFGRID TERMINAL", exact=True)
    if node is None:
        mark("title_fit", "FAIL", "title text was in the dump but no on-screen node matched")
        return
    x1, y1, x2, y2 = node["bounds"]
    if x1 < 0 or y1 < 0 or x2 > WIDTH or y2 > HEIGHT or x2 <= x1:
        mark("title_fit", "FAIL", f"title bounds {(x1, y1, x2, y2)} outside {WIDTH}x{HEIGHT}")
        return
    mark("title_fit", "PASS", f"title bounds {(x1, y1, x2, y2)} on {WIDTH}x{HEIGHT}")


def check_white() -> None:
    home = ensure_home("white")
    if not on_home(home):
        mark("white_skin", "BLOCKED", "home grid not shown before white skin")
        return
    opened, detail = open_folder("Panel settings")
    if opened is None:
        mark("white_skin", "BLOCKED", detail)
        return
    colors, detail = open_folder("Colors")
    if colors is None:
        mark("white_skin", "BLOCKED", detail)
        return
    skin, detail = open_folder("Skin color")
    if skin is None:
        mark("white_skin", "BLOCKED", detail)
        return
    node = None
    nodes = skin
    for attempt in range(8):
        problem = require_screen(f"white_before_{attempt}", nodes, ["Locked White"], True)
        node = find(nodes, "Locked White")
        if problem is None and node is not None:
            break
        swipe_list()
        time.sleep(0.4)
        nodes = dump(f"white_scroll_{attempt}")
    if node is None:
        mark("white_skin", "FAIL" if nodes else "BLOCKED", "Locked White not on the skin color screen")
        return
    if covered_by_hint(node, nodes):
        mark("white_skin", "FAIL", "fullscreen hint covers Locked White")
        return
    tap(node)
    time.sleep(1.0)
    after = dump("white_after")
    shot("white_after_tap")
    reason = status_text(after)
    if "Locked:" in reason and "white" in reason.lower():
        mark("white_skin", "PASS", reason)
    else:
        mark("white_skin", "FAIL", f"status after tap: {reason or blob(after)[:500]}")


def check_night_black() -> None:
    home = ensure_home("night")
    if not on_home(home):
        mark("night_black", "BLOCKED", "home grid not shown before Night")
        return
    settings, detail = open_folder("Panel settings")
    if settings is None:
        mark("night_black", "BLOCKED", detail)
        return
    theme, detail = open_folder("Theme mode")
    if theme is None:
        mark("night_black", "BLOCKED", detail)
        return
    problem = require_screen("night_before", theme, ["Night. Use a black panel"], True)
    night = find(theme, "Night. Use a black panel")
    if problem or night is None:
        mark("night_black", "FAIL", problem or "Night row missing")
        return
    if covered_by_hint(night, theme):
        mark("night_black", "FAIL", "fullscreen hint covers Night")
        return
    tap(night)
    time.sleep(0.8)
    chosen = dump("night_chosen")
    shot("night_chosen")
    chosen_text = blob(chosen)
    night_node = find(chosen, "Night. Use a black panel")
    if "Theme mode is now Night" not in chosen_text and not (night_node and night_node["checked"]):
        mark("night_black", "FAIL", "Night did not become the chosen theme")
        return
    back = find(chosen, "Go back one folder", exact=True)
    if back and not covered_by_hint(back, chosen):
        tap(back)
    else:
        key_back()
    time.sleep(0.6)
    colors, detail = open_folder("Colors")
    if colors is None:
        mark("night_black", "BLOCKED", detail)
        return
    text_folder, detail = open_folder("Typewriter text color")
    if text_folder is None:
        mark("night_black", "BLOCKED", detail)
        return
    node = None
    nodes = text_folder
    for attempt in range(8):
        node = find(nodes, "Locked Black")
        if node is not None and not on_home(nodes):
            shot(f"black_before_{attempt}")
            break
        swipe_list()
        time.sleep(0.4)
        nodes = dump(f"black_scroll_{attempt}")
    if node is None:
        mark("night_black", "FAIL", "Locked Black not on the typewriter color screen")
        return
    if covered_by_hint(node, nodes):
        mark("night_black", "FAIL", "fullscreen hint covers Locked Black")
        return
    tap(node)
    time.sleep(1.0)
    first = dump("black_after_1")
    shot("black_after_1")
    time.sleep(2.5)
    second = dump("black_after_2")
    shot("black_after_2")
    first_reason = status_text(first)
    second_reason = status_text(second)
    if "Locked:" in first_reason and "black" in first_reason.lower() and "Locked:" in second_reason and "black" in second_reason.lower():
        mark("night_black", "PASS", f"{first_reason} | stayed: {second_reason}")
    else:
        mark("night_black", "FAIL", f"first={first_reason} second={second_reason}")


def find_width_reel(nodes: list[dict]) -> dict | None:
    hits = [node for node in nodes if "Width," in node["desc"] and on_screen(node)]
    if not hits:
        return None
    hits.sort(key=lambda node: node["bounds"][3] - node["bounds"][1], reverse=True)
    return hits[0]


def reel_value(node: dict | None) -> int | None:
    if node is None:
        return None
    match = re.search(r"Width,\s*(\d+)", node["desc"])
    return int(match.group(1)) if match else None


def check_reel() -> None:
    home = ensure_home("reel")
    node = find_width_reel(home)
    if node is None:
        size, detail = open_folder("Panel settings")
        if size is None:
            mark("reel_click", "BLOCKED", detail)
            return
        size, detail = open_folder("Size")
        if size is None:
            mark("reel_click", "BLOCKED", detail)
            return
        node = find_width_reel(size)
        home = size
    if node is None:
        mark("reel_click", "BLOCKED", "Width reel not in the dump")
        return
    if on_home(home) is False and "Width," not in node["desc"]:
        mark("reel_click", "FAIL", "reel screen was not the expected Width reel")
        return
    before = reel_value(node)
    if covered_by_hint(node, home):
        mark("reel_click", "FAIL", "fullscreen hint covers the Width reel")
        return
    shot("reel_before")
    adb(["logcat", "-c"], timeout=20)
    x1, y1, x2, y2 = node["bounds"]
    cx = (x1 + x2) // 2
    # Travel most of the drum. A short swipe on a wheel already at 100 snaps back to 100.
    y_from = y2 - 20
    y_to = y1 + 20
    adb(["shell", "input", "swipe", str(cx), str(y_from), str(cx), str(y_to), "350"], timeout=20)
    time.sleep(1.2)
    after_nodes = dump("reel_after")
    shot("reel_after")
    after_node = find_width_reel(after_nodes)
    after = reel_value(after_node)
    if after == before:
        # The wheel was already at the top. Drag downward so a lower number snaps in.
        adb(
            ["shell", "input", "swipe", str(cx), str(y1 + 30), str(cx), str(y2 - 30), "350"],
            timeout=20,
        )
        time.sleep(1.2)
        after_nodes = dump("reel_after_2")
        shot("reel_after_2")
        after_node = find_width_reel(after_nodes)
        after = reel_value(after_node)
    click = logcat("OffgridClicker:I")
    (EVID / "reel_clicker.log").write_text(click, encoding="utf-8", errors="replace")
    play = [line.strip() for line in click.splitlines() if "SoundPool play" in line]
    stream = next((line for line in play if re.search(r"streamId=([1-9]\d*)", line)), "")
    if before is not None and after is not None and before != after and stream:
        mark("reel_click", "PASS", f"Width {before} -> {after}; {stream}")
    else:
        mark("reel_click", "FAIL", f"Width {before} -> {after}; log={play[:6]}")


def check_pairing() -> None:
    home = ensure_home("pair")
    if not on_home(home):
        mark("pc_or_android", "BLOCKED", "home grid missing before Live and Remote")
        mark("copy_pairing", "BLOCKED", "home grid missing before Live and Remote")
        return
    opened, detail = open_folder("Live and Remote")
    if opened is None:
        mark("pc_or_android", "BLOCKED", detail)
        mark("copy_pairing", "BLOCKED", detail)
        return
    problem = require_screen("pc_before", opened, ["Copy pairing key", "PC or an Android phone"], True)
    if problem:
        mark("pc_or_android", "FAIL", problem)
    else:
        mark("pc_or_android", "PASS", "Live and Remote shows Copy pairing key and PC or an Android phone")
    fresh = dump("copy_before")
    problem = require_screen("copy_before", fresh, ["Copy pairing key"], True)
    button = find(fresh, "Copy pairing key", exact=True)
    if problem or button is None:
        mark("copy_pairing", "FAIL", problem or "Copy pairing key missing before tap")
        return
    if covered_by_hint(button, fresh):
        mark("copy_pairing", "FAIL", "fullscreen hint covers Copy pairing key")
        return
    tap(button)
    copied = ""
    after: list[dict] = []
    deadline = time.time() + 12
    while time.time() < deadline:
        after = dump("copy_poll")
        copied = status_text(after)
        if "copied" in copied.lower():
            break
        time.sleep(0.6)
    shot("copy_after")
    board = adb(["shell", "dumpsys", "clipboard"], timeout=20).stdout or ""
    (EVID / "clipboard.txt").write_text(board[:8000], encoding="utf-8", errors="replace")
    still = "Copy pairing key" in blob(after) and not on_home(after)
    primary = board.split("primary clip", 1)[-1][:500].lower() if "primary clip" in board.lower() else board[:500].lower()
    clip_ok = "clipdata" in primary and "null" not in primary[:80]
    if "copied" in copied.lower() and still:
        mark("copy_pairing", "PASS", f"{copied} clipboard_dump={clip_ok}")
    else:
        mark("copy_pairing", "FAIL", f"status={copied} still_on_folder={still} clipboard_ok={clip_ok}")


def check_broadcast() -> None:
    home = ensure_home("broadcast")
    problem = require_screen("broadcast_before", home, ["Lock the panel"], False)
    lock = find(home, "Lock the panel", exact=True)
    if problem or lock is None or not on_home(home):
        mark("broadcast_lock", "BLOCKED" if not on_home(home) else "FAIL", problem or "Lock the panel missing on home")
        return
    if covered_by_hint(lock, home):
        mark("broadcast_lock", "FAIL", "fullscreen hint covers Lock before the hold")
        return
    long_press(lock)
    time.sleep(1.2)
    armed = dump("broadcast_armed")
    shot("broadcast_armed")
    unlock = unlock_strip(armed)
    if unlock is None:
        mark("broadcast_lock", "FAIL", "hold did not show the Unlock strip")
        return
    x1, y1, x2, y2 = unlock["bounds"]
    if y1 < 80 or (y2 - y1) < 40 or (x2 - x1) < 40:
        mark("broadcast_lock", "FAIL", f"Unlock strip bounds {(x1, y1, x2, y2)} are not an on-screen control")
        return
    if covered_by_hint(unlock, armed):
        mark("broadcast_lock", "FAIL", f"fullscreen hint covers Unlock at {(x1, y1, x2, y2)}")
        return
    tap(unlock)
    time.sleep(1.0)
    back = dump("broadcast_unlocked")
    shot("broadcast_unlocked")
    strip_gone = find(back, "LOCKED", exact=True) is None
    if find(back, "Lock the panel", exact=True) and strip_gone:
        mark("broadcast_lock", "PASS", f"Unlock at {(x1, y1, x2, y2)} restored the panel")
    elif find(back, "Unlock the panel", exact=True) and strip_gone:
        mark("broadcast_lock", "PASS", f"Unlock at {(x1, y1, x2, y2)} restored the panel with the edit lock still on")
    else:
        mark("broadcast_lock", "FAIL", "Unlock tap did not bring the panel back")


def check_minus() -> None:
    home = ensure_home("minus")
    shrink = find(home, "Shrink the panel", exact=True)
    problem = require_screen("minus_before", home, ["Shrink the panel"], False)
    if problem or shrink is None:
        mark("minus", "BLOCKED" if not on_home(home) else "FAIL", problem or "Shrink the panel missing")
        return
    if covered_by_hint(shrink, home):
        mark("minus", "FAIL", "fullscreen hint covers Shrink the panel")
        return
    tap(shrink)
    time.sleep(0.9)
    hidden = dump("minus_hidden")
    shot("minus_hidden")
    restore = find(hidden, "Open the panel", exact=True)
    if restore is None:
        mark("minus", "FAIL", "minus did not leave Open the panel")
        return
    x1, y1, x2, y2 = restore["bounds"]
    wide = (x2 - x1) >= 120 and (y2 - y1) >= 48 and y1 >= 80
    if not wide:
        mark("minus", "FAIL", f"restore tab bounds {(x1, y1, x2, y2)} are not a large control below the status bar")
        return
    if covered_by_hint(restore, hidden):
        mark("minus", "FAIL", f"fullscreen hint covers the restore tab {(x1, y1, x2, y2)}")
        return
    tap(restore)
    time.sleep(0.9)
    back = dump("minus_restored")
    shot("minus_restored")
    if find(back, "Shrink the panel", exact=True) or find(back, "Lock the panel", exact=True):
        mark("minus", "PASS", f"restore tab {(x1, y1, x2, y2)} brought the panel back")
    else:
        mark("minus", "FAIL", "restore tap did not bring the panel back")


def check_lock() -> None:
    home = ensure_home("lock")
    lock = find(home, "Lock the panel", exact=True)
    problem = require_screen("lock_before", home, ["Lock the panel"], False)
    if problem or lock is None:
        mark("edit_lock", "BLOCKED" if not on_home(home) else "FAIL", problem or "Lock the panel missing")
        return
    if covered_by_hint(lock, home):
        mark("edit_lock", "FAIL", "fullscreen hint covers Lock the panel")
        return
    tap(lock)
    time.sleep(0.7)
    locked = dump("lock_locked")
    shot("lock_locked")
    unlock = find(locked, "Unlock the panel", exact=True)
    if unlock is None:
        mark("edit_lock", "FAIL", "tap did not show Unlock the panel")
        return
    if covered_by_hint(unlock, locked):
        mark("edit_lock", "FAIL", "fullscreen hint covers Unlock the panel")
        return
    tap(unlock)
    time.sleep(0.7)
    opened = dump("lock_unlocked")
    shot("lock_unlocked")
    if find(opened, "Lock the panel", exact=True):
        mark("edit_lock", "PASS", "tap locked, second tap unlocked")
    else:
        mark("edit_lock", "FAIL", "second tap did not return Lock the panel")


def rect_overlap(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> int:
    height = min(a[3], b[3]) - max(a[1], b[1])
    width = min(a[2], b[2]) - max(a[0], b[0])
    if height > 8 and width > 8:
        return height
    return 0


def check_menu() -> None:
    home = ensure_home("menu")
    button = find(home, "More choices: view, sort, and folder size", exact=True)
    problem = require_screen("menu_before", home, ["More choices: view, sort, and folder size"], False)
    if problem or button is None:
        mark("menu", "BLOCKED" if not on_home(home) else "FAIL", problem or "menu button missing")
        return
    if covered_by_hint(button, home):
        mark("menu", "FAIL", "fullscreen hint covers the menu button")
        return
    tap(button)
    time.sleep(0.8)
    menu = dump("menu_open")
    shot("menu_open")
    text = blob(menu)
    if "Big tiles" not in text and "List" not in text:
        mark("menu", "FAIL", "menu did not show Big tiles or List")
        return
    status = None
    for node in menu:
        if node["desc"] in {"Choose how folders look.", "Terminal is on. Tap a folder to open it. Drag the top bar to move. Pinch, or type Width and Height, to resize."} or node["desc"].startswith("Choose how folders look"):
            if status is None or (node["bounds"][2] - node["bounds"][0]) * (node["bounds"][3] - node["bounds"][1]) < (status["bounds"][2] - status["bounds"][0]) * (status["bounds"][3] - status["bounds"][1]):
                status = node
    menu_nodes = [
        node
        for node in menu
        if node["text"] in {"VIEW", "SORT", "FOLDER SIZE", "Done", "Big tiles", "List", "Grid 2", "Grid 3"}
        or node["desc"].startswith("Big tiles")
        or node["desc"].startswith("List.")
        or node["desc"].startswith("Grid ")
    ]
    if status is None or not menu_nodes:
        mark("menu", "FAIL", f"menu items={len(menu_nodes)} status={'yes' if status else 'no'}")
        key_back()
        return
    card = (
        min(node["bounds"][0] for node in menu_nodes),
        min(node["bounds"][1] for node in menu_nodes),
        max(node["bounds"][2] for node in menu_nodes),
        max(node["bounds"][3] for node in menu_nodes),
    )
    overlap = rect_overlap(status["bounds"], card)
    below = card[1] + 4 >= status["bounds"][3]
    detail = f"status={status['bounds']} card={card} overlap_h={overlap} menu_below_status={below}"
    key_back()
    if overlap or not below:
        mark("menu", "FAIL", detail)
    else:
        mark("menu", "PASS", detail)


def reset_theme() -> None:
    """Night is persisted. Put Auto back so the instrument suite starts from the default theme."""
    try:
        home = ensure_home("reset")
        if not on_home(home):
            return
        if open_folder("Panel settings")[0] is None:
            return
        theme, _detail = open_folder("Theme mode")
        if theme is None:
            return
        auto = find(theme, "Auto. Follow the phone")
        if auto is not None and not covered_by_hint(auto, theme):
            tap(auto)
            time.sleep(0.6)
    except Exception as error:  # noqa: BLE001 - reset must not hide the walk result
        print(f"theme reset skipped: {error}", flush=True)


def splash_note() -> None:
    rows = results.get("cold", {}).get("offgrid", [])
    if not rows:
        mark("splash", "BLOCKED", "no offgrid cold start")
        return
    yellow = rows[0].get("yellow_sample")
    early = COLD / "offgrid_01_early.png"
    if yellow is None:
        mark("splash", "BLOCKED", f"no early frame sample; png_exists={early.exists()}")
        return
    # A dense yellow diamond samples well above a handful of status-bar pixels.
    if yellow > 40:
        mark("splash", "FAIL", f"early-frame yellow samples={yellow} (possible diamond before folders)")
    else:
        mark("splash", "PASS", f"early-frame yellow samples={yellow}; folders judged from offgrid_01.png")


def main() -> int:
    EVID.mkdir(parents=True, exist_ok=True)
    read_screen()
    results["screen"] = {"width": WIDTH, "height": HEIGHT}
    try:
        cold_series()
        nodes = ensure_home("walk")

        def guard(names: list[str], fn) -> None:
            try:
                fn()
            except Exception as error:  # noqa: BLE001 - one check must not skip the rest
                print(f"check {names} raised {error}", flush=True)
                for name in names:
                    if name not in results["items"]:
                        mark(name, "BLOCKED", f"script error: {error}")

        guard(["title_fit"], lambda: check_title(nodes))
        guard(["white_skin"], check_white)
        guard(["night_black"], check_night_black)
        guard(["reel_click"], check_reel)
        guard(["pc_or_android", "copy_pairing"], check_pairing)
        guard(["broadcast_lock"], check_broadcast)
        guard(["minus"], check_minus)
        guard(["edit_lock"], check_lock)
        guard(["menu"], check_menu)
        reset_theme()
        guard(["splash"], splash_note)
    finally:
        path = EVID / "verdict.json"
        path.write_text(json.dumps(results, indent=2), encoding="utf-8")
        print(f"wrote {path}", flush=True)
    failed = [name for name, row in results["items"].items() if row["result"] == "FAIL"]
    blocked = [name for name, row in results["items"].items() if row["result"] == "BLOCKED"]
    print(f"FAIL={failed} BLOCKED={blocked}", flush=True)
    return 1 if failed or blocked else 0


if __name__ == "__main__":
    raise SystemExit(main())
