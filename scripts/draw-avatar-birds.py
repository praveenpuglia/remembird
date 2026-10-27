"""Draws the no-photo avatar birds once and writes both an HTML preview and Android vector drawables.

Usage: python3 scripts/draw-avatar-birds.py /tmp/birds.html app/src/main/res/drawable
Add a bird here and to BIRDS in ui/ContactAvatar.kt.
"""
import sys, pathlib

INK = "#3B2F2F"
BLUSH = "#F7A1B5"
WHITE = "#FFFDF7"


def circle(cx, cy, r):
    return f"M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0 Z"


def ellipse(cx, cy, rx, ry):
    return f"M{cx - rx},{cy} a{rx},{ry} 0 1,0 {2 * rx},0 a{rx},{ry} 0 1,0 {-2 * rx},0 Z"


def P(d, fill=None, stroke=None, sw=None, alpha=None):
    return dict(d=d, fill=fill, stroke=stroke, sw=sw, alpha=alpha)


def inked(d, fill, sw=3):
    return P(d, fill, INK, sw)


def eye(cx, cy, r=3.4):
    return [P(circle(cx, cy, r), "#1A1414"), P(circle(cx + r * 0.35, cy - r * 0.35, r * 0.34), "#FFFFFF")]


def happy_eye(cx, cy):
    return [P(f"M{cx - 4.5},{cy + 2} Q{cx},{cy - 4} {cx + 4.5},{cy + 2}", None, INK, 3)]


def blush(cx, cy):
    return [P(ellipse(cx, cy, 4.6, 2.7), BLUSH, alpha=0.9)]


BODY = "M10,104 C10,76 28,64 50,64 C72,64 90,76 90,104 Z"
BG = "M0,0 h100 v100 h-100 Z"

BIRDS = {
    "myna": ("#A9C8F0", [
        inked(BODY, "#7A5341"),
        P("M16,94 C20,86 26,82 33,81", None, WHITE, 4),
        inked(circle(50, 44, 26), "#2B2222"),
        P("M54,42 C55,34 66,32 69,39 C71,46 62,50 56,47 Z", "#F2C230", INK, 2),
        *eye(62, 41),
        inked("M73,45 L89,51 L73,57 Z", "#F2C230", 2.5),
        *blush(64, 56),
    ]),
    "parakeet": ("#F7C6A3", [
        inked(BODY, "#6DBB5E"),
        inked(circle(50, 44, 26), "#86CE74"),
        P("M30,64 C40,72 60,72 72,62", None, "#2B2222", 3),
        P("M33,60 C42,66 58,66 70,58", None, "#E86F8E", 2.5),
        P(circle(58, 38, 5.6), "#F6E7A0", INK, 1.6),
        *eye(58, 38, 3),
        inked("M68,38 C79,35 86,45 81,58 C79,53 74,51 68,53 Z", "#D64545", 2.5),
        *blush(60, 52),
    ]),
    "kingfisher": ("#F6D186", [
        inked(BODY, "#8B4A2B"),
        inked("M10,104 C10,86 16,76 26,70 L36,104 Z", "#2F8FD1", 2.5),
        inked("M90,104 C90,86 84,76 74,70 L64,104 Z", "#2F8FD1", 2.5),
        inked(circle(48, 44, 25), "#8B4A2B"),
        P("M34,62 C42,58 60,58 68,62 C64,80 56,96 50,104 C44,96 38,80 34,62 Z", WHITE, INK, 2.5),
        *eye(56, 40),
        inked("M68,38 L97,47 L68,54 Z", "#E0412F", 2.5),
        *blush(58, 52),
    ]),
    "pigeon": ("#F7A1B5", [
        inked(BODY, "#8E98A4"),
        inked(circle(50, 42, 24), "#A3ACB7"),
        inked("M24,70 C34,61 66,61 76,70 C70,77 30,77 24,70 Z", "#5FAF8E", 2.5),
        P("M31,71 C41,67 59,67 69,71 C61,75 39,75 31,71 Z", "#8C6BB1"),
        P(circle(58, 38, 5.2), "#F29A3A", INK, 1.6),
        *eye(58, 38, 2.6),
        P(ellipse(69, 43, 3.2, 2.2), "#F4EEE2", INK, 1.6),
        inked("M70,45 L81,48 L70,52 Z", "#4A3A30", 2.5),
        *blush(60, 52),
    ]),
    "owlet": ("#9ED9C3", [
        inked(BODY, "#9C7B5B"),
        *[P(circle(x, y, 2.6), "#F4EEE2") for x, y in [(24, 88), (34, 80), (66, 80), (76, 88), (50, 94), (40, 96), (60, 96)]],
        inked(ellipse(50, 44, 31, 27), "#A88664"),
        *[P(circle(x, y, 1.9), "#F4EEE2") for x, y in [(38, 24), (50, 21), (62, 24)]],
        P("M25,34 C33,27 43,29 47,35", None, "#F4EEE2", 4),
        P("M53,35 C57,29 67,27 75,34", None, "#F4EEE2", 4),
        P(circle(37, 45, 9), "#F2C230", INK, 2.2),
        P(circle(63, 45, 9), "#F2C230", INK, 2.2),
        *eye(38, 46, 4.2),
        *eye(62, 46, 4.2),
        inked("M46,54 L54,54 L50,61 Z", "#E9C46A", 2),
        *blush(27, 58), *blush(73, 58),
    ]),
    "hoopoe": ("#CDBEEA", [
        inked("M30,36 C24,16 34,4 42,9 C46,2 56,1 59,8 C66,3 76,8 73,18 C80,19 81,29 70,34 Z", "#E59A4E", 2.5),
        P("M29,20 C28,9 37,4 43,9 C47,3 55,2 59,8 C65,4 74,8 73,17", None, "#2B2222", 4.5),
        inked(BODY, "#E3A764"),
        P("M13,92 L25,86 M15,101 L29,93", None, "#2B2222", 4),
        P("M14,97 L27,90", None, WHITE, 3),
        P("M87,92 L75,86 M85,101 L71,93", None, "#2B2222", 4),
        P("M86,97 L73,90", None, WHITE, 3),
        inked(circle(50, 46, 24), "#EDB073"),
        *happy_eye(57, 42),
        P("M71,48 C81,50 90,56 95,66", None, INK, 5.5),
        P("M71,48 C81,50 90,56 95,66", None, "#4A3A30", 2.6),
        *blush(60, 54),
    ]),
    "bulbul": ("#DCE8A8", [
        inked(BODY, "#8C7766"),
        P("M32,70 C40,66 60,66 68,70 C64,88 56,100 50,104 C44,100 36,88 32,70 Z", "#F4EEE2"),
        inked("M38,30 C36,16 42,6 50,2 C52,12 58,20 64,28 Z", "#2B2222"),
        inked(circle(50, 46, 23), "#2B2222"),
        P("M44,55 C52,51 64,51 72,55 C68,63 56,65 46,61 Z", WHITE),
        P(ellipse(46, 48, 4.6, 3.6), "#D64545", INK, 1.6),
        P(circle(60, 42, 4.4), WHITE),
        *eye(60, 42, 3.2),
        inked("M71,43 L82,46.5 L71,50 Z", "#2B2222", 2.5),
    ]),
}


def svg(name, bg, parts):
    out = [f'<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg"><path d="{BG}" fill="{bg}"/>']
    for p in parts:
        a = [f'd="{p["d"]}"', f'fill="{p["fill"] or "none"}"']
        if p["stroke"]:
            a += [f'stroke="{p["stroke"]}"', f'stroke-width="{p["sw"]}"', 'stroke-linecap="round"', 'stroke-linejoin="round"']
        if p["alpha"] is not None:
            a.append(f'fill-opacity="{p["alpha"]}"')
        out.append(f'<path {" ".join(a)}/>')
    return "".join(out) + "</svg>"


def vector(name, bg, parts):
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- Stand-in for a contact without a photo ({name}). Generated by scripts/draw-avatar-birds.py; edit there, not here. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="100dp"',
        '    android:height="100dp"',
        '    android:viewportWidth="100"',
        '    android:viewportHeight="100">',
        f'    <path android:pathData="{BG}" android:fillColor="{bg}" />',
    ]
    for p in parts:
        a = [f'android:pathData="{p["d"]}"']
        if p["fill"]:
            a.append(f'android:fillColor="{p["fill"]}"')
        if p["alpha"] is not None:
            a.append(f'android:fillAlpha="{p["alpha"]}"')
        if p["stroke"]:
            a += [f'android:strokeColor="{p["stroke"]}"', f'android:strokeWidth="{p["sw"]}"', 'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
        lines.append(f'    <path {" ".join(a)} />')
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    preview, res = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
    cells = "".join(
        f'<figure><div class="pol big">{svg(n, *b)}</div><div class="pol">{svg(n, *b)}</div><figcaption>{n}</figcaption></figure>'
        for n, b in BIRDS.items()
    )
    preview.write_text(
        "<style>body{background:#F3EAD8;font:14px system-ui;display:flex;flex-wrap:wrap;gap:24px;padding:24px}"
        "figure{margin:0;display:grid;gap:10px;justify-items:center}.pol{background:#FFFDF7;padding:3px 3px 10px;box-shadow:0 2px 4px #0003;rotate:-3deg}"
        ".pol svg{width:44px;height:44px;display:block}.big svg{width:140px;height:140px}</style>" + cells
    )
    for n, b in BIRDS.items():
        (res / f"avatar_{n}.xml").write_text(vector(n, *b))
