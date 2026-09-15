from __future__ import annotations

from pathlib import Path
from datetime import date
from docx import Document
from docx.enum.section import WD_ORIENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.shared import Inches, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "artifacts" / "KIDS-admin-panel-complete-architecture-and-implementation-specification.docx"
ASSETS = ROOT / "artifacts" / ".kids-panel-blueprint-assets"
OUT.parent.mkdir(parents=True, exist_ok=True)
ASSETS.mkdir(parents=True, exist_ok=True)

NAVY = "17324D"
BLUE = "246B9E"
TEAL = "0B7A75"
PALE = "EAF2F7"
PALE2 = "F6F8FA"
GRAY = "D5DCE2"
INK = "1B2631"
WHITE = "FFFFFF"
AMBER = "B86B00"
RED = "A62828"
GREEN = "26734D"


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_border(cell, color=GRAY, size="6"):
    tc_pr = cell._tc.get_or_add_tcPr()
    borders = tc_pr.first_child_found_in("w:tcBorders")
    if borders is None:
        borders = OxmlElement("w:tcBorders")
        tc_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        tag = "w:" + edge
        node = borders.find(qn(tag))
        if node is None:
            node = OxmlElement(tag)
            borders.append(node)
        node.set(qn("w:val"), "single")
        node.set(qn("w:sz"), size)
        node.set(qn("w:color"), color)


def set_repeat_table_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def keep_with_next(paragraph):
    paragraph.paragraph_format.keep_with_next = True


def add_field(paragraph, field_code):
    run = paragraph.add_run()
    fld_char = OxmlElement("w:fldChar")
    fld_char.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = field_code
    sep = OxmlElement("w:fldChar")
    sep.set(qn("w:fldCharType"), "separate")
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.extend([fld_char, instr, sep, end])


def add_table(doc, headers, rows, widths=None, font_size=8.2, repeat=True):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    hdr = table.rows[0]
    if repeat:
        set_repeat_table_header(hdr)
    for i, header in enumerate(headers):
        cell = hdr.cells[i]
        set_cell_shading(cell, NAVY)
        set_cell_border(cell)
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        p = cell.paragraphs[0]
        p.paragraph_format.space_after = Pt(0)
        run = p.add_run(str(header))
        run.bold = True
        run.font.color.rgb = RGBColor(255, 255, 255)
        run.font.size = Pt(font_size)
        if widths:
            cell.width = Inches(widths[i])
    for ridx, row in enumerate(rows):
        cells = table.add_row().cells
        fill = WHITE if ridx % 2 == 0 else PALE2
        for i, value in enumerate(row):
            cell = cells[i]
            set_cell_shading(cell, fill)
            set_cell_border(cell)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.TOP
            if widths:
                cell.width = Inches(widths[i])
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.0
            text = "" if value is None else str(value)
            # Word may enlarge a fixed-width table for a long identifier. Add one
            # visible line break in the first column only, preserving the identifier
            # characters while keeping the table inside the page margins.
            if i == 0 and " " not in text and "\n" not in text and len(text) > 27:
                candidates = [pos for pos, ch in enumerate(text) if ch in "._"]
                if candidates:
                    target = min(candidates, key=lambda pos: abs(pos - len(text) * 0.52))
                    text = text[: target + 1] + "\n" + text[target + 1 :]
            pieces = text.split("\n")
            for n, piece in enumerate(pieces):
                if n:
                    p.add_run().add_break()
                r = p.add_run(piece)
                r.font.size = Pt(font_size)
                if piece.startswith("MUST ") or piece.startswith("MUST NOT "):
                    r.bold = True
        tr_pr = table.rows[-1]._tr.get_or_add_trPr()
        cant_split = OxmlElement("w:cantSplit")
        tr_pr.append(cant_split)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)
    return table


def add_heading(doc, text, level=1):
    p = doc.add_heading(text, level=level)
    keep_with_next(p)
    return p


def add_para(doc, text="", style=None, bold_prefix=None):
    p = doc.add_paragraph(style=style)
    p.paragraph_format.space_after = Pt(5)
    if bold_prefix and text.startswith(bold_prefix):
        r = p.add_run(bold_prefix)
        r.bold = True
        p.add_run(text[len(bold_prefix):])
    else:
        p.add_run(text)
    return p


def bullets(doc, items, level=0):
    for item in items:
        p = doc.add_paragraph(style="List Bullet" if level == 0 else "List Bullet 2")
        p.paragraph_format.space_after = Pt(2)
        p.add_run(item)


def numbered(doc, items):
    for index, item in enumerate(items, 1):
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.left_indent = Inches(0.22)
        p.paragraph_format.first_line_indent = Inches(-0.22)
        p.add_run(f"{index}.  ").bold = True
        p.add_run(item)


def code_block(doc, text):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = table.cell(0, 0)
    set_cell_shading(cell, "F1F4F6")
    set_cell_border(cell, "B7C4CE", "8")
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    for i, line in enumerate(text.splitlines()):
        if i:
            p.add_run().add_break()
        r = p.add_run(line)
        r.font.name = "Consolas"
        r.font.size = Pt(8)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def make_diagram(path, title, columns, arrows=True):
    W, H = 1800, 900
    im = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(im)
    font_path = r"C:\Windows\Fonts\sylfaen.ttf"
    bold_path = r"C:\Windows\Fonts\sylfaen.ttf"
    title_font = ImageFont.truetype(bold_path, 44)
    box_font = ImageFont.truetype(font_path, 26)
    small = ImageFont.truetype(font_path, 21)
    d.text((70, 42), title, font=title_font, fill="#17324D")
    n = len(columns)
    gap = 38
    usable = W - 140 - gap * (n - 1)
    bw = usable // n
    x = 70
    centers = []
    for idx, (head, lines, color) in enumerate(columns):
        y0, y1 = 150, 795
        d.rounded_rectangle((x, y0, x + bw, y1), radius=24, fill=color, outline="#17324D", width=4)
        d.rounded_rectangle((x, y0, x + bw, y0 + 85), radius=24, fill="#17324D", outline="#17324D")
        d.text((x + 25, y0 + 24), head, font=box_font, fill="white")
        yy = y0 + 120
        for line in lines:
            d.ellipse((x + 24, yy + 7, x + 34, yy + 17), fill="#0B7A75")
            d.multiline_text((x + 48, yy), line, font=small, fill="#1B2631", spacing=6)
            yy += 70 + 24 * line.count("\n")
        centers.append((x + bw // 2, y0 + (y1 - y0) // 2))
        x += bw + gap
    if arrows and n > 1:
        for i in range(n - 1):
            x1 = centers[i][0] + bw // 2 + 7
            x2 = centers[i + 1][0] - bw // 2 - 7
            y = 470
            d.line((x1, y, x2, y), fill="#246B9E", width=8)
            d.polygon([(x2, y), (x2 - 24, y - 15), (x2 - 24, y + 15)], fill="#246B9E")
    im.save(path, quality=96)


def add_figure(doc, path, caption, width=7.15):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(str(path), width=Inches(width))
    c = doc.add_paragraph(caption)
    c.alignment = WD_ALIGN_PARAGRAPH.CENTER
    c.style = doc.styles["Caption"]


def cols(*items):
    return "\n".join(items)


def main():
    diagrams = {
        "planes": ASSETS / "01_planes.png",
        "workflow": ASSETS / "02_workflow.png",
        "generation": ASSETS / "03_generation.png",
        "stat": ASSETS / "04_stat_model.png",
        "runtime": ASSETS / "05_runtime.png",
    }
    make_diagram(diagrams["planes"], "KIDS პლატფორმის პასუხისმგებლობის სამი სიბრტყე", [
        ("მართვის სიბრტყე", ["კონტრაქტები და ვერსიები", "კლასიფიკატორები და სემანტიკა", "დამტკიცება და აუდიტი", "რელიზი და rollback"], "#EAF2F7"),
        ("მონაცემთა სიბრტყე", ["უცვლელი წყარო", "staging და validation", "canonical entity და statistics", "published snapshots"], "#E8F4F1"),
        ("გამოცდილების სიბრტყე", ["პანელი და API", "ცხრილი და დიაგრამა", "ძიება და export", "საჯარო KIDS საიტი"], "#F7F1E8"),
    ], arrows=False)
    make_diagram(diagrams["workflow"], "კონტრაქტის დამტკიცებისა და გამოშვების ნაკადი", [
        ("ავტორინგი", ["პროდუქტი და საიტის ხე", "dataset, field, key, relation", "classifier, DSD, metric", "policy და presentation"], "#EAF2F7"),
        ("მტკიცებულება", ["schema diff", "lint და impact analysis", "ოთხთვალის პრინციპი", "ხელმოწერილი გადაწყვეტილება"], "#F7F1E8"),
        ("გამოშვება", ["immutable revision", "checksum და manifest", "ცარიელი Access template", "API contract bundle"], "#E8F4F1"),
        ("შესრულება", ["შევსება და upload", "სტრუქტურული validation", "semantic gates", "atomic publish ან quarantine"], "#EEF0F8"),
    ])
    make_diagram(diagrams["generation"], "Access პაკეტის გენერაციისა და მიღების sequence", [
        ("Panel API", ["იღებს APPROVED revision-ს", "კეტავს snapshot-ს", "ქმნის generation job-ს"], "#EAF2F7"),
        ("Generator", ["ქმნის 21 ცხრილს", "PK, UX და 21 FK", "წერს manifest-ს", "ტოვებს მხოლოდ\nცარიელ data rows-ს"], "#E8F4F1"),
        ("Validator", ["checksum და revision", "exact schema", "relations და classifiers", "quality და confidentiality"], "#F7F1E8"),
        ("Publisher", ["staging snapshot", "atomic pointer switch", "outbox notification", "reproducible rollback"], "#EEF0F8"),
    ])
    make_diagram(diagrams["stat"], "სტატისტიკური სემანტიკის კანონიკური მოდელი", [
        ("სტრუქტურა", ["Dataflow", "DSD revision", "Dimension და codelist", "Measure და attributes"], "#EAF2F7"),
        ("მნიშვნელობა", ["Metric", "Unit და scale", "Aggregation", "Quality და confidentiality"], "#E8F4F1"),
        ("ფაქტი", ["Series key", "Observation", "Time period", "Dimension values და lineage"], "#F7F1E8"),
        ("პრეზენტაცია", ["Table definition", "Chart definition", "API projection", "Export profile"], "#EEF0F8"),
    ])
    make_diagram(diagrams["runtime"], "მოდულური მონოლითის runtime განაწილება", [
        ("web", ["Admin UI", "BFF security", "WCAG 2.2 AA", "optimistic concurrency UX"], "#EAF2F7"),
        ("api", ["command და query endpoints", "application services", "policy enforcement", "OpenAPI 3.2"], "#E8F4F1"),
        ("core", ["domain aggregates", "state machines", "ports და validators", "migration contracts"], "#F7F1E8"),
        ("infrastructure", ["SQL Server", "object storage", "Jackcess worker", "outbox and telemetry"], "#EEF0F8"),
    ])

    doc = Document()
    sec = doc.sections[0]
    sec.top_margin = Inches(0.65)
    sec.bottom_margin = Inches(0.65)
    sec.left_margin = Inches(0.72)
    sec.right_margin = Inches(0.72)
    sec.header_distance = Inches(0.25)
    sec.footer_distance = Inches(0.3)
    styles = doc.styles
    styles["Normal"].font.name = "Sylfaen"
    styles["Normal"]._element.rPr.rFonts.set(qn("w:eastAsia"), "Sylfaen")
    styles["Normal"].font.size = Pt(9.2)
    styles["Normal"].font.color.rgb = RGBColor.from_string(INK)
    for name, size, color in [("Title", 25, NAVY), ("Heading 1", 17, NAVY), ("Heading 2", 13, BLUE), ("Heading 3", 10.5, TEAL)]:
        s = styles[name]
        s.font.name = "Sylfaen"
        s._element.rPr.rFonts.set(qn("w:eastAsia"), "Sylfaen")
        s.font.size = Pt(size)
        s.font.color.rgb = RGBColor.from_string(color)
        s.font.bold = True
        s.paragraph_format.space_before = Pt(10)
        s.paragraph_format.space_after = Pt(5)
    styles["Title"].paragraph_format.space_after = Pt(14)
    title_ppr = styles["Title"]._element.get_or_add_pPr()
    for border in title_ppr.findall(qn("w:pBdr")):
        title_ppr.remove(border)
    styles["Caption"].font.name = "Sylfaen"
    styles["Caption"].font.size = Pt(8)
    styles["Caption"].font.italic = True
    styles["List Bullet"].font.name = "Sylfaen"
    styles["List Number"].font.name = "Sylfaen"

    header = sec.header.paragraphs[0]
    header.text = "KIDS მართვის პანელის ნორმატიული blueprint"
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    header.runs[0].font.size = Pt(7.5)
    header.runs[0].font.color.rgb = RGBColor.from_string(BLUE)
    footer = sec.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    footer.add_run("KIDS PORTAL  •  კონტრაქტი მართავს, Access ასრულებს  •  გვერდი ").font.size = Pt(7.5)
    add_field(footer, "PAGE")

    title = doc.add_paragraph(style="Title")
    title.alignment = WD_ALIGN_PARAGRAPH.LEFT
    title.add_run("KIDS მართვის პანელის სრული არქიტექტურული და იმპლემენტაციის სპეციფიკაცია")
    sub = doc.add_paragraph()
    sub.add_run("ნორმატიული blueprint რომელიც ტოვებს მხოლოდ კოდის დაწერას").bold = True
    sub.runs[0].font.size = Pt(14)
    add_para(doc, "საბაზისო კონტრაქტი  KIDS_PORTAL_V1 revision 7")
    add_para(doc, "საბაზისო წყარო  samples/kids-children-portal-full-data.accdb")
    add_para(doc, "საბაზისო შესრულებადი პაკეტი  samples/kids-portal-v1-canonical-r7.accdb")
    add_para(doc, f"სპეციფიკაციის თარიღი  {date.today().isoformat()}  Asia Tbilisi")
    add_para(doc, "სტატუსი  IMPLEMENTATION READY NORMATIVE BASELINE")
    doc.add_paragraph()
    add_table(doc, ["დოკუმენტის დაპირება", "შედეგი"], [
        ("Authority", "პანელი არის კონტრაქტის, სემანტიკის, კლასიფიკატორის, პოლიტიკისა და publication გადაწყვეტილების ერთადერთი authority."),
        ("Execution", "Access არის პანელის მიერ გამოშვებული კონტრაქტის data bearing distribution და ვერ ცვლის მის მნიშვნელობას."),
        ("Completeness", "ქვემოთ გამოცხადებულია მოდულები, ეკრანები, workflow, state machine, API, authoritative ცხრილები, ყველა Access ცხრილი და ველი, generation/import წესები, უსაფრთხოება და Definition of Done."),
        ("Non fabrication", "წყაროში დაუდასტურებელი მნიშვნელობა არ გამოიგონება. ის ინახება როგორც proposal და blocking gate ღიად რჩება."),
    ], [1.35, 5.85], 9)
    doc.add_page_break()

    add_heading(doc, "სარჩევი", 1)
    p = doc.add_paragraph()
    add_field(p, 'TOC \\o "1-3" \\h \\z \\u')
    add_para(doc, "სარჩეველი Word-ში იხსნება ავტომატური ველით და განახლდება Ctrl A შემდეგ F9 ბრძანებით.")
    doc.add_page_break()

    add_heading(doc, "ნორმატიული გადაწყვეტილება", 1)
    add_para(doc, "შენი ძირითადი აზრი სწორია და აქ დაზუსტებულია: საიტის სრული კონტრაქტი ჯერ იქმნება და მტკიცდება პანელში; შემდეგ პანელი გამოსცემს ამ კონკრეტული immutable revision-ის ცარიელ Access პაკეტს; Access-ის ავტორი მხოლოდ გამოცხადებულ data rows-ს ავსებს; import engine ამოწმებს exact contract-ს და მხოლოდ ყველა blocking gate-ის გავლის შემდეგ აქვეყნებს snapshot-ს.")
    add_table(doc, ["წესი", "ნორმატიული მოთხოვნა", "შეცდომა რომელსაც გამორიცხავს"], [
        ("ერთი authority", "MUST  კონტრაქტის source of truth არის Control Plane database და არა Access, JSON, UI state ან Java generator-ის hardcode.", "ორი განსხვავებული schema და სემანტიკის drift"),
        ("immutable revision", "MUST  APPROVED revision აღარ რედაქტირდება; ცვლილება ქმნის ახალ revision-ს და compatibility შეფასებას.", "ისტორიის გადაწერა და არარეპროდუცირებადი publish"),
        ("data only Access", "MUST NOT  Access შეიცავდეს credentials, target SQL names, arbitrary SQL, publication command ან დამოუკიდებელ approval-ს.", "privilege escalation და გარემოზე მიბმა"),
        ("no chart data ownership", "MUST  chart ებმის metric + dimensions + presentation specification-ს და არა ჩაშენებულ რიცხვებს.", "hardcoded სტატისტიკა"),
        ("raw boundary", "MUST  თავდაპირველი artifact ინახება უცვლელად object storage-ში; პაკეტში raw locator და checksum საკმარისია.", "raw JSON-ის გამეორება carrier table-ში"),
        ("fail closed", "MUST  გაურკვეველი classifier, unit, grain, relation ან confidentiality policy ბლოკავს publication-ს.", "ჩუმი არასწორი ინტერპრეტაცია"),
    ], [1.2, 3.8, 2.2], 8.5)
    add_figure(doc, diagrams["planes"], "ნახაზი 1  პასუხისმგებლობათა სიბრტყეები და authority boundary")

    add_heading(doc, "საბაზისო მტკიცებულება და ინვენტარი", 1)
    add_para(doc, "Blueprint ეყრდნობა უშუალოდ repository-ის migration-ებს 001–030, Java 17 Spring Boot კოდს, registry-driven Jackcess generator-სა და მიღებულ Access audit-ს. ძველი ბაზა არის მიგრაციის წყარო და არა სამიზნე schema.")
    add_table(doc, ["ობიექტი", "რაოდენობა", "კანონიკური როლი", "გადაწყვეტილება"], [
        ("goals_titles", "17", "კლასიფიკატორის წყარო", "goal category scheme და alias-ები"),
        ("goals", "36", "არასტატისტიკური entity", "KIDS_GOAL"),
        ("files", "225", "რესურსის entity და შესაძლო statistical carrier", "KIDS_RESOURCE; 43 parseable carrier"),
        ("glossary", "178", "არასტატისტიკური entity", "KIDS_GLOSSARY_ENTRY"),
        ("ყველა raw locator", "456", "lineage", "__raw_document; ორიგინალი bytes უცვლელ artifact-ში"),
        ("resource subcategory assignment", "230", "many to many relation", "ცალკე assignment table"),
        ("statistical carrier", "43", "resource bound carrier", "payload_checksum; raw JSON არა"),
        ("statistical input cell", "880", "lossless source cell", "long form period × age × lexical value"),
        ("unit და metric", "10 / 43", "governed semantics", "ერთი explicit binding თითო carrier-ზე"),
        ("package contract", "22 tables", "16 metadata/data structures, 165 fields", "all declared indexes, 21 RI relations, 5 projection slots"),
    ], [1.9, .75, 2.0, 2.55], 8.2)
    add_table(doc, ["ძველი ცხრილი", "ძველი ველები", "რატომ არ რჩება საბოლოო physical model-ად"], [
        ("goals_titles", "ID, category, title_geo, title_eng", "კოდი, ვერსია, label და authority ერთმანეთშია შერეული"),
        ("goals", "ID, category, title_geo, title_eng, path_geo, path_eng", "კლასიფიკაცია, multilingual text და locator ცალკე semantic roles უნდა იყოს"),
        ("files", "ID, category, sub_category, title_geo, title_eng, path_geo, path_eng, chartdata", "comma list და chart JSON არღვევს 1NF-ს და statistical meaning-ს მალავს"),
        ("glossary", "ID, lang, text", "language value domain და translation identity გამოცხადებული არ არის"),
    ], [1.15, 3.0, 3.15], 8.2)

    add_heading(doc, "საერთაშორისო საფუძველი", 1)
    standards = [
        ("SDMX 3.1 და ISO 17369", "dataflow, DSD, dimension, codelist, measure, attribute, constraint და statistical exchange", "სტატისტიკურ ფენას; არა ყველა content table-ს"),
        ("ISO IEC 11179", "registry identity, data element, value domain, definition და stewardship", "metadata registry-ს; არა physical SQL engine-ს"),
        ("W3C DCAT 3", "dataset, distribution, data service, version, checksum, catalog discovery", "catalog/export metadata-ს"),
        ("W3C PROV O", "entity, activity, agent და derivation lineage", "artifact row transform release provenance-ს"),
        ("JSON Schema 2020 12", "machine readable validation, dialect და reusable schema", "mapping, policy და presentation JSON payload-ებს"),
        ("OpenAPI 3.2", "language neutral HTTP contract", "Admin და public API description-ს"),
        ("WCAG 2.2 AA", "keyboard, focus, target size, errors და accessible authentication", "პანელის UI acceptance-ს"),
        ("OWASP ASVS 5.0", "application security verification baseline", "L2 ყველა panel ფუნქციაზე; L3 secrets და publication-ზე"),
        ("OAuth 2.0 Security BCP RFC 9700", "authorization code with PKCE, exact redirects, least scope და sender constraints", "OIDC/OAuth authentication-ს"),
        ("NIST SP 800 218 SSDF", "secure development practices და supply chain evidence", "CI CD და release assurance-ს"),
    ]
    add_table(doc, ["სტანდარტი", "რას იღებს სისტემა", "საზღვარი"], standards, [1.65, 3.25, 2.3], 8.2)

    add_heading(doc, "სისტემური არქიტექტურა", 1)
    add_para(doc, "სამიზნე არის მოდულური მონოლითი. ეს არჩევანი ინარჩუნებს ერთ ტრანზაქციას contract authoring-ისთვის, ამცირებს ოპერაციულ სირთულეს და bounded context-ებს ისე გამოყოფს, რომ მოგვიანებით მხოლოდ გაზომილი bottleneck-ის შემთხვევაში გახდეს შესაძლებელი ცალკე service-ად გატანა.")
    add_figure(doc, diagrams["runtime"], "ნახაზი 2  repository-ის შესაბამისი runtime არქიტექტურა")
    add_table(doc, ["bounded context", "ფლობს", "არ ფლობს", "public port"], [
        ("Identity and Access", "role, permission, scope, session policy", "dataset semantics", "AuthorizationService"),
        ("Catalog", "product, dataset, version, field, key, relation", "observations", "CatalogCommandService და CatalogQueryService"),
        ("Site Contract", "site tree, routes, contract revision, gates", "runtime page content", "ContractLifecycleService"),
        ("Classification", "scheme, version, item, hierarchy, alias, proposal", "entity rows", "ClassificationService"),
        ("Statistics", "unit, dimension, measure, metric, DSD, binding", "chart pixels", "StatisticalSemanticsService"),
        ("Quality and Privacy", "quality rule, score, confidentiality rule", "raw source mutation", "PolicyEvaluationService"),
        ("Ingestion", "source, mapping, preview, jobs, checkpoints", "approval authority", "IngestionOrchestrator"),
        ("Package", "manifest projection, Access generation, signature", "business data creation", "PackageGenerationService"),
        ("Publication", "release, snapshot pointer, rollback, outbox", "contract editing", "PublicationService"),
        ("Experience", "table, chart, export, API projection", "fact values", "PresentationRegistry"),
        ("Audit", "append only events and decision evidence", "mutable business state", "AuditQueryService"),
    ], [1.25, 2.1, 2.05, 1.8], 7.8)
    add_heading(doc, "Layer და dependency წესები", 2)
    bullets(doc, [
        "domain არ იმპორტავს Spring, SQL, Jackcess, HTTP ან UI კლასებს; ის შეიცავს aggregates, value objects, policies და state transitions-ს.",
        "application layer ახორციელებს use case-ს, authorization-ს, idempotency-ს და transaction boundary-ს.",
        "adapter in იღებს REST ან UI command-ს; adapter out წერს SQL Server-ში, object storage-ში ან Access-ში.",
        "CQRS გამოიყენება application boundary-ზე: command მოდელი იცავს invariants-ს, query მოდელი ემსახურება grid, graph და dashboard view-ებს; event sourcing არ გამოიყენება.",
        "ყველა cross-plane side effect იწერება transactional outbox-ში; მომხმარებლის request არ ელოდება გრძელ generator/import პროცესს.",
        "optimistic concurrency სავალდებულოა row_version ან ETag-ით ყველა editable aggregate-ზე.",
    ])

    doc.add_page_break()
    add_heading(doc, "პანელის ინფორმაციული არქიტექტურა", 1)
    screens = [
        ("მიმოხილვა", "Dashboard", "ღია approvals, failed gates, active imports, release health, outbox lag", "filter by product; open task; acknowledge incident"),
        ("პროდუქტები", "Product list and details", "code, bilingual title, owner, sensitivity, lifecycle, active revision", "create draft; clone; assign owner; retire"),
        ("საიტის ხე", "Site contract tree", "node code, parent, kind, path, dataset, order, labels", "add move reorder; route collision check; preview"),
        ("კონტრაქტის revisions", "Revision workspace", "parent, compatibility, checksum, status, change set", "branch; diff; submit; approve; issue; supersede"),
        ("datasets", "Dataset catalog", "family, grain, retention, version, table binding", "create version; edit grain; bind source; impact view"),
        ("fields", "Schema editor", "ordinal, code, type, role, required, key, classifier, source mapping", "add reorder deprecate; validate; generate preview"),
        ("keys and relations", "Relation graph", "PK UX FK hierarchy lineage cardinality enforcement load order", "connect; edit; cycle test; orphan preview"),
        ("classifiers", "Scheme registry", "authority, versions, items, labels, dates, hierarchy", "import; compare; publish; retire"),
        ("alias proposals", "Proposal inbox", "raw code, normalized key, candidate, confidence, evidence", "accept; remap; reject; bulk decide"),
        ("statistics", "Dataflow and DSD builder", "components, roles, attachment, grain, constraints", "compose; lint; compare; submit"),
        ("metrics", "Metric registry", "definition, measure, unit, aggregation, dimensions, owner", "create; bind carrier; approve; deprecate"),
        ("quality", "Quality policy builder", "rule type, severity, expression, threshold, scope", "test on sample; approve; version"),
        ("confidentiality", "Privacy policy builder", "class, thresholds, action, audience, legal basis", "simulate; approve; version"),
        ("sources", "Source registry", "type, trust, endpoint metadata, secret reference, schedule", "test connection; rotate reference; disable"),
        ("mappings", "Mapping studio", "source locator and field to canonical field transforms", "map; validate; preview 100 rows; save revision"),
        ("generation", "Package generator", "revision, profile, job, checksum, signature, expiry", "generate; verify; download; revoke"),
        ("imports", "Import jobs", "artifact, checksum, progress, counts, issues, checkpoint", "preview; start; resume; cancel; quarantine; retry"),
        ("publication", "Release workspace", "members, gate report, approval evidence, target snapshot", "prepare; approve; publish; rollback"),
        ("presentation", "Tables charts exports APIs", "metric, dimensions, filters, format, accessibility", "preview; approve; publish; retire"),
        ("audit", "Audit explorer", "actor, action, before after hashes, correlation, timestamp", "filter; export signed evidence"),
        ("security", "Roles and scopes", "role, permission, product scope, validity", "grant; revoke; emergency access review"),
        ("operations", "Jobs outbox migrations", "lease, queue age, retries, migration checksum", "retry safe job; pause worker; inspect failure"),
    ]
    add_table(doc, ["ნავიგაცია", "ეკრანი", "რა ჩანს", "დაშვებული მოქმედებები"], screens, [1.0, 1.25, 2.9, 2.15], 7.5)
    add_heading(doc, "ეკრანის უცვლელი UX წესები", 2)
    bullets(doc, [
        "ყველა რედაქტირების გვერდზე ჩანს product, contract revision, lifecycle state, unsaved changes და ETag.",
        "Save არ უდრის Approve-ს; approval ცალკე permission და ცალკე dialog-ია, სადაც change summary და gate report სავალდებულოა.",
        "საშიში მოქმედება იყენებს typed confirmation-ს მხოლოდ issue, publish, rollback, revoke და destructive retirement-ზე.",
        "validation error მიბმულია კონკრეტულ field/node/edge-ზე და აქვს machine code, ახსნა და remediation; მხოლოდ წითელი ფერი არასაკმარისია.",
        "graph editor-ს ყოველთვის აქვს ეკვივალენტური accessible table view; drag and drop-ს აქვს keyboard alternative.",
        "ყველა გრძელი job არის resumable და UI polling-ს ცვლის server sent events ან WebSocket progress; refresh არ კარგავს მდგომარეობას.",
    ])

    add_heading(doc, "როლები და პასუხისმგებლობათა გამიჯვნა", 1)
    roles = [
        ("Platform Administrator", "identity, environment policy, emergency recovery", "ვერ ამტკიცებს საკუთარ semantic change-ს"),
        ("Product Owner", "scope, site tree, release business approval", "ვერ ცვლის security policy-ს"),
        ("Data Architect", "dataset, field, key, relation, compatibility", "ვერ აქვეყნებს საკუთარ revision-ს მარტო"),
        ("Classifier Steward", "scheme, version, item, alias, hierarchy", "ვერ ცვლის observation value-ს"),
        ("Statistical Steward", "metric, unit, aggregation, DSD, quality", "ვერ ცვლის raw artifact-ს"),
        ("Privacy Officer", "confidentiality, audience, suppression decision", "ვერ ამტკიცებს source mapping-ს"),
        ("Data Engineer", "source, mapping, import execution, remediation", "ვერ ამტკიცებს contract-ს ან publish-ს"),
        ("Publisher", "release execution and rollback", "ვერ ცვლის approved contract-ს"),
        ("Auditor", "read all metadata, evidence and history", "write აკრძალულია"),
        ("Viewer", "read permitted product metadata and jobs", "approval, generation და publish აკრძალულია"),
    ]
    add_table(doc, ["როლი", "უფლებამოსილება", "აკრძალული კომბინაცია"], roles, [1.65, 3.25, 2.4], 8)
    add_para(doc, "ოთხთვალის წესი  author_user_id და approver_user_id MUST NOT იყოს ერთი მომხმარებელი CONTRACT_APPROVAL, CLASSIFIER_PUBLISH, SEMANTIC_APPROVAL, PRIVACY_APPROVAL და RELEASE_PUBLISH მოქმედებებზე. Emergency override მოითხოვს მიზეზს, ვადიან elevation-ს და შემდგომ დამოუკიდებელ review-ს.")

    add_heading(doc, "Lifecycle და state machine", 1)
    add_figure(doc, diagrams["workflow"], "ნახაზი 3  კონტრაქტიდან შესრულებად პაკეტამდე")
    state_rows = [
        ("Site contract revision", "DRAFT → IN_REVIEW → APPROVED → ISSUED → SUPERSEDED", "REJECTED აბრუნებს DRAFT-ში ახალი ცვლილების ნომრით; APPROVED immutable"),
        ("Dataset version", "DRAFT → IN_REVIEW → APPROVED → ACTIVE → DEPRECATED → RETIRED", "breaking change მხოლოდ ახალი version"),
        ("Classifier version", "DRAFT → IN_REVIEW → PUBLISHED → RETIRED", "published item delete აკრძალულია; successor mapping გამოიყენება"),
        ("Proposal", "OPEN → MATCHED or NEW_ITEM_REQUIRED → APPROVED or REJECTED", "OPEN proposal blocking-ია required field-ზე"),
        ("Metric semantic", "DRAFT → PROVISIONAL_APPROVED → APPROVED → DEPRECATED", "confidence ინახება; APPROVED-ს სჭირდება steward"),
        ("Package job", "QUEUED → GENERATING → VALIDATING → READY or FAILED or REVOKED", "READY artifact immutable; იგივე idempotency key იგივე შედეგს აბრუნებს"),
        ("Import batch", "RECEIVED → CATALOGED → VALIDATING → STAGING → SEMANTIC_VALIDATION → READY_TO_PUBLISH → PUBLISHED", "ნებისმიერი ეტაპიდან FAILED ან QUARANTINED; resume checkpoint-იდან"),
        ("Release", "DRAFT → READY → PUBLISHING → PUBLISHED → SUPERSEDED or ROLLED_BACK", "rollback ქმნის ახალ pointer event-ს; ძველ snapshot-ს არ ცვლის"),
    ]
    add_table(doc, ["aggregate", "დაშვებული გზა", "invariant"], state_rows, [1.4, 3.4, 2.5], 8)
    add_heading(doc, "Contract approval gate matrix", 2)
    gates = [
        ("G01 EXACT_SCHEMA", "Structure", "Access table and field set, order, type, requiredness", "BLOCK"),
        ("G02 KEY_UNIQUENESS", "Integrity", "PK and declared UX uniqueness", "BLOCK"),
        ("G03 RELATION_RESOLUTION", "Integrity", "required relation has no orphan and load order is acyclic", "BLOCK"),
        ("G04 CLASSIFIERS_PUBLISHED", "Semantics", "required aliases resolve to exact published version", "BLOCK"),
        ("G05 STATISTICAL_SEMANTICS", "Semantics", "metric, unit, aggregation, DSD, status and policies complete", "BLOCK"),
        ("G06 RAW_LINEAGE", "Provenance", "artifact checksum and row locator cover every source-derived row", "BLOCK"),
        ("G07 ATOMIC_PUBLICATION", "Publication", "staging snapshot complete and rollback pointer valid", "BLOCK"),
        ("G08 ROUTE_UNIQUENESS", "Site", "sibling path segment and public route unique", "BLOCK"),
        ("G09 LOCALIZATION", "Content", "required Georgian label and language tag valid", "BLOCK"),
        ("G10 COMPATIBILITY", "Evolution", "schema diff classification accepted", "BLOCK"),
        ("G11 PRIVACY", "Confidentiality", "audience, small count and export rules evaluated", "BLOCK"),
        ("G12 ACCESSIBILITY", "Presentation", "table/chart alternative, label, contrast and keyboard checks", "BLOCK for public"),
        ("G13 PERFORMANCE_BUDGET", "Serving", "query plan and row/export limits within budget", "WARN then BLOCK at publish"),
        ("G14 SECURITY_EVIDENCE", "Security", "ASVS tests, dependency scan and signed build evidence", "BLOCK"),
    ]
    add_table(doc, ["კოდი", "კატეგორია", "შემოწმება", "ეფექტი"], gates, [1.45, 1.05, 4.15, .65], 7.8)

    add_heading(doc, "პანელის authoritative მონაცემთა მოდელი", 1)
    add_para(doc, "ქვემოთ მოცემული dictionary არის სამიზნე Control Plane schema. EXISTING ნიშნავს რომ ცხრილი უკვე გამოცხადებულია migration 001–028-ში; EXTEND ნიშნავს თავსებად დამატებას; NEW ნიშნავს პანელის სრულყოფილი შესრულებისთვის საჭირო ახალ migration-ს. `row_version` არის SQL Server rowversion და ყველა editable aggregate-ზე სავალდებულოა.")
    panel_tables = [
        ("platform.data_product", "EXTEND", "მონაცემთა პროდუქტის root aggregate", cols("product_id BIGINT PK IDENTITY", "product_code NVARCHAR(120) UQ required", "page_id BIGINT FK nullable", "title_ka NVARCHAR(255) required", "title_en NVARCHAR(255) nullable", "description_ka NVARCHAR(MAX) nullable NEW", "description_en NVARCHAR(MAX) nullable NEW", "owner_user_id BIGINT FK nullable", "lifecycle_status VARCHAR(24) required", "sensitivity VARCHAR(24) required", "active_contract_revision_id BIGINT FK nullable NEW", "created_at DATETIME2 required", "updated_at DATETIME2 required", "row_version ROWVERSION NEW")),
        ("platform.dataset", "EXTEND", "ლოგიკური dataset identity", cols("dataset_id BIGINT PK", "product_id BIGINT FK required", "dataset_code NVARCHAR(120) required", "dataset_family VARCHAR(24) required", "business_grain NVARCHAR(500) required", "retention_policy NVARCHAR(64) required", "lifecycle_status VARCHAR(24) required", "owner_user_id BIGINT FK nullable NEW", "description_ka NVARCHAR(MAX) nullable NEW", "description_en NVARCHAR(MAX) nullable NEW", "created_at DATETIME2 required", "row_version ROWVERSION NEW", "UQ product_id + dataset_code")),
        ("platform.dataset_version", "EXTEND", "immutable schema version", cols("dataset_version_id BIGINT PK", "dataset_id BIGINT FK required", "version INT required", "status VARCHAR(24) required", "contract_checksum CHAR(64) nullable until approval", "compatibility_mode VARCHAR(32) NEW", "effective_from DATETIME2 nullable", "effective_to DATETIME2 nullable", "approved_by_user_id BIGINT FK nullable", "created_at DATETIME2 required", "row_version ROWVERSION NEW", "UQ dataset_id + version")),
        ("platform.attribute", "EXTEND", "field და ISO 11179 data element", cols("attribute_id BIGINT PK", "dataset_version_id BIGINT FK required", "attribute_code NVARCHAR(128) required", "logical_type VARCHAR(24) required", "attribute_role VARCHAR(24) required", "cardinality VARCHAR(16) required", "required BIT required", "searchable BIT required", "filterable BIT required", "groupable BIT required", "label_ka NVARCHAR(255) required", "label_en NVARCHAR(255) nullable", "definition_ka NVARCHAR(MAX) nullable", "definition_en NVARCHAR(MAX) nullable", "unit_code NVARCHAR(64) nullable", "json_path NVARCHAR(512) nullable", "ordinal INT required NEW", "default_value_json NVARCHAR(MAX) nullable NEW", "classifier_version_id BIGINT FK nullable NEW", "deprecated BIT required NEW", "row_version ROWVERSION NEW", "UQ dataset_version_id + attribute_code", "UQ dataset_version_id + ordinal")),
        ("platform.dataset_key", "NEW", "primary ან unique key declaration", cols("dataset_key_id BIGINT PK", "dataset_version_id BIGINT FK required", "key_code NVARCHAR(128) required", "key_type VARCHAR(16) required PK or UNIQUE", "null_policy VARCHAR(24) required", "status VARCHAR(24) required", "UQ dataset_version_id + key_code")),
        ("platform.dataset_key_field", "NEW", "ordered key field", cols("dataset_key_id BIGINT FK required", "attribute_id BIGINT FK required", "key_order INT required", "PK dataset_key_id + attribute_id", "UQ dataset_key_id + key_order")),
        ("platform.validation_rule", "EXTEND", "machine executable quality rule", cols("validation_rule_id BIGINT PK", "dataset_version_id BIGINT FK required", "rule_code NVARCHAR(128) required", "rule_type VARCHAR(32) required", "severity VARCHAR(16) required", "expression_json NVARCHAR(MAX) JSON required", "message_ka NVARCHAR(1000) NEW", "message_en NVARCHAR(1000) nullable NEW", "scope_json NVARCHAR(MAX) JSON NEW", "active BIT required", "created_at DATETIME2 required", "row_version ROWVERSION NEW", "UQ dataset_version_id + rule_code")),
        ("platform.relationship_type", "EXISTING", "კავშირის governed vocabulary", cols("relationship_type_id BIGINT PK", "code NVARCHAR(64) UQ required", "category VARCHAR(32) required", "directional BIT required", "transitive BIT required", "temporal BIT required", "description NVARCHAR(1000) nullable")),
        ("platform.dataset_relationship", "EXTEND", "dataset version endpoints და enforcement", cols("relationship_id BIGINT PK", "from_dataset_version_id BIGINT FK required", "to_dataset_version_id BIGINT FK required", "relationship_type_id BIGINT FK required", "source_attribute_id BIGINT FK nullable", "target_attribute_id BIGINT FK nullable", "cardinality VARCHAR(24) required", "required BIT required", "enforcement_policy VARCHAR(24) required", "delete_policy VARCHAR(24) required NEW", "load_priority INT required", "status VARCHAR(24) required NEW", "row_version ROWVERSION NEW")),
        ("platform.site_contract_revision", "EXTEND", "სრული საიტის immutable contract revision", cols("site_contract_revision_id BIGINT PK", "product_id BIGINT FK required", "contract_code NVARCHAR(120) required", "revision INT required", "parent_revision_id BIGINT self FK nullable", "schema_standard NVARCHAR(120) required", "compatibility_mode VARCHAR(32) required", "status VARCHAR(24) required", "contract_checksum CHAR(64) required", "contract_document_json NVARCHAR(MAX) JSON required", "effective_from DATETIME2 nullable", "approved_by_user_id BIGINT FK nullable", "issued_at DATETIME2 nullable NEW", "created_at DATETIME2 required", "row_version ROWVERSION NEW", "UQ contract_code + revision")),
        ("platform.site_contract_node", "EXTEND", "საიტის კანონიკური ხე და route", cols("node_id BIGINT PK", "site_contract_revision_id BIGINT FK required", "node_code NVARCHAR(120) required", "parent_node_id BIGINT self FK nullable", "node_kind VARCHAR(32) required", "dataset_code NVARCHAR(120) nullable", "path_segment NVARCHAR(255) required", "sort_order INT required", "required BIT required", "title_ka NVARCHAR(255) required", "title_en NVARCHAR(255) nullable", "visibility_policy_code NVARCHAR(120) nullable NEW", "seo_json NVARCHAR(MAX) JSON nullable NEW", "UQ revision + node_code", "UQ revision + parent_node_id + path_segment")),
        ("platform.site_contract_dataset", "EXISTING", "contract to dataset version and Access table binding", cols("contract_dataset_id BIGINT PK", "site_contract_revision_id BIGINT FK required", "dataset_version_id BIGINT FK required", "dataset_code NVARCHAR(120) required", "access_table_name NVARCHAR(128) required", "data_family VARCHAR(24) required", "business_grain NVARCHAR(500) required", "natural_key_expression NVARCHAR(1000) required", "row_role VARCHAR(32) required", "load_order INT required", "required BIT required", "UQ revision + dataset_code", "UQ revision + access_table_name")),
        ("platform.site_contract_field", "EXISTING", "contract field projection", cols("contract_field_id BIGINT PK", "contract_dataset_id BIGINT FK required", "field_name NVARCHAR(128) required", "logical_type VARCHAR(24) required", "semantic_role VARCHAR(32) required", "ordinal INT required", "required BIT required", "key_role VARCHAR(24) nullable", "classifier_scheme_code NVARCHAR(120) nullable", "source_expression NVARCHAR(1000) nullable", "normalization_rule NVARCHAR(120) nullable", "UQ dataset + field", "UQ dataset + ordinal")),
        ("platform.site_contract_relation", "EXISTING", "contract relation projection", cols("contract_relation_id BIGINT PK", "site_contract_revision_id BIGINT FK required", "relation_code NVARCHAR(120) required", "from_dataset_code NVARCHAR(120) required", "from_field_name NVARCHAR(128) required", "to_dataset_code NVARCHAR(120) required", "to_field_name NVARCHAR(128) required", "relation_kind VARCHAR(32) required", "cardinality VARCHAR(24) required", "required BIT required", "enforcement_policy VARCHAR(24) required", "load_priority INT required", "UQ revision + relation_code")),
        ("platform.site_contract_classifier", "EXISTING", "contract classifier policy", cols("contract_classifier_id BIGINT PK", "site_contract_revision_id BIGINT FK required", "scheme_code NVARCHAR(120) required", "version_policy VARCHAR(32) required", "authority_mode VARCHAR(32) required", "unknown_value_policy VARCHAR(32) required", "publication_policy VARCHAR(32) required", "UQ revision + scheme_code")),
        ("platform.site_contract_gate", "EXISTING", "blocking validation gate", cols("gate_id BIGINT PK", "site_contract_revision_id BIGINT FK required", "gate_code NVARCHAR(120) required", "gate_type VARCHAR(32) required", "severity VARCHAR(16) required", "blocking BIT required", "rule_json NVARCHAR(MAX) JSON required", "UQ revision + gate_code")),
        ("platform.classification_scheme", "EXTEND", "versioned value domain root", cols("scheme_id BIGINT PK", "scheme_code NVARCHAR(120) UQ required", "title_ka NVARCHAR(255) required", "title_en NVARCHAR(255) nullable", "definition_ka NVARCHAR(MAX) NEW", "definition_en NVARCHAR(MAX) nullable NEW", "standard_reference NVARCHAR(255) nullable", "authority_mode VARCHAR(32) required NEW", "owner_user_id BIGINT FK nullable", "created_at DATETIME2 required", "row_version ROWVERSION NEW")),
        ("platform.classification_version", "EXTEND", "immutable codelist version", cols("classification_version_id BIGINT PK", "scheme_id BIGINT FK required", "version NVARCHAR(64) required", "status VARCHAR(24) required", "valid_from DATE nullable", "valid_to DATE nullable", "checksum CHAR(64) NEW", "approved_by_user_id BIGINT FK nullable NEW", "row_version ROWVERSION NEW", "UQ scheme_id + version")),
        ("platform.classification_item", "EXTEND", "versioned code and label", cols("classification_item_id BIGINT PK", "classification_version_id BIGINT FK required", "code NVARCHAR(128) required", "label_ka NVARCHAR(255) required", "label_en NVARCHAR(255) nullable", "definition_ka NVARCHAR(MAX) nullable NEW", "definition_en NVARCHAR(MAX) nullable NEW", "status VARCHAR(24) required", "sort_order INT nullable", "valid_from DATE nullable", "valid_to DATE nullable", "successor_item_id BIGINT self FK nullable NEW", "UQ version + code")),
        ("platform.classification_hierarchy", "EXISTING", "parent child edge", cols("parent_item_id BIGINT FK required", "child_item_id BIGINT FK required", "hierarchy_type VARCHAR(32) required", "valid_from DATE nullable", "valid_to DATE nullable", "PK parent + child + hierarchy_type")),
        ("platform.classification_alias", "EXTEND", "external raw code mapping", cols("alias_id BIGINT PK", "classification_item_id BIGINT FK required", "external_system_code NVARCHAR(120) required", "external_code NVARCHAR(255) required", "normalized_code NVARCHAR(255) NEW", "normalization_rule VARCHAR(120) NEW", "confidence DECIMAL(5,4) NEW", "decision_id BIGINT FK nullable NEW", "valid_from DATE nullable", "valid_to DATE nullable", "UQ external_system_code + external_code")),
        ("platform.classifier_proposal", "EXISTING", "unknown source value review queue", cols("proposal_id BIGINT PK", "scheme_id BIGINT FK required", "source_system_id BIGINT FK required", "source_value_raw NVARCHAR(1000) required", "source_value_normalized NVARCHAR(1000) required", "source_context_json NVARCHAR(MAX) JSON nullable", "proposed_item_id BIGINT FK nullable", "status VARCHAR(24) required", "resolution_note NVARCHAR(2000) nullable", "created_at DATETIME2 required", "resolved_at DATETIME2 nullable", "resolved_by_user_id BIGINT nullable")),
        ("platform.statistical_unit", "EXISTING", "unit scale and denominator", cols("unit_id BIGINT PK", "unit_code NVARCHAR(120) UQ required", "quantity_kind VARCHAR(24) required", "scale_factor DECIMAL(28,10) required", "denominator_text NVARCHAR(500) nullable", "title NVARCHAR(255) required", "status VARCHAR(24) required")),
        ("platform.dimension", "EXTEND", "statistical dimension concept", cols("dimension_id BIGINT PK", "dimension_code NVARCHAR(120) UQ required", "classification_scheme_id BIGINT FK nullable", "value_type VARCHAR(24) required", "title_ka NVARCHAR(255) required", "title_en NVARCHAR(255) nullable", "definition_ka NVARCHAR(MAX) nullable NEW", "definition_en NVARCHAR(MAX) nullable NEW", "role VARCHAR(24) required NEW", "status VARCHAR(24) required NEW", "row_version ROWVERSION NEW")),
        ("platform.measure", "EXTEND", "measure concept", cols("measure_id BIGINT PK", "measure_code NVARCHAR(120) UQ required", "value_type VARCHAR(24) required", "unit_code NVARCHAR(64) nullable", "decimal_precision INT nullable", "aggregation_default VARCHAR(24) required", "title_ka NVARCHAR(255) required", "title_en NVARCHAR(255) nullable", "definition_ka NVARCHAR(MAX) nullable NEW", "status VARCHAR(24) required NEW")),
        ("platform.metric", "EXTEND", "business indicator identity", cols("metric_id BIGINT PK", "metric_code NVARCHAR(120) UQ required", "source_dataset_id BIGINT FK required", "measure_id BIGINT FK required", "aggregation VARCHAR(24) required", "allowed_dimension_set_json NVARCHAR(MAX) JSON nullable", "definition_ka NVARCHAR(MAX) NEW", "definition_en NVARCHAR(MAX) nullable NEW", "owner_user_id BIGINT FK nullable NEW", "status VARCHAR(24) required", "row_version ROWVERSION NEW")),
        ("platform.metric_alias", "EXISTING", "external indicator to metric mapping", cols("metric_alias_id BIGINT PK", "metric_id BIGINT FK required", "external_system_code NVARCHAR(120) required", "external_code NVARCHAR(255) required", "valid_from DATE nullable", "valid_to DATE nullable", "UQ external system + external code")),
        ("platform.statistical_dataflow", "EXISTING", "SDMX compatible dataflow", cols("dataflow_id BIGINT PK", "product_id BIGINT FK required", "dataflow_code NVARCHAR(120) required", "source_dataset_id BIGINT FK required", "title_ka NVARCHAR(255) required", "title_en NVARCHAR(255) nullable", "status VARCHAR(24) required", "UQ product + dataflow_code")),
        ("platform.statistical_dsd", "EXISTING", "DSD immutable revision", cols("dsd_id BIGINT PK", "dataflow_id BIGINT FK required", "dsd_code NVARCHAR(120) required", "revision INT required", "observation_grain NVARCHAR(1000) required", "status VARCHAR(24) required", "valid_from DATE nullable", "valid_to DATE nullable", "UQ dataflow + dsd_code + revision")),
        ("platform.statistical_component", "EXISTING", "dimension measure or attribute component", cols("component_id BIGINT PK", "dsd_id BIGINT FK required", "component_code NVARCHAR(120) required", "component_role VARCHAR(24) required", "component_order INT required", "required BIT required", "dimension_id BIGINT FK nullable", "measure_id BIGINT FK nullable", "classification_version_id BIGINT FK nullable", "attachment_level VARCHAR(24) required", "source_field NVARCHAR(128) nullable", "normalized_field NVARCHAR(128) nullable", "constraint_json NVARCHAR(MAX) JSON nullable", "status VARCHAR(24) required", "UQ dsd + component_code", "UQ dsd + order")),
        ("platform.data_quality_policy", "EXTEND", "versioned quality policy", cols("quality_policy_id BIGINT PK", "policy_code NVARCHAR(120) required", "revision INT required NEW", "status VARCHAR(24) required", "rules_json NVARCHAR(MAX) JSON required", "owner_user_id BIGINT FK nullable NEW", "approved_by_user_id BIGINT FK nullable NEW", "checksum CHAR(64) NEW", "UQ policy_code + revision NEW")),
        ("platform.confidentiality_policy", "EXTEND", "versioned confidentiality policy", cols("confidentiality_policy_id BIGINT PK", "policy_code NVARCHAR(120) required", "revision INT required NEW", "status VARCHAR(24) required", "rules_json NVARCHAR(MAX) JSON required", "owner_user_id BIGINT FK nullable NEW", "approved_by_user_id BIGINT FK nullable NEW", "checksum CHAR(64) NEW", "UQ policy_code + revision NEW")),
        ("platform.metric_semantic_profile", "EXISTING", "metric inference and approval evidence", cols("metric_semantic_profile_id BIGINT PK", "metric_id BIGINT FK UQ required", "source_resource_external_key NVARCHAR(255) required", "unit_id BIGINT FK required", "inference_method VARCHAR(48) required", "inference_confidence DECIMAL(5,4) required", "inference_rationale NVARCHAR(1000) required", "approved_basis NVARCHAR(255) required", "status VARCHAR(24) required")),
        ("platform.statistical_semantic_binding", "EXISTING", "carrier to complete semantics binding", cols("statistical_semantic_binding_id BIGINT PK", "dataflow_id BIGINT FK required", "carrier_external_code NVARCHAR(255) required", "metric_id BIGINT FK required", "unit_id BIGINT FK required", "aggregation VARCHAR(24) required", "obs_status VARCHAR(8) required", "conf_status VARCHAR(8) required", "quality_policy_id BIGINT FK required", "confidentiality_policy_id BIGINT FK required", "status VARCHAR(24) required", "UQ dataflow + carrier")),
        ("platform.source_system", "EXISTING", "source authority registry", cols("source_system_id BIGINT PK", "source_code NVARCHAR(120) UQ required", "source_type VARCHAR(24) required", "title NVARCHAR(255) required", "trust_level VARCHAR(24) required", "enabled BIT required")),
        ("platform.source_connection", "EXTEND", "connection metadata only", cols("source_connection_id BIGINT PK", "source_system_id BIGINT FK required", "connection_kind VARCHAR(32) required", "endpoint NVARCHAR(1024) required", "database_name NVARCHAR(255) nullable", "username NVARCHAR(255) nullable", "secret_reference NVARCHAR(255) required", "encryption_mode VARCHAR(32) required", "enabled BIT required", "last_tested_at DATETIME2 nullable NEW", "last_test_status VARCHAR(24) nullable NEW", "UQ source + endpoint + database")),
        ("platform.ingestion_contract", "EXTEND", "active contract pointer", cols("contract_id BIGINT PK", "contract_code NVARCHAR(160) UQ required", "contract_revision INT required", "source_system_id BIGINT FK required", "dataset_id BIGINT FK required", "format_profile VARCHAR(48) required", "ingestion_method VARCHAR(32) required", "raw_ingest_enabled BIT required", "auto_publish BIT required and false by default", "status VARCHAR(24) required", "row_version ROWVERSION NEW", "UQ source_system + dataset")),
        ("platform.ingestion_contract_revision", "EXTEND", "immutable executable ingest revision", cols("ingestion_contract_revision_id BIGINT PK", "contract_id BIGINT FK required", "revision INT required", "format_profile VARCHAR(48) required", "lifecycle_status VARCHAR(24) required", "compatibility_mode VARCHAR(32) required", "site_contract_revision_id BIGINT FK NEW", "checksum CHAR(64) NEW", "captured_at DATETIME2 required", "UQ contract + revision")),
        ("platform.contract_source", "EXISTING", "active source mapping", cols("contract_source_id BIGINT PK", "contract_id BIGINT FK required", "source_locator NVARCHAR(1024) required", "source_kind VARCHAR(32) required", "target_dataset_version_id BIGINT FK required", "source_key_expression NVARCHAR(1024) nullable", "row_role VARCHAR(32) required", "load_order INT required", "mapping_spec_json NVARCHAR(MAX) JSON required", "active BIT required", "UQ contract + source_locator")),
        ("platform.contract_revision_source", "EXISTING", "immutable mapping snapshot", cols("contract_revision_source_id BIGINT PK", "ingestion_contract_revision_id BIGINT FK required", "source_locator NVARCHAR(1024) required", "source_kind VARCHAR(32) required", "target_dataset_version_id BIGINT FK required", "source_key_expression NVARCHAR(1024) nullable", "row_role VARCHAR(32) required", "load_order INT required", "mapping_spec_json NVARCHAR(MAX) JSON required", "UQ revision + source_locator")),
        ("platform.schema_diff", "NEW", "compatibility and impact evidence", cols("schema_diff_id BIGINT PK", "from_revision_id BIGINT FK nullable", "to_revision_id BIGINT FK required", "diff_json NVARCHAR(MAX) JSON required", "compatibility_class VARCHAR(32) required", "breaking_reasons_json NVARCHAR(MAX) JSON required", "evaluated_by_version NVARCHAR(64) required", "created_at DATETIME2 required")),
        ("platform.package_generation_job", "NEW", "asynchronous Access build", cols("generation_job_id BIGINT PK", "site_contract_revision_id BIGINT FK required", "format_profile VARCHAR(48) required", "idempotency_key NVARCHAR(128) UQ required", "status VARCHAR(24) required", "requested_by_user_id BIGINT FK required", "artifact_uri NVARCHAR(2048) nullable", "artifact_checksum CHAR(64) nullable", "artifact_signature NVARCHAR(MAX) nullable", "generator_version NVARCHAR(64) required", "error_json NVARCHAR(MAX) JSON nullable", "created_at DATETIME2 required", "started_at DATETIME2 nullable", "finished_at DATETIME2 nullable")),
        ("platform.package_artifact", "NEW", "issued distribution registry", cols("package_artifact_id BIGINT PK", "generation_job_id BIGINT FK UQ required", "package_code NVARCHAR(120) required", "package_version NVARCHAR(64) required", "object_uri NVARCHAR(2048) required", "checksum CHAR(64) required", "byte_size BIGINT required", "signature_algorithm VARCHAR(32) required", "signature_value NVARCHAR(MAX) required", "status VARCHAR(24) required", "expires_at DATETIME2 nullable", "revoked_at DATETIME2 nullable", "UQ package_code + package_version")),
        ("platform.approval_policy", "NEW", "approval route by object and risk", cols("approval_policy_id BIGINT PK", "policy_code NVARCHAR(120) UQ required", "object_type VARCHAR(48) required", "risk_level VARCHAR(24) required", "minimum_approvers INT required", "separation_of_duties BIT required", "status VARCHAR(24) required", "row_version ROWVERSION")),
        ("platform.approval_step", "NEW", "ordered required role", cols("approval_step_id BIGINT PK", "approval_policy_id BIGINT FK required", "step_order INT required", "required_role_code NVARCHAR(120) required", "decision_mode VARCHAR(24) required", "sla_hours INT nullable", "UQ policy + step_order")),
        ("platform.approval_instance", "NEW", "approval case bound to exact checksum", cols("approval_instance_id BIGINT PK", "approval_policy_id BIGINT FK required", "object_type VARCHAR(48) required", "object_id BIGINT required", "object_revision INT required", "object_checksum CHAR(64) required", "status VARCHAR(24) required", "requested_by_user_id BIGINT FK required", "requested_at DATETIME2 required", "completed_at DATETIME2 nullable")),
        ("platform.approval_action", "NEW", "append only decision", cols("approval_action_id BIGINT PK", "approval_instance_id BIGINT FK required", "step_order INT required", "actor_user_id BIGINT FK required", "decision VARCHAR(24) required", "reason NVARCHAR(2000) required", "evidence_json NVARCHAR(MAX) JSON nullable", "signed_at DATETIME2 required", "signature_hash CHAR(64) required")),
        ("platform.decision_record", "NEW", "human or delegated semantic decision", cols("decision_id BIGINT PK", "decision_code NVARCHAR(160) UQ required", "subject_type VARCHAR(48) required", "subject_code NVARCHAR(255) required", "decision_type VARCHAR(48) required", "decision_json NVARCHAR(MAX) JSON required", "basis NVARCHAR(1000) required", "confidence DECIMAL(5,4) nullable", "decided_by_user_id BIGINT FK nullable", "authority_basis NVARCHAR(255) required", "status VARCHAR(24) required", "decided_at DATETIME2 required", "supersedes_decision_id BIGINT self FK nullable")),
        ("platform.release", "EXTEND", "publication aggregate", cols("release_id BIGINT PK", "product_id BIGINT FK required", "release_version INT required", "site_contract_revision_id BIGINT FK NEW", "status VARCHAR(24) required", "published_at DATETIME2 nullable", "published_by_user_id BIGINT FK nullable", "previous_release_id BIGINT self FK nullable", "gate_report_json NVARCHAR(MAX) JSON NEW", "row_version ROWVERSION NEW", "UQ product + release_version")),
        ("platform.release_member", "NEW", "release to dataset snapshot membership", cols("release_member_id BIGINT PK", "release_id BIGINT FK required", "dataset_version_id BIGINT FK required", "dataset_snapshot_id BIGINT required", "row_count BIGINT required", "checksum CHAR(64) required", "UQ release + dataset_version")),
        ("platform.visualization_definition", "EXTEND", "chart presentation only", cols("visualization_id BIGINT PK", "product_id BIGINT FK required", "visualization_code NVARCHAR(120) required", "metric_id BIGINT FK required", "chart_type VARCHAR(32) required", "config_json NVARCHAR(MAX) JSON required", "accessibility_json NVARCHAR(MAX) JSON required NEW", "revision INT required NEW", "status VARCHAR(24) required", "row_version ROWVERSION NEW", "UQ product + code + revision NEW")),
        ("platform.table_definition", "NEW", "published tabular projection", cols("table_definition_id BIGINT PK", "product_id BIGINT FK required", "table_code NVARCHAR(120) required", "metric_id BIGINT FK nullable", "dataset_version_id BIGINT FK required", "columns_json NVARCHAR(MAX) JSON required", "filters_json NVARCHAR(MAX) JSON required", "sort_json NVARCHAR(MAX) JSON required", "pagination_json NVARCHAR(MAX) JSON required", "revision INT required", "status VARCHAR(24) required", "UQ product + code + revision")),
        ("platform.export_definition", "NEW", "export policy and shape", cols("export_definition_id BIGINT PK", "product_id BIGINT FK required", "export_code NVARCHAR(120) required", "source_projection_code NVARCHAR(120) required", "formats_json NVARCHAR(MAX) JSON required", "row_limit BIGINT required", "confidentiality_policy_id BIGINT FK required", "revision INT required", "status VARCHAR(24) required", "UQ product + code + revision")),
        ("platform.api_projection", "NEW", "public query contract", cols("api_projection_id BIGINT PK", "product_id BIGINT FK required", "projection_code NVARCHAR(120) required", "route_template NVARCHAR(500) required", "http_method VARCHAR(8) required", "dataset_version_id BIGINT FK required", "query_contract_json NVARCHAR(MAX) JSON required", "response_schema_json NVARCHAR(MAX) JSON required", "rate_limit_policy_code NVARCHAR(120) required", "revision INT required", "status VARCHAR(24) required", "UQ product + projection_code + revision", "UQ route + method + revision")),
        ("platform.outbox_event", "EXISTING", "reliable cross-plane notification", cols("event_id BIGINT PK", "aggregate_type VARCHAR(48) required", "aggregate_id BIGINT required", "event_type VARCHAR(64) required", "payload_json NVARCHAR(MAX) JSON required", "status VARCHAR(24) required", "attempts INT required", "available_at DATETIME2 required", "processed_at DATETIME2 nullable", "last_error NVARCHAR(2000) nullable", "created_at DATETIME2 required")),
        ("platform.schema_migration", "EXISTING", "migration checksum ledger", cols("migration_id NVARCHAR(255) PK", "checksum CHAR(64) required", "applied_at DATETIME2 required", "applied_by NVARCHAR(128) required")),
        ("platform.job_lease", "EXISTING", "single worker lease", cols("job_name NVARCHAR(128) PK", "lease_owner NVARCHAR(128) required", "lease_until DATETIME2 required", "updated_at DATETIME2 required")),
        ("audit.audit_event", "NEW", "tamper evident append only audit", cols("audit_event_id BIGINT PK", "occurred_at DATETIME2 required", "actor_user_id BIGINT nullable", "actor_type VARCHAR(24) required", "action_code NVARCHAR(120) required", "object_type VARCHAR(48) required", "object_id NVARCHAR(255) required", "object_revision INT nullable", "before_hash CHAR(64) nullable", "after_hash CHAR(64) nullable", "correlation_id UNIQUEIDENTIFIER required", "request_id UNIQUEIDENTIFIER required", "ip_hash CHAR(64) nullable", "details_json NVARCHAR(MAX) JSON required", "previous_event_hash CHAR(64) nullable", "event_hash CHAR(64) required")),
        ("iam.role", "NEW", "role registry", cols("role_id BIGINT PK", "role_code NVARCHAR(120) UQ required", "title_ka NVARCHAR(255) required", "title_en NVARCHAR(255) nullable", "system_role BIT required", "status VARCHAR(24) required")),
        ("iam.permission", "NEW", "atomic capability registry", cols("permission_id BIGINT PK", "permission_code NVARCHAR(160) UQ required", "resource_type VARCHAR(64) required", "action VARCHAR(32) required", "risk_level VARCHAR(24) required")),
        ("iam.role_permission", "NEW", "role permission grant", cols("role_id BIGINT FK required", "permission_id BIGINT FK required", "PK role_id + permission_id")),
        ("iam.user_role_scope", "NEW", "time bounded scoped role", cols("user_role_scope_id BIGINT PK", "user_id BIGINT FK required", "role_id BIGINT FK required", "product_id BIGINT FK nullable means global", "valid_from DATETIME2 required", "valid_to DATETIME2 nullable", "granted_by_user_id BIGINT FK required", "grant_reason NVARCHAR(1000) required", "status VARCHAR(24) required", "UQ user + role + product + valid_from")),
    ]
    add_table(doc, ["ცხრილი", "სტატუსი", "დანიშნულება", "სვეტები და შეზღუდვები"], panel_tables, [1.30, .55, 1.25, 3.30], 7.15)

    add_heading(doc, "Data Plane და Archive Plane ცხრილები", 1)
    data_tables = [
        ("ingest.batch", "ერთი import request", cols("batch_id PK", "contract_id", "product_id", "status", "checksum", "requested_by_user_id", "started_at", "finished_at", "created_at")),
        ("ingest.artifact", "უცვლელი uploaded object", cols("artifact_id PK", "batch_id FK", "original_name", "format", "object_uri", "checksum", "byte_size", "received_at", "quarantine_uri nullable")),
        ("ingest.dataset_load", "ერთი source to dataset load", cols("dataset_load_id PK", "batch_id FK", "dataset_version_id", "source_name", "source_row_count", "accepted_count", "rejected_count", "status")),
        ("ingest.staged_row", "lossless staging row", cols("staged_row_id PK", "dataset_load_id FK", "source_row_number", "source_key", "raw_payload_json", "payload_hash", "validation_status", "error_json")),
        ("ingest.validation_issue", "row level issue", cols("issue_id PK", "staged_row_id FK", "rule_code", "severity", "message", "details_json", "created_at")),
        ("ingest.load_checkpoint", "resumable cursor", cols("dataset_load_id PK", "last_source_row_number", "status", "updated_at")),
        ("publication.snapshot", "immutable product publication", cols("snapshot_id PK", "product_id", "release_id UQ", "status", "published_at", "previous_snapshot_id", "created_at")),
        ("publication.dataset_snapshot", "immutable loaded dataset", cols("dataset_snapshot_id PK", "dataset_load_id UQ FK", "dataset_version_id", "status", "row_count", "checksum", "created_at")),
        ("publication.snapshot_member", "snapshot membership", cols("snapshot_id + dataset_version_id PK", "dataset_snapshot_id", "row_count", "checksum")),
        ("raw.source_record", "immutable raw row representation", cols("source_record_id PK", "dataset_snapshot_id", "artifact_id FK", "source_row_number", "source_key", "payload_json", "payload_hash", "extracted_at", "valid_from", "valid_to")),
        ("entity.entity_record", "canonical nonstatistical entity", cols("entity_id PK", "dataset_snapshot_id", "external_key", "record_type", "title", "payload_json", "payload_hash", "source_record_id FK", "valid_from", "valid_to", "is_current")),
        ("entity.localized_text", "localized content", cols("entity_id + field_code + language_tag PK", "text_value", "source_record_id FK")),
        ("entity.resource_locator", "URI or file locator", cols("entity_id + locator_kind + language_tag PK", "locator", "source_record_id FK")),
        ("entity.entity_link", "typed entity relation", cols("link_id PK", "from_entity_id FK", "relationship_type_id", "to_entity_id FK", "ordinal", "valid_from", "valid_to", "source_record_id")),
        ("entity.entity_classification", "entity classifier assignment", cols("entity_id + attribute_id + item_id + valid_from PK", "valid_to", "source_record_id")),
        ("statistics.series", "metric and dimension signature", cols("series_id PK", "dataset_snapshot_id", "metric_id", "series_key_hash", "unit_code", "status", "source_record_id", "UQ snapshot + metric + signature")),
        ("statistics.observation", "typed statistical fact", cols("observation_id PK", "series_id FK", "period_start", "period_end", "observation_status", "numeric_value", "text_value", "boolean_value", "source_record_id FK", "valid_from", "valid_to", "is_current", "exactly one value column CHECK required")),
        ("statistics.observation_dimension", "dimension coordinate", cols("observation_id + dimension_id PK", "classification_item_id nullable", "scalar_code nullable", "exactly one representation CHECK required")),
        ("statistics.observation_attribute", "observation metadata", cols("observation_id + attribute_code PK", "value_json")),
        ("reference.classification_item_snapshot", "release frozen classifier", cols("snapshot_id + classification_item_id PK", "scheme_version_id", "code", "label_ka", "label_en", "parent_item_id", "status")),
        ("serving.metric_cache", "rebuildable aggregate cache", cols("cache_id PK", "snapshot_id FK", "metric_id", "dimension_signature", "period_start", "period_end", "aggregate_value", "refreshed_at", "UQ snapshot + metric + signature + period")),
        ("archive.snapshot", "retained publication archive", cols("archive_snapshot_id PK", "product_id", "original_snapshot_id", "archived_at", "purge_after", "checksum", "status", "UQ product + original_snapshot")),
        ("archive.record", "archived source record", cols("archive_record_id PK", "archive_snapshot_id FK", "dataset_version_id", "original_source_record_id", "record_kind", "source_key", "payload_json", "payload_hash", "UQ archive + original record")),
        ("archive.artifact_reference", "archived object reference", cols("archive_artifact_id PK", "archive_snapshot_id FK", "object_uri", "checksum", "original_name", "UQ archive + object_uri")),
    ]
    add_table(doc, ["ცხრილი", "grain", "სვეტები"], data_tables, [1.75, 1.55, 3.70], 7.35)
    add_para(doc, "მნიშვნელოვანი გაუმჯობესება  current schema-ზე უნდა დაემატოს CHECK constraints exactly one observation value, exactly one dimension representation, JSON validity, legal lifecycle values და nonnegative row counts. ეს არის additive migration და კონტრაქტებს არ არღვევს.")

    add_heading(doc, "Access პაკეტის სრული physical dictionary", 1)
    add_para(doc, "ეს არის revision 7-ის 22 physical table-ის ზუსტი ჩამონათვალი. ყველა ცხრილის სახელი არის namespace-prefix-ით: `__gs_*`, `__cl_*`, `__stat_*`, `__raw_*` ან domain-ის `kids_*`; prefix-ის გარეშე ახალი physical table აკრძალულია. Generator ქმნის primary/unique/secondary indexes-ს, 21 relation-ს და ავსებს contract metadata rows-ს (`__gs_package`, datasets, fields, keys, relations, structure index, classifiers, units); domain data table-ები ცარიელ template-ში მხოლოდ სტრუქტურით იქმნება და შემავსებელი ამატებს rows-ს. `__raw_document` locator-ია და არ შეიცავს raw JSON-ს.")
    access_tables = [
        ("__gs_package", "metadata", "package manifest singleton", cols("product_code TEXT", "contract_code TEXT", "contract_revision TEXT", "package_code TEXT PK", "package_version TEXT", "load_mode TEXT", "source_system TEXT", "artifact_profile TEXT")),
        ("__gs_dataset", "metadata", "declared dataset", cols("dataset_code TEXT PK", "access_table_name TEXT", "data_family TEXT", "business_grain TEXT")),
        ("__gs_structure_index", "metadata", "namespace and structure registry index", cols("structure_code TEXT PK", "namespace_code TEXT", "structure_kind TEXT", "data_class TEXT", "grain MEMO", "authority_mode TEXT", "lifecycle_policy TEXT", "lifecycle_status TEXT", "revision LONG", "checksum TEXT nullable")),
        ("__gs_field", "metadata", "ordered field declaration", cols("dataset_code TEXT PK part", "field_name TEXT PK part", "logical_type TEXT", "semantic_role TEXT", "required TEXT boolean lexical")),
        ("__gs_key", "metadata", "primary/unique/secondary index declaration", cols("dataset_code TEXT PK part", "field_name TEXT", "key_role TEXT PK part (PRIMARY/UNIQUE/declared index code)", "key_order TEXT PK part")),
        ("__gs_relation", "metadata", "relation declaration", cols("relationship_code TEXT PK", "from_dataset_code TEXT", "from_field TEXT", "to_dataset_code TEXT", "to_field TEXT", "cardinality TEXT", "required TEXT")),
        ("__gs_projection", "metadata", "entity relation statistical projection", cols("projection_code TEXT PK", "dataset_code TEXT", "projection_family TEXT", "mapping_json MEMO", "approval_state TEXT")),
        ("__cl_scheme", "reference", "classifier scheme", cols("scheme_code TEXT PK", "authority_mode TEXT", "title MEMO", "standard_reference TEXT")),
        ("__cl_version", "reference", "classifier version", cols("version_ref TEXT PK", "scheme_code TEXT FK", "version_code TEXT", "lifecycle_state TEXT", "valid_from TEXT", "valid_to TEXT nullable", "UX scheme_code + version_code")),
        ("__cl_item", "reference", "versioned classifier item", cols("item_ref TEXT PK", "version_ref TEXT FK", "item_code TEXT", "label_ka MEMO nullable", "label_en MEMO nullable", "lifecycle_state TEXT", "UX version_ref + item_code")),
        ("__cl_alias", "reference", "source value alias", cols("alias_ref TEXT PK", "item_ref TEXT FK", "source_system TEXT", "source_code_raw MEMO", "lookup_code_normalized MEMO", "normalization_rule TEXT", "lifecycle_state TEXT")),
        ("__cl_hierarchy", "reference", "classifier tree edge", cols("version_ref TEXT FK", "parent_item_ref TEXT FK nullable", "child_item_ref TEXT PK FK", "ordinal LONG")),
        ("__stat_unit", "reference", "governed unit", cols("unit_code TEXT PK", "quantity_kind TEXT", "scale_factor DOUBLE", "denominator_text MEMO nullable", "title MEMO")),
        ("__stat_metric", "reference", "one governed metric per carrier resource", cols("metric_code TEXT PK", "source_resource_id TEXT FK", "title_ka MEMO nullable", "title_en MEMO nullable", "measure_component_code TEXT", "unit_code TEXT FK", "aggregation TEXT", "lifecycle_state TEXT", "inference_confidence DOUBLE", "inference_rationale MEMO", "UX source_resource_id")),
        ("__raw_document", "raw locator", "one immutable source-artifact envelope and locator", cols("source_row_key TEXT PK", "source_table TEXT", "source_primary_key TEXT", "extract_sequence LONG", "source_system TEXT", "source_feed TEXT nullable", "document_identity TEXT", "original_filename MEMO nullable", "mime_type TEXT nullable", "byte_size LONG nullable", "checksum TEXT", "received_at DATETIME", "source_uri MEMO nullable", "ingestion_batch TEXT", "provenance MEMO", "retention_policy TEXT", "confidentiality_class TEXT", "payload_reference MEMO", "encryption_reference TEXT nullable", "parser_status TEXT", "validation_status TEXT", "supersedes_source_row_key TEXT nullable", "UX source_table + source_primary_key")),
        ("__ent_kids_goal", "entity", "one KIDS goal", cols("source_goal_id TEXT PK", "category_item_ref TEXT FK", "title_ka MEMO nullable", "title_en MEMO nullable", "path_ka MEMO nullable", "path_en MEMO nullable", "source_row_key TEXT FK", "operation TEXT")),
        ("__ent_kids_resource", "entity", "one KIDS resource", cols("source_resource_id TEXT PK", "category_item_ref TEXT FK", "title_ka MEMO nullable", "title_en MEMO nullable", "path_ka MEMO nullable", "path_en MEMO nullable", "source_row_key TEXT FK", "operation TEXT")),
        ("__rel_kids_resource_subcategory_assignment", "relation", "one source subcategory token", cols("assignment_key TEXT PK", "source_resource_id TEXT FK", "subcategory_item_ref TEXT FK", "source_token_raw MEMO", "ordinal LONG", "source_row_key TEXT FK", "operation TEXT", "UX resource + ordinal")),
        ("__ent_kids_glossary_entry", "entity", "one independent glossary entry", cols("source_glossary_id TEXT PK", "language_item_ref TEXT FK", "language_raw MEMO", "entry_text MEMO", "source_row_key TEXT FK", "operation TEXT")),
        ("__raw_kids_statistical_carrier", "raw locator", "one parseable chart carrier per resource", cols("carrier_code TEXT PK", "source_resource_id TEXT FK", "payload_checksum TEXT", "parse_status TEXT", "source_row_key TEXT FK", "operation TEXT", "UX source_resource_id", "payload_raw forbidden")),
        ("__stat_kids_statistical_input", "statistical input", "one parsed source cell", cols("input_key TEXT PK", "carrier_code TEXT FK", "cell_ordinal LONG", "period_raw MEMO", "period_normalized TEXT nullable", "dimension_key_raw MEMO", "age_group_item_ref TEXT FK", "value_lexical MEMO", "value_decimal DOUBLE nullable", "json_path MEMO", "source_encoding TEXT", "source_row_key TEXT FK", "operation TEXT", "UX carrier + cell_ordinal")),
        ("__stat_kids_statistical_semantic_binding", "relation", "one complete semantic binding per carrier", cols("carrier_code TEXT PK FK", "metric_code TEXT FK", "unit_code TEXT FK", "aggregation TEXT", "obs_status TEXT", "conf_status TEXT", "quality_policy_code TEXT", "confidentiality_policy_code TEXT", "inference_method TEXT", "operation TEXT")),
    ]
    add_table(doc, ["ცხრილი", "ოჯახი", "grain", "ყველა სვეტი და key"], access_tables, [1.75, .85, 1.60, 2.80], 7.15)

    add_heading(doc, "Access physical relations", 2)
    rels = [
        ("FK_cl_version_scheme", "__cl_version.scheme_code", "__cl_scheme.scheme_code", "many to one"),
        ("FK_cl_item_version", "__cl_item.version_ref", "__cl_version.version_ref", "many to one"),
        ("FK_cl_alias_item", "__cl_alias.item_ref", "__cl_item.item_ref", "many to one"),
        ("FK_goal_category", "__ent_kids_goal.category_item_ref", "__cl_item.item_ref", "many to one"),
        ("FK_resource_category", "__ent_kids_resource.category_item_ref", "__cl_item.item_ref", "many to one"),
        ("FK_sub_resource", "assignment.source_resource_id", "__ent_kids_resource.source_resource_id", "many to one"),
        ("FK_sub_item", "assignment.subcategory_item_ref", "__cl_item.item_ref", "many to one"),
        ("FK_glossary_language", "glossary.language_item_ref", "__cl_item.item_ref", "many to one"),
        ("FK_carrier_resource", "carrier.source_resource_id", "__ent_kids_resource.source_resource_id", "one to one"),
        ("FK_input_carrier", "input.carrier_code", "carrier.carrier_code", "many to one"),
        ("FK_metric_resource", "metric.source_resource_id", "__ent_kids_resource.source_resource_id", "one to one"),
        ("FK_metric_unit", "metric.unit_code", "__stat_unit.unit_code", "many to one"),
        ("FK_semantic_carrier", "binding.carrier_code", "carrier.carrier_code", "one to one"),
        ("FK_semantic_metric", "binding.metric_code", "__stat_metric.metric_code", "many to one"),
        ("FK_semantic_unit", "binding.unit_code", "__stat_unit.unit_code", "many to one"),
        ("FK_goal_raw", "__ent_kids_goal.source_row_key", "__raw_document.source_row_key", "many to one"),
        ("FK_resource_raw", "__ent_kids_resource.source_row_key", "__raw_document.source_row_key", "many to one"),
        ("FK_sub_raw", "assignment.source_row_key", "__raw_document.source_row_key", "many to one"),
        ("FK_glossary_raw", "glossary.source_row_key", "__raw_document.source_row_key", "many to one"),
        ("FK_carrier_raw", "carrier.source_row_key", "__raw_document.source_row_key", "many to one"),
        ("FK_input_raw", "input.source_row_key", "__raw_document.source_row_key", "many to one"),
    ]
    add_table(doc, ["relation", "child field", "parent field", "cardinality"], rels, [1.65, 2.3, 2.3, 1.05], 7.8)

    add_heading(doc, "ფიზიკური ცხრილების დანიშნულება, საზღვრები და UI-მომზადების კონტრაქტი", 1)
    add_para(doc, "ეს თავი არის ნორმატიული. ცხრილის სახელი თავისთავად საკმარისი არ არის: თითოეულ ფიზიკურ ცხრილს აქვს ზუსტი grain, პასუხისმგებლობა, authoritative სტატუსი, დაშვებული/აკრძალული შინაარსი, lifecycle, lineage, quality და confidentiality policy. UI არ იგონებს ამ წესებს; UI კონფიგურირდება მხოლოდ ამ contract metadata-დან.")
    add_para(doc, "პანელის control database-ში აღწერილი ცხრილები არის contract registry-ის ფიზიკური ჩანაწერები. approved revision-ის შემდეგ generator ამ ჩანაწერებს მატერიალიზებს Access-ის ფიზიკურ ცხრილებად. Access-ში ფიზიკური ცხრილი ვერ იარსებებს, თუ იგი contract registry-ში არ არის გამოცხადებული, versioned და approved.")
    table_contract_fields = [
        ("table_id / structure_id", "უნიკალური identity", "MUST; immutable identity"),
        ("namespace_code", "GS, CL, STAT, RAW, ENT, REF, SERV, ARCH, AUDIT, SYS ან domain", "MUST; controlled vocabulary"),
        ("structure_kind", "CONTRACT, CLASSIFICATION, STATISTICAL_DSD, OBSERVATION, ENTITY, EVENT, DOCUMENT, RAW_RECORD, PROJECTION და სხვ.", "MUST; versioned vocabulary"),
        ("grain", "ერთი row რას წარმოადგენს", "MUST; natural key-სთან თანხვედრაში"),
        ("authority_mode", "AUTHORITATIVE, DERIVED, RAW_EVIDENCE, CACHE", "MUST; determines write authority"),
        ("allowed_content / forbidden_content", "დაშვებული და აკრძალული მნიშვნელობები", "MUST; validation and import gate"),
        ("lifecycle_policy", "append-only, mutable, immutable, rebuildable, archival", "MUST"),
        ("lineage_policy", "რომელი source/artifact/row უნდა მიებას", "MUST for source-derived rows"),
        ("quality_policy / confidentiality_policy", "ხარისხი და disclosure control", "MUST for published data"),
        ("ui_capabilities", "view, create, edit, approve, import, export, publish", "MUST; generated UI configuration"),
        ("contract_revision / schema_hash", "რომელ immutable revision-ს ეკუთვნის", "MUST; prevents drift"),
    ]
    add_table(doc, ["კონტრაქტული ველი", "მნიშვნელობა", "წესი"], table_contract_fields, [1.65, 3.85, 1.55], 7.05)
    boundary_matrix = [
        ("__gs_*", "კონტრაქტი, schema, keys, relations, package და approvals", "business rows, raw payload, სტატისტიკური observation", "authoritative, versioned", "contract editor; approve/review; no direct data entry"),
        ("__cl_*", "კონტროლირებადი კოდები, ვერსიები, aliases, hierarchy", "თავისუფალი ტექსტი, დაუდასტურებელი raw value", "authoritative reference", "browse/search; propose; approve; retire"),
        ("__stat_*", "DSD, dimensions, measures, metrics, units, observations", "document payload, entity profile, parser trace", "authoritative statistical contract/fact", "filter, dimension select, chart/table config, export"),
        ("__raw_*", "წყაროს უცვლელი artifact, record, locator და provenance", "გაწმენდილი business value, public serving row", "RAW_EVIDENCE append-only", "read-only evidence; import/reprocess; no manual edit"),
        ("__ent_* / kids_*", "canonical entity, domain content, typed relationships", "raw file bytes, metric semantics unless explicitly linked", "authoritative domain or derived by declared mapping", "form/grid/detail; controlled edit; approval if required"),
        ("__ref_*", "არასტატისტიკური reference data", "uncontrolled labels and arbitrary source payload", "versioned reference", "lookup/select/filter; controlled maintenance"),
        ("__serv_*", "published read model/cache", "source-of-truth edits", "DERIVED/REBUILDABLE", "read-only public/admin view; refresh/rebuild"),
        ("__arch_*", "retained historical release and artifact references", "current mutable state", "immutable archive", "read-only historical browsing/export"),
        ("__audit_* / __sys_*", "audit, jobs, leases, checksums, errors", "domain facts or user content", "append-only/system", "read-only audit/operations"),
    ]
    add_table(doc, ["namespace", "ინახავს", "არ ინახავს", "authority/lifecycle", "UI capability"], boundary_matrix, [1.05, 2.25, 2.15, 1.25, 1.6], 8.3)
    add_heading(doc, "ყველა გამოცხადებული ცხრილის ინდივიდუალური კონტრაქტები", 2)
    add_para(doc, "ზემოთ მოცემული namespace matrix-ის გარდა, ქვემოთ თითოეული გამოცხადებული physical/logical table იღებს ინდივიდუალურ კონტრაქტს. ეს არის generator-ის, importer-ის, backend-ისა და UI metadata compiler-ის ერთიანი წყარო. თუ ახალი table დაემატა, ამ ცხრილშიც უნდა დაემატოს მისი contract row იმავე migration-ში.")

    def table_contract_row(name, family, grain):
        n = name.lower()
        if name.startswith("__gs_") or name.startswith("platform."):
            return (name, family, grain, "contract/schema/governance metadata", "business rows, raw payload, final observations", "AUTHORITATIVE; versioned/immutable revision", "admin metadata; review/approve/diff")
        if name.startswith("__cl_") or name.startswith("platform.classification"):
            return (name, family, grain, "controlled vocabulary/classification data", "unresolved free text or source artifact", "AUTHORITATIVE; versioned reference", "lookup, propose, approve, retire")
        if name.startswith("__stat_") or name.startswith("statistics.") or "statistical" in n:
            return (name, family, grain, "statistical structure, semantics or observation", "raw documents and unrelated domain entities", "AUTHORITATIVE or declared DERIVED; revisioned", "dimension/filter/chart/table/export")
        if name.startswith("__raw_") or name.startswith("raw.") or "artifact" in n or "staged" in n:
            return (name, family, grain, "immutable source evidence, staging or provenance", "clean business rows or public projection", "RAW_EVIDENCE/append-only; reprocess by new batch", "read-only evidence, import, reprocess")
        if name.startswith("__audit") or name.startswith("audit.") or name.startswith("__sys") or name.startswith("platform.job") or name.startswith("platform.outbox"):
            return (name, family, grain, "audit, job, lease, checksum or runtime control", "business content and source facts", "SYSTEM; append-only or controlled runtime", "operations/audit read-only")
        if name.startswith("serving.") or name.startswith("__serv_") or name.startswith("archive.") or name.startswith("__arch_"):
            return (name, family, grain, "published projection/cache or retained history", "authoritative edits and new source truth", "DERIVED rebuildable or IMMUTABLE archive", "read-only browse/export/rebuild")
        if name.startswith("entity.") or name.startswith("__ent_") or name.startswith("kids_"):
            return (name, family, grain, "canonical/domain entity, content or typed relation", "raw bytes and unbound statistical semantics", "DOMAIN authoritative or declared DERIVED", "grid/detail/form; controlled edit")
        if name.startswith("reference.") or name.startswith("__ref_"):
            return (name, family, grain, "release-frozen non-statistical reference", "uncontrolled labels and raw payload", "VERSIONED reference", "lookup/select/filter")
        return (name, family, grain, "declared contract structure", "undeclared or unrelated data", "as declared by contract registry", "capabilities from contract metadata")

    boundary_rows = []
    for name, status, purpose, column_text in panel_tables:
        boundary_rows.append(table_contract_row(name, "Control Plane", purpose))
    for name, grain, column_text in data_tables:
        boundary_rows.append(table_contract_row(name, "Data/Archive Plane", grain))
    for name, family, grain, column_text in access_tables:
        boundary_rows.append(table_contract_row(name, f"Access {family}", grain))
    add_table(doc, ["table", "plane", "grain/purpose", "ინახავს", "არ ინახავს", "authority/lifecycle", "UI/backend use"], boundary_rows, [1.35, .85, 1.35, 1.45, 1.35, 1.25, 1.25], 6.7)

    add_heading(doc, "`__raw_document`-ის ნორმატიული კონტრაქტი", 2)
    add_para(doc, "`__raw_document` არის უცვლელი წყარო-არტეფაქტის envelope/locator და არა უნივერსალური raw business table. ერთი row წარმოადგენს ერთ მიღებულ source artifact-ს ან მის canonical locator-ს. იგი ინახავს source system-ს, source feed-ს, file/document identity-ს, original name-ს, MIME type-ს, byte size-ს, checksum-ს, received time-ს, source URI/path-ს, ingestion batch-ს, provenance-ს, retention-ს, confidentiality-ს, storage/payload reference-ს, encryption reference-ს, parser/validation status-ს და supersession metadata-ს.")
    add_para(doc, "`__raw_document`-ში MUST NOT ჩაიწეროს გაწმენდილი `kids_*` entity, საბოლოო `__stat_*` observation, metric/unit/aggregation-ის დამტკიცებული მნიშვნელობა, classifier item, serving projection ან ხელით შეცვლილი ბიზნეს-ლოგიკა. Original payload ინახება immutable object storage-ში ან სხვა კონტრაქტულ artifact store-ში; Access-ში ინახება მხოლოდ locator/identity/lineage envelope. Structured source-ისთვის გამოიყენება `__raw_source`, `__raw_document_part`, `__raw_record` და საჭიროებისას `__raw_record_field`.")
    add_heading(doc, "როგორ კონფიგურირდება UI contract metadata-ით", 2)
    ui_contract_rows = [
        ("list/grid", "dataset/table definition + field capabilities", "visible fields, labels, sort/filter, pagination, export"),
        ("detail/form", "field definition + relation + classifier binding", "control type, requiredness, options, help, validation, read/write"),
        ("statistical explorer", "DSD + dimension + measure + policy", "dimension selectors, period, unit, aggregation, chart/table mode"),
        ("approval screen", "approval policy/step/decision", "review evidence, diff, approve/reject, reason, four-eyes"),
        ("raw evidence viewer", "raw artifact + lineage", "read-only source, checksum, locator, parsed rows, reprocess"),
        ("relations view", "key/relation definitions", "linked records, cardinality, orphan/error state"),
        ("package/import view", "job, artifact, gate and issue metadata", "progress, errors, retry, checksum, download/revoke"),
    ]
    add_table(doc, ["UI surface", "authoritative metadata", "ავტომატურად განისაზღვრება"], ui_contract_rows, [1.4, 2.45, 3.25], 7.1)
    add_heading(doc, "ახალი ფიზიკური ცხრილის დამატების სავალდებულო migration sequence", 2)
    numbered(doc, [
        "შეიქმნას ახალი migration, რომელიც ამატებს structure/table/field metadata-ს `__gs_*` registry-ში; Access-ში პირდაპირი CREATE TABLE დაუშვებელია.",
        "დარეგისტრირდეს namespace, structure_kind, grain, authority_mode, lifecycle, allowed/forbidden content და UI capabilities.",
        "დარეგისტრირდეს ყველა field, type, nullability, default, ordinal, key, index და relation.",
        "დაემატოს lineage, quality, confidentiality, retention და mapping policy.",
        "დაემატოს contract revision, schema diff და compatibility classification.",
        "გაიაროს completeness, key, relation, classifier, statistical, privacy და security gates.",
        "მოხდეს approval და immutable revision-ის გამოცემა.",
        "Access generator-მა ამ revision-იდან შექმნას შესაბამისი physical table, metadata და constraints.",
        "import engine-მა შეავსოს მხოლოდ declared mapping-ით; ყოველი row მიებას source/lineage-ს.",
        "გამოქვეყნება მოხდეს მხოლოდ ყველა blocking gate-ის გავლის შემდეგ; generated UI configuration იკითხოს იგივე contract metadata-დან.",
    ])
    add_para(doc, "ამ sequence-ის შედეგად UI ვერ შექმნის ან შეცვლის კონტრაქტს. UI მხოლოდ გამოაჩენს და შეასრულებს უკვე approved contract-ს: fields, forms, relations, classifiers, validation, approval, import და publication behavior კონტრაქტიდან მოდის.")

    add_heading(doc, "KIDS სტატისტიკური სემანტიკის სრული გადაწყვეტილება", 1)
    add_figure(doc, diagrams["stat"], "ნახაზი 4  structure meaning fact presentation გამიჯვნა")
    metrics = [
        (162,"KIDS_ABORTIONS_COUNT","COUNT_EVENT",.99),(163,"KIDS_MALIGNANT_NEOPLASM_NEW_CASES","COUNT_CASE",.99),(164,"KIDS_SEXUALLY_TRANSMITTED_DISEASE_NEW_CASES","COUNT_CASE",.99),(165,"KIDS_DIABETES_MELLITUS_NEW_CASES","COUNT_CASE",.99),
        (166,"KIDS_RESPIRATORY_TUBERCULOSIS_CASES","COUNT_CASE",.98),(167,"KIDS_TUBERCULOUS_MENINGITIS_CASES","COUNT_CASE",.98),(168,"KIDS_MUSCULOSKELETAL_TUBERCULOSIS_CASES","COUNT_CASE",.98),(169,"KIDS_INFECTIOUS_PARASITIC_DISEASE_NEW_CASES","COUNT_CASE",.99),
        (170,"KIDS_BLOOD_DISEASE_NEW_CASES","COUNT_CASE",.99),(171,"KIDS_ENDOCRINE_METABOLIC_DISEASE_NEW_CASES","COUNT_CASE",.98),(172,"KIDS_MENTAL_BEHAVIOURAL_DISORDER_NEW_CASES","COUNT_CASE",.99),(173,"KIDS_NERVOUS_SYSTEM_DISEASE_NEW_CASES","COUNT_CASE",.99),
        (174,"KIDS_EYE_ADNEXA_DISEASE_NEW_CASES","COUNT_CASE",.99),(175,"KIDS_EAR_MASTOID_DISEASE_NEW_CASES","COUNT_CASE",.99),(176,"KIDS_CIRCULATORY_DISEASE_NEW_CASES","COUNT_CASE",.99),(177,"KIDS_RESPIRATORY_DISEASE_NEW_CASES","COUNT_CASE",.99),
        (178,"KIDS_DIGESTIVE_DISEASE_NEW_CASES","COUNT_CASE",.99),(179,"KIDS_SKIN_SUBCUTANEOUS_DISEASE_NEW_CASES","COUNT_CASE",.99),(180,"KIDS_MUSCULOSKELETAL_DISEASE_NEW_CASES","COUNT_CASE",.99),(181,"KIDS_UROGENITAL_DISEASE_NEW_CASES","COUNT_CASE",.99),(183,"KIDS_CONGENITAL_MALFORMATION_NEW_CASES","COUNT_CASE",.99),
        (297,"KIDS_DISABILITY_SOCIAL_PACKAGE_RECIPIENTS","PERSON",.98),(299,"KIDS_SURVIVOR_SOCIAL_PACKAGE_RECIPIENTS","PERSON",.95),(301,"KIDS_SUBSISTENCE_ALLOWANCE_BENEFICIARIES","PERSON",.99),(303,"KIDS_PLANNED_OUTPATIENT_REGISTERED_PERSONS","PERSON",.99),(305,"KIDS_CHRONIC_MEDICINE_PROGRAM_BENEFICIARIES","PERSON",.99),(306,"KIDS_ADOPTED_CHILDREN","PERSON",.99),(308,"KIDS_CHILDREN_GRANTED_ADOPTION_STATUS","PERSON",.96),(310,"KIDS_INTERNALLY_DISPLACED_PERSONS","PERSON",.99),
        (373,"KIDS_LABOUR_FORCE_POPULATION","THOUSAND_PERSONS",.85),(385,"KIDS_RESIDENT_VISITORS_MONTHLY_AVERAGE","THOUSAND_PERSONS",.99),(386,"KIDS_DOMESTIC_VISITS_MONTHLY_AVERAGE","THOUSAND_VISITS",.99),(387,"KIDS_AVERAGE_NIGHTS_PER_VISIT","AVERAGE_NIGHTS",.99),
        (404,"KIDS_MATERNAL_MORTALITY_RATIO","PER_100000_LIVE_BIRTHS",.99),(407,"KIDS_HIV_INCIDENCE_RATE","PER_1000_UNINFECTED_POPULATION",.99),(409,"KIDS_ANAEMIA_PREVALENCE","PERCENT",.95),(417,"KIDS_SUICIDES_COUNT","COUNT_EVENT",.99),(418,"KIDS_SUICIDE_MORTALITY_RATE","PER_100000_POPULATION",.97),
        (438,"KIDS_DAILY_INTERNET_USE_SHARE","PERCENT",.99),(439,"KIDS_RECENT_COMPUTER_USE_SHARE","PERCENT",.99),(440,"KIDS_MOBILE_PHONE_USE_SHARE","PERCENT",.99),(451,"KIDS_LITERACY_RATE","PERCENT",.99),(452,"KIDS_EARLY_EDUCATION_LEAVERS_RATE","PERCENT",.97),
    ]
    add_table(doc, ["resource", "metric code", "unit", "aggregation", "confidence"], [(a,b,c,"NONE",f"{d:.2f}") for a,b,c,d in metrics], [.65, 3.65, 1.75, .7, .7], 7.35)
    add_heading(doc, "Unit registry", 2)
    add_table(doc, ["unit", "quantity", "scale", "denominator", "validation"], [
        ("COUNT_EVENT","EVENT","1","none","finite nonnegative integer expected"),
        ("COUNT_CASE","CASE","1","none","finite nonnegative integer expected"),
        ("PERSON","PERSON","1","none","finite nonnegative integer expected"),
        ("THOUSAND_PERSONS","PERSON","1000","none","finite nonnegative decimal"),
        ("THOUSAND_VISITS","VISIT","1000","none","finite nonnegative decimal"),
        ("AVERAGE_NIGHTS","NIGHT","1","per visit","finite nonnegative decimal"),
        ("PER_100000_LIVE_BIRTHS","RATIO","1","100000 live births","finite nonnegative decimal"),
        ("PER_1000_UNINFECTED_POPULATION","RATIO","1","1000 uninfected population","finite nonnegative decimal"),
        ("PER_100000_POPULATION","RATIO","1","100000 population","finite nonnegative decimal"),
        ("PERCENT","RATIO","0.01","100 population","0 through 100 inclusive"),
    ], [1.95, 1.0, .55, 1.65, 2.15], 7.7)
    add_para(doc, "Aggregation ყველა 43 metric-ზე არის NONE. მიზეზი არა ტექნიკური შეზღუდვა, არამედ overlap-იანი age bands-ია: 0–17, 15–24, 15–29 და სხვები ერთმანეთის დამატებით double count-ს იწვევენ. SUM ან AVG დასაშვები გახდება მხოლოდ ახალი DSD revision-ით, ურთიერთგამომრიცხავი dimension values-ით ან steward-approved formula-ით.")
    add_heading(doc, "DSD revision 2", 2)
    add_table(doc, ["order", "component", "role", "attachment", "source or binding", "rule"], [
        (1,"TIME_PERIOD","DIMENSION","OBSERVATION","period_raw and period_normalized","four digit year required"),
        (2,"AGE_GROUP","DIMENSION","OBSERVATION","dimension_key_raw and age_group_item_ref","published alias required"),
        (3,"OBS_VALUE","PRIMARY_MEASURE","OBSERVATION","value_lexical and value_decimal","numeric finite and unit range"),
        (4,"UNIT_MULT","ATTRIBUTE","SERIES","unit scale_factor","derived from unit registry"),
        (5,"UNIT_MEASURE","ATTRIBUTE","SERIES","unit_code","binding required"),
        (6,"DECIMALS","ATTRIBUTE","SERIES","measure precision","contract controlled"),
        (7,"AGG_METHOD","ATTRIBUTE","SERIES","aggregation","NONE"),
        (8,"OBS_STATUS","ATTRIBUTE","OBSERVATION","obs_status","A default"),
        (9,"CONF_STATUS","ATTRIBUTE","OBSERVATION","conf_status","F default"),
        (10,"CARRIER_ID","ATTRIBUTE","SERIES","carrier_code","lineage required"),
        (11,"CELL_KEY","ATTRIBUTE","OBSERVATION","input_key","unique"),
        (12,"SOURCE_PATH","ATTRIBUTE","OBSERVATION","json_path","replay evidence"),
    ], [.5, 1.15, 1.2, 1.0, 2.15, 1.3], 7.3)
    add_heading(doc, "Quality და confidentiality", 2)
    add_table(doc, ["პოლიტიკა", "სავალდებულო წესები", "შედეგი"], [
        ("KIDS_AGGREGATE_QUALITY_V1", "period required and YYYY; published age alias; numeric finite minimum zero; percent 0–100; integer expected for count; unique carrier × period × age; source lineage; cross-age aggregation forbidden", "rule failure rejects cell or blocks release according to severity; no silent coercion"),
        ("KIDS_PUBLIC_AGGREGATE_V1", "PUBLIC_AGGREGATE; no microdata; no direct identifier; preserve source publication; smallCountThreshold 5; action FLAG_FOR_REVIEW_NO_AUTOMATIC_REWRITE; default CONF_STATUS F", "low counts are flagged, never silently suppressed or changed; privacy officer decides any new policy revision"),
    ], [1.85, 4.0, 1.45], 7.8)

    add_heading(doc, "კლასიფიკატორების სრული მოდელი", 1)
    add_table(doc, ["scheme", "source values", "panel policy", "publication condition"], [
        ("KIDS_GOAL_CATEGORY", "1 through 17 from goals_titles", "versioned source scheme; optional steward crosswalk to official SDG codelist; original labels retained", "all 36 goal and 225 resource category aliases resolve"),
        ("LANGUAGE", "ka, en, ena", "ka and en map to BCP 47 language items; ena remains explicit proposal until decision; no silent correction", "all glossary rows resolve; ena decision recorded"),
        ("KIDS_RESOURCE_SUBCATEGORY", "1, 2, 3, 4 encoded singly or comma separated", "split trim preserve raw token; items may display code fallback but invented semantic label is forbidden", "authoritative labels or explicit source-code-only approval"),
        ("AGE_GROUP", "0-5, 0-12, 0-17, 3-17, 13-17, 15-17, 15-24, 15-29", "trim alias lookup only; raw key retained; range inclusivity recorded in item definition", "every statistical cell resolves to exact published version"),
    ], [1.65, 1.75, 2.7, 1.2], 7.6)
    add_heading(doc, "Unknown value algorithm", 2)
    numbered(doc, [
        "Store source_value_raw unchanged and compute normalized lookup key only with the scheme-approved normalization rule.",
        "Resolve exact alias within source system and validity interval; fuzzy match can only suggest, never auto-approve.",
        "If unresolved, create one deduplicated OPEN proposal with source context and candidate confidence.",
        "Required field remains unresolved and blocks semantic publication; raw ingestion may continue if raw_ingest_enabled is true.",
        "Steward accepts existing item, creates a new item in a new draft version, or rejects with a reason.",
        "Decision creates an immutable alias and decision record; revalidation resumes from checkpoint.",
    ])

    add_heading(doc, "Access template generation contract", 1)
    add_figure(doc, diagrams["generation"], "ნახაზი 5  exact revision generation validation publication")
    add_heading(doc, "Generator preconditions", 2)
    bullets(doc, [
        "site_contract_revision.status = APPROVED და approval instance complete; checksum ხელახლა ემთხვევა canonical serialization-ს.",
        "ყველა referenced dataset version, classifier version, DSD, metric, unit, quality და confidentiality policy არის APPROVED ან PUBLISHED.",
        "relation graph acyclic-ია load_order-ის მიხედვით; ყველა field type map-დება Access type-ზე lossless წესით.",
        "არც ერთი table/field/relation/access_table_name არ არის reserved, დუბლირებული ან Access-ის ლიმიტებთან შეუთავსებელი.",
        "generation request შეიცავს idempotency key-ს და caller-ს აქვს PACKAGE_GENERATE კონკრეტულ product scope-ზე.",
    ])
    add_heading(doc, "Deterministic generation algorithm", 2)
    numbered(doc, [
        "BEGIN repeatable-read transaction; load exact approved revision and all child registries; canonical-sort by dataset load_order, field ordinal, relation code and classifier code.",
        "Serialize canonical manifest using UTF-8, stable property order and no insignificant whitespace; compute SHA-256; compare stored contract_checksum.",
        "Create a new temporary ACCDB. Never overwrite an existing target. Create six __gs metadata tables first.",
        "Create classifier and statistical reference tables, then raw locator, entity, relation and statistical input tables in declared load order.",
        "Create physical primary keys and unique indexes; then create all declared relationships with referential integrity.",
        "Populate only governed metadata/reference snapshots. For an empty template, leave source-derived entity, relation, locator and observation rows empty.",
        "Run independent reader validation: exact tables, columns, types, PK/UX/FK, metadata counts, checksum, no forbidden field, zero unexpected data rows.",
        "Close file, compute artifact SHA-256, sign metadata, upload immutable object, create package_artifact and mark job READY in one application transaction with outbox event.",
        "On any error close handles, retain diagnostic log, mark FAILED and never expose the partial file as downloadable.",
    ])
    add_heading(doc, "Access type mapping", 2)
    add_table(doc, ["logical type", "Access type", "validation", "SQL canonical target"], [
        ("CODE", "TEXT", "length and allowed pattern", "NVARCHAR bounded"),
        ("TEXT", "MEMO", "Unicode and max business length", "NVARCHAR MAX"),
        ("INTEGER", "LONG", "32-bit range in package", "BIGINT or INT per contract"),
        ("DECIMAL", "DOUBLE in r7", "lexical value also retained; no financial use", "DECIMAL 28 10"),
        ("BOOLEAN", "YESNO", "true false lexical accepted", "BIT"),
        ("DATE", "TEXT in r7 reference metadata", "ISO 8601 exact", "DATE or DATETIME2"),
        ("URI", "MEMO", "absolute or product-relative URI policy", "NVARCHAR 2048"),
        ("JSON", "MEMO", "JSON Schema validation", "NVARCHAR MAX with ISJSON CHECK"),
    ], [1.1, 1.2, 2.9, 2.1], 8)
    add_heading(doc, "Forbidden content", 2)
    bullets(doc, [
        "password, token, connection string, secret value ან target environment hostname",
        "arbitrary SQL, DDL, stored procedure name ან executable script",
        "database surrogate IDs; მხოლოდ stable codes და external keys",
        "publication instruction, role grant ან approval assertion generated outside panel",
        "chart-owned numeric arrays ან carrier.payload_raw duplicate",
        "undeclared table, field, relationship, formula ან classifier item",
    ])

    add_heading(doc, "Import execution specification", 1)
    add_heading(doc, "Request and idempotency", 2)
    code_block(doc, "POST /admin/api/v1/imports\nIdempotency-Key: <uuid>\nContent-Type: multipart/form-data\nartifact=<accdb>\ncontractCode=KIDS_PORTAL_V1\ncontractRevision=7\nexpectedArtifactChecksum=<sha256>")
    add_para(doc, "იგივე actor + endpoint + idempotency key + request hash აბრუნებს იმავე batch-ს. იგივე key განსხვავებული request hash-ით არის 409 IDEMPOTENCY_KEY_REUSED. checksum mismatch არის 422 ARTIFACT_CHECKSUM_MISMATCH.")
    add_heading(doc, "Execution stages", 2)
    stages = [
        (1,"Receive","stream to quarantine object; size limit; malware scan; SHA-256; never trust filename","artifact registered"),
        (2,"Catalog","read tables columns indexes relations without business writes","catalog snapshot"),
        (3,"Contract resolve","resolve code + revision + checksum; historical revision accepted only against its own bindings","resolved immutable contract"),
        (4,"Structural validate","exact table and field set, type, order, required, PK UX FK","gate report G01 G03"),
        (5,"Stage","load chunks in declared order; raw lexical values retained; checkpoint after committed chunk","staged rows"),
        (6,"Semantic validate","classifiers, metric unit DSD aggregation quality confidentiality lineage","issues and proposals"),
        (7,"Materialize","write canonical entities relations series observations in one dataset snapshot","prepared dataset snapshots"),
        (8,"Release prepare","freeze classifier snapshots; calculate counts and checksums; run all gates","READY release"),
        (9,"Publish","atomic active snapshot pointer switch and outbox event","PUBLISHED"),
        (10,"Archive","retention copy of superseded snapshot and artifact reference","reproducible rollback"),
    ]
    add_table(doc, ["#", "ეტაპი", "ზუსტი მოქმედება", "output"], stages, [.35, 1.05, 4.6, 1.3], 7.8)
    add_heading(doc, "Failure semantics", 2)
    add_table(doc, ["კლასი", "HTTP or job result", "retry", "მოქმედება"], [
        ("STRUCTURAL", "422 FAILED", "არა იმავე artifact-ზე", "reject; show exact table field diff"),
        ("UNKNOWN_CLASSIFIER", "202 REVIEW_REQUIRED", "proposal resolution შემდეგ", "retain raw and staged rows; block publish"),
        ("ROW_QUALITY", "PARTIAL or FAILED by policy", "after corrected artifact", "issue per row; no silent correction"),
        ("PRIVACY", "REVIEW_REQUIRED", "privacy decision შემდეგ", "block public release"),
        ("TRANSIENT_STORAGE", "503 RETRYABLE", "exponential backoff with jitter", "resume same checkpoint"),
        ("WORKER_CRASH", "RUNNING lease expires", "yes", "new worker acquires lease and resumes"),
        ("CONTRACT_CHANGED", "409", "new preview", "pinned revision means never auto-upgrade"),
        ("MALWARE_OR_CORRUPT", "QUARANTINED", "no", "restricted object and security event"),
    ], [1.45, 1.35, 1.45, 3.05], 7.8)

    add_heading(doc, "Admin API contract", 1)
    add_para(doc, "ყველა endpoint არის `/admin/api/v1`; JSON იყენებს camelCase-ს; IDs opaque string-ად გადაეცემა browser-ს; დრო UTC ISO 8601-ია; mutating request მოითხოვს If-Match ETag-ს და Idempotency-Key-ს იქ, სადაც ოპერაცია შეიძლება განმეორდეს. Error body არის RFC 9457 Problem Details-compatible და შეიცავს code, title, detail, instance, correlationId და violations.")
    api_rows = [
        ("Products","GET POST","/products","list or create product draft"),("Products","GET PATCH","/products/{productCode}","details or optimistic update"),
        ("Contracts","POST","/products/{code}/contract-revisions","branch new draft from parent"),("Contracts","GET PATCH","/contract-revisions/{id}","read or edit draft"),("Contracts","GET","/contract-revisions/{id}/diff","canonical diff and compatibility"),("Contracts","POST","/contract-revisions/{id}:submit","freeze checksum and request review"),("Contracts","POST","/contract-revisions/{id}:approve","record independent approval"),("Contracts","POST","/contract-revisions/{id}:issue","make immutable executable revision"),
        ("Site tree","GET PUT","/contract-revisions/{id}/nodes","read or replace validated tree"),("Datasets","GET POST","/contract-revisions/{id}/datasets","list or add binding"),("Fields","GET PUT","/contract-datasets/{id}/fields","ordered field editor"),("Relations","GET PUT","/contract-revisions/{id}/relations","relation graph editor"),("Gates","GET PUT","/contract-revisions/{id}/gates","gate policy editor"),
        ("Classifiers","GET POST","/classification-schemes","list or create"),("Classifiers","POST","/classification-schemes/{code}/versions","new draft version"),("Classifiers","PUT","/classification-versions/{id}/items","bulk item upsert with ETag"),("Classifiers","PUT","/classification-versions/{id}/hierarchy","replace acyclic hierarchy"),("Classifiers","POST","/classification-versions/{id}:publish","approve and publish"),("Proposals","GET","/classifier-proposals","filter review inbox"),("Proposals","POST","/classifier-proposals/{id}:resolve","accept remap create or reject"),
        ("Statistics","GET POST","/dataflows","list or create"),("Statistics","POST","/dataflows/{code}/dsd-revisions","create DSD draft"),("Statistics","PUT","/dsd-revisions/{id}/components","ordered components"),("Metrics","GET POST","/metrics","list or create"),("Metrics","PATCH","/metrics/{code}","edit draft semantics"),("Metrics","POST","/metrics/{code}:approve","approve exact checksum"),("Semantics","PUT","/dataflows/{code}/bindings/{carrier}","complete metric unit policy binding"),("Policies","GET POST","/quality-policies","versioned quality policies"),("Policies","GET POST","/confidentiality-policies","versioned privacy policies"),("Policies","POST","/policies/{type}/{id}:simulate","run on sample without state change"),
        ("Sources","GET POST","/source-systems","source registry"),("Sources","POST","/source-connections/{id}:test","test via secret reference"),("Mappings","GET PUT","/ingestion-contracts/{id}/sources","source mapping editor"),("Mappings","POST","/ingestion-contracts/{id}:preview","read-only 100 row preview"),
        ("Packages","POST","/contract-revisions/{id}/package-jobs","generate exact Access"),("Packages","GET","/package-jobs/{id}","job status and issues"),("Packages","GET","/package-artifacts/{id}/download","short-lived authorized download"),("Packages","POST","/package-artifacts/{id}:revoke","revoke distribution"),
        ("Imports","POST","/imports","upload and create idempotent batch"),("Imports","GET","/imports/{id}","progress counts and status"),("Imports","GET","/imports/{id}/issues","paged issues"),("Imports","POST","/imports/{id}:resume","resume checkpoint"),("Imports","POST","/imports/{id}:cancel","cooperative cancellation"),("Imports","POST","/imports/{id}:quarantine","security or governance hold"),
        ("Releases","POST","/releases","prepare release"),("Releases","GET","/releases/{id}/gate-report","complete evidence"),("Releases","POST","/releases/{id}:approve","independent approval"),("Releases","POST","/releases/{id}:publish","atomic switch"),("Releases","POST","/releases/{id}:rollback","new pointer event"),
        ("Experience","GET POST","/visualizations","versioned chart definitions"),("Experience","GET POST","/table-definitions","versioned table definitions"),("Experience","GET POST","/export-definitions","versioned export definitions"),("Experience","GET POST","/api-projections","versioned public endpoints"),("Experience","POST","/presentations/{type}/{id}:preview","render against snapshot"),
        ("Approvals","GET","/approval-tasks","current user inbox"),("Approvals","POST","/approval-instances/{id}:decide","approve or reject with evidence"),("Audit","GET","/audit-events","immutable filtered trail"),("Operations","GET","/operations/health","workers outbox storage migration health"),("Operations","POST","/outbox-events/{id}:retry","safe explicit retry"),
    ]
    add_table(doc, ["მოდული", "method", "path", "semantics"], api_rows, [1.05, .75, 3.2, 2.3], 7.35)
    add_heading(doc, "Canonical error codes", 2)
    errors = [
        ("CONTRACT_REVISION_NOT_FOUND",404,"code and revision unknown"),("CONTRACT_CHECKSUM_MISMATCH",409,"stored document changed or wrong artifact"),("CONTRACT_IMMUTABLE",409,"attempt to edit approved or issued revision"),("ETAG_MISMATCH",412,"concurrent edit"),("APPROVER_NOT_INDEPENDENT",403,"four eyes violation"),("GATE_FAILED",422,"one or more blocking gates"),("SCHEMA_TABLE_UNEXPECTED",422,"undeclared Access table"),("SCHEMA_FIELD_MISSING",422,"required field absent"),("SCHEMA_TYPE_MISMATCH",422,"lossy incompatible type"),("RELATION_ORPHAN",422,"required FK unresolved"),("CLASSIFIER_UNRESOLVED",202,"proposal created and review needed"),("METRIC_BINDING_INCOMPLETE",422,"metric unit aggregation or policy missing"),("OBSERVATION_GRAIN_DUPLICATE",422,"duplicate carrier period age"),("PRIVACY_REVIEW_REQUIRED",202,"policy blocks public release"),("IDEMPOTENCY_KEY_REUSED",409,"same key different request"),("ARTIFACT_QUARANTINED",422,"malware corrupt or forbidden content"),("PUBLICATION_POINTER_CONFLICT",409,"another release won optimistic lock"),("JOB_LEASE_LOST",503,"worker must stop and retry safely"),
    ]
    add_table(doc, ["code", "HTTP", "meaning"], errors, [2.7, .55, 4.75], 7.8)

    add_heading(doc, "Security privacy and audit", 1)
    security = [
        ("Authentication", "OIDC authorization code with PKCE; exact redirect URIs; MFA for privileged users; no implicit grant; short sessions and step-up for publish."),
        ("Authorization", "deny by default RBAC plus product scope; permission checked in application service and query filter; UI hiding alone never authorizes."),
        ("Secrets", "only secret_reference in SQL; secret value fetched at execution and never returned, logged or placed in Access."),
        ("Uploads", "streamed size limits, extension plus magic-byte validation, malware scan, isolated parser worker, decompression limits, random object key."),
        ("Injection", "no Access-supplied SQL; parameterized SQL only; JSON expressions interpreted by allow-listed DSL; HTML Markdown sanitized."),
        ("Data protection", "TLS in transit; storage encryption; public aggregate default; PII forbidden in KIDS template; configurable retention and legal hold."),
        ("Audit", "append-only hash-chained event; before and after hash, actor, scope, correlation, decision evidence; audit viewer read-only."),
        ("Supply chain", "pinned dependency versions, SBOM, signed build, SAST SCA secret scan, migration checksum, reproducible Access generator version."),
        ("Verification", "OWASP ASVS 5.0 L2 baseline; L3 controls for identity, secret, approval and publication boundaries."),
    ]
    add_table(doc, ["სფერო", "ნორმატიული კონტროლი"], security, [1.35, 5.95], 8.2)
    add_heading(doc, "Audit event minimum set", 2)
    bullets(doc, [
        "PRODUCT_CREATED, CONTRACT_BRANCHED, CONTRACT_CHANGED, CONTRACT_SUBMITTED, CONTRACT_APPROVED, CONTRACT_ISSUED",
        "CLASSIFIER_ITEM_CHANGED, ALIAS_RESOLVED, CLASSIFIER_VERSION_PUBLISHED",
        "METRIC_CHANGED, SEMANTIC_BINDING_APPROVED, QUALITY_POLICY_APPROVED, CONFIDENTIALITY_POLICY_APPROVED",
        "PACKAGE_GENERATED, PACKAGE_DOWNLOADED, PACKAGE_REVOKED, ARTIFACT_RECEIVED, ARTIFACT_QUARANTINED",
        "IMPORT_STARTED, IMPORT_RESUMED, IMPORT_FAILED, GATE_EVALUATED, RELEASE_APPROVED, RELEASE_PUBLISHED, RELEASE_ROLLED_BACK",
        "ROLE_GRANTED, ROLE_REVOKED, EMERGENCY_ACCESS_USED, SECRET_REFERENCE_TESTED, OUTBOX_RETRIED",
    ])

    add_heading(doc, "Observability and service levels", 1)
    add_table(doc, ["signal", "metric or trace", "target and alert"], [
        ("API", "request count latency error by route and outcome", "p95 query 500 ms; command acceptance 1000 ms; alert 5xx above 1 percent 5 min"),
        ("Generator", "queue age duration file size validation failures", "p95 under 120 s for KIDS template; queue age alert 5 min"),
        ("Import", "rows per second accepted rejected checkpoint age", "progress heartbeat 30 s; stalled alert 2 min"),
        ("Outbox", "pending oldest age retries dead letters", "oldest under 60 s; alert 5 min; dead letter immediate"),
        ("Publication", "gate duration pointer conflicts rollback time", "atomic switch under 5 s; rollback RTO under 15 min"),
        ("Quality", "rule failures by dataset field carrier", "no blocking issue in published release"),
        ("Security", "denied actions auth failures quarantine events", "privileged anomaly immediate"),
        ("Storage", "object checksum mismatch capacity archive lag", "checksum mismatch immediate; 30 day capacity forecast"),
    ], [1.2, 3.3, 2.8], 8)
    add_para(doc, "Trace propagation  HTTP request → application command → SQL transaction → outbox event → worker job → object storage → publication pointer იზიარებს ერთ correlation_id-ს. Log structured JSON-ია და არ შეიცავს source payload-ს, token-ს ან secret-ს.")

    add_heading(doc, "Testing strategy and acceptance", 1)
    add_table(doc, ["ტესტის ფენა", "სავალდებულო coverage", "release gate"], [
        ("Domain unit", "state transitions, compatibility classification, grain, relation, classifier and semantic invariants", "all deterministic tests pass"),
        ("Contract tests", "OpenAPI request response, Problem Details, JSON Schema fixtures", "no undocumented endpoint or field"),
        ("Migration", "fresh install plus upgrade 001 through target; checksum drift rejection; rollback rehearsal", "both paths pass on SQL Server"),
        ("Access golden", "empty generated template exact 21 tables, 104 fields, 15 keys, 21 RI relations; no data rows", "byte difference allowed only with identical semantic manifest and valid checksum"),
        ("KIDS round trip", "source → generated package → import → canonical → export; 456 locators, 36 goals, 225 resources, 230 assignments, 178 glossary, 43 carriers, 880 cells", "counts, keys, lexical values and lineage equal"),
        ("Property based", "random valid invalid contracts, relation graphs, codelists and observation values", "no invariant bypass or parser crash"),
        ("Security", "ASVS, authorization matrix, upload fuzzing, archive bomb and malicious JSON", "no high or critical finding"),
        ("Accessibility", "automated axe plus keyboard and screen reader manual suite", "WCAG 2.2 AA; zero critical issue"),
        ("Performance", "10x KIDS volume, concurrent preview and publish, outbox recovery", "published budgets met without data loss"),
        ("Disaster recovery", "database restore, object restore, outbox replay, release rollback", "RPO 15 min and RTO 4 h platform; release rollback 15 min"),
    ], [1.25, 4.35, 1.7], 7.8)
    add_heading(doc, "KIDS release acceptance assertions", 2)
    assertions = [
        "The source Access file is opened read-only and its SHA-256 is captured before parsing.",
        "The generator refuses to overwrite an existing output path.",
        "No payload_raw or chartdata column exists in kids_statistical_carrier.",
        "Each source-derived row resolves to one __raw_document locator and immutable artifact checksum.",
        "All 43 carriers have one metric, unit, aggregation, quality and confidentiality binding.",
        "All 880 cells preserve period_raw, dimension_key_raw, value_lexical, json_path and source encoding.",
        "All Access relations are both declared in __gs_relation and physically enforced.",
        "Auto publish is false; publication requires full gate report and independent approval.",
        "Historical revisions remain resolvable against their own immutable contract_revision_source bindings.",
        "Published snapshot mutation fails; rollback only changes the active pointer.",
    ]
    for i, a in enumerate(assertions, 1):
        add_para(doc, f"A{i:02d}  {a}")

    add_heading(doc, "Implementation backlog", 1)
    add_para(doc, "ქვემოთ რიგი dependency order-ია და არა სურვილების სია. ყოველი tranche მთავრდება deployable vertical slice-ით.")
    backlog = [
        ("0 Baseline hardening", "align Spring Boot plugin and BOM to one supported version; remove duplicate POI versions; add migration and dependency lock checks", "clean reproducible build and current tests green"),
        ("1 Governance kernel", "new IAM approval decision audit tables; rowversion; domain state machines; authorization ports", "contract draft can be independently approved with immutable evidence"),
        ("2 Contract studio", "product site tree dataset field key relation gate screens and APIs; schema diff", "revision can be authored linted diffed approved issued"),
        ("3 Classification studio", "scheme version item hierarchy alias proposal UX and APIs", "unknown values create resolvable proposals; published versions immutable"),
        ("4 Statistical studio", "unit dimension measure metric DSD component semantic quality privacy editors", "all 43 KIDS carriers visible and binding-complete"),
        ("5 Package service", "manifest compiler Jackcess generator independent validator signature artifact registry", "empty r7 Access produced from DB contract with no Java hardcoded schema"),
        ("6 Import orchestration", "upload quarantine catalog exact validation chunk staging checkpoint resume semantic materialization", "filled package imports idempotently and failures resume"),
        ("7 Publication", "release members gate report approval atomic pointer outbox rollback archive", "KIDS snapshot publishes and rolls back reproducibly"),
        ("8 Experience registry", "table chart export API projections plus public serving adapters", "site reads only published contract-driven projections"),
        ("9 Operational assurance", "telemetry dashboards alerts backup restore DR security and accessibility evidence", "production readiness review signed"),
    ]
    add_table(doc, ["tranche", "შესრულება", "exit criterion"], backlog, [1.3, 4.3, 1.7], 7.8)
    add_heading(doc, "Repository package map", 2)
    code_block(doc, "core/src/main/java/org/base/core/platform/\n  identity  governance  catalog  contract  classification\n  statistics  quality  privacy  ingestion  packagegen\n  publication  presentation  audit\napi/src/main/java/org/base/api/\n  controller/admin/v1  application  adapter/in  adapter/out\nweb/src/main/java/org/base/web/admin/\n  security  bff  viewmodel\nweb/src/main/resources/templates/admin/\n  dashboard  products  contracts  classifiers  statistics\n  sources  packages  imports  releases  audit  operations\ncore/src/main/resources/db/platform/\n  024_panel_governance_kernel.sql\n  025_panel_contract_completeness.sql\n  026_panel_generation_publication.sql")
    add_para(doc, "თუ SPA მოგვიანებით გახდება საჭირო, ის უნდა იყოს Experience adapter და არა domain-ის მეორე implementation. პირველ production release-ში server-rendered Thymeleaf + მცირე progressive enhancement შეესაბამება არსებულ stack-ს, ამცირებს supply-chain-ს და WCAG-ის კონტროლს; relation graph შეიძლება იზოლირებულ web component-ად დაემატოს accessible table fallback-ით.")

    add_heading(doc, "d txt მოთხოვნების საბოლოო gap audit", 1)
    add_para(doc, "ეს audit პირდაპირ ადარებს d.txt-ის ყველა სავალდებულო ბლოკს repository-ის registry-ს, migration-ებს, generator-სა და ამ blueprint-ს. UI განზრახ გამოტოვებულია, მაგრამ UI-სთვის საჭირო metadata, validation, API და workflow კონტრაქტები შედის.")
    d_audit = [
        ("Control Plane authority and immutable revision", "PASS", "platform contract registry; approved revision gate; Access has no authoring authority"),
        ("Namespace and structure identity", "PASS", "GS, CL, STAT, RAW, ENT, REF, SERV, ARCH, AUDIT, SYS and KIDS plus __gs_structure_index"),
        ("Prefix canon", "PASS", "__gs_*, __cl_*, __stat_*, __raw_* and kids_* domain prefix; unprefixed new physical tables are forbidden"),
        ("Raw envelope", "PASS", "__raw_document has the complete 22-field immutable envelope; payload remains an external artifact reference"),
        ("Keys, indexes and relations", "PASS", "registry-defined primary/secondary indexes and relation cardinality are materialized deterministically"),
        ("Classifiers and statistical semantics", "PASS", "scheme/version/item/alias/hierarchy, unit, metric and semantic binding contracts are relational"),
        ("Panel backend and API", "PASS", "governance, mapping, ingestion, validation, package, publication, audit and observability endpoints are specified"),
        ("Old Access import", "PASS", "inventory, mapping, quarantine, row issues, checkpoint, retry, idempotency and post-import gates are specified"),
        ("UI", "EXCLUDED BY REQUEST", "only declarative metadata and API capabilities are delivered; no UI is built"),
    ]
    add_table(doc, ["d.txt block", "result", "evidence and boundary"], d_audit, [2.0, 1.15, 4.15], 7.4)

    add_heading(doc, "Definition of Done", 1)
    dod = [
        ("Contract authority", "ერთი approved revision-იდან deterministic-ად გენერირდება manifest, Access, OpenAPI fragments და UI navigation projection."),
        ("No hardcode", "dataset, field, relation, metric, unit, classifier, gate და presentation definition აღარ არის Java ან template hardcode; seed migration შეიძლება შეიცავდეს metadata-ს, observation value-ს არასდროს."),
        ("Completeness", "UI-დან ივსება ამ დოკუმენტში გამოცხადებული ყველა required field; API და DB invariant ერთსა და იმავე domain policy-ს იყენებს."),
        ("Safety", "separation of duties, deny by default authorization, immutable approval and signed artifact მუშაობს integration test-ით."),
        ("Access", "empty template exact schema-ით იქმნება; შემავსებელს არ შეუძლია კონტრაქტის შეცვლა; filled package exact validator-ს გადის."),
        ("Statistics", "43 of 43 binding complete; 880 of 880 cell lineage complete; aggregation NONE enforced; policy simulation evidence retained."),
        ("Publication", "all gates PASS; atomic snapshot switch; no partial public state; tested rollback."),
        ("Operations", "metrics logs traces alerts backup restore runbook and ownership available."),
        ("Quality", "unit integration contract round-trip security accessibility performance and DR suites green."),
        ("Documentation", "OpenAPI, data dictionary, state diagrams and decision register generated from the same contract revision."),
    ]
    add_table(doc, ["სფერო", "დასრულების პირობა"], dod, [1.4, 5.9], 8.2)
    add_para(doc, "საბოლოო წითელი ხაზი  თუ პანელს შეუძლია ისეთი Access-ის გენერაცია, რომლის schema ან semantics მონაცემთა ბაზაში approved revision-ის სახით არ არსებობს, იმპლემენტაცია დასრულებული არ არის. თუ Access-ის შემვსებს შეუძლია schema-ს, classifier authority-ს, metric/unit-ს ან publication-ს თვითონ შეცვლა, იმპლემენტაცია არასწორია.")

    add_heading(doc, "გადაწყვეტილებათა რეესტრი", 1)
    decisions = [
        ("ADR 001", "Control Plane authority", "პანელი ქმნის და ამტკიცებს; Access ასრულებს", "prevents split brain contract"),
        ("ADR 002", "Modular monolith", "bounded contexts ერთ deployable Spring application-ში", "correct complexity for current scale"),
        ("ADR 003", "No event sourcing", "append-only audit plus current relational state", "auditability without replay complexity"),
        ("ADR 004", "Transactional outbox", "cross-plane side effects asynchronously", "recoverable delivery without distributed transaction"),
        ("ADR 005", "Immutable revisions", "approved objects never updated", "reproducibility and rollback"),
        ("ADR 006", "Raw artifact authority", "original Access bytes in object storage", "no duplicated raw JSON"),
        ("ADR 007", "SDMX statistical core", "dataflow DSD dimensions measure attributes", "interoperable statistical meaning"),
        ("ADR 008", "JSON only for extensible specs", "core identity and relations remain relational", "queryability plus controlled flexibility"),
        ("ADR 009", "Human approval for ambiguity", "fuzzy inference creates proposal only", "no fabricated semantics"),
        ("ADR 010", "Presentation separated from data", "chart table export API definitions bind semantic layer", "no hardcoded visual data"),
    ]
    add_table(doc, ["ID", "თემა", "არჩევანი", "მიზეზი"], decisions, [1.0, 1.6, 2.9, 1.8], 8)

    add_heading(doc, "ნორმატიული წყაროები", 1)
    refs = [
        ("SDMX 3.1 Technical Specifications", "https://sdmx.org/standards-2/", "2025 release; statistical information model and exchange"),
        ("W3C DCAT 3 Recommendation", "https://www.w3.org/TR/vocab-dcat-3/", "catalog, dataset, distribution, version and service metadata"),
        ("W3C PROV O Recommendation", "https://www.w3.org/TR/prov-o/", "provenance entities activities agents and derivation"),
        ("JSON Schema Draft 2020 12", "https://json-schema.org/draft/2020-12", "machine-readable JSON validation"),
        ("OpenAPI Specification", "https://spec.openapis.org/oas/latest.html", "current HTTP API contract; target 3.2"),
        ("WCAG 2.2", "https://www.w3.org/TR/WCAG22/", "AA accessibility target"),
        ("OWASP ASVS 5.0", "https://owasp.org/www-project-application-security-verification-standard/", "security verification baseline"),
        ("RFC 9700", "https://www.rfc-editor.org/rfc/rfc9700.html", "OAuth 2.0 security best current practice"),
        ("NIST SP 800 218", "https://csrc.nist.gov/pubs/sp/800/218/final", "secure software development framework"),
        ("Repository contract", "core/src/main/resources/db/platform/022_kids_complete_site_contract.sql", "KIDS revision 6 structural foundation"),
        ("Repository structure registry", "core/src/main/resources/db/platform/024_contract_structure_registry_and_ui_metadata.sql; 025_contract_registry_system_fields.sql; 026_contract_registry_keys_and_relations.sql; 027_contract_registry_approval_state.sql; 028_contract_index_registry.sql; 029_complete_structure_and_raw_contract.sql; 030_raw_document_envelope_fields.sql", "namespace, structure, physical table boundary, field, key, index, relation, prefix and raw-envelope registry"),
        ("Registry-driven generator", "ops/scripts/java/ContractRegistryAccessPackageGenerator.java", "empty Access package generated only from approved contract table/field/key/relation registry and populated with declared metadata"),
        ("Repository semantics", "core/src/main/resources/db/platform/023_kids_inferred_statistical_semantics.sql", "KIDS revision 7 metric unit policy decisions"),
        ("Repository generator", "ops/scripts/java/KidsPortalCanonicalAccessPackageGenerator.java", "current exact r7 Access implementation evidence"),
    ]
    add_table(doc, ["წყარო", "მისამართი", "გამოყენება"], refs, [1.8, 3.5, 2.0], 7.7)

    add_heading(doc, "საბოლოო დასკვნა", 1)
    add_para(doc, "ეს blueprint კეტავს მთავარ არქიტექტურულ თავისუფლებებს, რომლებმაც აქამდე შეიძლებოდა deviation გამოეწვია: authority, boundary, schema, lifecycle, approval, Access contract, statistical semantics, classifier resolution, ingestion, publication, security და acceptance. დარჩენილი სამუშაო არის ამ გამოცხადებული კონტრაქტის იმპლემენტაცია migration-ებად, domain/application code-ად, UI-დ და automated test-ებად — არა ახალი არქიტექტურის მოფიქრება.")

    core = doc.core_properties
    core.title = "KIDS მართვის პანელის სრული არქიტექტურული და იმპლემენტაციის სპეციფიკაცია"
    core.subject = "KIDS Control Plane and Access execution blueprint"
    core.author = "OpenAI Codex"
    core.keywords = "KIDS, Access, Control Plane, SDMX, ISO 11179, DCAT, governance, panel"
    core.comments = "Normative implementation-ready architecture based on KIDS_PORTAL_V1 revision 7."

    doc.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
