"""
Builds backend/src/main/resources/db/migration/V2__seed_data.sql from the raw data in
data_import/extracted/pc part db/ (that folder is git-ignored; see docs/data-sources.md).

Sources:
  - pc-builder-parts.json  (TechFuelHQ, CC BY 4.0): main source of parts + specs, no prices.
  - *.csv                  : used only to look up prices, CPU base clocks, and a few extra
                             parts the JSON lacks (DDR4 boards/RAM, hard drives, coolers).
  - Hand-filled values     : marked with "FILLED" below and listed in docs/data-sources.md.

Run from the repo root:   py -I tools/seed/build_seed_sql.py
"""
import csv
import json
import os
import re
import statistics

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DATA = os.path.join(ROOT, "data_import", "extracted", "pc part db")
OUT = os.path.join(ROOT, "backend", "src", "main", "resources", "db", "migration", "V2__seed_data.sql")

J = json.load(open(os.path.join(DATA, "pc-builder-parts.json"), encoding="utf-8"))


def load_csv(name):
    """Only rows that actually have a price are useful to us."""
    with open(os.path.join(DATA, name), encoding="utf-8") as f:
        return [r for r in csv.DictReader(f) if r["price"].strip()]


CSV = {
    "cpu": load_csv("cpu.csv"),
    "gpu": load_csv("video-card.csv"),
    "motherboard": load_csv("motherboard.csv"),
    "ram": load_csv("memory.csv"),
    "storage": load_csv("internal-hard-drive.csv"),
    "psu": load_csv("power-supply.csv"),
    "case": load_csv("case.csv"),
    "cooler": load_csv("cpu-cooler.csv"),
}

# ---------------------------------------------------------------- name / price helpers

STOP = {"edition", "oc", "gb", "with", "and", "the", "w", "nvidia", "amd", "intel",
        "gaming", "black", "2024", "geforce", "radeon"}


def tokens(text):
    text = re.sub(r"\(.*?\)", " ", text.lower()).replace("_", " ")
    return set(re.findall(r"[a-z0-9]+", text)) - STOP


def median_price(rows):
    return round(statistics.median(float(r["price"]) for r in rows), 2) if rows else None


def lowest_price(rows):
    """Same product listed by several sellers: resellers inflate prices, so take the lowest."""
    return min(float(r["price"]) for r in rows) if rows else None


def store_price(raw):
    """Turn a market price into a store-style price: 297.17 -> 296.99, 451.50 -> 451.99."""
    return round(round(raw) - 0.01, 2)


def clean_model(model):
    """Drop manufacturer part numbers in brackets, keep useful ones like (2x16GB) or (Black)."""
    def keep(m):
        inner = m.group(1)
        is_code = "-" in inner or "." in inner or re.fullmatch(r"[A-Z0-9]{5,}", inner)
        return "" if is_code else m.group(0)
    return re.sub(r"\s+", " ", re.sub(r"\s*\(([^)]*)\)", keep, model)).strip()


BRAND = {"NVIDIA AIB": "NVIDIA", "NVIDIA FE": "NVIDIA", "AMD AIB": "AMD", "WD": "Western Digital",
         "Fractal": "Fractal Design"}


def product_name(vendor, model):
    model = clean_model(model).replace("WD_BLACK", "WD Black")
    brand = BRAND.get(vendor, vendor)
    if vendor == "WD":
        return brand, "WD " + model
    if model.startswith("WD ") or model.lower().startswith(brand.lower()):
        return brand, model
    return brand, f"{brand} {model}"


# ---------------------------------------------------------------- performance scores
# All scores are 0-100 and only compared between products of the SAME type.

TIER_BAND = {"entry": (30, 49), "mid": (50, 69), "high": (70, 84), "flagship": (85, 100)}


def tier_scores(items, metric):
    """The dataset's tier picks the band; the spec metric orders parts inside the band."""
    scores = {}
    for tier, (lo, hi) in TIER_BAND.items():
        group = [x for x in items if x["performance_tier"] == tier]
        if not group:
            continue
        vals = [metric(x) for x in group]
        vmin, vmax = min(vals), max(vals)
        for x in group:
            pos = 0.5 if vmax == vmin else (metric(x) - vmin) / (vmax - vmin)
            scores[x["id"]] = round(lo + pos * (hi - lo))
    return scores


CHIPSET_SCORE = {"X870E": 95, "X870": 85, "Z890": 85, "Z790": 80, "B850": 70, "B650": 65,
                 "B860": 60, "B760": 50, "A620A": 35, "A620": 35}
EFFICIENCY_BONUS = {"80+ Bronze": 0, "80+ Gold": 10, "80+ Platinum": 20, "80+ Titanium": 30}

# ---------------------------------------------------------------- catalog building

products = []      # dicts: sku, name, brand, description, price, type, category, warranty, score, stock
specs = []         # (table, sku, {column: value})
report = []        # (sku, name, how the price was found)
STOCK_ZERO = {"amd-ryzen-9-9950x3d2-dual-edition", "gpu-nvidia-asus-rog-astral-rtx5090-o32g"}

WARRANTY = {"CPU": 36, "MOTHERBOARD": 36, "RAM": 60, "GPU": 36, "STORAGE": 60, "PSU": 84,
            "CASE": 24, "COOLER": 36, "LAPTOP": 12, "DESKTOP": 24, "PERIPHERAL": 24, "ACCESSORY": 12}
CATEGORY = {"CPU": "processors", "MOTHERBOARD": "motherboards", "RAM": "memory",
            "GPU": "graphics-cards", "STORAGE": "storage", "PSU": "power-supplies", "CASE": "cases",
            "COOLER": "cpu-coolers", "LAPTOP": "laptops", "DESKTOP": "desktops",
            "PERIPHERAL": "peripherals", "ACCESSORY": "accessories"}
counters = {}


def add_product(ptype, brand, name, description, price, price_note, score, src_id=None):
    counters[ptype] = counters.get(ptype, 0) + 1
    sku = f"FT-{ptype[:4]}-{counters[ptype]:03d}"
    n = len(products)
    stock = 0 if src_id in STOCK_ZERO else 3 + (n * 37) % 38   # deterministic demo stock 3-40
    products.append(dict(sku=sku, name=name, brand=brand, description=description, price=price,
                         type=ptype, category=CATEGORY[ptype], warranty=WARRANTY[ptype],
                         score=score, stock=stock))
    report.append((sku, name, price_note))
    return sku


def priced(raw, note, filled_price):
    """Use the CSV price if we found one, otherwise the hand-filled estimate."""
    if raw is not None:
        return store_price(raw), note
    return filled_price, "FILLED (estimated, not in CSV)"


WORD_FIX = {"4k": "4K", "3d": "3D", "ai": "AI", "llm": "LLM", "itx": "ITX", "sff": "small form factor",
            "am5": "AM5", "fps": "FPS", "rgb": "RGB", "nas": "NAS", "os": "OS",
            "htpc": "HTPC", "vm": "VM", "aio": "AIO", "matx": "mATX"}


def good_for(x):
    """Use cases from the dataset, e.g. "4k-gaming" -> "4K gaming"."""
    if not x.get("use_cases"):
        return ""
    nice = [" ".join(WORD_FIX.get(w.lower(), w) for w in u.replace("-", " ").split()) for u in x["use_cases"]]
    return f" Good for: {', '.join(nice)}."


# ---- CPUs
FILLED_CPU = {  # FILLED: not in the CSV (base clock in GHz, price in USD)
    "amd-ryzen-9-9950x3d2-dual-edition": (4.3, 899.99),
    "amd-ryzen-7-9850x3d": (4.7, 499.99),
}
cpu_scores = tier_scores(J["cpu"], lambda x: x["threads"] * x["boost_ghz"])
for x in J["cpu"]:
    brand, name = product_name(x["vendor"], x["model"])
    rows = [r for r in CSV["cpu"] if tokens(x["vendor"] + " " + x["model"]) <= tokens(r["name"])]
    if rows:
        base = float(rows[0]["core_clock"])
        price, note = priced(lowest_price(rows), f"CSV lowest of {len(rows)}: {rows[0]['name']}", None)
    else:
        base, filled = FILLED_CPU[x["id"]]
        price, note = filled, "FILLED (estimated, not in CSV)"
    igpu = not re.search(r"\d+K?F$", x["model"])           # Intel "F"/"KF" models have no iGPU
    desc = (f"{x['cores']} cores / {x['threads']} threads, {base} GHz base up to {x['boost_ghz']} GHz, "
            f"{x['socket']} socket, {x['tdp_w']} W TDP"
            f"{', integrated graphics' if igpu else ', no integrated graphics'}.{good_for(x)}")
    sku = add_product("CPU", brand, name, desc, price, note, cpu_scores[x["id"]], x["id"])
    specs.append(("cpu_specs", sku, dict(socket=x["socket"], cores=x["cores"], threads=x["threads"],
                  base_clock_ghz=base, boost_clock_ghz=x["boost_ghz"], tdp_watts=x["tdp_w"],
                  integrated_graphics=igpu)))

# ---- GPUs
FILLED_GPU = {"gpu-amd-radeon-rx-9070-gre-12gb": 549.99, "gpu-rtx-5080-fe": 999.99}


def gpu_chipset(model):
    m = re.search(r"(RTX ?\d{4}(?: Ti)?|RX \d{4}(?: XTX| XT| GRE)?|Arc [AB]\d{3})", model)
    chip = m.group(1).replace("RTX", "RTX ").replace("  ", " ")
    return ("GeForce " if chip.startswith("RTX") else "Radeon " if chip.startswith("RX") else "Intel ") + chip


gpu_scores = tier_scores(J["gpu"], lambda x: x["tgp_w"])
for x in J["gpu"]:
    chip = gpu_chipset(x["model"])
    csv_chip = chip.replace("Intel ", "")
    same_chip = [r for r in CSV["gpu"] if r["chipset"] == csv_chip and r["memory"] == str(x["vram_gb"])]
    rows, how = same_chip, "all cards"
    if x["vendor"] == "ASUS":
        series = next(s for s in ["ROG Astral", "TUF", "Prime", "Dual"] if x["model"].startswith(s))
        brand_rows = [r for r in same_chip if r["name"].lower().startswith(("asus " + series).lower())
                      and " LC" not in r["name"]]  # LC = liquid-cooled, a different card
        oc_rows = [r for r in brand_rows if "OC" in r["name"]]
        rows, how = (oc_rows, f"Asus {series} OC") if oc_rows else (brand_rows, f"Asus {series}")
        if not rows:
            rows, how = same_chip, "all cards"
    elif x["vendor"] == "NVIDIA FE":
        fe = [r for r in same_chip if "founders" in r["name"].lower()]
        rows, how = fe, "Founders Edition"  # none in the CSV -> FILLED with NVIDIA list price
    price, note = priced(median_price(rows), f"CSV median of {len(rows)} {how} ({csv_chip} {x['vram_gb']}GB)",
                         FILLED_GPU.get(x["id"]))
    brand, name = product_name(x["vendor"], x["model"])
    if x["vendor"] in ("NVIDIA AIB", "NVIDIA FE"):
        name = "NVIDIA GeForce " + clean_model(x["model"])
    if x["vendor"] == "AMD AIB":
        name = "AMD Radeon " + clean_model(x["model"])
    desc = (f"{chip}, {x['vram_gb']} GB VRAM, {x['tgp_w']} W board power, {round(x['length_mm'])} mm long, "
            f"PCIe {x['pcie']}.{good_for(x)}")
    sku = add_product("GPU", brand, name, desc, price, note, gpu_scores[x["id"]], x["id"])
    specs.append(("gpu_specs", sku, dict(chipset=chip, vram_gb=x["vram_gb"],
                  length_mm=round(x["length_mm"]), tdp_watts=x["tgp_w"])))

# ---- Motherboards (JSON) + two DDR4 boards from the CSV (FILLED m2_slots / chipset)
FORM = {"ATX": "ATX", "Micro-ATX": "MICRO_ATX", "Micro ATX": "MICRO_ATX", "mATX": "MICRO_ATX",
        "Mini-ITX": "MINI_ITX", "Mini ITX": "MINI_ITX", "ITX": "MINI_ITX"}
FILLED_MB = {"mb-asrock-a620am-x": 89.99, "mb-asrock-b860m-x-gen5-wifi": 119.99}
boards = [dict(x, chipset=x["chipset"].replace("AMD ", "").replace("Intel ", "")) for x in J["motherboard"]]
boards += [  # FILLED: DDR4 boards so the RAM-type rule has real DDR4 cases (JSON is DDR5-only)
    dict(id="csv-msi-pro-b760m-p-ddr4", vendor="MSI", model="PRO B760M-P DDR4", socket="LGA1700",
         chipset="B760", form_factor="Micro ATX", ram_type="DDR4", ram_slots=4, ram_max_gb=128,
         m2_slots=2, pcie_gen="Gen4", csv_name="MSI PRO B760M-P DDR4"),
    dict(id="csv-msi-pro-b760-p-wifi-ddr4", vendor="MSI", model="PRO B760-P WIFI DDR4", socket="LGA1700",
         chipset="B760", form_factor="ATX", ram_type="DDR4", ram_slots=4, ram_max_gb=128,
         m2_slots=2, pcie_gen="Gen4", csv_name="MSI PRO B760-P WIFI DDR4"),
]
for x in boards:
    if "csv_name" in x:
        rows = [r for r in CSV["motherboard"] if r["name"] == x["csv_name"]]
    else:
        rows = [r for r in CSV["motherboard"] if tokens(x["vendor"] + " " + x["model"]) <= tokens(r["name"])]
    price, note = priced(lowest_price(rows), f"CSV lowest of {len(rows)}: {rows[0]['name']}" if rows else "",
                         FILLED_MB.get(x["id"]))
    brand, name = product_name(x["vendor"], x["model"])
    ff = FORM[x["form_factor"]]
    desc = (f"{x['socket']} socket, {x['chipset']} chipset, {ff.replace('_', '-').title().replace('Atx', 'ATX').replace('Itx', 'ITX')} "
            f"form factor, {x['ram_slots']}x {x['ram_type']} slots (up to {x['ram_max_gb']} GB), "
            f"{x['m2_slots']}x M.2, PCIe {x['pcie_gen']}.")
    sku = add_product("MOTHERBOARD", brand, name, desc, price, note, CHIPSET_SCORE.get(x["chipset"], 50), x["id"])
    specs.append(("motherboard_specs", sku, dict(socket=x["socket"], chipset=x["chipset"], form_factor=ff,
                  ram_type=x["ram_type"], ram_slots=x["ram_slots"], max_ram_gb=x["ram_max_gb"],
                  m2_slots=x["m2_slots"])))

# ---- RAM (JSON) + two DDR4 kits from the CSV
FILLED_RAM = {"corsair-vengeance-ddr5-6000-cl36-16gb": 69.99,
              "corsair-vengeance-rgb-rs-ddr5-6000-cl40-64gb": 229.99,
              "gskill-trident-z5-ck-rgb-ddr5-8200-cl40-48gb": 299.99,
              "gskill-trident-z5-neo-rgb-ddr5-6000-cl36-128gb": 499.99}
kits = list(J["ram"]) + [
    dict(id="csv-corsair-lpx-16", vendor="Corsair", model="Vengeance LPX 16GB (2x8GB) DDR4-3200 CL16",
         capacity_gb=16, kit="2x8GB", speed_mtps=3200, cas_latency=16, ram_type="DDR4",
         csv_name="Corsair Vengeance LPX 16 GB"),
    dict(id="csv-corsair-lpx-32", vendor="Corsair", model="Vengeance LPX 32GB (2x16GB) DDR4-3200 CL16",
         capacity_gb=32, kit="2x16GB", speed_mtps=3200, cas_latency=16, ram_type="DDR4",
         csv_name="Corsair Vengeance LPX 32 GB"),
]
max_cap = max(k["capacity_gb"] for k in kits)
max_lat = max(k["speed_mtps"] / k["cas_latency"] for k in kits)
for x in kits:
    n, size = x["kit"].lower().replace("gb", "").split("x")
    gen = "4" if x["ram_type"] == "DDR4" else "5"
    match = lambda r: (r["speed"] == f"{gen},{x['speed_mtps']}" and r["modules"] == f"{n},{size}"
                       and r["cas_latency"] == str(x["cas_latency"]))
    if "csv_name" in x:
        rows = [r for r in CSV["ram"] if r["name"] == x["csv_name"] and match(r)]
    else:
        series = re.split(r" \d+GB| DDR5", x["model"])[0]
        rows = [r for r in CSV["ram"] if tokens(x["vendor"] + " " + series) <= tokens(r["name"]) and match(r)]
    price, note = priced(lowest_price(rows), f"CSV lowest of {len(rows)}: {rows[0]['name']}" if rows else "",
                         FILLED_RAM.get(x["id"]))
    brand, name = product_name(x["vendor"], x["model"])
    score = round(100 * (0.6 * x["capacity_gb"] / max_cap + 0.4 * (x["speed_mtps"] / x["cas_latency"]) / max_lat))
    desc = (f"{x['capacity_gb']} GB kit ({n} x {size} GB) {x['ram_type']}-{x['speed_mtps']}, "
            f"CL{x['cas_latency']}.{good_for(x)}")
    sku = add_product("RAM", brand, name, desc, price, note, score, x["id"])
    specs.append(("ram_specs", sku, dict(ram_type=x["ram_type"], capacity_gb=x["capacity_gb"],
                  modules=int(n), speed_mhz=x["speed_mtps"])))

# ---- Storage (JSON SSDs) + two hard drives from the CSV
drives = list(J["storage"]) + [
    dict(id="csv-hdd-barracuda-2tb", vendor="Seagate", model="BarraCuda 2TB 7200 RPM", capacity_gb=2000,
         interface="SATA III 6Gb/s", form_factor="3.5-inch", read_mbps=220, write_mbps=220,
         csv_name="Seagate Barracuda Compute", type="HDD"),
    dict(id="csv-hdd-barracuda-4tb", vendor="Seagate", model="BarraCuda 4TB 5400 RPM", capacity_gb=4000,
         interface="SATA III 6Gb/s", form_factor="3.5-inch", read_mbps=190, write_mbps=190,
         csv_name="Seagate BarraCuda", type="HDD"),
]
max_read = max(d["read_mbps"] for d in drives)
max_storage = max(d["capacity_gb"] for d in drives)
for x in drives:
    if "csv_name" in x:
        rows = [r for r in CSV["storage"] if r["name"] == x["csv_name"] and r["capacity"] == str(x["capacity_gb"])
                and r["type"] != "SSD"]
        stype = "HDD"
    else:
        rows = [r for r in CSV["storage"]
                if tokens(x["vendor"] + " " + x["model"]) - {"1tb", "2tb", "4tb", "8tb", "western", "digital", "wd"}
                <= tokens(r["name"]) and r["capacity"] == str(x["capacity_gb"])]
        stype = "NVME_SSD" if "NVMe" in x["interface"] else "SATA_SSD"
    price, note = priced(lowest_price(rows), f"CSV lowest of {len(rows)}: {rows[0]['name']}" if rows else "", None)
    brand, name = product_name(x["vendor"], x["model"])
    score = round(100 * (0.6 * x["read_mbps"] / max_read + 0.4 * x["capacity_gb"] / max_storage))
    size = f"{x['capacity_gb'] // 1000} TB" if x["capacity_gb"] >= 1000 else f"{x['capacity_gb']} GB"
    desc = (f"{size} {stype.replace('_', ' ').replace('NVME', 'NVMe')}, {x['interface']}, {x['form_factor']}, "
            f"up to {x['read_mbps']} MB/s read.{good_for(x)}")
    sku = add_product("STORAGE", brand, name, desc, price, note, score, x["id"])
    specs.append(("storage_specs", sku, dict(storage_type=stype, capacity_gb=x["capacity_gb"])))

# ---- PSUs
FILLED_PSU = {"psu-thermaltake-toughpower-gt-850w": 139.99, "psu-lianli-sp750-v2-gold": 129.99}
for x in J["psu"]:
    keys = tokens(x["vendor"] + " " + x["model"]) - {"80", "gold", "platinum", "titanium", "atx", "3", "1",
                                                     "750w", "850w", "1000w"}
    rows = [r for r in CSV["psu"] if keys <= tokens(r["name"]) and r["wattage"] == str(x["wattage_w"])]
    price, note = priced(lowest_price(rows), f"CSV lowest of {len(rows)}: {rows[0]['name']}" if rows else "",
                         FILLED_PSU.get(x["id"]))
    brand, name = product_name(x["vendor"], x["model"])
    score = round(70 * x["wattage_w"] / 1600 + EFFICIENCY_BONUS[x["efficiency"]])
    modular = x["modular"].lower().startswith("fully")
    desc = (f"{x['wattage_w']} W, {x['efficiency']}, {x['form_factor']}, "
            f"{'fully modular' if modular else 'non-modular'}.{good_for(x)}")
    sku = add_product("PSU", brand, name, desc, price, note, score, x["id"])
    specs.append(("psu_specs", sku, dict(wattage=x["wattage_w"], efficiency_rating=x["efficiency"], modular=modular)))

# ---- Cases
FILLED_CASE = {"case-o11-d-evo": 179.99, "case-lian-li-vector-v100rx": 99.99}
ORDER = ["MINI_ITX", "MICRO_ATX", "ATX"]
for x in J["case"]:
    rows = [r for r in CSV["case"] if tokens(x["vendor"] + " " + x["model"]) <= tokens(r["name"])]
    price, note = priced(lowest_price(rows), f"CSV lowest of {len(rows)}: {rows[0]['name']}" if rows else "",
                         FILLED_CASE.get(x["id"]))
    brand, name = product_name(x["vendor"], x["model"])
    max_ff = max((FORM[f] for f in x["supports_form_factors"]), key=ORDER.index)
    score = round(100 * (0.5 * x["max_gpu_length_mm"] / 430 + 0.5 * x["max_cpu_cooler_mm"] / 185))
    desc = (f"Fits motherboards up to {max_ff.replace('_', '-').replace('MICRO', 'Micro').replace('MINI', 'Mini')}, "
            f"GPUs up to {x['max_gpu_length_mm']} mm, CPU coolers up to {x['max_cpu_cooler_mm']} mm.{good_for(x)}")
    sku = add_product("CASE", brand, name, desc, price, note, score, x["id"])
    specs.append(("case_specs", sku, dict(max_motherboard_form_factor=max_ff,
                  max_gpu_length_mm=x["max_gpu_length_mm"], max_cooler_height_mm=x["max_cpu_cooler_mm"])))

# ---- CPU coolers: NOT in the JSON. Picked from the CSV (price); height + sockets are FILLED
# from the manufacturers' spec pages (the CSV has neither).
ALL = ["AM4", "AM5", "LGA1700", "LGA1851"]
COOLERS = [  # (csv name, brand, display name, type, height mm or None, sockets, radiator, score)
    ("Thermalright Peerless Assassin 120 SE", "Thermalright", "Thermalright Peerless Assassin 120 SE", "AIR", 155, ALL, None, 72),
    ("Thermalright Phantom Spirit 120 SE", "Thermalright", "Thermalright Phantom Spirit 120 SE", "AIR", 154, ALL, None, 74),
    ("Cooler Master Hyper 212 Black Edition", "Cooler Master", "Cooler Master Hyper 212 Black Edition", "AIR", 159, ALL, None, 60),
    ("Noctua NH-D15 G2", "Noctua", "Noctua NH-D15 G2", "AIR", 168, ALL, None, 88),
    ("be quiet! Dark Rock Pro 5", "be quiet!", "be quiet! Dark Rock Pro 5", "AIR", 168, ALL, None, 85),
    ("Noctua NH-L9A-AM5", "Noctua", "Noctua NH-L9a-AM5 Low-Profile", "AIR", 37, ["AM5"], None, 35),
    ("Noctua NH-L9i-17xx", "Noctua", "Noctua NH-L9i-17xx Low-Profile", "AIR", 37, ["LGA1700", "LGA1851"], None, 35),
    ("ARCTIC Liquid Freezer III Pro 360", "ARCTIC", "ARCTIC Liquid Freezer III Pro 360", "LIQUID", None, ALL, 360, 95),
    ("NZXT Kraken 240", "NZXT", "NZXT Kraken 240", "LIQUID", None, ALL, 240, 82),
]
for csv_name, brand, name, ctype, height, sockets, radiator, score in COOLERS:
    rows = [r for r in CSV["cooler"] if r["name"] == csv_name]
    raw = min(float(r["price"]) for r in rows)
    if ctype == "AIR":
        desc = f"Air cooler, {height} mm tall. Supports {', '.join(sockets)}."
    else:
        desc = f"{radiator} mm all-in-one liquid cooler. Supports {', '.join(sockets)}."
    sku = add_product("COOLER", brand, name, desc, store_price(raw), f"CSV lowest of {len(rows)} (height/sockets FILLED)", score)
    specs.append(("cooler_specs", sku, dict(cooler_type=ctype, height_mm=height, sockets=sockets)))

# ---------------------------------------------------------------- pre-built desktops
# FutureTech house builds made from catalog parts, so their specs are exact by construction.
# Price = sum of part prices + 99.99 assembly, rounded to .99.

by_name = {p["name"]: p for p in products}
spec_of = {sku: (table, cols) for table, sku, cols in specs}


def part(name):
    if name not in by_name:  # helpful error when a build references a renamed part
        import difflib
        raise KeyError(f"{name!r} not found; close: {difflib.get_close_matches(name, by_name, 3, 0.4)}")
    return by_name[name]


BUILDS = [
    ("FutureTech Forge Office", "Quiet everyday desktop for office work, school and streaming.",
     ["AMD Ryzen 5 8600G", "ASRock A620AM-X", "Corsair Vengeance DDR5 16GB (2x8GB) 6000MT/s CL36",
      "WD Black SN7100 1TB", "Cooler Master MWE Gold 850 V3", "Cooler Master MasterBox Q300L",
      "Thermalright Peerless Assassin 120 SE"], None),
    ("FutureTech Forge Gamer", "Entry gaming desktop for high settings at 1080p.",
     ["AMD Ryzen 5 7600", "MSI B650 Tomahawk WiFi", "G.Skill Flare X5 DDR5 32GB (2x16GB) 6000MT/s CL36",
      "WD Black SN7100 1TB", "Cooler Master MWE Gold 850 V3", "NZXT H5 Flow",
      "Thermalright Peerless Assassin 120 SE"], "ASUS Dual GeForce RTX 5060 8GB GDDR7 OC Edition"),
    ("FutureTech Forge Pro", "Mid-range gaming desktop, the 1440p sweet spot.",
     ["AMD Ryzen 5 9600X", "ASUS TUF Gaming B850-PLUS WiFi", "Corsair Vengeance DDR5 32GB (2x16GB) 6000MT/s CL36",
      "WD Black SN850X 2TB", "MSI MPG A850GS PCIE5", "Corsair FRAME 4000D RS ARGB",
      "Thermalright Phantom Spirit 120 SE"], "NVIDIA GeForce RTX 5060 Ti 16GB"),
    ("FutureTech Forge Elite", "High-end gaming desktop for 1440p ultra and 4K.",
     ["AMD Ryzen 7 9800X3D", "MSI B850 Edge WiFi", "G.Skill Trident Z5 Neo 32GB (2x16GB) DDR5-6000 CL30",
      "WD Black SN850X 2TB", "Corsair RM850x (2024) 850W 80+ Gold", "NZXT H7 Flow (2024)",
      "ARCTIC Liquid Freezer III Pro 360"], "NVIDIA GeForce RTX 5070 Ti 16GB"),
    ("FutureTech Forge Creator", "Workstation for video editing, 3D and heavy multitasking.",
     ["Intel Core Ultra 9 285K", "MSI Z890 Tomahawk WiFi", "G.Skill Flare X5 DDR5 64GB (2x32GB) 6000MT/s CL30",
      "Samsung 9100 PRO 4TB", "Corsair HX1000i 1000W 80+ Platinum", "Lian Li O11 Dynamic EVO RGB",
      "ARCTIC Liquid Freezer III Pro 360"], "NVIDIA GeForce RTX 5080 Founders Edition"),
]


def check_build(cpu, mb, ram, gpu, psu, case, cooler):
    """Same checks the Java rule engine will do later; fails loudly if a house build is invalid."""
    c, m, r, p, k, co = (spec_of[x["sku"]][1] for x in (cpu, mb, ram, psu, case, cooler))
    g = spec_of[gpu["sku"]][1] if gpu else None
    assert c["socket"] == m["socket"] and c["socket"] in co["sockets"], "socket"
    assert m["ram_type"] == r["ram_type"], "ram type"
    assert ORDER.index(m["form_factor"]) <= ORDER.index(k["max_motherboard_form_factor"]), "case size"
    assert co["height_mm"] is None or co["height_mm"] <= k["max_cooler_height_mm"], "cooler height"
    assert g is None or g["length_mm"] <= k["max_gpu_length_mm"], "gpu length"
    need = (c["tdp_watts"] + (g["tdp_watts"] if g else 0) + 150) * 1.2
    assert p["wattage"] >= need, "psu wattage"


for name, blurb, parts_, gpu_name in BUILDS:
    cpu, mb, ram, ssd, psu, case, cooler = (part(n) for n in parts_)
    gpu = part(gpu_name) if gpu_name else None
    check_build(cpu, mb, ram, gpu, psu, case, cooler)
    used = [cpu, mb, ram, ssd, psu, case, cooler] + ([gpu] if gpu else [])
    price = store_price(sum(x["price"] for x in used) + 99.99)
    gpu_label = gpu["name"] if gpu else "Integrated AMD Radeon 760M"
    ram_spec, ssd_spec = spec_of[ram["sku"]][1], spec_of[ssd["sku"]][1]
    score = round((cpu["score"] + (gpu["score"] if gpu else 20)) / 2)
    desc = (f"{blurb} {cpu['name']}, {gpu_label}, {ram_spec['capacity_gb']} GB RAM, "
            f"{ssd_spec['capacity_gb'] // 1000} TB SSD. Assembled and tested by FutureTech.")
    sku = add_product("DESKTOP", "FutureTech", name, desc, price, "sum of parts + 99.99 assembly", score)
    specs.append(("computer_specs", sku, dict(cpu_model=cpu["name"], gpu_model=gpu_label,
                  ram_gb=ram_spec["capacity_gb"], storage_gb=ssd_spec["capacity_gb"],
                  screen_size_inches=None, operating_system="Windows 11 Home")))

# ---------------------------------------------------------------- hand-added demo items
# FILLED: the zip has no laptops, peripherals or accessories. Prices are approximate US prices.
LAPTOPS = [  # brand, name, price, score, cpu, gpu, ram, storage, screen, os, blurb
    ("Apple", "Apple MacBook Air 13 (M4)", 999.99, 55, "Apple M4 (10-core CPU)", "Apple M4 8-core GPU",
     16, 256, 13.6, "macOS", "Thin, fanless and silent with all-day battery life."),
    ("Apple", "Apple MacBook Pro 14 (M4)", 1599.99, 65, "Apple M4 (10-core CPU)", "Apple M4 10-core GPU",
     16, 512, 14.2, "macOS", "Pro laptop for creative work with a Liquid Retina XDR display."),
    ("ASUS", "ASUS ROG Zephyrus G14 (2025)", 2199.99, 85, "AMD Ryzen AI 9 HX 370",
     "NVIDIA GeForce RTX 5070 Ti Laptop GPU", 32, 1000, 14.0, "Windows 11 Home",
     "Compact 14-inch gaming laptop with an OLED display."),
    ("Acer", "Acer Nitro V 15", 799.99, 45, "Intel Core i5-13420H", "NVIDIA GeForce RTX 4050 Laptop GPU",
     16, 512, 15.6, "Windows 11 Home", "Budget gaming laptop for 1080p games and school."),
]
for brand, name, price, score, cpu, gpu, ram, storage, screen, os_, blurb in LAPTOPS:
    size = f"{storage // 1000} TB" if storage >= 1000 else f"{storage} GB"
    desc = f"{blurb} {cpu}, {gpu}, {ram} GB RAM, {size} SSD, {screen}-inch screen."
    sku = add_product("LAPTOP", brand, name, desc, price, "FILLED (hand-added demo item)", score)
    specs.append(("computer_specs", sku, dict(cpu_model=cpu, gpu_model=gpu, ram_gb=ram, storage_gb=storage,
                  screen_size_inches=screen, operating_system=os_)))

EXTRAS = [  # type, brand, name, price, description
    ("PERIPHERAL", "Logitech", "Logitech G502 X Gaming Mouse", 79.99, "Wired gaming mouse with HERO 25K sensor and hybrid optical-mechanical switches."),
    ("PERIPHERAL", "Razer", "Razer DeathAdder V3 Gaming Mouse", 69.99, "Ultra-light ergonomic wired esports mouse."),
    ("PERIPHERAL", "Logitech", "Logitech MX Keys S Keyboard", 109.99, "Wireless full-size keyboard with smart backlighting, multi-device."),
    ("PERIPHERAL", "HyperX", "HyperX Cloud III Gaming Headset", 99.99, "Wired gaming headset with 53 mm drivers and detachable mic."),
    ("PERIPHERAL", "SteelSeries", "SteelSeries Arctis Nova 7 Wireless Headset", 179.99, "Wireless gaming headset with 2.4 GHz and Bluetooth."),
    ("PERIPHERAL", "Logitech", "Logitech C920s HD Pro Webcam", 69.99, "1080p webcam with privacy shutter and stereo mics."),
    ("ACCESSORY", "ARCTIC", "ARCTIC MX-6 Thermal Paste (4 g)", 8.99, "High-performance thermal compound for CPU coolers."),
    ("ACCESSORY", "SteelSeries", "SteelSeries QcK Large Mouse Pad", 14.99, "450 x 400 mm cloth gaming mouse pad."),
    ("ACCESSORY", "TP-Link", "TP-Link Archer TX55E Wi-Fi 6 PCIe Adapter", 39.99, "Wi-Fi 6 and Bluetooth 5.2 desktop expansion card."),
    ("ACCESSORY", "Samsung", "Samsung T7 Shield 1TB Portable SSD", 109.99, "Rugged USB 3.2 portable SSD, up to 1,050 MB/s."),
]
for ptype, brand, name, price, desc in EXTRAS:
    add_product(ptype, brand, name, desc, price, "FILLED (hand-added demo item)", None)

# ---------------------------------------------------------------- SQL output


def sql(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, (int, float)):
        return repr(v)
    return "'" + str(v).replace("'", "''") + "'"


CATEGORIES = [
    ("Processors", "processors", "Desktop CPUs from AMD and Intel."),
    ("Motherboards", "motherboards", "AM5, LGA1851 and LGA1700 motherboards."),
    ("Memory", "memory", "DDR5 and DDR4 desktop RAM kits."),
    ("Graphics Cards", "graphics-cards", "NVIDIA, AMD and Intel graphics cards."),
    ("Storage", "storage", "NVMe SSDs, SATA SSDs and hard drives."),
    ("Power Supplies", "power-supplies", "ATX and SFX power supplies."),
    ("Cases", "cases", "ATX, Micro-ATX and Mini-ITX PC cases."),
    ("CPU Coolers", "cpu-coolers", "Air and liquid CPU coolers."),
    ("Laptops", "laptops", "Laptops for work, study and gaming."),
    ("Desktops", "desktops", "Pre-built FutureTech desktops, assembled and tested."),
    ("Peripherals", "peripherals", "Mice, keyboards, headsets and webcams."),
    ("Accessories", "accessories", "Thermal paste, mouse pads, adapters and more."),
]

out = [
    "-- V2: demo catalog data. GENERATED by tools/seed/build_seed_sql.py - do not edit by hand;",
    "-- change the script and regenerate instead (and only before this migration has run anywhere",
    "-- that matters; after that, add a V3 migration).",
    "--",
    "-- Part specifications: " + J["_meta"]["attribution_required"],
    "-- Prices: approximate US street prices taken from the bundled CSV price lists, or estimated.",
    "-- Details and the list of hand-filled values: docs/data-sources.md",
    "",
    "INSERT INTO categories (name, slug, description) VALUES",
    ",\n".join(f"    ({sql(n)}, {sql(s)}, {sql(d)})" for n, s, d in CATEGORIES) + ";",
    "",
    "-- Products. category_id is looked up by slug so we never depend on generated ids.",
    "INSERT INTO products (sku, name, brand, description, image_url, price, stock_quantity, category_id,",
    "                      product_type, warranty_months, performance_score, active, created_at) VALUES",
]
rows = []
for p in products:
    rows.append(f"    ({sql(p['sku'])}, {sql(p['name'])}, {sql(p['brand'])}, {sql(p['description'])}, NULL, "
                f"{p['price']:.2f}, {p['stock']}, (SELECT id FROM categories WHERE slug = {sql(p['category'])}), "
                f"{sql(p['type'])}, {p['warranty']}, {sql(p['score'])}, TRUE, NOW())")
out.append(",\n".join(rows) + ";")

by_table = {}
for table, sku, cols in specs:
    by_table.setdefault(table, []).append((sku, cols))
for table, entries in by_table.items():
    cols = [c for c in entries[0][1] if c != "sockets"]
    out += ["", f"INSERT INTO {table} (product_id, {', '.join(cols)}) VALUES"]
    out.append(",\n".join(
        f"    ((SELECT id FROM products WHERE sku = {sql(sku)}), {', '.join(sql(v[c]) for c in cols)})"
        for sku, v in entries) + ";")
    if table == "cooler_specs":
        out += ["", "INSERT INTO cooler_supported_sockets (cooler_id, socket) VALUES"]
        out.append(",\n".join(f"    ((SELECT id FROM products WHERE sku = {sql(sku)}), {sql(s)})"
                              for sku, v in entries for s in v["sockets"]) + ";")

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(out) + "\n")

# ---------------------------------------------------------------- console report
print(f"Wrote {OUT}")
counts = {}
for p in products:
    counts[p["type"]] = counts.get(p["type"], 0) + 1
print("Products per type:", counts, "total", len(products))
for sku, name, note in report:
    price = next(p["price"] for p in products if p["sku"] == sku)
    print(f"{sku:15} {price:>9.2f}  {name[:55]:55}  {note}")
