#!/usr/bin/env python3
"""ERP 아키텍처 SVG 생성 — python3 docs/gen_arch_svg.py"""

W, H = 1160, 460
BG = "#0d1117"
C = {"box": "#21262d", "border": "#30363d", "text": "#e6edf3", "sub": "#8b949e",
     "arr": "#58a6ff", "green": "#3fb950", "orange": "#d29922", "red": "#f85149",
     "purple": "#bc8cff"}


def svg_open():
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
            f'style="font-family:\'SF Mono\',monospace;background:{BG}">\n'
            f'<rect width="{W}" height="{H}" fill="{BG}"/>\n')


def svg_close():
    return '</svg>\n'


def box(x, y, w, h, title, sub="", color="border"):
    bc = C.get(color, color)
    subs = [s.strip() for s in sub.split("·")] if sub else []
    ty = y + h // 2 - (8 * (len(subs) - 1) // 2) - (8 if subs else 0)
    lines = [
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" '
        f'fill="{C["box"]}" stroke="{bc}" stroke-width="1.5"/>',
        f'<text x="{x + w // 2}" y="{ty}" text-anchor="middle" '
        f'fill="{C["text"]}" font-size="13" font-weight="600">{title}</text>',
    ]
    for i, s in enumerate(subs):
        lines.append(
            f'<text x="{x + w // 2}" y="{ty + 17 + i * 14}" '
            f'text-anchor="middle" fill="{C["sub"]}" font-size="11">{s}</text>'
        )
    return "\n".join(lines) + "\n"


def line(x1, y1, x2, y2, label="", color="arr", dashed=False):
    c = C.get(color, color)
    dash = ' stroke-dasharray="6,4"' if dashed else ""
    mid_x, mid_y = (x1 + x2) // 2, (y1 + y2) // 2
    parts = [
        f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{c}" '
        f'stroke-width="1.5"{dash} marker-end="url(#arr-{color})"/>',
    ]
    if label:
        off = -8 if y1 == y2 else 0
        parts.append(
            f'<text x="{mid_x}" y="{mid_y + off}" text-anchor="middle" '
            f'fill="{c}" font-size="10">{label}</text>'
        )
    return "\n".join(parts) + "\n"


def curve(x1, y1, cx, cy, x2, y2, label="", color="arr", dashed=False):
    c = C.get(color, color)
    dash = ' stroke-dasharray="6,4"' if dashed else ""
    lx = (x1 + cx * 2 + x2) // 4
    ly = (y1 + cy * 2 + y2) // 4 - 10
    parts = [
        f'<path d="M{x1},{y1} Q{cx},{cy} {x2},{y2}" stroke="{c}" '
        f'stroke-width="1.5" fill="none"{dash} marker-end="url(#arr-{color})"/>',
    ]
    if label:
        parts.append(
            f'<text x="{lx}" y="{ly}" text-anchor="middle" '
            f'fill="{c}" font-size="10">{label}</text>'
        )
    return "\n".join(parts) + "\n"


def legend_row(x, y, color, label):
    c = C.get(color, color)
    return (
        f'<line x1="{x}" y1="{y + 5}" x2="{x + 20}" y2="{y + 5}" stroke="{c}" '
        f'stroke-width="1.5" marker-end="url(#arr-{color})"/>'
        f'<text x="{x + 26}" y="{y + 9}" fill="{C["sub"]}" font-size="11">{label}</text>\n'
    )


def wrap(content):
    colors = ["arr", "green", "orange", "purple", "sub"]
    defs = '<defs>\n'
    for col in colors:
        c = C.get(col, col)
        defs += (
            f'<marker id="arr-{col}" markerWidth="8" markerHeight="8" '
            f'refX="6" refY="3" orient="auto">'
            f'<path d="M0,0 L0,6 L8,3 z" fill="{c}"/></marker>\n'
        )
    defs += '</defs>\n'
    return svg_open() + defs + content + svg_close()


def gen_architecture():
    out = ""

    # ── 노드 (top-left 기준) ──────────────────────────────────────────────
    # Row 1 (y=170): Browser → Next.js → Spring Boot
    # Row 2 (y=330): Keycloak          PostgreSQL

    browser_x, browser_y, browser_w, bh = 40, 170, 150, 70
    nextjs_x,  nextjs_y,  nextjs_w       = 260, 170, 170
    spring_x,  spring_y,  spring_w       = 640, 170, 215
    kc_x,      kc_y,      kc_w           = 390, 330, 180
    pg_x,      pg_y,      pg_w           = 870, 330, 170

    # boxes
    out += box(browser_x, browser_y, browser_w, bh,
               "Browser", "", color="border")
    out += box(nextjs_x, nextjs_y, nextjs_w, bh,
               "Next.js 15", "Vercel · BFF · next-auth v5", color="green")
    out += box(spring_x, spring_y, spring_w, bh,
               "Spring Boot 3", "HR · Finance · Inventory · CRM", color="purple")
    out += box(kc_x, kc_y, kc_w, bh,
               "Keycloak", "OIDC · JWT · tenant_id claim", color="orange")
    out += box(pg_x, pg_y, pg_w, bh,
               "PostgreSQL 16", "Flyway · tenant_id · soft-delete", color="green")

    # ── 화살표 ───────────────────────────────────────────────────────────
    cy1 = browser_y + bh // 2   # row 1 center y = 205

    # Browser → Next.js
    out += line(browser_x + browser_w, cy1,
                nextjs_x, cy1, "HTTPS")

    # Next.js → Spring Boot
    out += line(nextjs_x + nextjs_w, cy1,
                spring_x, cy1, "REST + JWT Bearer")

    # Next.js → Keycloak (OAuth2 로그인)
    nx_cx = nextjs_x + nextjs_w // 2   # 345
    kc_cx = kc_x + kc_w // 2           # 480
    out += line(nx_cx, nextjs_y + bh,
                kc_cx, kc_y, "OAuth2 로그인", color="orange")

    # Spring Boot → Keycloak (JWKS 검증, 점선)
    sp_cx = spring_x + spring_w // 2   # 747
    kc_rx = kc_x + kc_w                # 570
    kc_cy = kc_y + bh // 2             # 365
    out += curve(sp_cx, spring_y + bh,
                 (sp_cx + kc_rx) // 2, kc_y - 10,
                 kc_rx, kc_cy,
                 "JWKS 검증", color="orange", dashed=True)

    # Spring Boot → PostgreSQL (JPA / Flyway)
    pg_cx = pg_x + pg_w // 2           # 955
    out += line(sp_cx, spring_y + bh,
                pg_cx, pg_y, "JPA / Flyway")

    # ── 범례 ─────────────────────────────────────────────────────────────
    lx, ly = 40, 405
    out += (f'<text x="{lx}" y="{ly - 14}" fill="{C["sub"]}" '
            f'font-size="11" font-weight="600">범례</text>\n')
    out += legend_row(lx,       ly, "arr",    "HTTPS / REST")
    out += legend_row(lx + 150, ly, "orange", "OIDC / JWT 흐름")
    out += legend_row(lx + 310, ly, "green",  "Next.js BFF · DB")
    out += legend_row(lx + 470, ly, "purple", "Spring Boot 모듈")

    svg = wrap(out)
    with open("docs/architecture.svg", "w") as f:
        f.write(svg)
    print("docs/architecture.svg 생성 완료")


if __name__ == '__main__':
    import os
    os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    gen_architecture()
