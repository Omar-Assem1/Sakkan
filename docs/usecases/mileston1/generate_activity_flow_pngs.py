"""Render the Markdown use case flows as individual Graphviz PNGs."""

from __future__ import annotations

import re
import subprocess
import textwrap
from pathlib import Path


HERE = Path(__file__).resolve().parent
SOURCE = HERE / "M1-technical-and-listing-use-cases.md"


def clean(text: str) -> str:
    text = re.sub(r"`([^`]*)`", r"\1", text)
    return re.sub(r"\*\*([^*]+)\*\*", r"\1", text).strip()


def wrap_label(text: str, width: int = 42) -> str:
    lines = textwrap.wrap(clean(text), width=width, break_long_words=False)
    escaped = [line.replace("\\", "\\\\").replace('"', '\\"') for line in lines]
    return "\\n".join(escaped) or " "


def parse_cases(markdown: str) -> list[dict[str, object]]:
    sections = re.split(r"(?m)(?=^## UC-\d+ )", markdown)
    cases = []
    for section in sections:
        heading = re.match(r"(?m)^## (UC-(\d+)) (.+)$", section)
        if not heading:
            continue
        actor_line = re.search(r"(?m)^\*\*Actor[s]?:\*\* (.+)$", section)
        # The flows with multiple actors use the plural label.
        if actor_line is None:
            actor_line = re.search(r"(?m)^\*\*Actors:\*\* (.+)$", section)
        main = re.search(r"(?ms)^\*\*Main flow:\*\*\s*\n(.*?)(?=^\*\*Alternate flows:\*\*)", section)
        alternate = re.search(r"(?m)^\*\*Alternate flows:\*\* (.+)$", section)
        post = re.search(r"(?m)^\*\*Postcondition:\*\* (.+)$", section)
        if not (actor_line and main and alternate and post):
            raise ValueError(f"Could not parse all flow fields for {heading.group(1)}")
        steps = []
        for line in main.group(1).splitlines():
            match = re.match(r"\s*\d+\.\s+(.+)", line)
            if match:
                steps.append(clean(match.group(1)))
        if not steps:
            raise ValueError(f"No main flow steps found for {heading.group(1)}")
        story = re.search(r"\((SKN-\d+), (SCRUM-\d+)\)$", heading.group(3))
        if not story:
            raise ValueError(f"Story identifiers missing from {heading.group(1)}")
        title = re.sub(r" \(SKN-\d+, SCRUM-\d+\)$", "", heading.group(3))
        precondition = re.search(r"\*\*Precondition:\*\* (.+)$", actor_line.group(1))
        actor = actor_line.group(1)
        if precondition:
            actor = actor[: precondition.start()].rstrip(" .")
            precondition_text = clean(precondition.group(1))
        else:
            raise ValueError(f"Precondition missing for {heading.group(1)}")
        cases.append(
            {
                "uc": heading.group(1),
                "uc_number": heading.group(2),
                "heading": heading.group(3),
                "title": clean(title),
                "skn": story.group(1),
                "scrum": story.group(2),
                "actor": clean(actor),
                "precondition": precondition_text,
                "steps": steps,
                "alternate": clean(alternate.group(1)),
                "postcondition": clean(post.group(1)),
            }
        )
    if len(cases) != 28:
        raise ValueError(f"Expected 28 use cases, found {len(cases)}")
    return cases


def render(case: dict[str, object]) -> tuple[Path, str]:
    uc = str(case["uc"])
    outname = f"{case['scrum']}_{uc}_Flow.png"
    output = HERE / outname
    lines = [
        "digraph usecase_flow {",
        '  graph [rankdir=TB, bgcolor="#ffffff", pad="0.28", nodesep="0.34", ranksep="0.48", splines=polyline, fontname="Arial", labelloc=t, labeljust=l,',
        f'         label="{uc}  |  {case["skn"]}  |  {case["scrum"]}\\n{wrap_label(str(case["title"]), 48)}", fontsize=21, fontcolor="#16324f"];',
        '  node [fontname="Arial", fontsize=12, color="#244b70", fontcolor="#19334d", penwidth=1.5, margin="0.18,0.12"];',
        '  edge [fontname="Arial", fontsize=10, color="#486987", fontcolor="#486987", penwidth=1.35, arrowsize=0.75];',
        '  start [shape=oval, style="filled", fillcolor="#dceeff", label="Start"];',
        f'  context [shape=box, style="rounded,filled", fillcolor="#edf5fc", label="Actor: {wrap_label(str(case["actor"]), 46)}\\nPrecondition: {wrap_label(str(case["precondition"]), 46)}"];',
    ]
    for i, step in enumerate(case["steps"], 1):
        lines.append(
            f'  step{i} [shape=box, style="rounded,filled", fillcolor="#f7fafc", label="{i}. {wrap_label(str(step), 44)}"];'
        )
    lines.extend(
        [
            f'  alternate [shape=note, style="filled", fillcolor="#fff1ee", color="#b64c42", fontcolor="#702a24", label="Alternate flows\\n{wrap_label(str(case["alternate"]), 43)}"];',
            f'  post [shape=box, style="rounded,filled", fillcolor="#e8f5e9", color="#47835a", label="Postcondition\\n{wrap_label(str(case["postcondition"]), 44)}"];',
            '  finish [shape=doublecircle, width=0.34, height=0.34, fixedsize=true, label="", color="#47835a"];',
            '  start -> context;',
        ]
    )
    for i in range(1, len(case["steps"]) + 1):
        lines.append(f"  {'context' if i == 1 else f'step{i-1}'} -> step{i};")
    mid = max(1, (len(case["steps"]) + 1) // 2)
    lines.extend(
        [
            f"  step{len(case['steps'])} -> post;",
            "  post -> finish;",
            f'  step{mid} -> alternate [style=dashed, color="#b64c42", arrowhead=none, constraint=false];',
            f"  {{ rank=same; step{mid}; alternate; }}",
            "}",
        ]
    )
    dot = "\n".join(lines)
    if not output.exists():
        subprocess.run(["dot", "-Tpng", "-Gdpi=135", "-o", str(output)], input=dot, text=True, check=True)
    md_link = f"[PNG activity flow]({outname})"
    return output, md_link


def main() -> None:
    markdown = SOURCE.read_text(encoding="utf-8")
    cases = parse_cases(markdown)
    for case in cases:
        output, link = render(case)
        heading = f"## {case['uc']} {case['heading']}"
        markdown = markdown.replace(heading, f"{heading}\n\n{link}", 1)
        print(f"Created {output.name}")
    SOURCE.write_text(markdown, encoding="utf-8", newline="\n")
    print(f"Rendered {len(cases)} use-case flow PNGs and linked them from {SOURCE.name}")


if __name__ == "__main__":
    main()
