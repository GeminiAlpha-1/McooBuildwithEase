"""把 Noto Sans SC 变量字体实例化成 Medium 并子集化。

**为什么是这套流程（别改成 OTF）**：
  ImGui 用的 stb_truetype **只支持 glyf 轮廓**；Noto 官方发的 .otf 是 CFF → 喂进去画不出字。
  Google Fonts 仓库里的 `NotoSansSC[wght].ttf` 是 glyf + 变量轴（wght 100..900），
  所以先 `varLib.instancer` 定死到 500（Medium）再子集。

**字符集怎么定**：
  本模组 7 个 lang 文件用到的全部字符
  ∪ GB2312 一级 3755 常用汉字
  ∪ ASCII / Latin-1 / 通用标点 / CJK 标点 / 全角 / 西里尔（ru_ru）
  ∪ 界面上直接用的符号（修饰键、√✖、箭头）
  ⇒ 4878 字形 / 约 1.3MB。全量 Noto Sans SC 是 10MB，我们一个字形都用不到的也一起带走不值得。

用法（需要 fontTools：`pip install fonttools`）：
    python tools/font/make_subset.py     # 产出 subset-lang+gb1.ttf
    python tools/font/build_font.py      # 改名 + 落到 src/main/resources/assets/.../fonts/
"""
import json
import pathlib
import sys
import urllib.request

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools import subset

HERE = pathlib.Path(__file__).resolve().parent
PROJECT = HERE.parents[1]
CACHE = HERE / ".cache"
LANG_DIR = PROJECT / "src/main/resources/assets/mcoo_build_with_ease/lang"
VF = CACHE / "NotoSansSC-VF.ttf"
VF_URL = "https://raw.githubusercontent.com/google/fonts/main/ofl/notosanssc/NotoSansSC%5Bwght%5D.ttf"

# 这一档是实测过的平衡点（见 README）：lang-only 0.33MB / lang+GB2312一级 1.27MB / 全集 2.23MB
CHARSET_NAME = "lang+gb1"


def gb2312_chars(level_one_only: bool) -> set[str]:
    """GB2312 汉字集（用编解码反推，避免内置字表过期）。"""
    out = set()
    hi_start, hi_end = (0xB0, 0xD8) if level_one_only else (0xB0, 0xF8)
    for hi in range(hi_start, hi_end):
        for lo in range(0xA1, 0xFF):
            try:
                out.add(bytes([hi, lo]).decode("gb2312"))
            except UnicodeDecodeError:
                continue
    return out


def lang_chars() -> set[str]:
    out = set()
    for path in sorted(LANG_DIR.glob("*.json")):
        # strict=False：lang 文件里有未转义的控制字符，MC 自己的 GSON 是宽松解析的
        data = json.loads(path.read_text(encoding="utf-8"), strict=False)
        for value in data.values():
            if isinstance(value, str):
                out.update(value)
    return out


def base_chars() -> set[str]:
    out = set()
    out.update(chr(c) for c in range(0x20, 0x100))       # ASCII + Latin-1
    out.update(chr(c) for c in range(0x2000, 0x2070))    # 通用标点
    out.update(chr(c) for c in range(0x3000, 0x3040))    # CJK 标点
    out.update(chr(c) for c in range(0xFF00, 0xFFF0))    # 全角
    out.update(chr(c) for c in range(0x400, 0x500))      # 西里尔（ru_ru）
    # 界面里会直接出现的符号：
    #   修饰键/状态图标（⌘⌃⎇⇧❖⚠✔✖⬆⬇➡⬅☠☢❤✏←↑→↓）
    #   列表行首标记与折叠三角（●○■□▸▾）
    # 少一个就会在界面上显示成方块/问号 —— 踩过（模块列表前的 ● 就漏了）。
    out.update("⌘⌃⎇⇧❖⚠✔✖⬆⬇➡⬅☠☢❤✏←↑→↓●○■□▸▾")
    return out


def main() -> int:
    CACHE.mkdir(parents=True, exist_ok=True)
    if not VF.exists():
        print(f"下载变量字体 -> {VF}")
        with urllib.request.urlopen(VF_URL, timeout=180) as response:
            VF.write_bytes(response.read())
    print(f"源字体 {VF.stat().st_size / 1024 / 1024:.2f} MB")

    chars = base_chars() | lang_chars() | gb2312_chars(True)
    print(f"字符集 {CHARSET_NAME}：{len(chars)} 个字符")

    font = TTFont(VF)
    static = instancer.instantiateVariableFont(font, {"wght": 500}, updateFontNames=True)

    options = subset.Options()
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    options.notdef_outline = True
    options.recalc_bounds = True

    sub = subset.Subsetter(options=options)
    sub.populate(text="".join(sorted(chars)))
    sub.subset(static)

    out = CACHE / f"subset-{CHARSET_NAME}.ttf"
    static.save(out)
    glyphs = TTFont(out)["maxp"].numGlyphs
    print(f"子集 -> {out}  {out.stat().st_size / 1024 / 1024:.2f} MB  glyphs {glyphs}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
