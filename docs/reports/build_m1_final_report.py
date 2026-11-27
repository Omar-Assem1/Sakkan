#!/usr/bin/env python3
"""Build the revised Milestone 1 report from the supplied PDF and repo diagrams."""
from __future__ import annotations

import subprocess
import tempfile
from pathlib import Path

from PIL import Image
from pypdf import PdfReader, PdfWriter
from pypdf.generic import ArrayObject, ContentStream, FloatObject, NameObject, TextStringObject

ROOT = Path(__file__).resolve().parents[2]
REPORTS = ROOT / "docs" / "reports"
BASE = ROOT / "docs" / "milestone-1" / "Sakkan_M1_Report_Base.pdf"
OUTPUT = REPORTS / "Sakkan_Milestone_1_Report_Final.pdf"


def compile_tex(tex_path: Path, output_dir: Path) -> Path:
    subprocess.run(
        ["pdflatex", "-interaction=nonstopmode", "-halt-on-error", f"-output-directory={output_dir}", str(tex_path)],
        cwd=REPORTS, check=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
    )
    return output_dir / f"{tex_path.stem}.pdf"


def patch_image(page, old_size: tuple[int, int], image_path: Path, box: tuple[float, float, float, float]) -> None:
    """Replace an embedded image and update its three cm matrices to fit a PDF box."""
    image = next((img for img in page.images if img.image.size == old_size), None)
    if image is None:
        raise ValueError(f"Could not find embedded image of size {old_size}")
    resource_name = "/" + image.name.rsplit(".", 1)[0]
    replacement = Image.open(image_path).convert("RGBA")
    image.replace(replacement)

    contents = ContentStream(page.get_contents(), page.pdf)
    do_index = next((i for i, (operands, op) in enumerate(contents.operations)
                     if op == b"Do" and str(operands[0]) == resource_name), None)
    if do_index is None:
        raise ValueError(f"Could not locate image draw operation for {resource_name}")
    cms = [i for i in range(do_index - 1, -1, -1) if contents.operations[i][1] == b"cm"][:3]
    if len(cms) != 3:
        raise ValueError(f"Expected three transformation matrices before {resource_name}")
    x, y, width, height = box
    nw, nh = replacement.size
    matrices = [
        [1, 0, 0, 1, x, y],
        [width / nw, 0, 0, height / nh, 0, 0],
        [nw, 0, 0, nh, 0, 0],
    ]
    # The nearest preceding cm is the intrinsic image size, then scale, then translation.
    for idx, values in zip(cms, reversed(matrices)):
        contents.operations[idx] = (ArrayObject([FloatObject(v) for v in values]), b"cm")
    page[NameObject("/Contents")] = page.pdf._add_object(contents)


def add_caption(page, text: str, x: float, y: float) -> None:
    fonts = page["/Resources"].get("/Font")
    if fonts is None:
        fonts = page["/Resources"][NameObject("/Font")] = page.pdf._add_object(__import__("pypdf").generic.DictionaryObject())
    font_ref = page.pdf._add_object(__import__("pypdf").generic.DictionaryObject({
        NameObject("/Type"): NameObject("/Font"),
        NameObject("/Subtype"): NameObject("/Type1"),
        NameObject("/BaseFont"): NameObject("/Helvetica"),
    }))
    fonts[NameObject("/FRev")] = font_ref
    contents = ContentStream(page.get_contents(), page.pdf)
    contents.operations.extend([
        ([], b"q"),
        ([FloatObject(0.18), FloatObject(0.18), FloatObject(0.18)], b"rg"),
        ([], b"BT"),
        ([NameObject("/FRev"), FloatObject(9.5)], b"Tf"),
        ([FloatObject(1), FloatObject(0), FloatObject(0), FloatObject(1), FloatObject(x), FloatObject(y)], b"Tm"),
        ([TextStringObject(text)], b"Tj"),
        ([], b"ET"),
        ([], b"Q"),
    ])
    page[NameObject("/Contents")] = page.pdf._add_object(contents)


def remove_caption(page, operation_index: int) -> None:
    contents = ContentStream(page.get_contents(), page.pdf)
    if contents.operations[operation_index][1] != b"TJ":
        raise ValueError(f"Expected caption at content operation {operation_index}")
    del contents.operations[operation_index]
    page[NameObject("/Contents")] = page.pdf._add_object(contents)


def main() -> None:
    required = [
        BASE,
        ROOT / "docs/diagrams/SCRUM-56-sequence.png",
        ROOT / "docs/diagrams/seqdiagramscrum-65.png",
        ROOT / "docs/usecases/SCRUM-73-77-manage-listings.png",
        ROOT / "docs/usecases/SCRUM-81-browse-properties.png",
        REPORTS / "M1_Final_Appendix.tex",
    ]
    for path in required:
        if not path.is_file():
            raise FileNotFoundError(f"Required report input is missing: {path}")

    with tempfile.TemporaryDirectory(prefix="sakkan-m1-final-") as tmp_name:
        tmp = Path(tmp_name)
        appendix_pdf = compile_tex(REPORTS / "M1_Final_Appendix.tex", tmp)
        writer = PdfWriter(clone_from=str(BASE))
        if len(writer.pages) != 38:
            raise ValueError(f"Expected 38 base pages, found {len(writer.pages)}")

        patch_image(writer.pages[15], (1152, 1620), ROOT / "docs/diagrams/SCRUM-56-sequence.png", (27, 72, 540, 325))
        remove_caption(writer.pages[16], 7)
        add_caption(writer.pages[15], "SCRUM-56 sequence diagram: Google sign-up and sign-in.", 165, 57)

        patch_image(writer.pages[19], (1107, 1415), ROOT / "docs/diagrams/seqdiagramscrum-65.png", (112, 435, 371, 351))
        remove_caption(writer.pages[19], 20)
        add_caption(writer.pages[19], "SCRUM-65 sequence diagram: RBAC request flow and failure paths.", 118, 414)

        appendix = PdfReader(str(appendix_pdf))
        if len(appendix.pages) != 3:
            raise ValueError(f"Expected 3 appendix pages, found {len(appendix.pages)}")
        appendix_start = len(writer.pages)
        for page in appendix.pages:
            writer.add_page(page)
        writer.add_outline_item("Appendices: diagrams and identifier crosswalk", appendix_start)
        writer.add_metadata({
            "/Title": "Sakkan Milestone 1 Report - Final Revised Edition",
            "/Author": "Sakan Project Team",
            "/Subject": "Milestone 1 stories, use cases, sequence diagrams, and traceability",
        })
        with OUTPUT.open("wb") as stream:
            writer.write(stream)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
