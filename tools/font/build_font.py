"""把子集字体改名并落到项目 resources（第二步，接 make_subset.py）。

**为什么要改名**：子集在 OFL 定义下算「修改」。Noto Sans SC 的版权声明里
Reserved Font Name 是 'Source'，严格说保留 "Noto Sans SC" 并不违规，但改名更干净，
也让玩家一眼看出这是本模组的 UI 字体。OFL.txt 必须随字体一起分发（已一并复制）。
"""
import pathlib
import shutil
import sys

from fontTools.ttLib import TTFont

HERE = pathlib.Path(__file__).resolve().parent
PROJECT = HERE.parents[1]
RES = PROJECT / "src/main/resources/assets/mcoo_build_with_ease/fonts"

SRC = HERE / ".cache/subset-lang+gb1.ttf"
FAMILY = "McooUI SC"
VERSION = "Version 1.000 (subset of Noto Sans SC v2.004)"
COPYRIGHT = ("McooUI SC is a subset of Noto Sans SC (c) 2014-2021 Adobe "
             "(http://www.adobe.com/), with Reserved Font Name 'Source'. "
             "Licensed under the SIL Open Font License, Version 1.1.")
LICENSE = "This Font Software is licensed under the SIL Open Font License, Version 1.1."


def set_name(name_table, name_id: int, value: str) -> None:
    for platform_id, encoding_id, language_id in ((3, 1, 0x409), (1, 0, 0)):
        name_table.setName(value, name_id, platform_id, encoding_id, language_id)


def main() -> int:
    if not SRC.exists():
        print(f"先跑 make_subset.py 生成 {SRC}")
        return 1
    font = TTFont(SRC)
    names = font["name"]
    set_name(names, 0, COPYRIGHT)
    set_name(names, 1, FAMILY)
    set_name(names, 2, "Medium")
    set_name(names, 4, f"{FAMILY} Medium")
    set_name(names, 5, VERSION)
    set_name(names, 6, "McooUISC-Medium")
    set_name(names, 13, LICENSE)
    set_name(names, 14, "https://scripts.sil.org/OFL")

    RES.mkdir(parents=True, exist_ok=True)
    out = RES / "McooUI-SC-Medium.ttf"
    font.save(out)
    print(f"字体 -> {out}  {out.stat().st_size / 1024 / 1024:.2f} MB")

    ofl = HERE / "OFL.txt"
    if ofl.exists():
        shutil.copyfile(ofl, RES / "OFL.txt")
        print(f"许可证 -> {RES / 'OFL.txt'}")
    else:
        print(f"⚠ {ofl} 缺失：OFL 要求随字体分发许可证全文，补上再提交")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
