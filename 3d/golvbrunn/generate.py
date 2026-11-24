"""Spiralgaller (golvbrunnsgaller) – genererar STL för 3D-utskrift.

Mått: Ø170 mm. Ytterkant 2 mm hög, gallerområdet + några mm in på
den släta kanten 4 mm högt (45° fas ner till 2 mm).
Kör:  python3 generate.py   -> spiral_galler_170mm.stl + preview.png
"""
import math
import numpy as np
from shapely.geometry import LineString
from shapely.ops import unary_union
import manifold3d as m3d
import trimesh

# ---- Parametrar (mm) -------------------------------------------------------
D_OUTER = 170.0      # total diameter
H_RIM = 2.0          # höjd på yttre kanten
H_GRILLE = 4.0       # höjd på gallret
RAISED_R = 75.0      # radie där 4 mm-platån slutar (överkant)
CHAMFER = 2.0        # fasbredd från 4 mm ner till 2 mm
N_RINGS = 8          # antal ringar med hål
HUB_R = 4.0          # radie på centrumnav
SLOT_W = 6.0         # hålens bredd (radiellt)
WALL = 2.5           # väggtjocklek mellan hål
SLOT_TARGET = 19.0   # ungefärlig håll-längd längs bågen
TWIST_DEG = 14.0     # vridning per ring -> spiraleffekt
SEG = 64             # upplösning för rundade ändar

def slots():
    polys = []
    pitch_r = SLOT_W + WALL
    for i in range(N_RINGS):
        r_in = HUB_R + i * pitch_r
        rm = r_in + SLOT_W / 2
        n = 3 if i == 0 else max(3, round(2 * math.pi * rm / (SLOT_TARGET + WALL)))
        step = 2 * math.pi / n
        phase = math.radians(TWIST_DEG * i) + (step / 2 if i % 2 else 0)
        trim = (WALL / 2 + SLOT_W / 2) / rm  # vinkel så att väggen blir WALL tjock
        for k in range(n):
            a0 = phase + k * step + trim
            a1 = phase + (k + 1) * step - trim
            ts = np.linspace(a0, a1, 40)
            line = LineString(np.c_[rm * np.cos(ts), rm * np.sin(ts)])
            polys.append(line.buffer(SLOT_W / 2, quad_segs=SEG // 4, cap_style="round"))
    return unary_union(polys)

def body():
    R = D_OUTER / 2
    profile = [(0, 0), (R, 0), (R, H_RIM),
               (RAISED_R + CHAMFER, H_RIM), (RAISED_R, H_GRILLE), (0, H_GRILLE)]
    return m3d.Manifold.revolve(m3d.CrossSection([np.array(profile, float)]), circular_segments=360)

def main():
    holes = slots()
    geoms = holes.geoms if hasattr(holes, "geoms") else [holes]
    cs = m3d.CrossSection([np.array(g.exterior.coords[:-1]) for g in geoms],
                          m3d.FillRule.NonZero)
    cutter = m3d.Manifold.extrude(cs, H_GRILLE + 2).translate([0, 0, -1])
    part = body() - cutter
    mesh = part.to_mesh()
    tm = trimesh.Trimesh(mesh.vert_properties[:, :3], mesh.tri_verts)
    tm.export("spiral_galler_170mm.stl")
    print("watertight:", tm.is_watertight, "volym cm3:", round(tm.volume / 1000, 1),
          "PETG ca g:", round(tm.volume / 1000 * 1.27, 1), "bbox:", tm.extents.round(2))

    import matplotlib; matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(figsize=(7, 7))
    ax.add_patch(plt.Circle((0, 0), D_OUTER / 2, color="#ddd"))
    ax.add_patch(plt.Circle((0, 0), RAISED_R, color="#bbb"))
    for g in geoms:
        x, y = g.exterior.xy
        ax.fill(x, y, color="white", ec="k", lw=0.5)
    ax.set_aspect("equal"); ax.set_xlim(-88, 88); ax.set_ylim(-88, 88); ax.axis("off")
    fig.savefig("preview.png", dpi=110, bbox_inches="tight")

if __name__ == "__main__":
    main()
