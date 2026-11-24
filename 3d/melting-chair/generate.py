"""
Melting monobloc chair – SDF model -> watertight mesh (STL + 3MF) for Bambu Studio.
Units: mm. Z up, front of chair = -Y. Flat puddle feet sit on Z=0.
Run:  pip install numpy scipy scikit-image trimesh fast-simplification lxml networkx
      python generate.py
"""
import numpy as np
from skimage.measure import marching_cubes
import trimesh, fast_simplification, os

RES = 0.32           # voxel size (mm)
TARGET_FACES = 450_000
OUT = os.path.dirname(os.path.abspath(__file__))

# ------------------------------------------------------------------ grid
BMIN = np.array([-64.0, -64.0, 0.0])
BMAX = np.array([64.0, 56.0, 128.0])
xs = np.arange(BMIN[0] - 2, BMAX[0] + 2, RES, dtype=np.float32)
ys = np.arange(BMIN[1] - 2, BMAX[1] + 2, RES, dtype=np.float32)
zs = np.arange(BMIN[2] - 2, BMAX[2] + 2, RES, dtype=np.float32)
FAR = 20.0
field = np.full((len(xs), len(ys), len(zs)), FAR, dtype=np.float32)
MARGIN = 8.0  # > largest smoothing radius

def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b + (a - b) * h - k * h * (1 - h)

def smax(a, b, k):
    return -smin(-a, -b, k)

def region(lo, hi):
    lo = np.asarray(lo) - MARGIN; hi = np.asarray(hi) + MARGIN
    i0 = [max(0, int(np.searchsorted(g, l))) for g, l in zip((xs, ys, zs), lo)]
    i1 = [int(np.searchsorted(g, h)) for g, h in zip((xs, ys, zs), hi)]
    sl = tuple(slice(a, b) for a, b in zip(i0, i1))
    X, Y, Z = np.meshgrid(xs[sl[0]], ys[sl[1]], zs[sl[2]], indexing="ij")
    return sl, X, Y, Z

def add(fn, lo, hi, k=2.0):
    sl, X, Y, Z = region(lo, hi)
    if X.size == 0: return
    field[sl] = smin(field[sl], fn(X, Y, Z), k)

def sub(fn, lo, hi, k=1.0):
    sl, X, Y, Z = region(lo, hi)
    if X.size == 0: return
    field[sl] = smax(field[sl], -fn(X, Y, Z), k)

# ------------------------------------------------------------------ primitives
def cone_capsule(a, b, ra, rb):
    a = np.asarray(a, np.float32); b = np.asarray(b, np.float32)
    ba = b - a; bb = float(ba @ ba)
    def f(X, Y, Z):
        px, py, pz = X - a[0], Y - a[1], Z - a[2]
        h = np.clip((px * ba[0] + py * ba[1] + pz * ba[2]) / bb, 0, 1)
        dx, dy, dz = px - ba[0] * h, py - ba[1] * h, pz - ba[2] * h
        return np.sqrt(dx * dx + dy * dy + dz * dz) - (ra + (rb - ra) * h)
    r = max(ra, rb)
    return f, np.minimum(a, b) - r, np.maximum(a, b) + r

def sphere(c, r, sz=1.0):
    """sphere, optionally squashed in z by factor sz (>1 = flatter)"""
    c = np.asarray(c, np.float32)
    def f(X, Y, Z):
        return (np.sqrt((X - c[0])**2 + (Y - c[1])**2 + ((Z - c[2]) * sz)**2) - r) / sz
    return f, c - r, c + r

def round_box(c, h, r):
    c = np.asarray(c, np.float32); h = np.asarray(h, np.float32)
    def f(X, Y, Z):
        qx = np.abs(X - c[0]) - h[0] + r
        qy = np.abs(Y - c[1]) - h[1] + r
        qz = np.abs(Z - c[2]) - h[2] + r
        out = np.sqrt(np.maximum(qx, 0)**2 + np.maximum(qy, 0)**2 + np.maximum(qz, 0)**2)
        return out + np.minimum(np.maximum(qx, np.maximum(qy, qz)), 0) - r
    return f, c - h, c + h

def catmull(pts, n=10):
    pts = [np.asarray(p, float) for p in pts]
    P = [pts[0]] + pts + [pts[-1]]
    out = []
    for i in range(1, len(P) - 2):
        p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
        for t in np.linspace(0, 1, n, endpoint=False):
            t2, t3 = t * t, t * t * t
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                              + (-p0 + 3 * p1 - 3 * p2 + p3) * t3))
    out.append(pts[-1])
    return np.array(out)

def tube(pts, radii, k=1.5, n=8):
    path = catmull(pts, n)
    rr = np.interp(np.linspace(0, 1, len(path)), np.linspace(0, 1, len(radii)), radii)
    for i in range(len(path) - 1):
        add(*cone_capsule(path[i], path[i + 1], rr[i], rr[i + 1]), k=k)

def drip(top, length, r, lean=(0.0, 0.0)):
    """Teardrop drip hanging from `top`: thin neck, fat rounded bulb at the end."""
    top = np.asarray(top, float)
    mid = top + np.array([lean[0] * 0.5, lean[1] * 0.5, -length * 0.62])
    end = top + np.array([lean[0], lean[1], -length])
    add(*cone_capsule(top + [0, 0, 1.5], mid, r * 1.25, r * 0.80), k=1.8)
    add(*cone_capsule(mid, end, r * 0.80, r * 1.05), k=1.2)
    add(*sphere(end - [0, 0, r * 0.15], r * 1.18, sz=0.92), k=1.2)

# ------------------------------------------------------------------ backrest frame
TILT = np.radians(12.0)
O = np.array([0.0, 28.0, 59.0])
U = np.array([0.0, np.sin(TILT), np.cos(TILT)])     # up along the back
N = np.array([0.0, np.cos(TILT), -np.sin(TILT)])    # back-normal (towards +Y)
R = 70.0                                            # horizontal wrap radius

def back_pt(x, zl, off=0.0):
    yl = -(R - np.sqrt(R * R - x * x)) + off
    return O + np.array([x, 0, 0]) + yl * N + zl * U

def back_shell(X, Y, Z):
    qy, qz = Y - O[1], Z - O[2]
    zl = qy * U[1] + qz * U[2]
    yl = qy * N[1] + qz * N[2]
    d = np.abs(np.sqrt(X * X + (yl + R)**2) - R) - 2.1
    d = np.maximum(d, np.abs(X) - 39.5)
    d = np.maximum(d, zl - 66.0)
    d = np.maximum(d, -zl - 3.0)
    return d

def back_slot(xc, hw, z0, z1):
    def f(X, Y, Z):
        qy, qz = Y - O[1], Z - O[2]
        zl = qy * U[1] + qz * U[2]
        # rounded-rectangle (stadium) in the (x, zl) plane, extruded through the shell
        cz = np.clip(zl, z0 + hw, z1 - hw)
        return np.sqrt((X - xc)**2 + (zl - cz)**2) - hw
    p0, p1 = back_pt(xc, z0), back_pt(xc, z1)
    return f, np.minimum(p0, p1) - [hw, 8, 3], np.maximum(p0, p1) + [hw, 8, 3]

# ------------------------------------------------------------------ build: seat
SEAT_Z = 58.0
add(*round_box((0, -5, SEAT_Z), (43, 34, 2.6), 2.0), k=1.0)                 # seat plate
add(*round_box((0, 0, SEAT_Z + 1.2), (30, 22, 0.8), 0.8), k=6.0)            # slight melt-bulge on top
tube([(-43, -38, SEAT_Z), (0, -39.5, SEAT_Z - 0.6), (43, -38, SEAT_Z)], [4.3, 4.6, 4.3], k=2.5)   # front roll
for s in (-1, 1):
    tube([(s * 44, -36, SEAT_Z), (s * 44.8, -5, SEAT_Z - 0.4), (s * 43.5, 27, SEAT_Z)], [3.6, 3.9, 3.6], k=2.5)
add(*round_box((0, -38.5, SEAT_Z - 5.5), (40, 2.4, 4.5), 2.0), k=3.0)        # sagging front apron
for s in (-1, 1):
    add(*round_box((s * 44.5, -6, SEAT_Z - 4.5), (2.2, 26, 3.5), 1.8), k=3.0)  # side aprons

# backrest shell
lo = np.minimum(back_pt(-40, -4), back_pt(40, 68)) - [0, 14, 0]
hi = np.maximum(back_pt(-40, -4), back_pt(40, 68)) + [0, 4, 0]
add(back_shell, lo, hi, k=4.0)

# seat slots (transverse, near the back half like the original)
for yc, hw in ((-17, 24), (-7.5, 27), (2, 28), (11.5, 27)):
    sub(*round_box((0, yc, SEAT_Z), (hw, 1.7, 10), 1.69), k=0.9)
# backrest slots (5 vertical)
for xc in (-24, -12, 0, 12, 24):
    sub(*back_slot(xc, 2.7, 12.0, 58.0), k=0.9)

# top rail + side edges of the backrest
tube([back_pt(x, 65.0) for x in np.linspace(-39, 39, 9)], [4.6, 4.8, 5.0, 4.9, 4.8, 4.9, 5.0, 4.8, 4.6], k=2.0)
for s in (-1, 1):
    tube([back_pt(s * 39.5, 0), back_pt(s * 39.5, 32), back_pt(s * 39.5, 63)], [3.6, 3.3, 3.8], k=2.0)

# arms: continuous loop from back top corner -> armrest -> front post into seat
for s in (-1, 1):
    tc = back_pt(s * 39, 65.0)
    pts = [tc, (s * 46, 27, 117), (s * 50, 19, 105), (s * 51, 9, 92), (s * 50.5, -3, 85),
           (s * 50, -18, 84), (s * 49, -30, 83.5), (s * 48, -36.5, 79), (s * 47, -38, 70),
           (s * 45.5, -37, 61), (s * 44.5, -35, 56)]
    tube(pts, [4.8, 4.6, 4.4, 4.4, 4.6, 4.7, 4.7, 4.8, 4.9, 5.2, 5.4], k=2.2, n=10)

# legs (splayed, tapered) + puddle feet
LEGS = {}
for s in (-1, 1):
    LEGS[(s, "f")] = ((s * 43, -34, 56), (s * 50.5, -46.5, 2.5))
    LEGS[(s, "b")] = ((s * 41.5, 23, 56), (s * 49.5, 40.5, 2.5))
rng = np.random.default_rng(7)
for (s, fb), (a, b) in LEGS.items():
    add(*cone_capsule(a, b, 5.6, 4.3), k=3.0)
    # puddle: a few flattened lobes, merged with a big smoothing radius -> melted flare
    bx, by = b[0], b[1]
    add(*sphere((bx, by, 0.4), 8.5, sz=3.2), k=4.0)
    for j in range(6):
        ang = rng.uniform(0, 2 * np.pi); dist = rng.uniform(5.0, 9.0)
        add(*sphere((bx + dist * np.cos(ang), by + dist * np.sin(ang), 0.3), rng.uniform(4.0, 6.0), sz=3.0), k=3.0)
    # goo running down the outer face of the leg
    a_, b_ = np.array(a, float), np.array(b, float)
    for t_end, side in ((rng.uniform(0.35, 0.6), 1), (rng.uniform(0.2, 0.4), -1)):
        off = np.array([s * 4.0, side * 2.5, 0])
        p0 = a_ + off; p1 = a_ + (b_ - a_) * t_end + off * 0.85
        add(*cone_capsule(p0, p1, 1.9, 1.4), k=2.0)
        add(*sphere(p1, 2.2), k=1.2)

# ------------------------------------------------------------------ drips
# front edge of seat: wavy sagging lip + uneven drips (some merged)
for x in np.linspace(-38, 38, 13):
    add(*sphere((x + rng.uniform(-1.5, 1.5), -39.0, SEAT_Z - 8.0 - rng.uniform(0, 2.5)),
                rng.uniform(3.0, 4.6), sz=1.25), k=2.5)
for x, L, r, ln in ((-31, 6, 2.6, 0), (-22, 13, 3.0, -0.6), (-17.5, 7, 2.3, 0), (-6, 24, 3.2, -1.0),
                    (5, 9, 2.7, 0), (13.5, 30, 3.3, -1.2), (18, 11, 2.4, 0), (26, 17, 2.9, -0.6), (34, 7, 2.5, 0)):
    drip((x, -39.5, SEAT_Z - 9.5), L, r, lean=(0, ln))
# seat sides
for s in (-1, 1):
    for y, L, r in ((-20, 11, 2.4), (6, 6, 2.2)):
        drip((s * 46.5, y + s, SEAT_Z - 7.0), L * (1.0 if s > 0 else 0.8), r, lean=(s * 0.6, 0))
# arms – drips on the outer side so they hang free over the plate
for s, items in ((-1, ((20, 106, 11, 2.3), (12, 96, 17, 2.5), (-14, 83, 9, 2.2), (-27, 82.5, 6, 2.0))),
                 (1, ((16, 101, 7, 2.1), (6, 88, 16, 2.6), (-8, 84, 11, 2.3), (-22, 83, 19, 2.6), (-31, 82, 8, 2.1)))):
    for y, z, L, r in items:
        drip((s * 52.0, y, z - 2.5), L, r, lean=(s * 0.8, 0))
# back top rail – drips on the rear face of the backrest
for x, L, r in ((-30, 10, 2.3), (-6, 6, 2.0), (18, 13, 2.4), (34, 18, 2.6)):
    p = back_pt(x, 64.0, off=3.5)
    drip(p, L, r, lean=(0, 1.0))

# ------------------------------------------------------------------ finish
X, Y, Z = np.meshgrid(xs, ys, zs, indexing="ij", sparse=True)
field += (0.22 * np.sin(0.21 * X + 1.3) * np.sin(0.17 * Y + 0.4) * np.sin(0.19 * Z + 0.5)).astype(np.float32)
field = np.maximum(field, -Z.astype(np.float32))      # flat bottom at Z=0 (bed contact)
field[0, :, :] = field[-1, :, :] = FAR                # sealed borders -> watertight
field[:, 0, :] = field[:, -1, :] = FAR
field[:, :, 0] = field[:, :, -1] = FAR

verts, faces, _, _ = marching_cubes(field, level=0.0, spacing=(RES, RES, RES))
verts += np.array([xs[0], ys[0], zs[0]])
print("raw tris:", len(faces))
v2, f2 = fast_simplification.simplify(verts.astype(np.float32), faces.astype(np.int64),
                                      target_reduction=1 - TARGET_FACES / len(faces))
mesh = trimesh.Trimesh(v2, f2, process=True)
mesh.update_faces(mesh.nondegenerate_faces())
mesh.remove_unreferenced_vertices()
trimesh.repair.fix_normals(mesh)
# keep only the main body
parts = mesh.split(only_watertight=False)
mesh = max(parts, key=lambda m: len(m.faces))
mesh.apply_translation([0, 0, -mesh.bounds[0][2]])
print("faces:", len(mesh.faces), "watertight:", mesh.is_watertight, "winding ok:", mesh.is_winding_consistent,
      "volume cm3: %.1f" % (mesh.volume / 1000), "extents:", np.round(mesh.extents, 1), "parts:", len(parts))
mesh.export(os.path.join(OUT, "melting_chair.stl"))
mesh.export(os.path.join(OUT, "melting_chair.3mf"))
