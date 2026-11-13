# Demo data sources

The catalog in `backend/src/main/resources/db/migration/V2__seed_data.sql` is **generated** by
`tools/seed/build_seed_sql.py` from the raw files in `data_import/` (git-ignored, not in the repo).

To regenerate, put `pc part db.zip` in `data_import/`, extract it to `data_import/extracted/`, and run:

```
py -I tools/seed/build_seed_sql.py
```

## Sources

| Source | Used for | License |
|---|---|---|
| `pc-builder-parts.json`: TechFuelHQ PC Builder Parts Dataset v2.1.1 | All CPU, GPU, motherboard, RAM, storage, PSU and case **specs** (126 parts), performance tiers, "Good for" use cases | CC BY 4.0, attribution below |
| `*.csv` price lists (cpu, video-card, motherboard, memory, internal-hard-drive, power-supply, case, cpu-cooler) | **Prices**, CPU base clocks, and the extra parts listed below | No license file in the zip. Only facts (prices, specs) are used. |
| Hand-filled ("FILLED" in the script) | Gaps neither source covers | n/a |

**Attribution (required by CC BY 4.0):** TechFuelHQ PC Builder Parts Dataset (CC BY 4.0).
https://techfuelhq.com/tools/pc-builder/. Changes: renamed and reformatted fields, removed
part numbers from names, added prices, generated descriptions and performance scores.

## Cleaning rules

- **Prices:**
  - Same product listed by several sellers: take the **lowest** listing, because resellers inflate prices.
  - Generic GPUs (e.g. "RTX 5060 Ti 16GB"): take the **median** over all cards with that chip and VRAM.
  - Every price is turned into a store-style price, e.g. 297.17 becomes 296.99.
- **Rows without a price** in the CSV (about 75% of them) and exact duplicates are ignored.
- **Names:** manufacturer part numbers in brackets are removed, e.g. `(CMK32GX5M2E6000Z36)`.
  Brands are normalized: "NVIDIA AIB" becomes "NVIDIA", "WD" becomes "Western Digital".
- **Enums:**
  - Form factors: "Micro-ATX" / "mATX" become `MICRO_ATX`.
  - Chipsets: "AMD B850" becomes `B850`.
  - Storage interface: "NVMe" becomes `NVME_SSD`, "SATA" becomes `SATA_SSD`.
- **Case max form factor:** the largest entry in the dataset's `supports_form_factors`.
- **Integrated graphics:** Intel models ending in "F" / "KF" have none; all others do.

## Performance score (0-100, compared only within the same product type)

| Type | Formula |
|---|---|
| CPU, GPU | The dataset's tier sets the band: entry 30-49, mid 50-69, high 70-84, flagship 85-100. Inside a band, parts are ordered by threads × boost GHz (CPU) or board power (GPU). |
| RAM | 60% capacity + 40% speed/latency (MT/s ÷ CL), each relative to the best kit |
| Storage | 60% read speed + 40% capacity, each relative to the best drive |
| PSU | 70 × wattage / 1600, plus an efficiency bonus (Gold +10, Platinum +20, Titanium +30) |
| Case | Average of GPU clearance (relative to 430 mm) and cooler clearance (relative to 185 mm) |
| Motherboard | Fixed value per chipset (X870E 95 … A620 35) |
| Cooler | Hand-set: 360 mm liquid 95, big dual-tower air about 85, single tower about 72, low-profile 35 |
| Desktop | Average of its CPU and GPU scores |
| Laptop | Hand-set estimate |
| Peripherals and accessories | None |

## Hand-filled values (verify before the demo)

**Estimated prices** (product missing from the CSV):

| Product | Price |
|---|---|
| Ryzen 9 9950X3D2 Dual Edition | 899.99 |
| Ryzen 7 9850X3D | 499.99 |
| RX 9070 GRE 12GB | 549.99 |
| RTX 5080 Founders Edition (NVIDIA list price) | 999.99 |
| ASRock A620AM-X | 89.99 |
| ASRock B860M-X Gen5 WiFi | 119.99 |
| Corsair Vengeance DDR5 16GB | 69.99 |
| Corsair Vengeance RGB RS 64GB | 229.99 |
| G.Skill Trident Z5 CK 48GB 8200 | 299.99 |
| G.Skill Trident Z5 Neo RGB 128GB | 499.99 |
| Thermaltake Toughpower GT 850W | 139.99 |
| Lian Li SP750 V2 | 129.99 |
| Lian Li O11 Dynamic EVO RGB | 179.99 |
| Lian Li Vector V100RX | 99.99 |

**Estimated specs:**
- CPU base clocks: Ryzen 9 9950X3D2 4.3 GHz and Ryzen 7 9850X3D 4.7 GHz.
- Stock quantities are generated demo values from 3 to 40.
- Two products are deliberately out of stock: Ryzen 9 9950X3D2 and ROG Astral 5090.

**Extra parts the JSON lacks** (price from the CSV, specs filled from the manufacturers' spec sheets):
- **DDR4 parts**, so the RAM-type rule has real cases (the JSON is DDR5 only):
  - MSI PRO B760M-P DDR4 and MSI PRO B760-P WIFI DDR4 (M.2 slots and chipset filled)
  - Corsair Vengeance LPX 16GB and 32GB DDR4-3200 kits
- **Hard drives:** Seagate BarraCuda 2TB and 4TB. Read speeds (about 220 and 190 MB/s) are filled.
- **CPU coolers (9):** the CSV has price only. Height and supported sockets are filled from the
  manufacturers' spec sheets, e.g. Peerless Assassin 120 SE 155 mm, NH-D15 G2 168 mm,
  NH-L9a-AM5 37 mm (AM5 only).

**Hand-added demo items** (the zip has none; prices are approximate US prices):
- 4 laptops: MacBook Air 13 M4, MacBook Pro 14 M4, ROG Zephyrus G14 (2025), Acer Nitro V 15
- 6 peripherals and 4 accessories

**FutureTech house desktops (5):**
- Built only from catalog parts, so their specs are exact.
- Price = sum of the parts + 99.99 assembly.
- The script checks each build against the compatibility rules and stops if one is invalid.

## V3 values (PSU size and radiator fit)

`V3__psu_size_and_radiator_fit.sql` adds these values by hand:

- **PSU form factor:** Lian Li SP750 V2 is `SFX`. This comes from the JSON's `form_factor`; all others are ATX.
- **Case max PSU:** Fractal Ridge and Terra take `SFX` only; all other cases take ATX.
- **Cooler radiator:** ARCTIC Liquid Freezer III Pro 360 = 360 mm, NZXT Kraken 240 = 240 mm.
- **Case max radiator** (filled from spec sheets, **verify**; the zip has no radiator data):

| Max radiator | Cases |
|---|---|
| 360 mm | NZXT H7 Flow, Meshify 2 Compact, O11 Dynamic EVO RGB, FRAME 4000D, NZXT H5 Flow, Vector V100RX, View 270 Plus, Montech XR, Pure Base 501, Lian Li A3-mATX |
| 280 mm | Pop Air Mini, NR200 |
| 240 mm | MasterBox Q300L |
| 0 (none) | Ridge, Terra |

## Known data limitations

- **Montech XR:** the dataset lists ATX support only. We store "max ATX", which also allows
  smaller boards. That is very likely correct for the real case.
- **CPU ↔ chipset support** (`supported_chipsets` in the JSON) is not stored. It isn't one of
  the six ForgeAI rules.
- **Prices are market snapshots.** Some older parts cost more than newer ones (e.g. Ryzen 9
  7950X3D 768.99 vs 9950X3D 649.99). That is realistic, and it gives the budget optimizer
  bad-value parts to catch.
- **No product images** (`image_url` is NULL). We avoid redistributing copyrighted images.
